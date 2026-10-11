package io.github.hellenter.eam.mobs.entity.cavalry;

import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightSettings;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * An unhorsed cavalryman on foot: walks up behind his shield, slower than a skeleton, and runs for a few seconds when the
 * player keeps out of reach. In reach he picks an axe move by the player's shield: plain cuts to knock it away, now and
 * then a heavy strike through it or a sweep round it, and quick double cuts at a player with no shield ready.
 */
final class CavalryFootGoal extends Goal {
	private static final int REPATH_INTERVAL = 10;

	private final LostCavalryman man;
	private LivingEntity target;
	private int chaseTicks;
	private int strafeTicks;
	private boolean strafeRight;

	CavalryFootGoal(LostCavalryman man) {
		this.man = man;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity candidate = man.getTarget();
		if (!man.isOnFoot() || man.isPassenger() || candidate == null || !candidate.isAlive()) {
			return false;
		}
		target = candidate;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return canUse() || man.isOnFoot() && man.isBusy();
	}

	@Override
	public void start() {
		man.setAggressive(true);
		chaseTicks = 0;
	}

	@Override
	public void stop() {
		target = null;
		man.setAggressive(false);
		man.getNavigation().stop();
		man.getMoveControl().strafe(0.0F, 0.0F);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (man.isBusy() || target == null || !target.isAlive() || target != man.getTarget()) {
			return;
		}
		man.getLookControl().setLookAt(target, 30.0F, 30.0F);
		double distance = man.horizontalDistanceTo(target);
		SkeletonKnightSettings axe = man.settings();
		LostCavalrySettings settings = LostCavalryman.cavalry();

		chaseTicks = distance > axe.reach() + 1.0 ? chaseTicks + 1 : 0;
		if (!man.isSprintingNow() && LostCavalryLogic.shouldSprint(chaseTicks, sprintCooldown(), settings.speed())) {
			man.startSprint();
		}

		boolean sees = man.getSensing().hasLineOfSight(target);
		if (sees && man.isIdleFor(axe.attackPause()) && Math.abs(target.getY() - man.getY()) < 2.5) {
			LostCavalryLogic.FootAttack attack = LostCavalryLogic.chooseFoot(distance, LostCavalryman.shieldReady(target), man.heavyCooldown(),
					man.doubleCooldown(), man.sweepCooldown(), axe, settings.moves(), man.getRandom().nextDouble(), man.getRandom().nextDouble());
			if (attack != null) {
				man.getMoveControl().strafe(0.0F, 0.0F);
				chaseTicks = 0;
				man.startFootAttack(attack, target);
				return;
			}
		}

		if (distance > axe.reach() - 0.4 || !sees) {
			if (man.tickCount % REPATH_INTERVAL == 0 || man.getNavigation().isDone()) {
				double speed = man.isSprintingNow() ? LostCavalryLogic.modifierFor(settings.speed().sprint()) : 1.0;
				man.getNavigation().moveTo(target, speed);
			}
			return;
		}
		// In reach, waiting for the next move: shield up, a slow step to one side.
		man.getNavigation().stop();
		if (--strafeTicks <= 0) {
			strafeTicks = 20 + man.getRandom().nextInt(30);
			strafeRight = man.getRandom().nextBoolean();
		}
		man.getMoveControl().strafe(distance < 1.4 ? -0.4F : 0.0F, strafeRight ? 0.3F : -0.3F);
		man.lookAt(target, 30.0F, 30.0F);
	}

	private int sprintCooldown() {
		return man.sprintCooldownLeft();
	}
}
