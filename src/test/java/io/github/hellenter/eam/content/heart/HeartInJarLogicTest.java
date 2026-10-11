package io.github.hellenter.eam.content.heart;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HeartInJarLogicTest {
	@Test
	void flightStartsAtTheHandAndEndsInTheChest() {
		assertEquals(0.0, HeartInJarLogic.flightProgress(0));
		assertEquals(1.0, HeartInJarLogic.flightProgress(HeartInJarLogic.FLIGHT_TICKS));
		assertEquals(1.0, HeartInJarLogic.flightProgress(HeartInJarLogic.FLIGHT_TICKS + 5));
	}

	@Test
	void flightSpeedsUp() {
		double previousStep = 0.0;
		for (int age = 1; age <= HeartInJarLogic.FLIGHT_TICKS; age++) {
			double step = HeartInJarLogic.flightProgress(age) - HeartInJarLogic.flightProgress(age - 1);
			assertTrue(step > previousStep);
			previousStep = step;
		}
	}

	@Test
	void pathRunsFromTheHandOverTheTopIntoTheChest() {
		double[] chest = {0.0, -0.5, 0.1};
		double[] p = HeartInJarLogic.HAND;
		double[] b = HeartInJarLogic.RISE;
		double[] c = HeartInJarLogic.ACROSS;
		assertArrayEquals(p, HeartInJarLogic.bezier(0.0, p, b, c, chest), 1.0E-9);
		assertArrayEquals(chest, HeartInJarLogic.bezier(1.0, p, b, c, chest), 1.0E-9);
		double[] middle = HeartInJarLogic.bezier(0.5, p, b, c, chest);
		assertTrue(middle[1] > p[1], "rises above the hand");
		assertTrue(middle[0] < p[0], "swings away from the jar hand");
	}
}
