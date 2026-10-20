package io.github.hellenter.eam.mobs.entity.koschei;

import io.github.hellenter.eam.mobs.entity.koschei.KoscheiLogic.DaggerAttack;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Koschei in his third phase, with the daggers: he comes straight at his target, slow at first but faster the longer it
 * keeps out of his reach, and starts his dagger attacks; he never backs off.
 */
final class KoscheiDaggerGoal extends Goal {
	private static final int REPATH_INTERVAL = 5;

	private final Koschei koschei;
	private LivingEntity target;
	private int repath;

	KoscheiDaggerGoal(Koschei koschei) {
		this.koschei = koschei;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity candidate = koschei.getTarget();
		if (koschei.phase() != 3 || candidate == null || !candidate.isAlive()) {
			return false;
		}
		target = candidate;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return canUse() || koschei.isBusy() && koschei.phase() == 3;
	}

	@Override
	public void start() {
		koschei.setAggressive(true);
		repath = 0;
	}

	@Override
	public void stop() {
		target = null;
		koschei.setAggressive(false);
		koschei.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (koschei.isBusy() || target == null || !target.isAlive() || target != koschei.getTarget()) {
			return;
		}
		koschei.getLookControl().setLookAt(target, 30.0F, 30.0F);
		double distance = koschei.horizontalDistanceTo(target);
		boolean sees = koschei.getSensing().hasLineOfSight(target);
		koschei.noteReach(distance <= koschei.phaseThree().daggers().reach() + 0.5 && sees);
		if (koschei.readyToAttack()) {
			DaggerAttack attack = koschei.chooseDagger(distance, sees);
			if (attack != null) {
				koschei.getNavigation().stop();
				koschei.startDagger(attack, target);
				return;
			}
		}
		if (--repath <= 0 || koschei.getNavigation().isDone()) {
			repath = REPATH_INTERVAL;
			if (distance > 1.2) {
				koschei.getNavigation().moveTo(target, koschei.chaseSpeed());
			} else {
				koschei.getNavigation().stop();
			}
		}
	}
}
