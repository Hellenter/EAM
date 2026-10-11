package io.github.hellenter.eam.mobs.entity.cavalry;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A mounted cavalryman between his attacks: he keeps his distance, riding round the player at a canter, and decides
 * when to charge, to follow his partner's charge or to throw his lance. The charge and the throw themselves are his
 * actions, which steer the horse.
 */
final class CavalryMountedGoal extends Goal {
	private static final int REPATH_INTERVAL = 10;
	private static final int REACH_CHECK_INTERVAL = 20;
	/** How far round the player he moves his aim each time he picks the next point of his circle, in radians. */
	private static final double ORBIT_STEP = 0.6;

	private final LostCavalryman man;
	private LivingEntity target;
	private boolean reachable = true;
	private double keepDistance;
	private boolean clockwise;

	CavalryMountedGoal(LostCavalryman man) {
		this.man = man;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity candidate = man.getTarget();
		if (man.horse() == null || candidate == null || !candidate.isAlive()) {
			return false;
		}
		target = candidate;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return canUse() || man.horse() != null && man.isBusy();
	}

	@Override
	public void start() {
		man.setAggressive(true);
		pickOrbit();
	}

	@Override
	public void stop() {
		target = null;
		man.setAggressive(false);
		man.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	private void pickOrbit() {
		keepDistance = LostCavalryLogic.keepDistance(LostCavalryman.cavalry().lance(), man.covering(), man.getRandom().nextDouble());
		clockwise = man.getRandom().nextBoolean();
	}

	@Override
	public void tick() {
		if (man.isBusy() || target == null || !target.isAlive() || target != man.getTarget()) {
			return;
		}
		man.getLookControl().setLookAt(target, 30.0F, 30.0F);
		LostCavalrySettings settings = LostCavalryman.cavalry();
		double distance = man.horizontalDistanceTo(target);
		if (man.tickCount % REACH_CHECK_INTERVAL == 0) {
			reachable = canReach();
		}
		if (man.hasLance()) {
			if (man.followUpDue()) {
				man.startCharge(target);
				return;
			}
			double throwRoll = man.tickCount % 20 == 0 ? man.getRandom().nextDouble() : 1.0;
			switch (LostCavalryLogic.chooseMounted(distance, reachable && man.hasLineOfSight(target), man.chargeCooldown(), man.throwCooldown(),
					man.sparesLeft(), settings.lance(), settings.throwing(), throwRoll)) {
				case CHARGE -> {
					man.startCharge(target);
					pickOrbit();
					return;
				}
				case THROW -> {
					man.startThrow(target);
					return;
				}
				case RIDE -> {
				}
			}
		}
		ride(settings, distance);
	}

	/** Rides round the player at his distance, a little further round each time. */
	private void ride(LostCavalrySettings settings, double distance) {
		if (man.tickCount % REPATH_INTERVAL != 0 && !man.getNavigation().isDone()) {
			return;
		}
		double angle = Mth.atan2(man.getZ() - target.getZ(), man.getX() - target.getX()) + (clockwise ? ORBIT_STEP : -ORBIT_STEP);
		// Too close, he turns away first; far off, he comes straight back.
		double radius = distance < keepDistance * 0.6 ? keepDistance * 1.2 : keepDistance;
		Vec3 point = new Vec3(target.getX() + Math.cos(angle) * radius, target.getY(), target.getZ() + Math.sin(angle) * radius);
		double speed = LostCavalryLogic.modifierFor(settings.speed().canter() / settings.speed().gallop());
		if (!man.getNavigation().moveTo(point.x, point.y, point.z, speed)) {
			// Nowhere to go round this way: try the other.
			clockwise = !clockwise;
		}
	}

	/** Whether the horse can get to the target: not far above or below, and a path there. */
	private boolean canReach() {
		if (Math.abs(target.getY() - man.getY()) > 3.0) {
			return false;
		}
		Path path = man.getNavigation().createPath(target, 1);
		return path != null && path.canReach();
	}
}
