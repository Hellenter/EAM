package io.github.hellenter.eam.mobs.entity.lurker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.mobs.entity.lurker.SwampLurkerLogic.Launch;
import io.github.hellenter.eam.mobs.entity.lurker.SwampLurkerLogic.Move;

import org.junit.jupiter.api.Test;

class SwampLurkerLogicTest {
	private static final SwampLurkerSettings S = SwampLurkerSettings.DEFAULT;

	@Test
	void comboInReach() {
		assertEquals(Move.COMBO, SwampLurkerLogic.choose(1.5, 0.0, 0, 10, true, S));
	}

	@Test
	void leapsInFromAfar() {
		assertEquals(Move.LEAP_IN, SwampLurkerLogic.choose(6.0, 0.0, 0, 0, true, S));
		assertEquals(Move.LEAP_IN, SwampLurkerLogic.choose(5.0, 4.0, 0, 0, true, S));
		// Not ready, too close, too far, too high or not standing: it runs instead.
		assertEquals(Move.APPROACH, SwampLurkerLogic.choose(6.0, 0.0, 0, 5, true, S));
		assertEquals(Move.APPROACH, SwampLurkerLogic.choose(2.5, 0.0, 0, 0, true, S));
		assertEquals(Move.APPROACH, SwampLurkerLogic.choose(20.0, 0.0, 0, 0, true, S));
		assertEquals(Move.APPROACH, SwampLurkerLogic.choose(5.0, 6.0, 0, 0, true, S));
		assertEquals(Move.APPROACH, SwampLurkerLogic.choose(6.0, 0.0, 0, 0, false, S));
	}

	@Test
	void backsOffAfterCombo() {
		assertEquals(Move.LEAP_BACK, SwampLurkerLogic.choose(1.5, 0.0, 30, 0, true, S));
		assertEquals(Move.PROWL, SwampLurkerLogic.choose(1.5, 0.0, 30, 10, true, S));
		assertEquals(Move.PROWL, SwampLurkerLogic.choose(6.0, 0.0, 30, 0, true, S));
	}

	@Test
	void leapReachesFlatTarget() {
		for (double d : new double[] {3.0, 5.0, 7.0}) {
			Launch launch = SwampLurkerLogic.leap(d, 0.0, SwampLurkerLogic.arcFor(d), 5.0, 7.0);
			int ticks = SwampLurkerLogic.flightTicks(launch.vertical(), 0.0);
			assertEquals(d, SwampLurkerLogic.flatRun(launch.horizontal(), ticks), 1.0E-6);
		}
	}

	@Test
	void leapCappedAtFiveUpSevenLong() {
		Launch launch = SwampLurkerLogic.leap(12.0, 9.0, 3.0, 5.0, 7.0);
		assertEquals(5.0, SwampLurkerLogic.apex(launch.vertical()), 0.01);
		int ticks = SwampLurkerLogic.flightTicks(launch.vertical(), 4.9);
		assertEquals(7.0, SwampLurkerLogic.flatRun(launch.horizontal(), ticks), 1.0E-6);
	}

	@Test
	void leapUpOntoLedge() {
		Launch launch = SwampLurkerLogic.leap(4.0, 3.0, SwampLurkerLogic.arcFor(4.0), 5.0, 7.0);
		assertTrue(SwampLurkerLogic.apex(launch.vertical()) > 3.0);
	}

	@Test
	void emergesWhenClose() {
		assertTrue(SwampLurkerLogic.emerges(4.0, false, S));
		assertFalse(SwampLurkerLogic.emerges(4.0, true, S));
		assertFalse(SwampLurkerLogic.emerges(6.0, false, S));
	}

	@Test
	void stopsRisingOnlyPastItsHighestLeap() {
		assertFalse(SwampLurkerLogic.risesTooHigh(5.5, 0.3, 5.0));
		assertTrue(SwampLurkerLogic.risesTooHigh(6.5, 0.3, 5.0));
		// Falling back already: left alone.
		assertFalse(SwampLurkerLogic.risesTooHigh(9.0, -0.1, 5.0));
	}
}
