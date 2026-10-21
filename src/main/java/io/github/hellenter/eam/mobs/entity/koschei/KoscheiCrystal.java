package io.github.hellenter.eam.mobs.entity.koschei;

import io.github.hellenter.eam.content.EamParticles;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * The ice crystal Koschei waits sealed in at his castle: who wakes him, and the burst that frees him. The crystal is the
 * ice and glass round him; the burst takes it away block by block in a spray of shards.
 */
final class KoscheiCrystal {
	/** The crystal reaches this far out from his feet across the floor... */
	private static final int HALF_WIDTH = 2;
	/** ... and this high above them. */
	private static final int HEIGHT = 7;
	/** Shards flung from each block of the crystal. */
	private static final int SHARDS_PER_BLOCK = 4;

	private KoscheiCrystal() {
	}

	/** A player close enough on his floor to wake him; spectators don't. */
	static @Nullable Player waker(ServerLevel level, Koschei koschei, KoscheiSettings.Crystal crystal) {
		for (Player player : level.players()) {
			if (player.isAlive() && !player.isSpectator()
					&& KoscheiLogic.wakesFromCrystal(koschei.horizontalDistanceTo(player), player.getY() - koschei.getY(), crystal)) {
				return player;
			}
		}
		return null;
	}

	/**
	 * The crystal bursts: its ice and glass fly apart in shards with a crack and a ring, and whoever stands near is cut and
	 * thrown back, the more the nearer.
	 */
	static void burst(ServerLevel level, Koschei koschei, KoscheiSettings.Crystal crystal) {
		RandomSource random = koschei.getRandom();
		Vec3 heart = koschei.position().add(0.0, koschei.getBbHeight() * 0.5, 0.0);
		BlockPos feet = koschei.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-HALF_WIDTH, -1, -HALF_WIDTH), feet.offset(HALF_WIDTH, HEIGHT, HALF_WIDTH))) {
			if (!isCrystal(level.getBlockState(pos))) continue;
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			Vec3 centre = Vec3.atCenterOf(pos);
			Vec3 out = centre.subtract(heart.x, centre.y, heart.z).add(0.0, 0.3, 0.0);
			for (int i = 0; i < SHARDS_PER_BLOCK; i++) {
				Vec3 fly = out.add(random.nextGaussian() * 0.5, random.nextDouble() * 0.6, random.nextGaussian() * 0.5).normalize();
				level.sendParticles(EamParticles.KOSCHEI_CRYSTAL_SHARD, centre.x + random.nextGaussian() * 0.2, centre.y + random.nextGaussian() * 0.2,
						centre.z + random.nextGaussian() * 0.2, 0, fly.x, fly.y, fly.z, 0.35 + random.nextDouble() * 0.45);
			}
		}
		level.sendParticles(EamParticles.KOSCHEI_CRYSTAL_SHARD, heart.x, heart.y, heart.z, 40, 0.5, 1.0, 0.5, 0.6);
		level.sendParticles(EamParticles.KOSCHEI_SNOWFLAKE, heart.x, heart.y, heart.z, 50, 1.2, 1.4, 1.2, 0.25);
		level.playSound(null, heart.x, heart.y, heart.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3.0F, 0.5F);
		level.playSound(null, heart.x, heart.y, heart.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.8F);
		level.playSound(null, heart.x, heart.y, heart.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 2.5F, 0.55F);
		level.playSound(null, heart.x, heart.y, heart.z, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 2.0F, 0.6F);

		double radius = crystal.burstRadius();
		for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, new AABB(heart, heart).inflate(radius),
				near -> near != koschei && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(near))) {
			double share = KoscheiLogic.burstShare(near.position().distanceTo(koschei.position()), radius);
			if (share <= 0.0) continue;
			near.hurtServer(level, koschei.damageSources().source(KoscheiModule.SORCERY_DAMAGE, koschei), crystal.damage() * (float) share);
			Vec3 away = near.position().subtract(koschei.position()).multiply(1.0, 0.0, 1.0);
			away = away.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, koschei.getYRot()) : away.normalize();
			double speed = crystal.push() * share;
			near.setDeltaMovement(away.x * speed, 0.3 + 0.2 * share, away.z * speed);
			near.syncVelocity = true;
			near.needsSync = true;
		}
	}

	/** The crystal's ice and glass. */
	private static boolean isCrystal(BlockState state) {
		return state.is(BlockTags.ICE) || state.getBlock() instanceof TransparentBlock;
	}
}
