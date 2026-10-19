package io.github.hellenter.eam.mobs.vanilla.raid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.trophy.KingHelmBlock;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PoiHelper;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Village raids. A lit Pillager King's helm keeps every raid away from around it, the ominous ones too.
 * Raids also come on their own, every 15 to 25 days, to a village a player is in. Each wave brings the vanilla raiders
 * plus pillager guards and elite crossbowmen, and raiders of a wave that drags on past two minutes glow. Numbers are in
 * {@code data/eam/events/village_raids.json}; the random raid calendar is saved on the overworld.
 */
public final class VillageRaidsModule {
	/**
	 * A lit King's helm is a point of interest, so raids find it without scanning blocks; turning it off changes its state
	 * and the game drops the point of interest.
	 */
	public static final ResourceKey<PoiType> HELM = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, Eam.id("pillager_king_helm"));

	/**
	 * The random raid calendar, stored on the overworld.
	 *
	 * @param nextDay the day the next random raid is due; 0 until the first one is rolled
	 */
	public record State(int nextDay) {
		static final State NONE = new State(0);
		static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.fieldOf("next_day").forGetter(State::nextDay)
		).apply(instance, State::new));
	}

	public static final AttachmentType<State> STATE = AttachmentRegistry.create(Eam.id("village_raids"),
			builder -> builder.persistent(State.CODEC));

	/** Glowing is renewed this often, and lasts a bit longer so it never blinks. */
	private static final int GLOW_INTERVAL = 20;
	private static final int GLOW_TICKS = 50;

	/** The wave each raid is on and when it began, to make raiders glow; a reloaded raid restarts its count. */
	private static final Map<Raid, Wave> WAVES = Collections.synchronizedMap(new WeakHashMap<>());

	/** How long a "guarded" answer for a chunk is kept, and how many are kept at most. */
	private static final int GUARD_CACHE_TICKS = 40;
	private static final int GUARD_CACHE_SIZE = 1024;
	private static final Map<GuardKey, Guard> GUARDED = new ConcurrentHashMap<>();

	private static volatile VillageRaidsSettings settings = VillageRaidsSettings.DEFAULT;
	private static volatile boolean enabled;

	private record Wave(int number, long start) {
	}

	private record GuardKey(ResourceKey<Level> dimension, long chunk) {
	}

	private record Guard(long time, boolean guarded) {
	}

	private VillageRaidsModule() {
	}

	/** The helm point of interest and the settings exist whatever the config says. */
	public static void register() {
		PoiHelper.register(HELM.identifier(), 0, 1, EamBlocks.PILLAGER_KING_HELM.getStateDefinition().getPossibleStates().stream()
				.filter(state -> state.getValue(KingHelmBlock.ENABLED))
				.toList());
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(VillageRaidsSettingsLoader.ID, new VillageRaidsSettingsLoader());
	}

	public static void init() {
		enabled = true;
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % settings.checkTicks() == 0) {
				updateCalendar(server);
			}
		});
	}

	static void setSettings(VillageRaidsSettings value) {
		settings = value;
	}

	// --- King's helm ---

	/**
	 * A lit King's helm within {@code helm_radius} of {@code pos} keeps raids away. Bad Omen asks this every tick, so the
	 * answer is kept for a couple of seconds per chunk.
	 */
	public static boolean guarded(ServerLevel level, BlockPos pos) {
		if (!enabled) {
			return false;
		}
		long now = level.getGameTime();
		GuardKey key = new GuardKey(level.dimension(), ChunkPos.pack(pos));
		Guard cached = GUARDED.get(key);
		if (cached != null && now - cached.time() < GUARD_CACHE_TICKS) {
			return cached.guarded();
		}
		boolean guarded = level.getPoiManager().getInRange(type -> type.is(HELM), pos, settings.helmRadius(), PoiManager.Occupancy.ANY)
				.findAny().isPresent();
		if (GUARDED.size() > GUARD_CACHE_SIZE) {
			GUARDED.clear();
		}
		GUARDED.put(key, new Guard(now, guarded));
		return guarded;
	}

	// --- Random raids ---

	private static void updateCalendar(MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		int today = VillageRaidsLogic.dayNumber(overworld.getOverworldClockTime());
		State state = overworld.getAttachedOrElse(STATE, State.NONE);
		if (state.nextDay() == 0) {
			// The first one is counted from the day the calendar starts: a new world, or an old one this update came to.
			int first = VillageRaidsLogic.nextDay(today, settings.intervalMinDays(), settings.intervalMaxDays(), overworld.getRandom()::nextInt);
			overworld.setAttached(STATE, new State(first));
			return;
		}
		if (VillageRaidsLogic.due(today, state.nextDay()) && startRandomRaid(overworld)) {
			int next = VillageRaidsLogic.nextDay(today, settings.intervalMinDays(), settings.intervalMaxDays(), overworld.getRandom()::nextInt);
			overworld.setAttached(STATE, new State(next));
		}
	}

	/** Starts a raid on the village of a random player who is in one; false if nobody is. */
	private static boolean startRandomRaid(ServerLevel level) {
		List<ServerPlayer> players = new ArrayList<>(level.players());
		Collections.shuffle(players, new Random(level.getRandom().nextLong()));
		for (ServerPlayer player : players) {
			BlockPos pos = player.blockPosition();
			if (player.isSpectator() || !level.isVillage(pos) || level.getRaidAt(pos) != null || !hasVillagers(level, pos)) {
				continue;
			}
			Raid raid = level.getRaids().createOrExtendRaid(player, pos);
			if (raid == null) {
				continue;
			}
			if (raid.getRaidOmenLevel() == 0) {
				raid.setRaidOmenLevel(1);
			}
			Eam.LOGGER.info("Random raid on the village at {}", raid.getCenter());
			Component message = Component.translatable("event.eam.random_raid").withStyle(ChatFormatting.DARK_RED);
			for (ServerPlayer near : level.players()) {
				if (near.blockPosition().closerThan(raid.getCenter(), Math.sqrt(Raid.VALID_RAID_RADIUS_SQR))) {
					near.sendSystemMessage(message);
				}
			}
			return true;
		}
		return false;
	}

	private static boolean hasVillagers(ServerLevel level, BlockPos pos) {
		return !level.getEntitiesOfClass(AbstractVillager.class, new AABB(pos).inflate(settings.villagerRadius()), AbstractVillager::isAlive).isEmpty();
	}

	// --- Waves ---

	/** A wave has just spawned: pillager guards and elite crossbowmen join it on top of the vanilla raiders. */
	public static void waveSpawned(Raid raid, ServerLevel level, BlockPos pos) {
		if (!enabled) {
			return;
		}
		int wave = raid.getGroupsSpawned();
		WAVES.put(raid, new Wave(wave, level.getGameTime()));
		reinforce(raid, level, pos, wave, EamEntities.PILLAGER_GUARD, VillageRaidsLogic.perWave(settings.guardsPerWave(), wave));
		reinforce(raid, level, pos, wave, EamEntities.ELITE_CROSSBOWMAN, VillageRaidsLogic.perWave(settings.elitesPerWave(), wave));
		raid.updateBossbar();
	}

	private static void reinforce(Raid raid, ServerLevel level, BlockPos pos, int wave, EntityType<? extends Raider> type, int count) {
		for (int i = 0; i < count; i++) {
			Raider raider = type.create(level, EntitySpawnReason.EVENT);
			if (raider == null) {
				return;
			}
			raid.joinRaid(level, wave, raider, pos, false);
		}
	}

	/** Every raid tick: raiders of a wave that has gone on too long glow, so the last ones can be found. */
	public static void tick(Raid raid, ServerLevel level) {
		if (!enabled || raid.isStopped() || raid.isOver() || !raid.isActive() || level.getGameTime() % GLOW_INTERVAL != 0) {
			return;
		}
		int alive = raid.getTotalRaidersAlive();
		if (alive == 0) {
			return;
		}
		long now = level.getGameTime();
		Wave wave = WAVES.get(raid);
		if (wave == null || wave.number() != raid.getGroupsSpawned()) {
			wave = new Wave(raid.getGroupsSpawned(), now);
			WAVES.put(raid, wave);
		}
		if (!VillageRaidsLogic.glows(wave.start(), now, settings.glowSeconds() * 20)) {
			return;
		}
		for (Raider raider : raid.getAllRaiders()) {
			if (raider.isAlive() && raider.level() == level) {
				raider.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS, 0, false, false));
			}
		}
	}
}
