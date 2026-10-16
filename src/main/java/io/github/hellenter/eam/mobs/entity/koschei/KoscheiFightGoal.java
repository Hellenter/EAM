package io.github.hellenter.eam.mobs.entity.koschei;

import io.github.hellenter.eam.mobs.entity.koschei.KoscheiLogic.Choice;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Walks Koschei up to his target, sidesteps while in reach, hops aside now and then, vanishes to stand behind it and starts
 * his attacks.
 */
final class KoscheiFightGoal extends Goal {
	private static final int REPATH_INTERVAL = 10;

	private final Koschei koschei;
	private LivingEntity target;
	private int strafeTicks;
	private boolean strafeRight;

	KoscheiFightGoal(Koschei koschei) {
		this.koschei = koschei;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity candidate = koschei.getTarget();
		if (candidate == null || !candidate.isAlive()) {
			return false;
		}
		target = candidate;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return canUse() || koschei.isBusy();
	}

	@Override
	public void start() {
		koschei.setAggressive(true);
	}

	@Override
	public void stop() {
		target = null;
		koschei.setAggressive(false);
		koschei.getNavigation().stop();
		koschei.getMoveControl().strafe(0.0F, 0.0F);
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
		KoscheiSettings settings = KoscheiModule.settings();
		KoscheiSettings.PhaseOne one = settings.phaseOne();
		boolean sees = koschei.getSensing().hasLineOfSight(target);
		boolean inReach = distance <= one.reach();

		if (sees && koschei.readyToAttack() && Math.abs(target.getY() - koschei.getY()) < 2.5) {
			if (koschei.teleportReady() && distance > 2.0 && koschei.getRandom().nextFloat() < settings.teleport().chance()) {
				koschei.getMoveControl().strafe(0.0F, 0.0F);
				koschei.startTeleport(target);
				return;
			}
			if (inReach && koschei.hopCooldown() <= 0 && koschei.getRandom().nextFloat() < settings.hop().chance()) {
				koschei.hopAround(target);
				return;
			}
			Choice choice = KoscheiLogic.choose(distance, Koschei.shieldReady(target), koschei.shieldTicks(target), koschei.lungeFails(),
					koschei.cooldowns(), koschei.lastAttack(), one, koschei.getRandom()::nextDouble);
			if (choice.lungeFailed()) {
				koschei.lungeFailed();
			}
			if (choice.attack() != null) {
				koschei.getMoveControl().strafe(0.0F, 0.0F);
				koschei.startAttack(choice.attack(), target);
				return;
			}
		}

		if (distance > one.reach() - 0.4 || !sees) {
			if (koschei.tickCount % REPATH_INTERVAL == 0 || koschei.getNavigation().isDone()) {
				koschei.getNavigation().moveTo(target, 1.0);
			}
			return;
		}
		// In reach, waiting for the next attack: a slow step to one side.
		koschei.getNavigation().stop();
		if (--strafeTicks <= 0) {
			strafeTicks = 20 + koschei.getRandom().nextInt(30);
			strafeRight = koschei.getRandom().nextBoolean();
		}
		koschei.getMoveControl().strafe(distance < 1.6 ? -0.4F : 0.0F, strafeRight ? 0.3F : -0.3F);
	}
}
