package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.content.lockedchest.LockedChestBlockEntity;
import io.github.hellenter.eam.content.lockedchest.LockedChestModule;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The cartographer is the village elder: what a player gives him through his inventory he hands out to the villagers
 * near him, one thing at a time. Armor, weapons and shields go to one villager until he has all he can take, then to
 * the next, so the village gets guards one by one. Tools go to a villager of the trade that uses them, a cleric's
 * ingredients to a cleric, food to the hungriest villager, potions to the guards and then to the others, arrows to the
 * guards with bows. What a villager near him does not need (potions the cleric brewed, arrows without a bow, gear and
 * tools a player gave him) he takes, to hand it on. What nobody can take goes to the village store, the locked chests
 * of the village near him, and he takes it out again when someone needs it. Numbers are in
 * {@code data/eam/villagers/elder.json}.
 * <p>
 * The village button of his inventory and of his trade panel opens the village menu: every villager within his reach
 * with his profession, level, health and gear, and a button to make each grown villager a guard or an ordinary villager
 * again; a click on a villager opens his page. Every villager has a first name and a surname, {@link VillagerNames}. A locked chest placed in his reach lists his villagers as owners.
 */
public final class VillagerElderModule {
	private static final byte HAPPY_PARTICLES = 14;

	private static volatile ElderSettings settings = ElderSettings.DEFAULT;
	private static boolean enabled;

	private VillagerElderModule() {
	}

	/** Settings and payloads register whatever the config says, so the data is always valid and client and server match. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(ElderSettingsLoader.ID, new ElderSettingsLoader());
		VillagerNames.register();
		PayloadTypeRegistry.serverboundPlay().register(VillageOpenPayload.TYPE, VillageOpenPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(VillageGuardPayload.TYPE, VillageGuardPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(VillageListPayload.TYPE, VillageListPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(VillageResidentRequestPayload.TYPE, VillageResidentRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(VillageResidentPayload.TYPE, VillageResidentPayload.STREAM_CODEC);
	}

	public static void init() {
		enabled = true;
		ServerPlayNetworking.registerGlobalReceiver(VillageOpenPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			Villager elder = player.containerMenu instanceof VillagerGearMenu menu ? menu.villager() : VillagerTradePanel.trading(player);
			if (elder == null || !isElder(elder)) return;
			player.closeContainer();
			sendVillage(player, elder);
		});
		ServerPlayNetworking.registerGlobalReceiver(VillageResidentRequestPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			ServerLevel level = player.level();
			if (level.getEntity(payload.elderId()) instanceof Villager elder && isElder(elder) && inReach(elder, player)
					&& level.getEntity(payload.villagerId()) instanceof Villager villager && villager.isAlive() && inReach(elder, villager)) {
				ServerPlayNetworking.send(player, details(villager, elder, level));
			}
		});
		LockedChestModule.setVillage(VillagerElderModule::villageAround);
		ServerPlayNetworking.registerGlobalReceiver(VillageGuardPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			ServerLevel level = player.level();
			if (!VillagerGuardModule.enabled() || !(level.getEntity(payload.elderId()) instanceof Villager elder) || !isElder(elder)
					|| !inReach(elder, player)) {
				return;
			}
			if (level.getEntity(payload.villagerId()) instanceof Villager villager && villager != elder && villager.isAlive()
					&& !villager.isBaby() && inReach(elder, villager)) {
				VillagerGuardModule.setGuard(villager, level, payload.guard());
			}
			sendVillage(player, elder);
		});
	}

	/** A grown, living cartographer: the elder of the villagers around him. */
	public static boolean isElder(Villager villager) {
		return villager.isAlive() && !villager.isBaby() && villager.getVillagerData().profession().is(VillagerProfession.CARTOGRAPHER);
	}

	/** Whether the villager's panels get the village button: he is the elder and the elder's module is on. */
	public static boolean opensVillage(Villager villager) {
		return enabled && isElder(villager);
	}

	private static boolean inReach(Villager elder, Entity entity) {
		double radius = settings.radius();
		return entity.distanceToSqr(elder) <= radius * radius;
	}

	/** Sends the player the villagers of the elder's village: the elder first, then the guards, the others and the children. */
	private static void sendVillage(ServerPlayer player, Villager elder) {
		List<VillageListPayload.Resident> residents = new ArrayList<>();
		for (Villager villager : village(player.level(), elder)) residents.add(resident(villager, elder));
		ServerPlayNetworking.send(player, new VillageListPayload(elder.getId(), VillagerGuardModule.enabled(), residents));
	}

	/** Everyone living within the elder's reach, him included: the elder first, then the guards, the others and the children. */
	private static List<Villager> village(ServerLevel level, Villager elder) {
		List<Villager> villagers = new ArrayList<>(level.getEntitiesOfClass(Villager.class, elder.getBoundingBox().inflate(settings.radius()),
				villager -> villager.isAlive() && inReach(elder, villager)));
		villagers.sort(Comparator.comparing((Villager villager) -> role(villager, elder))
				.thenComparing(villager -> villager.getVillagerData().profession().value().name().getString())
				.thenComparingInt(Villager::getId));
		return villagers;
	}

	/**
	 * The grown villagers of the village a locked chest at {@code pos} stands in, for its owner list: those of the
	 * nearest elder whose reach takes in the chest, the elder left out (the village entry is his). {@code null} when no
	 * elder's reach does.
	 */
	private static @Nullable List<Villager> villageAround(ServerLevel level, BlockPos pos) {
		if (!enabled) return null;
		double radius = settings.radius();
		Vec3 center = Vec3.atCenterOf(pos);
		List<Villager> elders = level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(radius),
				villager -> isElder(villager) && villager.distanceToSqr(center) <= radius * radius);
		if (elders.isEmpty()) return null;
		Villager elder = elders.stream().min(Comparator.comparingDouble(villager -> villager.distanceToSqr(center))).orElseThrow();
		List<Villager> villagers = new ArrayList<>(village(level, elder));
		villagers.removeIf(villager -> villager == elder || villager.isBaby());
		return villagers;
	}

	private static VillageListPayload.Role role(Villager villager, Villager elder) {
		return villager == elder ? VillageListPayload.Role.ELDER
				: villager.isBaby() ? VillageListPayload.Role.CHILD
				: VillagerGuardModule.isGuardProfession(villager) ? VillageListPayload.Role.GUARD
				: VillageListPayload.Role.VILLAGER;
	}

	/** One villager in detail: his row, experience, work item, inventory, job site, bed and chest. */
	private static VillageResidentPayload details(Villager villager, Villager elder, ServerLevel level) {
		List<ItemStack> inventory = new ArrayList<>();
		for (ItemStack stack : villager.getInventory().getItems()) inventory.add(stack.copy());
		List<LockedChestBlockEntity> chests = villager == elder
				? VillagerChestModule.store(level, elder, settings.radius())
				: LockedChestModule.ownedBy(level, villager.blockPosition(), settings.radius(), villager.getUUID());
		LockedChestBlockEntity chest = chests.isEmpty() ? null : chests.getFirst();
		List<ItemStack> chestItems = new ArrayList<>();
		if (chest != null) {
			for (int i = 0; i < chest.getContainerSize(); i++) chestItems.add(chest.getItem(i).copy());
		}
		return new VillageResidentPayload(elder.getId(), resident(villager, elder), villager.getVillagerXp(),
				VillagerGearModule.workItem(villager).copy(), inventory, memory(villager, level, MemoryModuleType.JOB_SITE),
				memory(villager, level, MemoryModuleType.HOME), Optional.ofNullable(chest).map(LockedChestBlockEntity::getBlockPos),
				chestItems);
	}

	private static Optional<BlockPos> memory(Villager villager, ServerLevel level, MemoryModuleType<GlobalPos> type) {
		return villager.getBrain().getMemory(type).filter(pos -> pos.dimension() == level.dimension()).map(GlobalPos::pos);
	}

	private static VillageListPayload.Resident resident(Villager villager, Villager elder) {
		List<ItemStack> gear = new ArrayList<>();
		for (EquipmentSlot slot : VillagerGearContainer.ARMOR) gear.add(VillagerMilitiaModule.gear(villager, slot).copy());
		gear.add(VillagerMilitiaModule.gear(villager, EquipmentSlot.MAINHAND).copy());
		gear.add(VillagerMilitiaModule.gear(villager, EquipmentSlot.OFFHAND).copy());
		VillageListPayload.Role role = role(villager, elder);
		return new VillageListPayload.Resident(villager.getId(), VillagerNames.name(villager),
				villager.getVillagerData().profession().value().name(), villager.getVillagerData().level(), villager.getHealth(),
				villager.getMaxHealth(), gear, role);
	}

	/** How far, in blocks, the elder's village reaches. */
	static int radius() {
		return settings.radius();
	}

	static void setSettings(ElderSettings value) {
		settings = value;
	}

	/**
	 * Called every tick a villager thinks on the server: now and then the elder takes from the villagers near him what
	 * they do not need, and hands one thing out. What nobody takes goes to the village store; with nothing to hand out,
	 * he takes from the store one thing a villager needs now.
	 */
	public static void tick(Villager villager, ServerLevel level) {
		if (!enabled) return;
		ElderSettings current = settings;
		if (!VillagerRepairLogic.checksNow(level.getGameTime(), villager.getId(), current.intervalTicks())) return;
		if (villager.isSleeping() || !isElder(villager)) return;
		List<Villager> near = level.getEntitiesOfClass(Villager.class, villager.getBoundingBox().inflate(current.radius()),
				other -> other != villager && other.isAlive() && !other.isBaby());
		near.sort(Comparator.comparing(Villager::getUUID));
		SimpleContainer inventory = villager.getInventory();
		for (Villager other : near) collect(other, inventory, current);
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.isEmpty()) continue;
			Gift gift = gift(stack, near, current);
			if (gift == null) continue;
			hand(stack, gift);
			inventory.setChanged();
			villager.getLookControl().setLookAt(gift.taker());
			level.broadcastEntityEvent(gift.taker(), HAPPY_PARTICLES);
			return;
		}
		List<LockedChestBlockEntity> store = VillagerChestModule.store(level, villager, current.radius());
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.isEmpty()) continue;
			for (LockedChestBlockEntity chest : store) {
				if (chest.insert(stack)) {
					inventory.setChanged();
					return;
				}
			}
		}
		if (inventory.getItems().stream().noneMatch(ItemStack::isEmpty)) return;
		for (LockedChestBlockEntity chest : store) {
			for (int i = 0; i < chest.getContainerSize(); i++) {
				ItemStack stack = chest.getItem(i);
				if (stack.isEmpty()) continue;
				Gift gift = gift(stack, near, current);
				if (gift == null) continue;
				inventory.addItem(stack.split(gift.slot() != null ? 1 : gift.count()));
				chest.setChanged();
				return;
			}
		}
	}

	/**
	 * Takes from {@code villager} what he does not need into the elder's {@code inventory} while it has room, and puts a
	 * tool of his trade in his hand if it is free. See {@link VillagerElderLogic#keeps} for what he keeps.
	 */
	private static void collect(Villager villager, SimpleContainer into, ElderSettings current) {
		SimpleContainer inventory = villager.getInventory();
		boolean guard = VillagerGuardModule.isGuard(villager);
		boolean cleric = villager.getVillagerData().profession().is(VillagerProfession.CLERIC);
		boolean bow = VillagerGuardModule.hasBow(villager);
		int potionMax = VillagerClericModule.settings().potionMax();
		Map<VillagerElderLogic.Kind, Integer> counted = new EnumMap<>(VillagerElderLogic.Kind.class);
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.isEmpty()) continue;
			if (handFree(villager) && matches(stack, VillagerGearModule.tools(villager))) {
				VillagerGearModule.give(villager, EquipmentSlot.MAINHAND, stack.split(1));
				inventory.setChanged();
				continue;
			}
			VillagerElderLogic.Kind kind = kind(villager, stack);
			int keep = VillagerElderLogic.keeps(kind, guard, cleric, bow, potionMax, current.arrowMax());
			int had = counted.getOrDefault(kind, 0);
			int kept = Math.min(stack.getCount(), Math.max(0, keep - had));
			counted.put(kind, had + kept);
			int surplus = stack.getCount() - kept;
			if (surplus <= 0) continue;
			ItemStack left = into.addItem(stack.split(surplus));
			stack.grow(left.getCount());
			inventory.setChanged();
		}
	}

	private static VillagerElderLogic.Kind kind(Villager villager, ItemStack stack) {
		if (stack.has(DataComponents.VILLAGER_FOOD) || stack.is(ItemTags.VILLAGER_PICKS_UP)) return VillagerElderLogic.Kind.FOOD;
		if (matches(stack, VillagerGearModule.goods(villager))) return VillagerElderLogic.Kind.GOODS;
		if (isPotion(stack)) return VillagerElderLogic.Kind.POTION;
		if (stack.is(ItemTags.ARROWS)) return VillagerElderLogic.Kind.ARROW;
		return VillagerElderLogic.Kind.OTHER;
	}

	/** Whether the potion heals: the only kind a villager who is no guard drinks. */
	private static boolean heals(ItemStack stack) {
		PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
		if (contents == null) return false;
		for (MobEffectInstance effect : contents.getAllEffects()) {
			if (effect.is(MobEffects.INSTANT_HEALTH) || effect.is(MobEffects.REGENERATION)) return true;
		}
		return false;
	}

	private static boolean isPotion(ItemStack stack) {
		return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
	}

	/**
	 * Who gets {@code stack}, and how: one piece in an equipment {@code slot}, or {@code count} of it in his inventory.
	 *
	 * @param slot the slot a piece of gear goes in, or {@code null} for the inventory
	 */
	private record Gift(Villager taker, @Nullable EquipmentSlot slot, int count) {
	}

	/**
	 * Who should get some of {@code stack} now, if anyone. Armor, weapons and shields go to the best-armed villager who
	 * can take them, tools to a villager of their trade with a free hand, goods to a villager of their trade, food to
	 * the hungriest. Potions go to the guard with the fewest short of his share, then healing ones to a villager with none;
	 * arrows to the guard with a bow and the fewest. Nothing is moved yet.
	 */
	private static @Nullable Gift gift(ItemStack stack, List<Villager> near, ElderSettings current) {
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		if (equippable != null && List.of(VillagerGearContainer.ARMOR).contains(equippable.slot())) {
			return arm(near, equippable.slot(), villager -> VillagerMilitiaModule.gear(villager, equippable.slot()).isEmpty());
		}
		if (stack.is(VillagerGearModule.WEAPONS)) {
			return arm(near, EquipmentSlot.MAINHAND, villager -> handFree(villager) && VillagerMilitiaModule.stowed(villager, EquipmentSlot.MAINHAND).isEmpty());
		}
		if (stack.is(VillagerGearModule.OFF_HAND)) {
			return arm(near, EquipmentSlot.OFFHAND, villager -> VillagerMilitiaModule.gear(villager, EquipmentSlot.OFFHAND).isEmpty());
		}
		for (Villager villager : near) {
			if (handFree(villager) && matches(stack, VillagerGearModule.tools(villager))) return new Gift(villager, EquipmentSlot.MAINHAND, 1);
		}
		for (Villager villager : near) {
			if (matches(stack, VillagerGearModule.goods(villager)) && villager.getInventory().canAddItem(stack)) {
				return new Gift(villager, null, stack.getCount());
			}
		}
		if (stack.has(DataComponents.VILLAGER_FOOD)) {
			return near.stream().filter(villager -> villager.wantsMoreFood() && villager.getInventory().canAddItem(stack))
					.min(Comparator.comparingInt(VillagerFoodModule::points))
					.map(villager -> new Gift(villager, null, current.foodPerGift())).orElse(null);
		}
		if (isPotion(stack)) {
			int potionMax = VillagerClericModule.settings().potionMax();
			Gift guard = near.stream()
					.filter(villager -> VillagerGuardModule.isGuard(villager) && potions(villager) < potionMax && villager.getInventory().canAddItem(stack))
					.min(Comparator.comparingInt(VillagerElderModule::potions))
					.map(villager -> new Gift(villager, null, 1)).orElse(null);
			if (guard != null || !heals(stack)) return guard;
			return near.stream()
					.filter(villager -> !VillagerGuardModule.isGuard(villager) && !villager.getVillagerData().profession().is(VillagerProfession.CLERIC)
							&& potions(villager) == 0 && villager.getInventory().canAddItem(stack))
					.findFirst().map(villager -> new Gift(villager, null, 1)).orElse(null);
		}
		if (stack.is(ItemTags.ARROWS)) {
			return near.stream()
					.filter(villager -> VillagerGuardModule.isGuard(villager) && VillagerGuardModule.hasBow(villager)
							&& arrows(villager) < current.arrowMax() && villager.getInventory().canAddItem(stack))
					.min(Comparator.comparingInt(VillagerElderModule::arrows))
					.map(villager -> new Gift(villager, null, Math.min(current.arrowsPerGift(), current.arrowMax() - arrows(villager))))
					.orElse(null);
		}
		return null;
	}

	/** Gives {@code gift}'s share of {@code stack}; what does not fit in the taker's inventory stays in it. */
	private static void hand(ItemStack stack, Gift gift) {
		if (gift.slot() != null) {
			VillagerGearModule.give(gift.taker(), gift.slot(), stack.split(1));
			return;
		}
		ItemStack left = gift.taker().getInventory().addItem(stack.split(Math.min(stack.getCount(), gift.count())));
		stack.grow(left.getCount());
	}

	/** One piece in {@code slot} of the best-armed villager who can take it. */
	private static @Nullable Gift arm(List<Villager> near, EquipmentSlot slot, Predicate<Villager> canTake) {
		List<Villager> takers = near.stream().filter(canTake).toList();
		int best = VillagerElderLogic.armsFirst(takers.stream().mapToInt(VillagerElderModule::gear).toArray());
		return best < 0 ? null : new Gift(takers.get(best), slot, 1);
	}

	private static int potions(Villager villager) {
		int count = 0;
		for (ItemStack stack : villager.getInventory().getItems()) {
			if (isPotion(stack)) count += stack.getCount();
		}
		return count;
	}

	private static int arrows(Villager villager) {
		int count = 0;
		for (ItemStack stack : villager.getInventory().getItems()) {
			if (stack.is(ItemTags.ARROWS)) count += stack.getCount();
		}
		return count;
	}

	/** Pieces of gear a player gave the villager: armor, a shield and what he holds instead of his work item. */
	private static int gear(Villager villager) {
		int gear = 0;
		for (EquipmentSlot slot : VillagerGearContainer.ARMOR) {
			if (!VillagerMilitiaModule.gear(villager, slot).isEmpty()) gear++;
		}
		if (!VillagerMilitiaModule.gear(villager, EquipmentSlot.OFFHAND).isEmpty()) gear++;
		if (!VillagerMilitiaModule.gear(villager, EquipmentSlot.MAINHAND).isEmpty()) gear++;
		return gear;
	}

	/** Whether the villager's main hand holds nothing a player gave him, only his work item or nothing. */
	private static boolean handFree(Villager villager) {
		ItemStack hand = VillagerGearModule.realHand(villager);
		return hand.isEmpty() || VillagerGearModule.isWorkItem(hand);
	}

	private static boolean matches(ItemStack stack, Optional<TagKey<Item>> tag) {
		return tag.isPresent() && stack.is(tag.get());
	}
}
