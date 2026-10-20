package io.github.hellenter.eam.mobs.entity.koschei;

import io.github.hellenter.eam.mobs.entity.koschei.KoscheiLogic.DaggerAttack;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Koschei in his third phase, with the daggers: he comes straight at his target, slow at first but faster the longer it
 * keeps out of his reach, darting in at it, and to the sides and back while he waits in reach; now and then he dashes
 * round to its back or vanishes to stand behind or before it, and the blow follows at once. He never backs off.
 */
final class KoscheiDaggerGoal extends Goal {
	private static final int REPATH_INTERVAL = 5;
	/** He darts in at a target at least this far off. */
	private static final double DART_IN_FROM = 4.0;
	/** He vanishes to a target that has kept out of his reach this long, if he can. */
	private static final int AWAY_TICKS = 100;

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
		if (koschei.raisePending()) {
			// Getting away to raise his draugr: no coming back in meanwhile.
			koschei.getNavigation().stop();
			if (koschei.readyToAttack() && koschei.onGround()) {
				koschei.continueRaise(target);
			}
			return;
		}
		PhaseThreeSettings three = koschei.phaseThree();
		PhaseThreeSettings.Moves moves = three.moves();
		double distance = koschei.horizontalDistanceTo(target);
		boolean sees = koschei.getSensing().hasLineOfSight(target);
		boolean inReach = distance <= three.combo().reach() + 0.5;
		koschei.noteReach(inReach && sees);
		boolean level = Math.abs(target.getY() - koschei.getY()) < 2.5;
		if (koschei.readyToAttack()) {
			if (koschei.teleportReady() && (koschei.outOfReach() >= AWAY_TICKS || sees && distance > 2.0
					&& koschei.getRandom().nextFloat() < moves.teleportChance())) {
				koschei.getNavigation().stop();
				koschei.startTeleportThree(target);
				return;
			}
			if (sees && level && distance <= moves.flankRange() && koschei.flankReady() && koschei.getRandom().nextFloat() < moves.flankChance()
					&& koschei.flankInPhaseThree(target)) {
				return;
			}
			DaggerAttack attack = level ? koschei.chooseDagger(distance, sees) : null;
			if (attack != null) {
				koschei.getNavigation().stop();
				koschei.startDagger(attack, target);
				return;
			}
		}
		if (sees && level && koschei.dartReady() && koschei.getRandom().nextFloat() < moves.dartChance()) {
			if (inReach) {
				// Waiting for his next blow: a dart to one side or back.
				koschei.dart(target, false);
				return;
			}
			if (distance >= DART_IN_FROM) {
				koschei.dart(target, true);
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
