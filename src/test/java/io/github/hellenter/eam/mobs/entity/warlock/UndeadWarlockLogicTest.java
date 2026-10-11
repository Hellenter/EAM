package io.github.hellenter.eam.mobs.entity.warlock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.mobs.entity.warlock.UndeadWarlockLogic.Move;

import org.junit.jupiter.api.Test;

class UndeadWarlockLogicTest {
	private static final UndeadWarlockSettings S = UndeadWarlockSettings.DEFAULT;

	private static boolean[] ready(WarlockSpell... spells) {
		boolean[] ready = new boolean[WarlockSpell.values().length];
		for (WarlockSpell spell : spells) {
			ready[spell.ordinal()] = true;
		}
		return ready;
	}

	private static final boolean[] ALL = ready(WarlockSpell.values());

	@Test
	void keepsCastingRange() {
		assertEquals(Move.RETREAT, UndeadWarlockLogic.move(4.0, true, 7.0, 14.0));
		assertEquals(Move.HOLD, UndeadWarlockLogic.move(10.0, true, 7.0, 14.0));
		assertEquals(Move.APPROACH, UndeadWarlockLogic.move(20.0, true, 7.0, 14.0));
		assertEquals(Move.APPROACH, UndeadWarlockLogic.move(10.0, false, 7.0, 14.0));
	}

	@Test
	void gustFirstWhenThePlayerIsClose() {
		assertEquals(WarlockSpell.WIND, UndeadWarlockLogic.choose(3.0, true, ALL, 3, 0, 0.0, S));
		assertEquals(WarlockSpell.BATS, UndeadWarlockLogic.choose(10.0, true, ALL, 3, 0, 0.0, S));
	}

	@Test
	void batsOnlySometimesAndNotTooMany() {
		assertEquals(WarlockSpell.BUFF, UndeadWarlockLogic.choose(10.0, true, ALL, 3, 0, 0.9, S));
		assertEquals(WarlockSpell.BUFF, UndeadWarlockLogic.choose(10.0, true, ALL, 3, 8, 0.0, S));
	}

	@Test
	void buffOnlyWhileSomeLostLackIt() {
		assertEquals(WarlockSpell.MISSILES, UndeadWarlockLogic.choose(10.0, true, ALL, 0, 8, 0.0, S));
	}

	@Test
	void missilesNeedSightAndRange() {
		boolean[] missiles = ready(WarlockSpell.MISSILES);
		assertEquals(WarlockSpell.MISSILES, UndeadWarlockLogic.choose(15.0, true, missiles, 0, 0, 0.0, S));
		assertEquals(WarlockSpell.NONE, UndeadWarlockLogic.choose(15.0, false, missiles, 0, 0, 0.0, S));
		assertEquals(WarlockSpell.NONE, UndeadWarlockLogic.choose(25.0, true, missiles, 0, 0, 0.0, S));
		assertEquals(WarlockSpell.NONE, UndeadWarlockLogic.choose(10.0, true, ready(), 3, 0, 0.0, S));
	}

	@Test
	void missilesLeaveOneAfterAnother() {
		assertEquals(-1, UndeadWarlockLogic.missileAt(15, 3));
		assertEquals(0, UndeadWarlockLogic.missileAt(16, 3));
		assertEquals(-1, UndeadWarlockLogic.missileAt(17, 3));
		assertEquals(1, UndeadWarlockLogic.missileAt(20, 3));
		assertEquals(2, UndeadWarlockLogic.missileAt(24, 3));
		assertEquals(-1, UndeadWarlockLogic.missileAt(28, 3));
		assertEquals(28, WarlockSpell.MISSILES.length(3));
	}

	@Test
	void hasteForFarLostStrengthForNear() {
		assertTrue(UndeadWarlockLogic.haste(10.0));
		assertFalse(UndeadWarlockLogic.haste(3.0));
	}

	@Test
	void flockOfThreeOrFour() {
		assertEquals(3, UndeadWarlockLogic.flockSize(3, 4, 0.0));
		assertEquals(4, UndeadWarlockLogic.flockSize(3, 4, 0.999));
	}

	@Test
	void gustOnlyInFront() {
		assertTrue(UndeadWarlockLogic.inGust(0.0, 3.0, 0.0, 1.0));
		assertTrue(UndeadWarlockLogic.inGust(2.0, 3.0, 0.0, 1.0));
		assertFalse(UndeadWarlockLogic.inGust(3.0, 0.0, 0.0, 1.0));
		assertFalse(UndeadWarlockLogic.inGust(0.0, -3.0, 0.0, 1.0));
	}

	@Test
	void missileTurnsTowardItsTargetAtFullSpeed() {
		double[] v = UndeadWarlockLogic.steer(1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.5, 0.2);
		assertEquals(0.5, Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]), 1.0E-9);
		assertTrue(v[2] > 0.0 && v[0] > v[2]);
		double[] straight = UndeadWarlockLogic.steer(1.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.5, 0.5);
		assertEquals(0.5, Math.sqrt(straight[0] * straight[0] + straight[1] * straight[1] + straight[2] * straight[2]), 1.0E-9);
	}
}
