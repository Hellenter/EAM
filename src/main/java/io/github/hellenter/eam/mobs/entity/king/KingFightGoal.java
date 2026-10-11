package io.github.hellenter.eam.mobs.entity.king;

import io.github.hellenter.eam.mobs.entity.king.PillagerKingLogic.Attack;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Walks the Pillager King up to his target, dashes in from a few blocks off, sidesteps while in reach, hops back now
 * and then and starts his attacks.
 */
final class KingFightGoal extends Goal {
	private static final int REPATH_INTERVAL = 10;

	private final PillagerKing king;
	private LivingEntity target;
	private int strafeTicks;
	private boolean strafeRight;

	KingFightGoal(PillagerKing king) {
		this.king = king;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity candidate = king.getTarget();
		if (candidate == null || !candidate.isAlive()) {
			return false;
		}
		target = candidate;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return canUse() || king.isBusy();
	}

	@Override
	public void start() {
		king.setAggressive(true);
	}

	@Override
	public void stop() {
		target = null;
		king.setAggressive(false);
		king.getNavigation().stop();
		king.getMoveControl().strafe(0.0F, 0.0F);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (king.isBusy() || target == null || !target.isAlive() || target != king.getTarget()) {
			return;
		}
		king.getLookControl().setLookAt(target, 30.0F, 30.0F);
		if (king.isDashing()) {
			return;
		}
		double distance = king.horizontalDistanceTo(target);
		PillagerKingSettings settings = PillagerKingModule.settings();
		boolean sees = king.getSensing().hasLineOfSight(target);
		boolean inReach = distance <= settings.reach();

		if (sees && king.readyToAttack() && Math.abs(target.getY() - king.getY()) < 2.5) {
			if (inReach && !king.followUp() && king.hopCooldown() <= 0 && king.getRandom().nextDouble() < settings.hop().chance()) {
				king.hopBack(target, settings.hop());
				return;
			}
			Attack attack = king.followUp()
					? (inReach ? PillagerKingLogic.chooseMelee(false, king.cooldowns(), settings, king.getRandom()::nextDouble) : null)
					: PillagerKingLogic.choose(distance, target.isBlocking(), king.outOfReach(), king.cooldowns(), settings,
							king.getRandom()::nextDouble);
			if (attack != null) {
				king.getMoveControl().strafe(0.0F, 0.0F);
				king.startAttack(attack, target);
				return;
			}
			if (PillagerKingLogic.dashIn(distance, king.dashCooldown(), settings.dash())
					|| king.followUp() && king.dashCooldown() <= 0 && distance <= settings.dash().to()) {
				king.getNavigation().stop();
				king.dashAt(target);
				return;
			}
		}

		if (distance > settings.reach() - 0.4 || !sees) {
			if (king.tickCount % REPATH_INTERVAL == 0 || king.getNavigation().isDone()) {
				king.getNavigation().moveTo(target, 1.0);
			}
			return;
		}
		// In reach, waiting for the next attack: a slow step to one side.
		king.getNavigation().stop();
		if (--strafeTicks <= 0) {
			strafeTicks = 20 + king.getRandom().nextInt(30);
			strafeRight = king.getRandom().nextBoolean();
		}
		king.getMoveControl().strafe(distance < 1.6 ? -0.4F : 0.0F, strafeRight ? 0.3F : -0.3F);
		king.lookAt(target, 30.0F, 30.0F);
	}
}
