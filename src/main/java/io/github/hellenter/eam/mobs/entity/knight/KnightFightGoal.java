package io.github.hellenter.eam.mobs.entity.knight;

import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightLogic.Attack;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Walks the skeleton knight up to his target with the shield raised, sidesteps while in reach and starts his attacks.
 */
final class KnightFightGoal extends Goal {
	private static final int REPATH_INTERVAL = 10;

	private final SkeletonKnight knight;
	private LivingEntity target;
	private int strafeTicks;
	private boolean strafeRight;

	KnightFightGoal(SkeletonKnight knight) {
		this.knight = knight;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity candidate = knight.getTarget();
		if (candidate == null || !candidate.isAlive()) {
			return false;
		}
		target = candidate;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return canUse() || knight.isBusy();
	}

	@Override
	public void start() {
		knight.setAggressive(true);
	}

	@Override
	public void stop() {
		target = null;
		knight.setAggressive(false);
		knight.getNavigation().stop();
		knight.getMoveControl().strafe(0.0F, 0.0F);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (knight.isBusy() || target == null || !target.isAlive() || target != knight.getTarget()) {
			return;
		}
		knight.getLookControl().setLookAt(target, 30.0F, 30.0F);
		double distance = knight.horizontalDistanceTo(target);
		SkeletonKnightSettings settings = SkeletonKnightModule.settings();

		boolean sees = knight.getSensing().hasLineOfSight(target);
		if (sees && knight.isIdleFor(settings.attackPause()) && Math.abs(target.getY() - knight.getY()) < 2.5) {
			Attack attack = SkeletonKnightLogic.choose(distance, target.isBlocking(), knight.comboCooldown(), knight.heavyCooldown(),
					settings, knight.getRandom().nextDouble(), knight.getRandom().nextDouble());
			if (attack != null) {
				knight.getMoveControl().strafe(0.0F, 0.0F);
				knight.startAttack(attack, target);
				return;
			}
		}

		if (distance > settings.reach() - 0.4 || !sees) {
			if (knight.tickCount % REPATH_INTERVAL == 0 || knight.getNavigation().isDone()) {
				knight.getNavigation().moveTo(target, 1.0);
			}
			return;
		}
		// In reach, waiting for the next attack: shield up, a slow step to one side.
		knight.getNavigation().stop();
		if (--strafeTicks <= 0) {
			strafeTicks = 20 + knight.getRandom().nextInt(30);
			strafeRight = knight.getRandom().nextBoolean();
		}
		knight.getMoveControl().strafe(distance < 1.4 ? -0.4F : 0.0F, strafeRight ? 0.3F : -0.3F);
		knight.lookAt(target, 30.0F, 30.0F);
	}
}
