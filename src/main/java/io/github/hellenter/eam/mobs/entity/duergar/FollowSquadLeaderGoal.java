package io.github.hellenter.eam.mobs.entity.duergar;

import net.minecraft.world.entity.ai.goal.Goal;

import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/** Out of a fight, a renegade keeps up with his leader. */
final class FollowSquadLeaderGoal extends Goal {
	private static final double START_DISTANCE = 7.0;
	private static final double STOP_DISTANCE = 3.0;
	private static final int REPATH_INTERVAL = 10;

	private final DuergarRenegade renegade;
	private @Nullable DuergarRenegade leader;
	private int repath;

	FollowSquadLeaderGoal(DuergarRenegade renegade) {
		this.renegade = renegade;
		setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (renegade.getTarget() != null) {
			return false;
		}
		leader = renegade.leaderNearby();
		return leader != null && renegade.distanceToSqr(leader) > START_DISTANCE * START_DISTANCE;
	}

	@Override
	public boolean canContinueToUse() {
		return renegade.getTarget() == null && leader != null && leader.isAlive()
				&& renegade.distanceToSqr(leader) > STOP_DISTANCE * STOP_DISTANCE;
	}

	@Override
	public void start() {
		repath = 0;
	}

	@Override
	public void stop() {
		leader = null;
		renegade.getNavigation().stop();
	}

	@Override
	public void tick() {
		if (leader != null && --repath <= 0) {
			repath = REPATH_INTERVAL;
			renegade.getNavigation().moveTo(leader, 1.0);
		}
	}
}
