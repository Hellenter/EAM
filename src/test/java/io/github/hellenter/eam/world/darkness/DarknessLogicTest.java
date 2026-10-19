package io.github.hellenter.eam.world.darkness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.world.darkness.DarknessLogic.Keyframe;

import org.junit.jupiter.api.Test;

import java.util.List;

class DarknessLogicTest {
	@Test
	void newMoonNightIsTheDarkestAndFullMoonTheBrightest() {
		float[] factors = DarknessLogic.NIGHT_SKY_FACTOR;
		assertEquals(8, factors.length);
		for (float factor : factors) {
			assertTrue(factor >= factors[4] && factor <= factors[0]);
		}
		// Waning and waxing phases of the same size are equally dark.
		for (int phase = 1; phase < 4; phase++) {
			assertEquals(factors[phase], factors[8 - phase]);
		}
	}

	@Test
	void keyframesCoverTheMoonCycleInOrderWithFullLightByDay() {
		List<Keyframe> keyframes = DarknessLogic.nightKeyframes(DarknessLogic.NIGHT_SKY_FACTOR);
		assertEquals(32, keyframes.size());
		for (int i = 1; i < keyframes.size(); i++) {
			assertTrue(keyframes.get(i).ticks() > keyframes.get(i - 1).ticks());
		}
		assertTrue(keyframes.getLast().ticks() < DarknessLogic.DAY_TICKS * 8);
		// New moon night: the fifth day of the cycle.
		assertEquals(new Keyframe(4 * 24000 + 730, 1.0F), keyframes.get(16));
		assertEquals(new Keyframe(4 * 24000 + 13140, 0.15F), keyframes.get(18));
		assertEquals(new Keyframe(4 * 24000 + 22860, 0.15F), keyframes.get(19));
	}

	@Test
	void brightnessSettingIsCappedButLowerSettingsStay() {
		assertEquals(0.5F, DarknessLogic.capBrightness(1.0F, 0.5F));
		assertEquals(0.2F, DarknessLogic.capBrightness(0.2F, 0.5F));
		assertEquals(1.0F, DarknessLogic.capBrightness(1.0F, 1.0F));
	}

	@Test
	void onlyRainPutsFiresOut() {
		assertTrue(DarknessLogic.douses(true, 0.5F, 0.49F));
		assertFalse(DarknessLogic.douses(true, 0.5F, 0.5F));
		assertFalse(DarknessLogic.douses(false, 1.0F, 0.0F));
		assertFalse(DarknessLogic.douses(true, 0.0F, 0.0F));
	}
}
