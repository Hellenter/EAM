package io.github.hellenter.eam.mobs.entity.koschei;

import io.github.hellenter.eam.mobs.entity.koschei.KoscheiLogic.Attack;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiLogic.Choice;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Walks Koschei up to his target, sidesteps while in reach, hops aside now and then and in at it from a few blocks off, vanishes to stand behind it and starts
 * his attacks. A target that keeps out of his reach gets the bolt, or finds him behind it; one up on a pillar finds him
 * standing by it, is thrown off with the burst of force or held in the bolt.
 */
final class KoscheiFightGoal extends Goal {
	private static final int REPATH_INTERVAL = 10;

	private final Koschei koschei;
	private LivingEntity target;
	private int strafeTicks;
	private boolean strafeRight;
	/** Ticks in a row the target has kept out of his reach. */
	private int awayTicks;

	KoscheiFightGoal(Koschei koschei) {
		this.koschei = koschei;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity candidate = koschei.getTarget();
		if (koschei.phase() != 1 || candidate == null || !candidate.isAlive()) {
			return false;
		}
		target = candidate;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return canUse() || koschei.isBusy() && koschei.phase() == 1;
	}

	@Override
	public void start() {
		koschei.setAggressive(true);
	}

	@Override
	public void stop() {
		target = null;
		awayTicks = 0;
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
		if (koschei.answerHeight(target)) {
			// Out of his reach above him: he stands by it up there, throws it off or holds it in the bolt.
			awayTicks = 0;
			return;
		}
		awayTicks = distance > one.reach() + 1.0 ? awayTicks + 1 : 0;
		if (awayTicks >= settings.teleport().retreatTicks() && koschei.readyToAttack()) {
			// It keeps backing off: the bolt after it, or he stands at its back.
			awayTicks = 0;
			koschei.getMoveControl().strafe(0.0F, 0.0F);
			int[] cooldowns = koschei.cooldowns();
			if (KoscheiLogic.answerRetreat(distance, sees, cooldowns[Attack.BOLT.ordinal()], koschei.boltBlocked(target), one.bolt()) != null) {
				koschei.startAttack(Attack.BOLT, target);
			} else {
				koschei.startTeleport(target);
			}
			return;
		}

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
			float flankChance = settings.hop().flankChance() * (inReach ? 1.0F : 0.2F);
			if (distance <= settings.hop().flankRange() && koschei.flankReady() && koschei.getRandom().nextFloat() < flankChance
					&& koschei.flankDash(target)) {
				// Round to its side or back, and the blow follows on landing.
				koschei.getMoveControl().strafe(0.0F, 0.0F);
				return;
			}
			if (KoscheiLogic.callsRavens(distance, koschei.cooldowns()[Attack.RAVENS.ordinal()], settings.ravens(), koschei.getRandom().nextDouble())) {
				koschei.getMoveControl().strafe(0.0F, 0.0F);
				koschei.startAttack(Attack.RAVENS, target);
				return;
			}
			Choice choice = KoscheiLogic.choose(distance, Koschei.shieldReady(target), koschei.shieldTicks(target), koschei.lungeFails(),
					koschei.cooldowns(), koschei.lastAttack(), koschei.boltBlocked(target), koschei.boltHungry(), one,
					koschei.getRandom()::nextDouble);
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
			KoscheiSettings.Hop hop = settings.hop();
			if (sees && distance >= hop.inFrom() && koschei.hopCooldown() <= 0 && Math.abs(target.getY() - koschei.getY()) < 1.5
					&& koschei.getRandom().nextFloat() < hop.inChance()) {
				// Closing in as the Fallen Lord does: a short hop at the target.
				koschei.hopIn(target);
				return;
			}
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
