package io.github.hellenter.eam.mobs.entity.guard;

import io.github.hellenter.eam.mobs.entity.guard.PillagerGuardLogic.Attack;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * Now and then starts a guard's onslaught or axe throw at his target; between them his plain axe swings (the vindicator
 * melee goal) do the work. Holds his movement while a move plays out.
 */
final class GuardMovesGoal extends Goal {
	private final PillagerGuard guard;
	private @Nullable LivingEntity target;
	private @Nullable Attack attack;
	private int nextCheck;

	GuardMovesGoal(PillagerGuard guard) {
		this.guard = guard;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (guard.isBusy()) {
			return false;
		}
		LivingEntity candidate = guard.getTarget();
		if (candidate == null || !candidate.isAlive() || guard.tickCount < nextCheck) {
			return false;
		}
		PillagerGuardSettings settings = PillagerGuardModule.settings();
		nextCheck = guard.tickCount + settings.decideEvery();
		if (Math.abs(candidate.getY() - guard.getY()) >= 2.5 || !guard.getSensing().hasLineOfSight(candidate)) {
			return false;
		}
		attack = PillagerGuardLogic.choose(guard.horizontalDistanceTo(candidate), guard.onslaughtCooldown(), guard.throwCooldown(),
				guard.hasAxe(), settings, guard.getRandom()::nextDouble);
		target = candidate;
		return attack != null;
	}

	@Override
	public boolean canContinueToUse() {
		return guard.isBusy();
	}

	@Override
	public void start() {
		if (attack != null && target != null) {
			guard.getNavigation().stop();
			guard.setAggressive(true);
			guard.startAttack(attack, target);
		}
	}

	@Override
	public void stop() {
		target = null;
		attack = null;
	}
}
