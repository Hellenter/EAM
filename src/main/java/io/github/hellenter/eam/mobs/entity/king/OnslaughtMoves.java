package io.github.hellenter.eam.mobs.entity.king;

import io.github.hellenter.eam.content.EamSounds;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * The onslaught's movement and cues, shared by the Pillager King and his guards: a rush at the target until
 * {@link KingAction#ONSLAUGHT_RUSH_END}, a crouch, a leap with a bell on {@link KingAction#ONSLAUGHT_LEAP} (the moment
 * to parry), steering in the air to come down just short of the target, and the blow on {@link KingAction#ONSLAUGHT_HIT}.
 * The caller deals the blow.
 */
public final class OnslaughtMoves {
	/** The rush stops this close, flat from centre to centre. */
	private static final double RUSH_STOP = 1.8;
	/** Upward speed of the leap: about the ticks it has in the air. */
	private static final double LEAP_UP = 0.45;
	/** The leap comes down this short of the target, in reach of its parry. */
	private static final double LAND_SHORT = 1.2;
	private static final double MAX_LEAP_SPEED = 1.3;
	/** The blow lands this much past the reach, for a moving target. */
	private static final double REACH_SLACK = 0.3;

	private OnslaughtMoves() {
	}

	/**
	 * Moves {@code mob} on tick {@code t} of its onslaught at {@code target} and plays the cues. True on the hit tick if
	 * the target is in {@code reach}: the caller deals the blow then.
	 */
	public static boolean tick(Mob mob, ServerLevel level, int t, LivingEntity target, double rushSpeed, double reach) {
		if (t == 0) {
			mob.playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.5F, 0.6F);
		}
		if (t < KingAction.ONSLAUGHT_RUSH_END && flatDistance(mob, target) > RUSH_STOP) {
			Vec3 to = towards(mob, target);
			mob.setDeltaMovement(to.x * rushSpeed, mob.getDeltaMovement().y, to.z * rushSpeed);
		}
		if (t == KingAction.ONSLAUGHT_LEAP) {
			// A bell: the moment to parry, while the leap is in the air.
			level.playSound(null, mob.getX(), mob.getEyeY(), mob.getZ(), EamSounds.FALLEN_LORD_PARRY_WINDOW, mob.getSoundSource(), 1.5F, 0.7F);
			Vec3 to = towards(mob, target);
			double speed = leapSpeed(mob, target, KingAction.ONSLAUGHT_HIT - KingAction.ONSLAUGHT_LEAP);
			mob.setDeltaMovement(to.x * speed, LEAP_UP, to.z * speed);
		} else if (t > KingAction.ONSLAUGHT_LEAP && t < KingAction.ONSLAUGHT_HIT && !mob.onGround()) {
			// The target may back off: steer in the air to come down on it.
			Vec3 to = towards(mob, target);
			double speed = leapSpeed(mob, target, KingAction.ONSLAUGHT_HIT - t);
			mob.setDeltaMovement(to.x * speed, mob.getDeltaMovement().y, to.z * speed);
		}
		if (t != KingAction.ONSLAUGHT_HIT) {
			return false;
		}
		level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.MACE_SMASH_GROUND_HEAVY, mob.getSoundSource(), 1.5F, 0.7F);
		Vec3 look = mob.getLookAngle();
		level.sendParticles(ParticleTypes.EXPLOSION, mob.getX() + look.x * 1.5, mob.getY() + 0.2, mob.getZ() + look.z * 1.5, 1, 0.0, 0.0, 0.0, 0.0);
		return flatDistance(mob, target) <= reach + REACH_SLACK && Math.abs(target.getY() - mob.getY()) < 2.5 && mob.hasLineOfSight(target);
	}

	/** Whether {@code target} holds a shield up against {@code attacker}: it only softens the blow. */
	public static boolean shielded(LivingEntity target, Entity attacker) {
		return target.isBlocking() && FallenLordLogic.shieldCovers(target.getYHeadRot(), attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
	}

	private static double leapSpeed(Mob mob, LivingEntity target, int ticks) {
		return Math.min(MAX_LEAP_SPEED, PillagerKingLogic.leapSpeed(flatDistance(mob, target) - LAND_SHORT, ticks));
	}

	private static double flatDistance(Entity from, Entity to) {
		double dx = to.getX() - from.getX();
		double dz = to.getZ() - from.getZ();
		return Math.sqrt(dx * dx + dz * dz);
	}

	private static Vec3 towards(Entity from, Entity to) {
		Vec3 flat = new Vec3(to.getX() - from.getX(), 0.0, to.getZ() - from.getZ());
		return flat.lengthSqr() < 1.0E-6 ? Vec3.ZERO : flat.normalize();
	}
}
