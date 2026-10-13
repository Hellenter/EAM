package io.github.hellenter.eam.mobs.entity.warg;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A pack member's part in the hunt: circle the player at its place in the ring, and when its leader gives it the turn,
 * dash in for one bite. If the player turns to face it before it is close, it backs off.
 */
final class WargPackGoal extends Goal {
	private static final double CIRCLE_SPEED = 1.15;
	private static final double STRIKE_SPEED = 1.5;
	/** Close enough to its place in the ring to stop and watch. */
	private static final double IN_PLACE = 1.5;

	private final Warg warg;
	private int recalc;

	WargPackGoal(Warg warg) {
		this.warg = warg;
		setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		return !warg.isBroken() && !warg.fightsAlone() && warg.packTarget() != null;
	}

	@Override
	public boolean canContinueToUse() {
		return canUse();
	}

	@Override
	public void start() {
		recalc = 0;
		warg.setAggressive(true);
	}

	@Override
	public void stop() {
		warg.setAggressive(false);
		warg.setTarget(null);
		warg.endStrike(false);
		warg.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity target = warg.packTarget();
		if (target == null) {
			return;
		}
		warg.setTarget(target);
		warg.getLookControl().setLookAt(target, 30.0F, 30.0F);
		if (warg.isStriker()) {
			strike(target);
		} else {
			circle(target);
		}
	}

	private void strike(LivingEntity target) {
		double abort = Warg.settings().abortDistance();
		if (warg.distanceToSqr(target) > abort * abort && warg.watchedBy(target)) {
			// Spotted on the way in: back to the ring, and another tries.
			warg.endStrike(false);
			return;
		}
		if (warg.isWithinMeleeAttackRange(target) && warg.getSensing().hasLineOfSight(target)) {
			warg.bite(getServerLevel(warg), target);
			recalc = 0;
			return;
		}
		if (--recalc <= 0) {
			recalc = 4;
			warg.getNavigation().moveTo(target, STRIKE_SPEED);
		}
	}

	private void circle(LivingEntity target) {
		Vec3 spot = warg.ringSpot(target);
		if (warg.position().distanceToSqr(spot.x, warg.getY(), spot.z) < IN_PLACE * IN_PLACE) {
			warg.getNavigation().stop();
			return;
		}
		if (--recalc <= 0 || warg.getNavigation().isDone()) {
			recalc = 8;
			warg.getNavigation().moveTo(spot.x, spot.y, spot.z, CIRCLE_SPEED);
		}
	}

	private static ServerLevel getServerLevel(Warg warg) {
		return (ServerLevel) warg.level();
	}
}
