package io.github.hellenter.eam.mobs.entity.duergar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RenegadeAttackLogicTest {
	@Test
	void blastFallsOffWithDistance() {
		assertEquals(12.0F, DuergarLogic.blastDamage(12.0F, 0.0, 4.0), 1.0E-6);
		assertEquals(6.0F, DuergarLogic.blastDamage(12.0F, 2.0, 4.0), 1.0E-6);
		assertEquals(0.0F, DuergarLogic.blastDamage(12.0F, 4.0, 4.0), 1.0E-6);
		assertEquals(0.0F, DuergarLogic.blastDamage(12.0F, 9.0, 4.0), 1.0E-6);
		assertTrue(DuergarLogic.blastDamage(12.0F, 1.0, 4.0) > DuergarLogic.blastDamage(12.0F, 3.0, 4.0));
	}

	@Test
	void fartherLobsFlyLonger() {
		assertEquals(10, DuergarLogic.flightTicks(2.0, 1.4, 10, 30));
		assertEquals(14, DuergarLogic.flightTicks(10.0, 1.4, 10, 30));
		assertEquals(30, DuergarLogic.flightTicks(40.0, 1.4, 10, 30));
	}

	@Test
	void bombLandsWhereAimed() {
		// A projectile: slowed before it moves.
		for (double distance : new double[] {4.0, 9.0, 14.0}) {
			for (double rise : new double[] {-3.0, 0.0, 2.5}) {
				int ticks = DuergarLogic.flightTicks(distance, 1.4, 10, 30);
				double speed = DuergarLogic.launchSpeed(distance, ticks, 0.0, 0.99, true);
				double vy = DuergarLogic.launchSpeed(rise, ticks, 0.03, 0.99, true);
				assertEquals(distance, DuergarLogic.travel(speed, ticks, 0.0, 0.99, true), 1.0E-6);
				assertEquals(rise, DuergarLogic.travel(vy, ticks, 0.03, 0.99, true), 1.0E-6);
			}
		}
	}

	@Test
	void thrownMateLandsOnThePlayer() {
		// A living body: moves, then slows; air drag 0.91 sideways, 0.98 up and down.
		int ticks = DuergarLogic.flightTicks(12.0, 1.3, 10, 30);
		double speed = DuergarLogic.launchSpeed(12.0, ticks, 0.0, 0.91, false);
		double vy = DuergarLogic.launchSpeed(-1.5, ticks, 0.08, 0.98, false);
		assertEquals(12.0, DuergarLogic.travel(speed, ticks, 0.0, 0.91, false), 1.0E-6);
		assertEquals(-1.5, DuergarLogic.travel(vy, ticks, 0.08, 0.98, false), 1.0E-6);
		assertTrue(vy > 0.0, "the mate goes up in an arc before he comes down");
	}

	@Test
	void bombRarerThanChargeAndThrowRarerThanBomb() {
		DuergarRenegadeAttacks attacks = DuergarRenegadeAttacks.DEFAULT;
		assertTrue(attacks.bomb().chance() < attacks.charge().chance());
		assertTrue(attacks.allyThrow().chance() < attacks.bomb().chance());
		assertTrue(attacks.allyThrow().cooldownTicks() > attacks.bomb().cooldownTicks());
		assertEquals(1.5F, attacks.charge().damageMultiplier(), 1.0E-6);
	}
}
