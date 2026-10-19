package io.github.hellenter.eam.mobs.entity.lost;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * A player who comes too close to the longbowman gets a volley: he lowers his draw and snaps off a few weak arrows one
 * after another, which push the player back, then draws again from further away.
 */
final class LongbowBarrageGoal extends Goal {
	private static final float TURN = 40.0F;

	private final LostLongbowman longbowman;
	/** Game time from which he may loose another volley. */
	private long readyAt;
	private int shotsLeft;
	private int nextShot;

	LongbowBarrageGoal(LostLongbowman longbowman) {
		this.longbowman = longbowman;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (longbowman.level().getGameTime() < readyAt) {
			return false;
		}
		LongbowmanSettings.Barrage barrage = LostLongbowman.longbowman().barrage();
		LivingEntity target = longbowman.getTarget();
		return barrage.arrows() > 0 && longbowman.role() == LostRole.LONGBOWMAN && target != null && target.isAlive()
				&& longbowman.distanceToSqr(target) <= barrage.range() * barrage.range() && longbowman.getSensing().hasLineOfSight(target)
				&& !longbowman.longbow().isEmpty();
	}

	@Override
	public boolean canContinueToUse() {
		LivingEntity target = longbowman.getTarget();
		return shotsLeft > 0 && target != null && target.isAlive();
	}

	@Override
	public void start() {
		LongbowmanSettings.Barrage barrage = LostLongbowman.longbowman().barrage();
		longbowman.stopUsingItem();
		longbowman.setAggressive(true);
		longbowman.getNavigation().stop();
		shotsLeft = barrage.arrows();
		nextShot = 0;
	}

	@Override
	public void stop() {
		longbowman.setAggressive(false);
		readyAt = longbowman.level().getGameTime() + LostLongbowman.longbowman().barrage().cooldown();
		shotsLeft = 0;
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
		// He stands his ground and turns to the player while he shoots.
		longbowman.getNavigation().stop();
		longbowman.getMoveControl().setWantedPosition(longbowman.getX(), longbowman.getY(), longbowman.getZ(), 0.0);
		float yaw = (float) (Mth.atan2(target.getZ() - longbowman.getZ(), target.getX() - longbowman.getX()) * Mth.RAD_TO_DEG) - 90.0F;
		longbowman.setYRot(LostLogic.turnTowards(longbowman.getYRot(), yaw, TURN));
		longbowman.setYBodyRot(longbowman.getYRot());
		longbowman.setYHeadRot(longbowman.getYRot());
		longbowman.getLookControl().setLookAt(target, 30.0F, 30.0F);
		if (--nextShot <= 0) {
			LongbowmanSettings.Barrage barrage = LostLongbowman.longbowman().barrage();
			longbowman.shootBarrage(level, target, barrage);
			nextShot = barrage.interval();
			shotsLeft--;
		}
	}
}
