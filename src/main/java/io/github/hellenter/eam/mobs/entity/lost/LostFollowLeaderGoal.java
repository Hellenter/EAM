package io.github.hellenter.eam.mobs.entity.lost;

import net.minecraft.world.entity.ai.goal.Goal;

import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/** Out of a fight, a follower keeps up with his leader. */
final class LostFollowLeaderGoal extends Goal {
	private static final double START_DISTANCE = 8.0;
	private static final double STOP_DISTANCE = 3.5;
	private static final int REPATH_INTERVAL = 10;

	private final Lost lost;
	private @Nullable Lost leader;
	private int repath;

	LostFollowLeaderGoal(Lost lost) {
		this.lost = lost;
		setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (lost.getTarget() != null) {
			return false;
		}
		leader = lost.leaderNearby();
		return leader != null && lost.distanceToSqr(leader) > START_DISTANCE * START_DISTANCE;
	}

	@Override
	public boolean canContinueToUse() {
		return lost.getTarget() == null && leader != null && leader.isAlive() && lost.distanceToSqr(leader) > STOP_DISTANCE * STOP_DISTANCE;
	}

	@Override
	public void start() {
		repath = 0;
	}

	@Override
	public void stop() {
		leader = null;
		lost.getNavigation().stop();
	}

	@Override
	public void tick() {
		if (leader != null && --repath <= 0) {
			repath = REPATH_INTERVAL;
			lost.getNavigation().moveTo(leader, 1.0);
		}
	}
}
