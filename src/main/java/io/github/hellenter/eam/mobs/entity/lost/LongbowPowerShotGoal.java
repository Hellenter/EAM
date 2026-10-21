package io.github.hellenter.eam.mobs.entity.lost;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.EamParticles;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * The longbowman's rare power shot, always warned the same way: he stops, a warning sound rings out and red glints
 * fly into his bow while he draws, and exactly {@code warning_ticks} after the sound he looses a very fast red arrow.
 * A player who breaks his line of sight before then makes him lower the bow.
 */
final class LongbowPowerShotGoal extends Goal {
	private static final float TURN = 40.0F;
	/** After a shot called off for lack of sight he tries again this soon. */
	private static final int RETRY_TICKS = 100;
	/** Share of the distance to the bow a glint flies in its first tick; its drag of 0.86 a tick brings it there in about ten. */
	private static final double GLINT_PULL = 0.19;

	private final LostLongbowman longbowman;
	/** Game time from which he may start another power shot; 0 until his first fight. */
	private long readyAt;
	private int ticks;
	/** Whether the shot is over, loosed or called off, and whether it was loosed. */
	private boolean done;
	private boolean fired;

	LongbowPowerShotGoal(LostLongbowman longbowman) {
		this.longbowman = longbowman;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = longbowman.getTarget();
		if (longbowman.role() != LostRole.LONGBOWMAN || target == null || !target.isAlive() || longbowman.longbow().isEmpty()) {
			return false;
		}
		LongbowmanSettings.PowerShot shot = LostLongbowman.longbowman().powerShot();
		long now = longbowman.level().getGameTime();
		if (readyAt == 0L) {
			// Not right at the start of a fight: the first one comes half a cooldown in.
			readyAt = now + nextCooldown(shot) / 2;
		}
		return now >= readyAt && longbowman.distanceToSqr(target) >= shot.minRange() * shot.minRange()
				&& longbowman.getSensing().hasLineOfSight(target);
	}

	@Override
	public boolean canContinueToUse() {
		LivingEntity target = longbowman.getTarget();
		return !done && target != null && target.isAlive();
	}

	@Override
	public void start() {
		ticks = 0;
		done = false;
		fired = false;
		longbowman.stopUsingItem();
		longbowman.setAggressive(true);
		longbowman.getNavigation().stop();
		longbowman.startUsingItem(ProjectileUtil.getWeaponHoldingHand(longbowman, EamItems.LONGBOW));
		longbowman.playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 2.0F, 0.7F);
		if (longbowman.level() instanceof ServerLevel level) {
			glints(level, 14);
		}
	}

	@Override
	public void stop() {
		longbowman.stopUsingItem();
		longbowman.setAggressive(false);
		long now = longbowman.level().getGameTime();
		readyAt = now + (fired ? nextCooldown(LostLongbowman.longbowman().powerShot()) : RETRY_TICKS);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity target = longbowman.getTarget();
		if (target == null || !(longbowman.level() instanceof ServerLevel level)) {
			return;
		}
		// He stands still and keeps facing the target while the warning runs.
		longbowman.getNavigation().stop();
		longbowman.getMoveControl().setWantedPosition(longbowman.getX(), longbowman.getY(), longbowman.getZ(), 0.0);
		longbowman.setXxa(0.0F);
		float yaw = (float) (Mth.atan2(target.getZ() - longbowman.getZ(), target.getX() - longbowman.getX()) * Mth.RAD_TO_DEG) - 90.0F;
		longbowman.setYRot(LostLogic.turnTowards(longbowman.getYRot(), yaw, TURN));
		longbowman.setYBodyRot(longbowman.getYRot());
		longbowman.setYHeadRot(longbowman.getYRot());
		longbowman.getLookControl().setLookAt(target, 30.0F, 30.0F);
		LongbowmanSettings.PowerShot shot = LostLongbowman.longbowman().powerShot();
		ticks++;
		// Red glints fly into his bow, thicker as the shot comes.
		glints(level, 1 + 3 * ticks / shot.warningTicks());
		if (ticks < shot.warningTicks()) {
			return;
		}
		// Out of sight by then, he lowers the bow and tries again later.
		done = true;
		longbowman.stopUsingItem();
		if (longbowman.getSensing().hasLineOfSight(target)) {
			longbowman.shootRed(level, target, shot);
			fired = true;
		}
	}

	/** {@code count} red glints from around his bow flying into it. */
	private void glints(ServerLevel level, int count) {
		Vec3 bow = longbowman.bowPosition();
		for (int i = 0; i < count; i++) {
			Vec3 from = bow.add(new Vec3(longbowman.getRandom().nextGaussian(), longbowman.getRandom().nextGaussian() * 0.7,
					longbowman.getRandom().nextGaussian()).normalize().scale(1.0 + longbowman.getRandom().nextDouble() * 0.5));
			// With the particle's drag, this flight ends at the bow just as it fades.
			Vec3 flight = bow.subtract(from).scale(GLINT_PULL);
			level.sendParticles(EamParticles.LONGBOW_CHARGE, from.x, from.y, from.z, 0, flight.x, flight.y, flight.z, 1.0);
		}
	}

	private int nextCooldown(LongbowmanSettings.PowerShot shot) {
		return LongbowmanLogic.powerCooldown(shot.cooldownMin(), shot.cooldownMax(), longbowman.getRandom().nextFloat());
	}
}
