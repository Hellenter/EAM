package io.github.hellenter.eam.content.heart;

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
}
