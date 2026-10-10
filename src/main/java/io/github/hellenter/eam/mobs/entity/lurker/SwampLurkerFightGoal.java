package io.github.hellenter.eam.mobs.entity.lurker;

import io.github.hellenter.eam.mobs.entity.lurker.SwampLurkerLogic.Move;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Hunts the swamp lurker's target: runs or leaps at it, swipes, leaps away and circles it until it may go in again.
 */
final class SwampLurkerFightGoal extends Goal {
	private static final int REPATH_INTERVAL = 10;
	/** Speed while running at the target on all fours. */
	private static final double RUN_SPEED = 1.3;

	private final SwampLurker lurker;
	private LivingEntity target;
	private int circleTicks;
	private boolean circleRight;

	SwampLurkerFightGoal(SwampLurker lurker) {
		this.lurker = lurker;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		LivingEntity candidate = lurker.getTarget();
		if (candidate == null || !candidate.isAlive()) {
			return false;
		}
		target = candidate;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return canUse() || lurker.isBusy();
	}

	@Override
	public void start() {
		lurker.setAggressive(true);
	}

	@Override
	public void stop() {
		target = null;
		lurker.setAggressive(false);
		lurker.getNavigation().stop();
		lurker.getMoveControl().strafe(0.0F, 0.0F);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (lurker.isBusy() || target == null || !target.isAlive() || target != lurker.getTarget()) {
			return;
		}
		lurker.getLookControl().setLookAt(target, 30.0F, 30.0F);
		double distance = lurker.horizontalDistanceTo(target);
		boolean sees = lurker.getSensing().hasLineOfSight(target);
		Move move = SwampLurkerLogic.choose(distance, target.getY() - lurker.getY(), lurker.attackCooldown(), lurker.leapCooldown(),
				lurker.canLeap() && sees, SwampLurkerModule.settings());
		switch (move) {
			case COMBO -> {
				if (sees) {
					lurker.startCombo(target);
				}
			}
			case LEAP_IN -> lurker.leapAt(target);
			case LEAP_BACK -> lurker.leapAway(target);
			case APPROACH -> {
				if (lurker.tickCount % REPATH_INTERVAL == 0 || lurker.getNavigation().isDone()) {
					lurker.getNavigation().moveTo(target, RUN_SPEED);
				}
			}
			case PROWL -> prowl(distance);
		}
	}

	/** Circles the target at about the retreat distance, waiting for its next attack. */
	private void prowl(double distance) {
		lurker.getNavigation().stop();
		if (--circleTicks <= 0) {
			circleTicks = 20 + lurker.getRandom().nextInt(30);
			circleRight = lurker.getRandom().nextBoolean();
		}
		double keep = SwampLurkerModule.settings().leap().retreatDistance();
		float forward = distance < keep - 1.5 ? -0.5F : distance > keep + 1.5 ? 0.5F : 0.0F;
		lurker.getMoveControl().strafe(forward, circleRight ? 0.5F : -0.5F);
		lurker.lookAt(target, 30.0F, 30.0F);
	}
}
