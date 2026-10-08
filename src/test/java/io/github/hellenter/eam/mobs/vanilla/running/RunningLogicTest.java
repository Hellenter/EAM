package io.github.hellenter.eam.mobs.vanilla.running;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RunningLogicTest {
	@Test
	void vanillaZombieWalksAtHalfAPlayersPace() {
		assertEquals(0.53, RunningLogic.paceVsPlayerWalk(0.23), 0.01);
	}

	@Test
	void chasingZombieReachesTheWantedPace() {
		double multiplier = RunningLogic.speedMultiplier(0.23, 0.85);
		assertEquals(0.85, RunningLogic.paceVsPlayerWalk(0.23 * multiplier), 1.0E-9);
	}

	@Test
	void chasingZombieStaysSlowerThanAWalkingPlayer() {
		double multiplier = RunningLogic.speedMultiplier(0.23, 0.85);
		assertEquals(true, RunningLogic.paceVsPlayerWalk(0.23 * multiplier) < 1.0);
		assertEquals(true, multiplier > 1.0);
	}

	@Test
	void fastMobsAreNeverSlowedDown() {
		assertEquals(1.0, RunningLogic.speedMultiplier(0.35, 0.85));
	}

	@Test
	void zeroPaceTurnsRunningOff() {
		assertEquals(1.0, RunningLogic.speedMultiplier(0.23, 0.0));
	}
}
