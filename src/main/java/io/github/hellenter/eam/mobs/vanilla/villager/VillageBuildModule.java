package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Buildings from blueprints. A player holding a blueprint sees a ghost of the building where he looks; sneak and use
 * turns it, use sets it down where nothing is in the way, in an elder's village. Then villagers of that village come by
 * day and build it block by block, bottom up, a few at a time: each walks up to the edge of the building and places its
 * blocks from there, reach or no reach. In creative mode a blueprint builds at once, anywhere. While placing blocks, a villager's brain waits; on his way it runs, so he opens
 * doors and flees as ever. What players placed in the way after the site was set down is left alone.
 */
public final class VillageBuildModule {
	/** A building set down and not yet finished. */
	public record Site(UUID id, Identifier template, BlockPos origin, int turns, int next) {
		public static final Codec<Site> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				UUIDUtil.CODEC.fieldOf("id").forGetter(Site::id),
				Identifier.CODEC.fieldOf("template").forGetter(Site::template),
				BlockPos.CODEC.fieldOf("origin").forGetter(Site::origin),
				Codec.INT.fieldOf("turns").forGetter(Site::turns),
				Codec.INT.fieldOf("next").forGetter(Site::next)
		).apply(instance, Site::new));

		Site advanced(int to) {
			return new Site(id, template, origin, turns, to);
		}
	}

	/** What a blueprint is of. */
	public static final DataComponentType<BlueprintData> BLUEPRINT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Eam.id("blueprint"),
			DataComponentType.<BlueprintData>builder().persistent(BlueprintData.CODEC).networkSynchronized(BlueprintData.STREAM_CODEC).build());

	/** The buildings under way in a level. */
	public static final AttachmentType<List<Site>> SITES = AttachmentRegistry.create(Eam.id("village_construction_sites"),
			builder -> builder.persistent(Site.CODEC.listOf()));

	/**
	 * The site a villager builds, when he took it up and last placed a block, and whether he stands at it: not saved, he
	 * takes it up again.
	 */
	private record Builder(UUID site, long since, long placed, boolean working) {
	}

	private static final AttachmentType<Builder> BUILDER = AttachmentRegistry.create(Eam.id("village_builder"));
	private static final int LOOK_TICKS = 100;
	private static final int PATH_TICKS = 20;
	/** After this long on his way, a builder builds from where he is. */
	private static final int STUCK_TICKS = 600;
	/** A builder this far from his site when he gets stuck gives it up. */
	private static final double STUCK_DISTANCE = 16.0;
	private static final double REACH = 2.5;
	private static final byte HAPPY_PARTICLES = 14;
	private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

	/** The blocks of each site in building order, worked out once. */
	private static final Map<UUID, List<BlueprintTemplate.Placed>> ORDERS = new ConcurrentHashMap<>();
	/** The templates each player was sent, so a client can't have one sent over and over. */
	private static final Map<UUID, Set<Identifier>> SENT = new ConcurrentHashMap<>();
	private static boolean enabled;

	private VillageBuildModule() {
	}

	/** The component and payloads register whatever the config says, so client and server always match. */
	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(BlueprintTemplatePayloads.Request.TYPE, BlueprintTemplatePayloads.Request.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(BlueprintTemplatePayloads.Reply.TYPE, BlueprintTemplatePayloads.Reply.STREAM_CODEC);
	}

	public static void init() {
		enabled = true;
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> clearCaches());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> clearCaches());
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SENT.remove(handler.player.getUUID()));
		ServerPlayNetworking.registerGlobalReceiver(BlueprintTemplatePayloads.Request.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (!holds(player, payload.template())) return;
			if (!SENT.computeIfAbsent(player.getUUID(), id -> ConcurrentHashMap.newKeySet()).add(payload.template())) return;
			BlueprintTemplate template = BlueprintTemplates.get(player.level(), payload.template());
			if (template != null) ServerPlayNetworking.send(player, new BlueprintTemplatePayloads.Reply(template));
		});
	}

	private static void clearCaches() {
		BlueprintTemplates.clear();
		ORDERS.clear();
		SENT.clear();
	}

	/** Whether the player holds a blueprint of that template: templates are sent only for those. */
	private static boolean holds(ServerPlayer player, Identifier template) {
		for (InteractionHand hand : InteractionHand.values()) {
			BlueprintData data = player.getItemInHand(hand).get(BLUEPRINT);
			if (data != null && data.template().equals(template)) return true;
		}
		return false;
	}

	public static boolean enabled() {
		return enabled;
	}

	/**
	 * Sets the blueprint down where the player looks, if nothing is in the way and an elder's village is there; the
	 * blueprint is used up. Else the player is told why not.
	 */
	public static void place(ServerPlayer player, ItemStack stack, BlueprintData data) {
		if (!enabled) return;
		ServerLevel level = player.level();
		BlueprintTemplate template = BlueprintTemplates.get(level, data.template());
		BlockPos look = BlueprintTemplate.target(player, 1.0F);
		if (template == null || look == null) return;
		BlockPos origin = template.origin(look, data.turns());
		AABB box = template.box(origin, data.turns());
		if (!player.mayBuild() || !level.mayInteract(player, origin)
				|| !level.mayInteract(player, BlockPos.containing(box.maxX - 1, box.maxY - 1, box.maxZ - 1))) {
			return;
		}
		long blocked = template.placed(origin, data.turns()).stream().filter(block -> BlueprintTemplate.blocked(level, block)).count();
		if (blocked > 0) {
			player.sendOverlayMessage(Component.translatable("item.eam.blueprint.blocked", blocked));
			return;
		}
		if (player.getAbilities().instabuild) {
			buildAtOnce(level, template, origin, data.turns());
			player.sendOverlayMessage(Component.translatable("item.eam.blueprint.built"));
			level.playSound(null, look, SoundEvents.BOOK_PUT, SoundSource.PLAYERS, 1.0F, 1.0F);
			return;
		}
		if (elderNear(level, box.getCenter()) == null) {
			player.sendOverlayMessage(Component.translatable("item.eam.blueprint.no_village", VillagerElderModule.radius()));
			return;
		}
		List<Site> sites = new ArrayList<>(level.getAttachedOrElse(SITES, List.of()));
		for (Site site : sites) {
			BlueprintTemplate other = BlueprintTemplates.get(level, site.template());
			if (other != null && other.box(site.origin(), site.turns()).intersects(box)) {
				player.sendOverlayMessage(Component.translatable("item.eam.blueprint.overlaps"));
				return;
			}
		}
		sites.add(new Site(UUID.randomUUID(), data.template(), origin, data.turns(), 0));
		level.setAttached(SITES, List.copyOf(sites));
		if (!player.getAbilities().instabuild) stack.shrink(1);
		player.sendOverlayMessage(Component.translatable("item.eam.blueprint.placed"));
		level.playSound(null, look, SoundEvents.BOOK_PUT, SoundSource.PLAYERS, 1.0F, 1.0F);
	}

	/** In creative mode the building stands at once, in the same order villagers would build it. */
	private static void buildAtOnce(ServerLevel level, BlueprintTemplate template, BlockPos origin, int turns) {
		Site site = new Site(UUID.randomUUID(), template.id(), origin, turns, 0);
		for (BlueprintTemplate.Placed block : buildingOrder(template, site)) {
			if (!done(level, block)) level.setBlock(block.pos(), block.state(), FLAGS);
		}
	}

		private static @Nullable Villager elderNear(ServerLevel level, Vec3 pos) {
		int radius = VillagerElderModule.radius();
		return level.getEntitiesOfClass(Villager.class, new AABB(pos, pos).inflate(radius),
						villager -> VillagerElderModule.isElder(villager) && villager.distanceToSqr(pos) <= (double) radius * radius)
				.stream().min(Comparator.comparingDouble(villager -> villager.distanceToSqr(pos))).orElse(null);
	}

	/** Whether the villager's brain runs this tick: not while he places blocks. */
	public static boolean thinks(Villager villager) {
		Builder builder = enabled ? villager.getAttached(BUILDER) : null;
		return builder == null || !builder.working();
	}

	/** Called every tick a villager thinks on the server: by day a free grown villager takes up a site near him and builds it. */
	public static void tick(Villager villager, ServerLevel level) {
		if (!enabled) return;
		Builder builder = villager.getAttached(BUILDER);
		if (builder == null) {
			if (VillagerRepairLogic.checksNow(level.getGameTime(), villager.getId(), LOOK_TICKS) && canBuild(villager, level)) takeUp(villager, level);
			return;
		}
		Site site = site(level, builder.site());
		if (site == null || !canBuild(villager, level)) {
			release(villager);
			return;
		}
		build(villager, level, site, builder);
	}

	/** A grown villager who is no guard and no elder, awake, by day, not panicking. */
	private static boolean canBuild(Villager villager, ServerLevel level) {
		return villager.isAlive() && !villager.isBaby() && !villager.isSleeping() && level.isBrightOutside()
				&& !VillagerElderModule.isElder(villager) && !VillagerGuardModule.isGuardProfession(villager) && villager.getTarget() == null
				&& villager.getLastHurtByMob() == null;
	}

	private static void release(Villager villager) {
		villager.removeAttached(BUILDER);
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		villager.getNavigation().stop();
	}

	private static @Nullable Site site(ServerLevel level, UUID id) {
		for (Site site : level.getAttachedOrElse(SITES, List.<Site>of())) {
			if (site.id().equals(id)) return site;
		}
		return null;
	}

	/** Takes up the nearest site in his elder's reach that has fewer than as many builders as the settings allow. */
	private static void takeUp(Villager villager, ServerLevel level) {
		List<Site> sites = level.getAttachedOrElse(SITES, List.of());
		if (sites.isEmpty()) return;
		int radius = VillagerElderModule.radius();
		Site best = null;
		double bestDistance = (double) radius * radius;
		for (Site site : sites) {
			BlueprintTemplate template = BlueprintTemplates.get(level, site.template());
			if (template == null) continue;
			AABB box = template.box(site.origin(), site.turns());
			double distance = villager.distanceToSqr(box.getCenter());
			if (distance >= bestDistance) continue;
			int builders = level.getEntitiesOfClass(Villager.class, box.inflate(radius),
					other -> other.getAttached(BUILDER) != null && other.getAttached(BUILDER).site().equals(site.id())).size();
			if (builders >= VillageEconomyModule.settings().builders()) continue;
			best = site;
			bestDistance = distance;
		}
		if (best != null) villager.setAttached(BUILDER, new Builder(best.id(), level.getGameTime(), level.getGameTime(), false));
	}

	/** Walks to the edge of the site, then places its next block every {@code ticks_per_block} ticks. */
	private static void build(Villager villager, ServerLevel level, Site site, Builder builder) {
		BlueprintTemplate template = BlueprintTemplates.get(level, site.template());
		if (template == null) {
			finish(level, site);
			return;
		}
		AABB box = template.box(site.origin(), site.turns());
		if (!loaded(level, box)) {
			release(villager);
			return;
		}
		Vec3 edge = closest(box, villager.position());
		long time = level.getGameTime();
		double distance = villager.position().distanceTo(edge);
		boolean stuck = time - builder.since() > STUCK_TICKS;
		if (stuck && distance > STUCK_DISTANCE) {
			release(villager);
			return;
		}
		if (distance > REACH && !stuck) {
			// The brain walks him there, so he opens doors on the way.
			if ((time + villager.getId()) % PATH_TICKS == 0) {
				villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new Vec3(edge.x, box.minY, edge.z), 0.6F, 1));
			}
			return;
		}
		if (!builder.working()) {
			villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			villager.getNavigation().stop();
		}
		if (time - builder.placed() < VillageEconomyModule.settings().ticksPerBlock()) {
			if (!builder.working()) villager.setAttached(BUILDER, new Builder(builder.site(), builder.since(), builder.placed(), true));
			return;
		}
		villager.setAttached(BUILDER, new Builder(builder.site(), builder.since(), time, true));
		List<BlueprintTemplate.Placed> order = ORDERS.computeIfAbsent(site.id(), id -> buildingOrder(template, site));
		int next = site.next();
		while (next < order.size() && done(level, order.get(next))) next++;
		if (next >= order.size()) {
			finish(level, site);
			return;
		}
		BlueprintTemplate.Placed block = order.get(next);
		BlockState old = level.getBlockState(block.pos());
		if (!old.isAir() && !old.canBeReplaced()) {
			if (keeps(level, block, old)) {
				// Placed by someone after the site was set down: the builders leave it and go on.
				replace(level, site.advanced(next + 1));
				return;
			}
			level.destroyBlock(block.pos(), true, villager);
		}
		level.setBlock(block.pos(), block.state(), FLAGS);
		villager.getLookControl().setLookAt(Vec3.atCenterOf(block.pos()));
		villager.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT);
		if (!block.state().isAir()) {
			SoundType sound = block.state().getSoundType();
			level.playSound(null, block.pos(), sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
		}
		replace(level, site.advanced(next + 1));
	}

	/** Whether every chunk of the box is loaded: builders never load one. */
	private static boolean loaded(ServerLevel level, AABB box) {
		int minX = ((int) Math.floor(box.minX)) >> 4;
		int maxX = ((int) Math.floor(box.maxX)) >> 4;
		int minZ = ((int) Math.floor(box.minZ)) >> 4;
		int maxZ = ((int) Math.floor(box.maxZ)) >> 4;
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				if (!level.hasChunk(x, z)) return false;
			}
		}
		return true;
	}

	/** What builders never break: unbreakable blocks, chests and such, and anything in the way but the ground. */
	private static boolean keeps(ServerLevel level, BlueprintTemplate.Placed block, BlockState old) {
		return old.getDestroySpeed(level, block.pos()) < 0 || old.hasBlockEntity() || BlueprintTemplate.blocked(level, block);
	}

	/** Whether the block is in place already: the same block, or air where air goes. */
	private static boolean done(ServerLevel level, BlueprintTemplate.Placed block) {
		BlockState there = level.getBlockState(block.pos());
		return there.equals(block.state()) || block.state().isAir() && there.isAir();
	}

	/**
	 * Bottom up; in each layer what is in the way is cleared first, then full blocks are laid, then what leans on them:
	 * torches, doors, panes, beds.
	 */
	private static List<BlueprintTemplate.Placed> buildingOrder(BlueprintTemplate template, Site site) {
		List<BlueprintTemplate.Placed> order = new ArrayList<>(template.placed(site.origin(), site.turns()));
		order.sort(Comparator.<BlueprintTemplate.Placed>comparingInt(block -> block.pos().getY()).thenComparingInt(block ->
				block.state().isAir() ? 0 : block.state().isSolid() && !block.state().hasBlockEntity() ? 1 : 2));
		return order;
	}

	private static void replace(ServerLevel level, Site updated) {
		List<Site> sites = new ArrayList<>(level.getAttachedOrElse(SITES, List.of()));
		sites.replaceAll(site -> site.id().equals(updated.id()) ? updated : site);
		level.setAttached(SITES, List.copyOf(sites));
	}

	/** The site is done: its builders go back to their lives and cheer. */
	private static void finish(ServerLevel level, Site done) {
		List<Site> sites = new ArrayList<>(level.getAttachedOrElse(SITES, List.of()));
		sites.removeIf(site -> site.id().equals(done.id()));
		level.setAttached(SITES, List.copyOf(sites));
		ORDERS.remove(done.id());
		BlueprintTemplate template = BlueprintTemplates.get(level, done.template());
		AABB area = template == null ? new AABB(done.origin()).inflate(16) : template.box(done.origin(), done.turns()).inflate(16);
		for (Villager villager : level.getEntitiesOfClass(Villager.class, area,
				villager -> villager.getAttached(BUILDER) != null && villager.getAttached(BUILDER).site().equals(done.id()))) {
			release(villager);
			level.broadcastEntityEvent(villager, HAPPY_PARTICLES);
			villager.makeSound(SoundEvents.VILLAGER_CELEBRATE);
		}
	}

	/** The point of the box's outline nearest {@code pos}, at the box's floor. */
	private static Vec3 closest(AABB box, Vec3 pos) {
		double x = Math.clamp(pos.x, box.minX - 0.5, box.maxX + 0.5);
		double z = Math.clamp(pos.z, box.minZ - 0.5, box.maxZ + 0.5);
		if (x > box.minX && x < box.maxX && z > box.minZ && z < box.maxZ) {
			// Inside the footprint: step out to the nearest side.
			double toMinX = x - box.minX;
			double toMaxX = box.maxX - x;
			double toMinZ = z - box.minZ;
			double toMaxZ = box.maxZ - z;
			double min = Math.min(Math.min(toMinX, toMaxX), Math.min(toMinZ, toMaxZ));
			if (min == toMinX) x = box.minX - 0.5;
			else if (min == toMaxX) x = box.maxX + 0.5;
			else if (min == toMinZ) z = box.minZ - 0.5;
			else z = box.maxZ + 0.5;
		}
		return new Vec3(x, Math.clamp(pos.y, box.minY, box.maxY), z);
	}

	/** A blueprint order's template for the village's style, its villager type, else for {@code default}; one at random. */
	static Optional<Identifier> template(EconomySettings.Order order, String villagerType, RandomSource random) {
		List<Identifier> templates = order.templates().getOrDefault(villagerType, order.templates().get("default"));
		if (templates == null || templates.isEmpty()) return Optional.empty();
		return Optional.of(templates.get(random.nextInt(templates.size())));
	}
}
