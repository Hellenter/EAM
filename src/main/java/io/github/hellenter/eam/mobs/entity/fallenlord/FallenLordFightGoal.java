package io.github.hellenter.eam.mobs.entity.fallenlord;

import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic.Attack;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic.Situation;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Moves the Fallen Lord around his target and starts attacks. Phase one is a duel: he keeps a few blocks away,
 * circles with the shield up and steps in to jab. Phase two he runs straight at the target.
 */
final class FallenLordFightGoal extends Goal {
	/** Phase one keeps the target between these distances. */
	private static final double DUEL_MIN = 3.0;
	private static final double DUEL_MAX = 5.0;
	private static final int REPATH_INTERVAL = 10;

	private final FallenLord lord;
	private LivingEntity target;
	private int strafeTicks;
	private boolean strafeRight;

	FallenLordFightGoal(FallenLord lord) {
		this.lord = lord;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity candidate = lord.getTarget();
		if (candidate == null || !candidate.isAlive()) {
			return false;
		}
		target = candidate;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return canUse() || lord.isBusy();
	}

	@Override
	public void stop() {
		target = null;
		lord.getNavigation().stop();
		lord.getMoveControl().strafe(0.0F, 0.0F);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		// Running goals tick between re-checks, so the target may have died since canContinueToUse.
		if (lord.isBusy() || target == null || !target.isAlive() || target != lord.getTarget()) {
			return;
		}
		lord.getLookControl().setLookAt(target, 30.0F, 30.0F);
		double distance = lord.horizontalDistanceTo(target);
		int phase = lord.phase();

		lord.tryClimb(target);
		lord.breakThrough(target);
		if (lord.canLeapUp(target)) {
			// Out of reach on a pillar or a ledge: he leaps up at them.
			lord.getMoveControl().strafe(0.0F, 0.0F);
			lord.startLeapUp(target);
			return;
		}
		// Higher still, or no leap ready: his weapon is thrown at them, and no swings at the air below them.
		boolean above = lord.targetAboveReach(target);
		if (lord.isBusy()) {
			lord.getMoveControl().strafe(0.0F, 0.0F);
			return;
		}

		FallenLordSettings settings = FallenLordModule.settings();
		int pause = phase == 1 ? settings.phaseOne().attackPause() : settings.phaseTwo().attackPause();
		if (lord.isIdleFor(pause) && lord.wantsToDrink()) {
			lord.startDrinking(target);
			return;
		}
		if (!above && lord.hasWeapon() && lord.isIdleFor(pause)) {
			int crowd = phase == 2 ? lord.playersWithin(settings.phaseTwo().whirlRadius()).size() : 0;
			Situation situation = new Situation(distance, lord.getSensing().hasLineOfSight(target), lord.hasSpear(),
					lord.ticksOutOfReach(), crowd, lord.ticksSinceHit(), lord.hopBackPending(), target.isBlocking());
			Attack attack = FallenLordLogic.choose(phase, situation, lord.cooldowns(), lord.lastAttack(), settings,
					lord.getRandom().nextDouble());
			if (attack != null) {
				lord.getMoveControl().strafe(0.0F, 0.0F);
				lord.startAttack(attack, target);
				return;
			}
		}

		if (phase == 1) {
			duel(distance);
		} else {
			chase(distance);
		}
	}

	private void duel(double distance) {
		if (distance > DUEL_MAX + 2.0 || !lord.getSensing().hasLineOfSight(target)) {
			walkTo(1.0);
			return;
		}
		lord.getNavigation().stop();
		if (--strafeTicks <= 0) {
			strafeTicks = 30 + lord.getRandom().nextInt(40);
			strafeRight = lord.getRandom().nextBoolean();
		}
		boolean jabReady = lord.cooldowns()[Attack.POKE.ordinal()] <= 0 && lord.hasSpear();
		float forward;
		if (distance < DUEL_MIN) {
			forward = -0.5F;
		} else if (distance > DUEL_MAX || jabReady && distance > FallenLordLogic.POKE_REACH - 0.5) {
			// Steps in when the jab is ready, keeps away while it is not.
			forward = 0.5F;
		} else {
			forward = 0.0F;
		}
		lord.getMoveControl().strafe(forward, strafeRight ? 0.4F : -0.4F);
		lord.lookAt(target, 30.0F, 30.0F);
	}

	private void chase(double distance) {
		if (distance > 1.8) {
			walkTo(FallenLordModule.settings().phaseTwo().speed());
		} else {
			lord.getNavigation().stop();
		}
	}

	/**
	 * Walks the path to the target; with no path, as when a wall cuts them off, straight at them, so he comes up to the
	 * wall and can jump it.
	 */
	private void walkTo(double speed) {
		if (lord.tickCount % REPATH_INTERVAL == 0 || lord.getNavigation().isDone()) {
			if (!lord.getNavigation().moveTo(target, speed)) {
				lord.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), speed);
			}
		}
	}
}
