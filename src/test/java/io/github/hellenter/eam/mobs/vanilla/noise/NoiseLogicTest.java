package io.github.hellenter.eam.mobs.vanilla.noise;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoiseLogicTest {
	private static final NoiseSettings SETTINGS = NoiseSettings.DEFAULT;

	@Test
	void silentWithoutNoise() {
		assertEquals(0.0F, NoiseLogic.radius(null, 100));
	}

	@Test
	void noiseDiesDownAfterHoldTicks() {
		NoiseLogic.Noise noise = NoiseLogic.add(null, 10.0F, 100, 60);
		assertEquals(10.0F, NoiseLogic.radius(noise, 159));
		assertEquals(0.0F, NoiseLogic.radius(noise, 160));
	}

	@Test
	void quieterNoiseDoesNotCutALouderOneShort() {
		NoiseLogic.Noise loud = NoiseLogic.add(null, 12.0F, 100, 60);
		assertSame(loud, NoiseLogic.add(loud, 8.0F, 120, 60));
	}

	@Test
	void louderNoiseReplacesAndRestarts() {
		NoiseLogic.Noise quiet = NoiseLogic.add(null, 8.0F, 100, 60);
		NoiseLogic.Noise loud = NoiseLogic.add(quiet, 12.0F, 150, 60);
		assertEquals(12.0F, NoiseLogic.radius(loud, 200));
	}

	@Test
	void anyNoiseAfterSilenceCounts() {
		NoiseLogic.Noise old = NoiseLogic.add(null, 16.0F, 0, 60);
		assertEquals(5.0F, NoiseLogic.radius(NoiseLogic.add(old, 5.0F, 100, 60), 100));
	}

	@Test
	void silentActionChangesNothing() {
		assertSame(null, NoiseLogic.add(null, 0.0F, 100, 60));
	}

	@Test
	void stoneIsLouderThanDirtAndCapped() {
		float dirt = NoiseLogic.dig(SETTINGS, 0.5F);
		float stone = NoiseLogic.dig(SETTINGS, 1.5F);
		assertTrue(stone > dirt);
		assertEquals(7.5F, dirt, 1e-4);
		assertEquals(SETTINGS.digMax(), NoiseLogic.dig(SETTINGS, 50.0F));
	}

	@Test
	void sneakingHalvesNoise() {
		assertEquals(5.0F, NoiseLogic.sneak(10.0F, true, 0.5F));
		assertEquals(10.0F, NoiseLogic.sneak(10.0F, false, 0.5F));
	}

	@Test
	void heardOnlyWithinRadius() {
		assertTrue(NoiseLogic.hears(100.0, 10.0F));
		assertFalse(NoiseLogic.hears(100.1, 10.0F));
		assertFalse(NoiseLogic.hears(0.0, 0.0F));
	}
}
