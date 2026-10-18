package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamItems;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Prediction;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The village economy. Every half a day each villager brings the elder his share of one of five resources (wood, metal
 * and tools, stone, money, provisions) and eats some provisions from the store. A share is an average of his work,
 * never counted piece by piece, so it costs the server nothing between times: his trade's base amount (a farmer's grows
 * with the farmland around his job site), times the tier of the tool a player gave him, his level and the components he
 * used up. With the stock the player orders an iron golem from the elder's upgrades menu, or has the village save up for
 * one. Villagers have no children while the village lacks a bed for the child or provisions to feed him. The elder also
 * appoints guards, one for each eight grown villagers: nitwits first, then the jobless, then one of the most numerous
 * trade. Numbers are in {@code data/eam/villagers/economy.json}.
 */
public final class VillageEconomyModule {
	/** The elder's store of resources and the order the village saves up for. */
	public static final AttachmentType<VillageTreasury> TREASURY = AttachmentRegistry.create(Eam.id("village_treasury"),
			builder -> builder.persistent(VillageTreasury.CODEC));
	/** When the villager next brings his share, in game time. */
	public static final AttachmentType<Long> NEXT_SHARE = AttachmentRegistry.create(Eam.id("village_next_share"),
			builder -> builder.persistent(Codec.LONG));
	/** The villager's elder and whether his village has a bed to spare, until when that holds: not saved. */
	private static final AttachmentType<BreedCheck> BREED_CHECK = AttachmentRegistry.create(Eam.id("village_breed_check"));
	private static final int CHECK_TICKS = 100;
	private static final int BREED_CHECK_TICKS = 200;
	private static final byte HAPPY_PARTICLES = 14;

	private static volatile EconomySettings settings = EconomySettings.DEFAULT;
	private static boolean enabled;

	private VillageEconomyModule() {
	}

	/**
	 * @param elderId the elder of his village, or -1 outside any
	 * @param beds    whether the village has a bed for one more
	 */
	private record BreedCheck(long until, int elderId, boolean beds) {
	}

	/** Settings and payloads register whatever the config says, so the data is always valid and client and server match. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(EconomySettingsLoader.ID, new EconomySettingsLoader());
		PayloadTypeRegistry.serverboundPlay().register(VillageEconomyOpenPayload.TYPE, VillageEconomyOpenPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(VillageOrderPayload.TYPE, VillageOrderPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(VillageEconomyPayload.TYPE, VillageEconomyPayload.STREAM_CODEC);
	}

	public static void init() {
		enabled = true;
		ServerPlayNetworking.registerGlobalReceiver(VillageEconomyOpenPayload.TYPE, (payload, context) -> {
			Villager elder = elderFor(context.player(), payload.elderId());
			if (elder == null) return;
			// From the elder's trades: the trade menu closes for the upgrades menu.
			if (context.player().containerMenu != context.player().inventoryMenu) context.player().closeContainer();
			send(context.player(), elder);
		});
		ServerPlayNetworking.registerGlobalReceiver(VillageOrderPayload.TYPE, (payload, context) -> {
			Villager elder = elderFor(context.player(), payload.elderId());
			if (elder == null) return;
			if (payload.order().isEmpty()) {
				elder.setAttached(TREASURY, treasury(elder).withGoal(Optional.empty()));
			} else if (payload.take()) {
				take(elder, context.player(), payload.order());
			} else if (settings.orders().containsKey(payload.order())) {
				order(elder, context.player().level(), payload.order(), context.player());
			}
			send(context.player(), elder);
		});
	}

	static void setSettings(EconomySettings value) {
		settings = value;
	}

	static EconomySettings settings() {
		return settings;
	}

	private static @Nullable Villager elderFor(ServerPlayer player, int elderId) {
		if (!(player.level().getEntity(elderId) instanceof Villager elder) || !VillagerElderModule.isElder(elder)) return null;
		int radius = VillagerElderModule.radius();
		return player.distanceToSqr(elder) <= (double) radius * radius ? elder : null;
	}

	private static VillageTreasury treasury(Villager elder) {
		return elder.getAttachedOrElse(TREASURY, VillageTreasury.EMPTY);
	}

	/** Called every tick a villager thinks on the server: now and then he brings his share, and the elder sees to his guards. */
	public static void tick(Villager villager, ServerLevel level) {
		if (!enabled || !VillagerRepairLogic.checksNow(level.getGameTime(), villager.getId(), CHECK_TICKS)) return;
		EconomySettings current = settings;
		long time = level.getGameTime();
		Long next = villager.getAttached(NEXT_SHARE);
		if (next == null) {
			// Spread first shares over the period, so a village does not bring everything in one tick.
			villager.setAttached(NEXT_SHARE, time + villager.getRandom().nextInt(current.periodTicks()));
		} else if (time >= next) {
			villager.setAttached(NEXT_SHARE, time + current.periodTicks());
			bringShare(villager, level, current);
		}
		if (VillagerElderModule.isElder(villager) && VillagerRepairLogic.checksNow(time, villager.getId(), current.guardCheckTicks())) {
			appointGuard(villager, level, current);
		}
	}

	/** The elder whose village the villager lives in: the nearest within an elder's reach, or himself if he is one. */
	static @Nullable Villager elderOf(Villager villager, ServerLevel level) {
		if (VillagerElderModule.isElder(villager)) return villager;
		int radius = VillagerElderModule.radius();
		return level.getEntitiesOfClass(Villager.class, villager.getBoundingBox().inflate(radius),
						other -> other != villager && VillagerElderModule.isElder(other) && other.distanceToSqr(villager) <= (double) radius * radius)
				.stream().min(Comparator.comparingDouble(other -> other.distanceToSqr(villager))).orElse(null);
	}

	/** The villager eats his upkeep and, grown, brings what his trade gives; the elder then sees whether the goal is met. */
	private static void bringShare(Villager villager, ServerLevel level, EconomySettings current) {
		Villager elder = elderOf(villager, level);
		if (elder == null) return;
		Map<VillageResource, Double> amounts = new EnumMap<>(VillageResource.class);
		amounts.put(VillageResource.PROVISIONS, -current.upkeep());
		Share share = share(villager, level, current, true);
		if (share != null) amounts.merge(share.resource(), share.amount(), Double::sum);
		elder.setAttached(TREASURY, treasury(elder).add(amounts));
		if (share != null && share.amount() > 0) level.broadcastEntityEvent(villager, HAPPY_PARTICLES);
		meetGoal(elder, level, current);
	}

	private record Share(VillageResource resource, double amount) {
	}

	/**
	 * What the villager brings in half a day, or {@code null} if he brings nothing: a child, a guard, or a trade with no
	 * share in the settings.
	 *
	 * @param useUp whether he uses up his components now; else they are only counted, for the menu's forecast
	 */
	private static @Nullable Share share(Villager villager, ServerLevel level, EconomySettings current, boolean useUp) {
		if (villager.isBaby()) return null;
		Identifier profession = villager.getVillagerData().profession().unwrapKey().map(key -> key.identifier()).orElse(null);
		EconomySettings.Yield yield = profession == null ? null : current.professions().get(profession);
		if (yield == null) return null;
		double farmland = yield.perFarmland() > 0 ? fieldShare(villager, level, current) : 0;
		int components = yield.components().map(tag -> components(villager.getInventory(), tag, current.componentsMax(), useUp)).orElse(0);
		double amount = VillageEconomyLogic.yield(yield.base(), yield.perFarmland(), farmland, toolRate(villager, current),
				villager.getVillagerData().level(), current.levelBonus(), components, current.componentBonus());
		return new Share(yield.resource(), amount);
	}

	/**
	 * The multiplier of the tool a player gave the villager for his trade, by its material's tier; his own work item, or
	 * anything that is no tool of his trade, counts as 1.
	 */
	private static double toolRate(Villager villager, EconomySettings current) {
		ItemStack hand = VillagerGearModule.realHand(villager);
		if (hand.isEmpty() || VillagerGearModule.isWorkItem(hand)) return 1.0;
		if (VillagerGearModule.tools(villager).map(tag -> !hand.is(tag)).orElse(true)) return 1.0;
		Tool tool = hand.get(DataComponents.TOOL);
		if (tool != null) {
			for (Tool.Rule rule : tool.rules()) {
				if (rule.correctForDrops().orElse(true)) continue;
				Optional<TagKey<Block>> tier = rule.blocks().unwrapKey();
				if (tier.isPresent() && current.toolTiers().containsKey(tier.get())) return current.toolTiers().get(tier.get());
			}
		}
		return current.untieredTool();
	}

	/** Components of {@code tag} in the inventory, at most {@code max}, taken out if {@code useUp}. */
	private static int components(SimpleContainer inventory, TagKey<net.minecraft.world.item.Item> tag, int max, boolean useUp) {
		int found = 0;
		for (int i = 0; i < inventory.getContainerSize() && found < max; i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.isEmpty() || !stack.is(tag) || VillagerGearModule.isWorkItem(stack)) continue;
			int taken = Math.min(stack.getCount(), max - found);
			found += taken;
			if (useUp) stack.shrink(taken);
		}
		if (useUp && found > 0) inventory.setChanged();
		return found;
	}

	/**
	 * The farmland a farmer tends: the farmland within {@code farm_radius} of his job site, split evenly among the
	 * farmers whose job sites are as near.
	 */
	private static double fieldShare(Villager villager, ServerLevel level, EconomySettings current) {
		BlockPos site = jobSite(villager, level);
		if (site == null) return 0;
		int radius = current.farmRadius();
		int farmland = 0;
		for (BlockPos pos : BlockPos.betweenClosed(site.offset(-radius, -3, -radius), site.offset(radius, 3, radius))) {
			// Never load a chunk for this.
			if (level.hasChunkAt(pos) && level.getBlockState(pos).is(Blocks.FARMLAND)) farmland++;
		}
		Identifier profession = villager.getVillagerData().profession().unwrapKey().map(key -> key.identifier()).orElse(null);
		int farmers = 1;
		for (Villager other : level.getEntitiesOfClass(Villager.class, villager.getBoundingBox().inflate(radius * 3), other -> other != villager)) {
			if (!other.getVillagerData().profession().unwrapKey().map(key -> key.identifier()).equals(Optional.ofNullable(profession))) continue;
			BlockPos otherSite = jobSite(other, level);
			if (otherSite != null && otherSite.distManhattan(site) <= radius * 2) farmers++;
		}
		return (double) farmland / farmers;
	}

	private static @Nullable BlockPos jobSite(Villager villager, ServerLevel level) {
		return villager.getBrain().getMemory(MemoryModuleType.JOB_SITE)
				.filter(site -> site.dimension() == level.dimension())
				.map(GlobalPos::pos).orElse(null);
	}

	/** Carries the village's goal out once the stock covers it. */
	private static void meetGoal(Villager elder, ServerLevel level, EconomySettings current) {
		treasury(elder).goal().ifPresent(goal -> {
			EconomySettings.Order order = current.orders().get(goal);
			if (order == null) {
				elder.setAttached(TREASURY, treasury(elder).withGoal(Optional.empty()));
			} else if (VillageEconomyLogic.affords(treasury(elder).stock(), order.cost())) {
				order(elder, level, goal, null);
			}
		});
	}

	/**
	 * Carries the order out if the stock covers it and it is available; else the village saves up for it. A blueprint
	 * goes to {@code player}, or with none (the village saved up for it) waits for a player to take it.
	 */
	static void order(Villager elder, ServerLevel level, String id, @Nullable ServerPlayer player) {
		EconomySettings current = settings;
		EconomySettings.Order order = current.orders().get(id);
		if (order == null || !available(elder, level, id, current)) return;
		VillageTreasury treasury = treasury(elder);
		if (!VillageEconomyLogic.affords(treasury.stock(), order.cost())) {
			elder.setAttached(TREASURY, treasury.withGoal(Optional.of(id)));
			return;
		}
		if (order.blueprint()) {
			if (player == null) {
				treasury = treasury.withReady(id, true);
			} else if (!give(elder, player, id, order)) {
				return;
			}
		} else if (!carryOut(elder, level, id)) {
			return;
		}
		VillageTreasury paid = treasury.pay(order.cost());
		if (paid.goal().filter(id::equals).isPresent()) paid = paid.withGoal(Optional.empty());
		elder.setAttached(TREASURY, paid);
		level.broadcastEntityEvent(elder, HAPPY_PARTICLES);
		elder.makeSound(SoundEvents.VILLAGER_CELEBRATE);
	}

	/**
	 * Whether the order can be had now, the stock aside: a golem only while the village may have one more, a blueprint
	 * only while buildings are on.
	 */
	private static boolean available(Villager elder, ServerLevel level, String id, EconomySettings current) {
		if (EconomySettings.IRON_GOLEM.equals(id)) return golems(elder, level) < golemsAllowed(elder, level, current);
		EconomySettings.Order order = current.orders().get(id);
		return order == null || !order.blueprint()
				|| VillageBuildModule.enabled() && VillageBuildModule.template(order, style(elder), elder.getRandom()).isPresent();
	}

	/** The village's building style: the elder's villager type. */
	private static String style(Villager elder) {
		return elder.getVillagerData().type().unwrapKey().map(key -> key.identifier().getPath()).orElse("plains");
	}

	/** Gives the player a blueprint of the order in the village's style; whether one could be made. */
	private static boolean give(Villager elder, ServerPlayer player, String id, EconomySettings.Order order) {
		Optional<Identifier> template = VillageBuildModule.template(order, style(elder), elder.getRandom());
		if (template.isEmpty()) return false;
		ItemStack blueprint = new ItemStack(EamItems.BLUEPRINT);
		blueprint.set(VillageBuildModule.BLUEPRINT, new BlueprintData(template.get(), id, 0));
		if (!player.getInventory().add(blueprint)) player.drop(blueprint, false, Prediction.SERVER_ONLY);
		return true;
	}

	/** The player takes a blueprint the village saved up for. */
	private static void take(Villager elder, ServerPlayer player, String id) {
		EconomySettings.Order order = settings.orders().get(id);
		VillageTreasury treasury = treasury(elder);
		if (order == null || !treasury.ready().contains(id) || !give(elder, player, id, order)) return;
		elder.setAttached(TREASURY, treasury.withReady(id, false));
	}

	private static boolean carryOut(Villager elder, ServerLevel level, String id) {
		if (EconomySettings.IRON_GOLEM.equals(id)) {
			return SpawnUtil.trySpawnMob(EntityTypes.IRON_GOLEM, EntitySpawnReason.MOB_SUMMONED, level, elder.blockPosition(), 10, 8, 6,
					SpawnUtil.Strategy.LEGACY_IRON_GOLEM, false).isPresent();
		}
		return false;
	}

	private static List<Villager> villagers(Villager elder, ServerLevel level) {
		int radius = VillagerElderModule.radius();
		return level.getEntitiesOfClass(Villager.class, elder.getBoundingBox().inflate(radius),
				villager -> villager.isAlive() && villager.distanceToSqr(elder) <= (double) radius * radius);
	}

	private static int golems(Villager elder, ServerLevel level) {
		int radius = VillagerElderModule.radius();
		return level.getEntitiesOfClass(IronGolem.class, elder.getBoundingBox().inflate(radius),
				golem -> golem.isAlive() && golem.distanceToSqr(elder) <= (double) radius * radius).size();
	}

	private static int golemsAllowed(Villager elder, ServerLevel level, EconomySettings current) {
		return VillageEconomyLogic.golemsAllowed(villagers(elder, level).size(), current.villagersPerGolem());
	}

	private static int beds(Villager elder, ServerLevel level) {
		return (int) level.getPoiManager().getCountInRange(type -> type.is(PoiTypes.HOME), elder.blockPosition(), VillagerElderModule.radius(),
				PoiManager.Occupancy.ANY);
	}

	/**
	 * Makes one more guard if the village has fewer than one for each {@code villagers_per_guard} grown villagers:
	 * see {@link VillageEconomyLogic#guardRank} for who.
	 */
	private static void appointGuard(Villager elder, ServerLevel level, EconomySettings current) {
		if (!VillagerGuardModule.enabled()) return;
		List<Villager> adults = villagers(elder, level).stream().filter(villager -> !villager.isBaby()).toList();
		int guards = (int) adults.stream().filter(VillagerGuardModule::isGuardProfession).count();
		if (guards >= VillageEconomyLogic.guardsNeeded(adults.size(), current.villagersPerGuard())) return;
		Map<Object, Integer> trades = new HashMap<>();
		for (Villager villager : adults) trades.merge(villager.getVillagerData().profession(), 1, Integer::sum);
		Villager best = null;
		long bestRank = Long.MAX_VALUE;
		for (Villager villager : adults) {
			var profession = villager.getVillagerData().profession();
			boolean excluded = VillagerElderModule.isElder(villager) || VillagerGuardModule.isGuardProfession(villager)
					|| villager.hasAttached(VillagerGuardModule.DISMISSED);
			long rank = VillageEconomyLogic.guardRank(profession.is(VillagerProfession.NITWIT), profession.is(VillagerProfession.NONE), excluded,
					trades.get(profession), villager.getVillagerXp());
			if (rank < bestRank) {
				best = villager;
				bestRank = rank;
			}
		}
		if (best != null) VillagerGuardModule.makeGuard(best, level);
	}

	/**
	 * Whether a villager may have a child: in an elder's village only with a bed free for the child and provisions in the
	 * store for him. Outside any elder's village vanilla decides alone. The village and its beds are looked up every few
	 * seconds at most; the provisions every time, so one store never pays for two children.
	 */
	public static boolean canBreed(Villager villager, boolean vanilla) {
		if (!enabled || !vanilla || !(villager.level() instanceof ServerLevel level)) return vanilla;
		long time = level.getGameTime();
		BreedCheck check = villager.getAttached(BREED_CHECK);
		if (check == null || time >= check.until()) {
			Villager elder = elderOf(villager, level);
			check = elder == null ? new BreedCheck(time + BREED_CHECK_TICKS, -1, true)
					: new BreedCheck(time + BREED_CHECK_TICKS, elder.getId(), villagers(elder, level).size() < beds(elder, level));
			villager.setAttached(BREED_CHECK, check);
		}
		if (check.elderId() < 0) return true;
		if (!(level.getEntity(check.elderId()) instanceof Villager elder) || !VillagerElderModule.isElder(elder)) return true;
		return VillageEconomyLogic.canBreed(0, check.beds() ? 1 : 0, treasury(elder).get(VillageResource.PROVISIONS), settings.breedCost());
	}

	/** Two villagers had {@code child}: the village pays for him from its provisions, and the beds are counted again. */
	public static @Nullable Villager born(Villager parent, @Nullable Villager child) {
		if (!enabled || child == null || !(parent.level() instanceof ServerLevel level)) return child;
		Villager elder = elderOf(parent, level);
		if (elder == null) return child;
		elder.setAttached(TREASURY, treasury(elder).add(Map.of(VillageResource.PROVISIONS, -settings.breedCost())));
		for (Villager villager : villagers(elder, level)) villager.removeAttached(BREED_CHECK);
		return child;
	}

	/** Sends the player the elder's economy for the upgrades menu. */
	private static void send(ServerPlayer player, Villager elder) {
		ServerLevel level = player.level();
		EconomySettings current = settings;
		VillageTreasury treasury = treasury(elder);
		// Only those who bring their shares to this elder, where villages overlap.
		List<Villager> villagers = villagers(elder, level).stream().filter(villager -> elderOf(villager, level) == elder).toList();
		Map<VillageResource, Double> income = new EnumMap<>(VillageResource.class);
		for (Villager villager : villagers) {
			income.merge(VillageResource.PROVISIONS, -current.upkeep(), Double::sum);
			Share share = share(villager, level, current, false);
			if (share != null) income.merge(share.resource(), share.amount(), Double::sum);
		}
		List<Double> stock = new ArrayList<>();
		List<Double> incomes = new ArrayList<>();
		for (VillageResource resource : VillageResource.values()) {
			stock.add(treasury.get(resource));
			incomes.add(income.getOrDefault(resource, 0.0));
		}
		int adults = (int) villagers.stream().filter(villager -> !villager.isBaby()).count();
		int guards = (int) villagers.stream().filter(VillagerGuardModule::isGuardProfession).count();
		VillageEconomyPayload.Status status = new VillageEconomyPayload.Status(villagers.size(), beds(elder, level), current.breedCost(), guards,
				VillageEconomyLogic.guardsNeeded(adults, current.villagersPerGuard()), golems(elder, level),
				VillageEconomyLogic.golemsAllowed(villagers.size(), current.villagersPerGolem()));
		List<VillageEconomyPayload.OrderView> orders = new ArrayList<>();
		current.orders().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
			List<Double> cost = new ArrayList<>();
			for (VillageResource resource : VillageResource.values()) cost.add(entry.getValue().cost().getOrDefault(resource, 0.0));
			int ready = (int) treasury.ready().stream().filter(entry.getKey()::equals).count();
			orders.add(new VillageEconomyPayload.OrderView(entry.getKey(), cost, available(elder, level, entry.getKey(), current), ready));
		});
		ServerPlayNetworking.send(player, new VillageEconomyPayload(elder.getId(), stock, incomes, status, treasury.goal(), orders));
	}
}
