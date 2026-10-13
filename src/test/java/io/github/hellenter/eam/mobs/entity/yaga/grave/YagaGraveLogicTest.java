package io.github.hellenter.eam.mobs.entity.yaga.grave;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class YagaGraveLogicTest {
	@Test
	void candidatesLieOnRingsBetweenTheLimitsNearestFirst() {
		List<int[]> offsets = YagaGraveLogic.candidates(YagaGraveLogic.NEAR_MIN, YagaGraveLogic.NEAR_MAX, () -> 0.3);
		assertTrue(offsets.size() > 100);
		double last = 0.0;
		for (int[] offset : offsets) {
			double distance = Math.hypot(offset[0], offset[1]);
			assertTrue(distance >= YagaGraveLogic.NEAR_MIN - 1 && distance <= YagaGraveLogic.NEAR_MAX + 1, "distance " + distance);
			assertTrue(distance >= last - 1.5, "rings go outwards");
			last = distance;
		}
	}

	@Test
	void ringStartsAtTheRandomAngle() {
		List<int[]> offsets = YagaGraveLogic.candidates(40, 40, () -> 0.25);
		assertArrayEquals(new int[] {0, 40}, offsets.getFirst());
		assertEquals(31, offsets.size());
	}

	@Test
	void ringOfZeroIsTheSpotItself() {
		assertArrayEquals(new int[] {0, 0}, YagaGraveLogic.candidates(0, 0, () -> 0.0).getFirst());
	}
}
