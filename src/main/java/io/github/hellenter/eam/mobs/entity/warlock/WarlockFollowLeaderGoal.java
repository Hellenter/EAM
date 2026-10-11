package io.github.hellenter.eam.mobs.entity.warlock;

import io.github.hellenter.eam.mobs.entity.lost.Lost;

import net.minecraft.world.entity.ai.goal.Goal;

import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/** Out of a fight, the warlock keeps up with the leader of his squad, a few steps behind. */
final class WarlockFollowLeaderGoal extends Goal {
	private static final double START_DISTANCE = 9.0;
	private static final double STOP_DISTANCE = 5.0;
	private static final int REPATH_INTERVAL = 10;

	private final UndeadWarlock warlock;
	private @Nullable Lost leader;
	private int repath;

	WarlockFollowLeaderGoal(UndeadWarlock warlock) {
		this.warlock = warlock;
		setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (warlock.getTarget() != null) {
			return false;
		}
		leader = warlock.leaderNearby();
		return leader != null && warlock.distanceToSqr(leader) > START_DISTANCE * START_DISTANCE;
	}

	@Override
	public boolean canContinueToUse() {
		return warlock.getTarget() == null && leader != null && leader.isAlive()
				&& warlock.distanceToSqr(leader) > STOP_DISTANCE * STOP_DISTANCE;
	}

	@Override
	public void start() {
		repath = 0;
	}

	@Override
	public void stop() {
		leader = null;
		warlock.getNavigation().stop();
	}

	@Override
	public void tick() {
		if (leader != null && --repath <= 0) {
			repath = REPATH_INTERVAL;
			warlock.getNavigation().moveTo(leader, 1.0);
		}
	}
}
