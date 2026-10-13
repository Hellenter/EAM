package io.github.hellenter.eam.mobs.entity.lost;

import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightLogic.Attack;
import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightSettings;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Sword and spear fighting. The leader walks straight at his target with the shield up and attacks; a shield bearer
 * with archers behind him takes his spot between the target and them instead, shield raised, and strikes only at
 * what comes into his reach. Without archers left, shield bearers close in like the leader. Spearmen of a shield wall
 * stand in their row in front of their commander and jab at whoever comes into reach.
 */
final class LostFightGoal extends Goal {
	private static final int REPATH_INTERVAL = 10;
	/** Archers further than this from the target are not covered. */
	private static final double COVER_RANGE = 24.0;
	/** A guard this close to his spot stops there. */
	private static final double ON_SPOT = 0.8;
	/** A spearman further than this from his spot in the row walks there by path; closer, he steps straight onto it. */
	private static final double ROW_PATH_DISTANCE = 2.5;
	/** And stops this close to it. */
	private static final double ROW_ON_SPOT = 0.2;
	private static final double ROW_RUN_SPEED = 1.2;

	private final Lost lost;
	private LivingEntity target;
	private int strafeTicks;
	private boolean strafeRight;
	private int repath;

	LostFightGoal(Lost lost) {
		this.lost = lost;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity candidate = lost.getTarget();
		if (lost.role().shooter() || candidate == null || !candidate.isAlive()) {
			return false;
		}
		target = candidate;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return canUse() || lost.isBusy();
	}

	@Override
	public void start() {
		lost.setAggressive(true);
		repath = 0;
	}

	@Override
	public void stop() {
		target = null;
		lost.setAggressive(false);
		lost.getNavigation().stop();
		lost.getMoveControl().strafe(0.0F, 0.0F);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (lost.isBusy() || target == null || !target.isAlive() || target != lost.getTarget()) {
			return;
		}
		repath--;
		lost.getLookControl().setLookAt(target, 30.0F, 30.0F);
		double distance = lost.horizontalDistanceTo(target);
		SkeletonKnightSettings settings = lost.settings();
		LostSettings lostSettings = LostModule.settings();
		LostRole role = lost.role();

		boolean sees = lost.getSensing().hasLineOfSight(target);
		if (sees && lost.isIdleFor(settings.attackPause()) && Math.abs(target.getY() - lost.getY()) < 2.5) {
			Attack attack = LostLogic.choose(role, distance, target.isBlocking(), lost.comboCooldown(), lost.heavyCooldown(), settings,
					lost.meleeReach(), lost.getRandom().nextDouble(), lost.getRandom().nextDouble());
			if (attack != null) {
				lost.getMoveControl().strafe(0.0F, 0.0F);
				lost.startAttack(attack, target);
				return;
			}
		}

		// Spearmen of a shield wall hold their place in the row in front of their commander.
		if (role == LostRole.WALL_SPEARMAN && !lost.defending() && holdRow(lostSettings)) {
			return;
		}
		// Shield bearers hold their place in front of the archers, unless their centurion needs them.
		if (role.guards() && !lost.defending() && guard(lostSettings)) {
			return;
		}

		double reach = lost.meleeReach();
		if (distance > reach - 0.4 || !sees) {
			if (repath <= 0 || lost.getNavigation().isDone()) {
				repath = REPATH_INTERVAL;
				lost.getNavigation().moveTo(target, lost.defending() ? lostSettings.centurion().defend().speed() : 1.0);
			}
			return;
		}
		// In reach, waiting for the next attack: shield up, a slow step to one side.
		lost.getNavigation().stop();
		if (--strafeTicks <= 0) {
			strafeTicks = 20 + lost.getRandom().nextInt(30);
			strafeRight = lost.getRandom().nextBoolean();
		}
		lost.getMoveControl().strafe(distance < 1.4 ? -0.4F : 0.0F, strafeRight ? 0.3F : -0.3F);
		lost.lookAt(target, 30.0F, 30.0F);
	}

	/**
	 * Takes his spot in the shield wall's row, shield raised to the target, and stands there. Returns false without a
	 * commander of his to cover: then he fights like any spearman.
	 */
	private boolean holdRow(LostSettings settings) {
		if (!(lost.level() instanceof ServerLevel level)) {
			return false;
		}
		Vec3 spot = ShieldWall.spotOf(level, lost, target);
		if (spot == null) {
			return false;
		}
		double off = Math.hypot(spot.x - lost.getX(), spot.z - lost.getZ());
		if (off > ROW_PATH_DISTANCE || Math.abs(spot.y - lost.getY()) > 1.5) {
			// Far from his spot: walk there, a little faster than usual.
			if (repath <= 0 || lost.getNavigation().isDone()) {
				repath = REPATH_INTERVAL / 2;
				lost.getNavigation().moveTo(spot.x, spot.y, spot.z, ROW_RUN_SPEED);
			}
		} else if (off > ROW_ON_SPOT) {
			// Close: step right onto it. A path would end in the middle of a block, and the spots are closer than that.
			lost.getNavigation().stop();
			lost.getMoveControl().setWantedPosition(spot.x, lost.getY(), spot.z, off > 1.0 ? ROW_RUN_SPEED : 0.7);
		} else {
			lost.getNavigation().stop();
			lost.getMoveControl().setWantedPosition(lost.getX(), lost.getY(), lost.getZ(), 0.0);
			lost.setXxa(0.0F);
			lost.setZza(0.0F);
		}
		lost.getLookControl().setLookAt(target, 30.0F, 30.0F);
		return true;
	}

	/** Takes his spot between the target and the squad's archers. Returns false when there are none to cover. */
	private boolean guard(LostSettings settings) {
		Vec3 archers = lost.archersCentre();
		if (archers == null) {
			return false;
		}
		double archersFromTarget = Math.hypot(archers.x - target.getX(), archers.z - target.getZ());
		if (archersFromTarget > COVER_RANGE || archersFromTarget < settings.guardDistance()) {
			return false;
		}
		double[] spot = LostLogic.guardPoint(target.getX(), target.getZ(), archers.x, archers.z, settings.guardDistance(), lost.guardSide());
		double off = Math.hypot(spot[0] - lost.getX(), spot[1] - lost.getZ());
		if (off > ON_SPOT) {
			if (repath <= 0 || lost.getNavigation().isDone()) {
				repath = REPATH_INTERVAL / 2;
				lost.getNavigation().moveTo(spot[0], target.getY(), spot[1], 1.0);
			}
		} else {
			lost.getNavigation().stop();
			lost.getMoveControl().strafe(0.0F, 0.0F);
		}
		lost.lookAt(target, 30.0F, 30.0F);
		return true;
	}
}
