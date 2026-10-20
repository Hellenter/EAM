package io.github.hellenter.eam.mobs.entity.lost;

import io.github.hellenter.eam.content.EamItems;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.ProjectileUtil;

import java.util.EnumSet;

/**
 * The longbowman's rare power shot, always warned the same way: he stops, a warning sound rings out and red sparks
 * gather around him while he draws, and exactly {@code warning_ticks} after the sound he looses a very fast red arrow.
 * A player who breaks his line of sight before then makes him lower the bow.
 */
final class LongbowPowerShotGoal extends Goal {
	private static final float TURN = 40.0F;
	private static final DustParticleOptions SPARK = new DustParticleOptions(0xFF2A10, 1.2F);
	/** After a shot called off for lack of sight he tries again this soon. */
	private static final int RETRY_TICKS = 100;

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
			level.sendParticles(SPARK, longbowman.getX(), longbowman.getY(1.0), longbowman.getZ(), 20, 0.5, 0.6, 0.5, 0.0);
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
		// Sparks gather round him, thicker as the shot comes.
		int sparks = 1 + 4 * ticks / shot.warningTicks();
		level.sendParticles(SPARK, longbowman.getX(), longbowman.getY(0.7), longbowman.getZ(), sparks, 0.45, 0.5, 0.45, 0.0);
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

	private int nextCooldown(LongbowmanSettings.PowerShot shot) {
		return LongbowmanLogic.powerCooldown(shot.cooldownMin(), shot.cooldownMax(), longbowman.getRandom().nextFloat());
	}
}
