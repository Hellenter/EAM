package io.github.hellenter.eam.mobs.entity.yaga;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.mobs.entity.boss.BossParry;
import io.github.hellenter.eam.mobs.entity.drowned.DrownedSoldier;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Baba Yaga, the swamp witch boss. In phase one she circles in her mortar, out of reach of anything but a parry, and
 * fights with waves of swamp creatures, poison flasks, spells and glyphs; in phase two, on foot, she keeps behind one
 * group of creatures and fights with bolts, a fire beam and her claws. She waits in front of her hut in the middle of every swampy forest
 * ({@link io.github.hellenter.eam.world.structures.yagahut.YagaHutModule}); also the
 * spawn egg or {@code /summon eam:baba_yaga}. Numbers are in {@code data/eam/mobs/baba_yaga.json}.
 */
public final class BabaYagaModule {
	/** Her glyph of harm and her acid: through armor and shields. */
	public static final ResourceKey<DamageType> CURSE_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Eam.id("baba_yaga_curse"));
	/** Her fire beam: through armor; a shield held towards her blocks it, which she checks herself. */
	public static final ResourceKey<DamageType> BEAM_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Eam.id("baba_yaga_beam"));
	/** The entity tag on the creatures she raised. */
	public static final String MINION_TAG = "eam.baba_yaga_minion";
	/** Her creatures this close to her never burn in daylight. */
	public static final double SUN_SHELTER_RADIUS = 100.0;
	/** No other hostile mob spawns naturally this close to her. */
	public static final double SPAWN_CLEAR_RADIUS = 100.0;
	/** Every Baba Yaga ticking on the server; dropped once unloaded or dead. */
	private static final Set<BabaYaga> ACTIVE = Collections.newSetFromMap(new WeakHashMap<>());

	/** Players last told they stand in a storm; the others were told they do not. */
	private static final Map<UUID, Boolean> IN_STORM = new ConcurrentHashMap<>();
	/** How often players are checked against the storms. */
	private static final int STORM_CHECK = 10;

	private static volatile BabaYagaSettings settings = BabaYagaSettings.DEFAULT;

	private BabaYagaModule() {
	}

	public static void register() {
		BossParry.register();
		FabricDefaultAttributeRegistry.register(EamEntities.BABA_YAGA, BabaYaga.createAttributes());
		FabricDefaultAttributeRegistry.register(EamEntities.HAG_THREAD, HagThread.createAttributes());
		FabricDefaultAttributeRegistry.register(EamEntities.DROWNED_SOLDIER, DrownedSoldier.createAttributes());
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(BabaYagaSettingsLoader.ID, new BabaYagaSettingsLoader());
		PayloadTypeRegistry.clientboundPlay().register(YagaStormPayload.TYPE, YagaStormPayload.STREAM_CODEC);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % STORM_CHECK == 0) {
				syncStorms(server);
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> IN_STORM.remove(handler.player.getUUID()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> IN_STORM.remove(handler.player.getUUID()));
	}

	/** Tells each player whose storm state changed whether they now stand within a Baba Yaga's storm. */
	private static void syncStorms(MinecraftServer server) {
		List<BabaYaga> storms = ACTIVE.stream().filter(BabaYaga::stormActive).toList();
		double radius = settings.storm().radius();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			boolean inStorm = false;
			for (BabaYaga yaga : storms) {
				if (yaga.level() == player.level() && yaga.distanceToSqr(player) <= radius * radius) {
					inStorm = true;
					break;
				}
			}
			Boolean told = IN_STORM.get(player.getUUID());
			if ((told == null ? !inStorm : told == inStorm) || !ServerPlayNetworking.canSend(player, YagaStormPayload.TYPE)) {
				continue;
			}
			IN_STORM.put(player.getUUID(), inStorm);
			ServerPlayNetworking.send(player, new YagaStormPayload(inStorm));
		}
	}

	public static BabaYagaSettings settings() {
		return settings;
	}

	static void setSettings(BabaYagaSettings value) {
		settings = value;
	}

	/** Whether Baba Yaga raised this creature: it fights at once, in the open, and her spells and glyphs help it. */
	public static boolean isMinion(Entity entity) {
		return entity.entityTags().contains(MINION_TAG);
	}

	/** Whether daylight spares this mob: one of her creatures within {@link #SUN_SHELTER_RADIUS} of a Baba Yaga. */
	public static boolean shelteredFromSun(Mob mob) {
		return isMinion(mob) && !mob.level().getEntitiesOfClass(BabaYaga.class, mob.getBoundingBox().inflate(SUN_SHELTER_RADIUS),
				yaga -> yaga.isAlive() && yaga.distanceToSqr(mob) <= SUN_SHELTER_RADIUS * SUN_SHELTER_RADIUS).isEmpty();
	}

	/** She is about: called each server tick she is alive. */
	static void track(BabaYaga yaga) {
		ACTIVE.add(yaga);
	}

	/** Whether {@code mob}, spawning naturally, is a hostile one too close to a Baba Yaga to be allowed. */
	public static boolean keepsAway(Mob mob) {
		if (!(mob instanceof Enemy) || mob instanceof BabaYaga || ACTIVE.isEmpty()) {
			return false;
		}
		ACTIVE.removeIf(yaga -> yaga.isRemoved() || !yaga.isAlive());
		for (BabaYaga yaga : ACTIVE) {
			if (yaga.level() == mob.level() && yaga.distanceToSqr(mob) <= SPAWN_CLEAR_RADIUS * SPAWN_CLEAR_RADIUS) {
				return true;
			}
		}
		return false;
	}
}
