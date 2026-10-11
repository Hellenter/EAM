package io.github.hellenter.eam.mobs.entity.bat;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamSounds;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Vampire bats: flocks of four to six appear some 40-50 blocks from a player in the biomes tagged
 * {@code eam:has_vampire_bats} (the upper underworld) with a screech the player can hear, and go straight for them.
 * They do not spawn naturally otherwise. Numbers are in {@code data/eam/mobs/vampire_bat.json}.
 */
public final class VampireBatModule {
	public static final TagKey<Biome> SPAWNS_IN = TagKey.create(Registries.BIOME, Eam.id("has_vampire_bats"));
	/** Tries to find open air for a flock before giving up until the next check. */
	private static final int PLACE_ATTEMPTS = 24;
	/** The warning screech plays this far from the player, toward the flock, so it is loud enough to notice. */
	private static final double WARNING_DISTANCE = 10.0;

	private static volatile VampireBatSettings settings = VampireBatSettings.DEFAULT;

	private VampireBatModule() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(EamEntities.VAMPIRE_BAT, VampireBat.createAttributes());
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(VampireBatSettingsLoader.ID, new VampireBatSettingsLoader());
	}

	/** Flock spawning; the bats themselves exist whatever the config says. */
	public static void init() {
		ServerTickEvents.END_LEVEL_TICK.register(VampireBatModule::tick);
	}

	static VampireBatSettings settings() {
		return settings;
	}

	static void setSettings(VampireBatSettings value) {
		settings = value;
	}

	private static void tick(ServerLevel level) {
		VampireBatSettings current = settings;
		if (level.getDifficulty() == Difficulty.PEACEFUL || level.getGameTime() % current.checkInterval() != 0) {
			return;
		}
		for (ServerPlayer player : level.players()) {
			if (!player.isAlive() || player.isSpectator() || player.isCreative() || !level.getBiome(player.blockPosition()).is(SPAWNS_IN)) {
				continue;
			}
			int nearby = level.getEntitiesOfClass(VampireBat.class, player.getBoundingBox().inflate(64.0)).size();
			if (VampireBatLogic.flockDue(nearby, current.maxNearby(), level.getRandom().nextFloat(), current.flockChance())) {
				spawnFlock(level, player, current);
			}
		}
	}

	private static void spawnFlock(ServerLevel level, ServerPlayer player, VampireBatSettings current) {
		RandomSource random = level.getRandom();
		Vec3 center = findAir(level, player, current, random);
		if (center == null) {
			return;
		}
		int size = VampireBatLogic.flockSize(current.minFlock(), current.maxFlock(), random.nextFloat());
		if (summon(level, center, player, size) > 0) {
			warn(level, player, center, random);
		}
	}

	/**
	 * Up to {@code size} vampire bats in the open air around {@code center}, all set on {@code target}; the undead
	 * warlock calls them this way too.
	 *
	 * @return how many came
	 */
	public static int summon(ServerLevel level, Vec3 center, LivingEntity target, int size) {
		RandomSource random = level.getRandom();
		int spawned = 0;
		for (int i = 0; i < size * 3 && spawned < size; i++) {
			Vec3 pos = center.add(random.nextDouble() * 3.0 - 1.5, random.nextDouble() * 2.0 - 1.0, random.nextDouble() * 3.0 - 1.5);
			VampireBat bat = EamEntities.VAMPIRE_BAT.create(level, EntitySpawnReason.EVENT);
			if (bat == null) {
				return spawned;
			}
			bat.snapTo(pos.x, pos.y, pos.z, random.nextFloat() * 360.0F, 0.0F);
			if (!level.noCollision(bat) || level.containsAnyLiquid(bat.getBoundingBox())) {
				continue;
			}
			bat.finalizeSpawn(level, level.getCurrentDifficultyAt(bat.blockPosition()), EntitySpawnReason.EVENT, null);
			bat.setTarget(target);
			level.addFreshEntity(bat);
			spawned++;
		}
		return spawned;
	}

	/** A spot of open air 40-50 blocks away that the flock fits into, roughly at the player's height. */
	private static @Nullable Vec3 findAir(ServerLevel level, ServerPlayer player, VampireBatSettings current, RandomSource random) {
		for (int i = 0; i < PLACE_ATTEMPTS; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double distance = VampireBatLogic.flockDistance(current.minDistance(), current.maxDistance(), random.nextFloat());
			BlockPos pos = BlockPos.containing(player.getX() + Math.cos(angle) * distance, player.getY() + random.nextInt(17) - 8,
					player.getZ() + Math.sin(angle) * distance);
			if (pos.getY() <= level.getMinY() || pos.getY() >= level.getMaxY() || !level.isPositionEntityTicking(pos)) {
				continue;
			}
			AABB room = new AABB(pos).inflate(1.0, 0.5, 1.0).move(0.0, 0.5, 0.0);
			if (level.noCollision(room) && !level.containsAnyLiquid(room)) {
				return Vec3.atCenterOf(pos);
			}
		}
		return null;
	}

	/** A chorus of screeches from the flock's direction, so the player has time to get ready. */
	private static void warn(ServerLevel level, ServerPlayer player, Vec3 flock, RandomSource random) {
		Vec3 eye = player.getEyePosition();
		Vec3 toward = flock.subtract(eye);
		Vec3 at = eye.add(toward.normalize().scale(Math.min(WARNING_DISTANCE, toward.length())));
		for (int i = 0; i < 3; i++) {
			level.playSound(null, at.x, at.y, at.z, EamSounds.VAMPIRE_BAT_SWARM, SoundSource.HOSTILE, 1.2F,
					Mth.lerp(i / 2.0F, 0.8F, 1.2F) + (random.nextFloat() - 0.5F) * 0.1F);
		}
	}
}
