package io.github.hellenter.eam.mobs.entity.koschei;

import io.github.hellenter.eam.mobs.entity.koschei.KoscheiLogic.StaffAttack;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Koschei in his second phase, with the staff: as Baba Yaga in hers he keeps his distance from his target, runs from it
 * when it comes close, vanishes and stands far off, hops aside, and starts his staff attacks. With the trap laid he stands
 * still instead and lets the target walk right up to him; behind his shield bearers he neither vanishes nor hops.
 */
final class KoscheiStaffGoal extends Goal {
	private static final int REPATH_INTERVAL = 10;
	/** He judges how fast the target closes in over this many ticks. */
	private static final int RUSH_TICKS = 10;

	private final Koschei koschei;
	private LivingEntity target;
	private int repath;
	/** The target's distance over the last ticks, newest last, to tell when it runs at him. */
	private final double[] distances = new double[RUSH_TICKS];
	private int samples;

	KoscheiStaffGoal(Koschei koschei) {
		this.koschei = koschei;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity candidate = koschei.getTarget();
		if (koschei.phase() < 2 || candidate == null || !candidate.isAlive()) {
			return false;
		}
		target = candidate;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return canUse() || koschei.isBusy() && koschei.phase() >= 2;
	}

	@Override
	public void start() {
		koschei.setAggressive(true);
	}

	@Override
	public void stop() {
		target = null;
		samples = 0;
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
			samples = 0;
			return;
		}
		koschei.getLookControl().setLookAt(target, 30.0F, 30.0F);
		PhaseTwoSettings two = koschei.phaseTwo();
		double distance = koschei.horizontalDistanceTo(target);
		boolean sees = koschei.getSensing().hasLineOfSight(target);
		boolean rushing = rushing(distance, two.iceWall().rushSpeed());
		if (koschei.trapLaid()) {
			// Standing his ground, letting it come.
			koschei.getNavigation().stop();
			if (distance <= two.trap().triggerRange() && Math.abs(target.getY() - koschei.getY()) < 2.5) {
				koschei.triggerTrap(target);
			}
			return;
		}
		PhaseTwoSettings.Kite kite = two.kite();
		// Behind his shield bearers he keeps still in the row's shadow: no vanishing, no hops.
		boolean turtling = koschei.turtling();
		if (!turtling && distance < kite.minDistance() && koschei.teleportReady() && koschei.getRandom().nextFloat() < two.teleport().chance()) {
			koschei.startTeleportAway(target);
			return;
		}
		if (!turtling && distance < kite.minDistance() && koschei.hopCooldown() <= 0 && koschei.getRandom().nextFloat() < kite.hopChance()) {
			koschei.hopAround(target);
			return;
		}
		if (koschei.readyToAttack()) {
			StaffAttack attack = koschei.chooseStaff(target, distance, sees, rushing);
			if (attack != null) {
				koschei.getNavigation().stop();
				koschei.startStaff(attack, target);
				return;
			}
		}
		keepDistance(distance, sees, kite);
	}

	/** Whether the target has closed in on him at least {@code speed} blocks a tick lately: it runs at him. */
	private boolean rushing(double distance, double speed) {
		System.arraycopy(distances, 1, distances, 0, RUSH_TICKS - 1);
		distances[RUSH_TICKS - 1] = distance;
		samples = Math.min(RUSH_TICKS, samples + 1);
		return samples == RUSH_TICKS && (distances[0] - distance) / (RUSH_TICKS - 1) >= speed;
	}

	/** Away from a target that comes too close, fast when it is right on him; up to one too far or out of sight. */
	private void keepDistance(double distance, boolean sees, PhaseTwoSettings.Kite kite) {
		boolean repathNow = --repath <= 0 || koschei.getNavigation().isDone();
		if (distance < kite.minDistance()) {
			if (repathNow) {
				repath = REPATH_INTERVAL;
				Vec3 away = DefaultRandomPos.getPosAway(koschei, 12, 6, target.position());
				if (away != null) {
					koschei.getNavigation().moveTo(away.x, away.y, away.z, distance < kite.fleeDistance() ? kite.fleeSpeed() : 1.0);
				}
			}
		} else if (distance > kite.maxDistance() || !sees) {
			if (repathNow) {
				repath = REPATH_INTERVAL;
				koschei.getNavigation().moveTo(target, 1.0);
			}
		} else {
			koschei.getNavigation().stop();
		}
	}
}
