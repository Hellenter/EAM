package io.github.hellenter.eam.mobs.entity.lost;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LostWaveTest {
	@Test
	void sneakUseCyclesThroughAllWaves() {
		assertEquals(LostWave.CAVALRY, LostWave.SQUAD.next());
		assertEquals(LostWave.CENTURION, LostWave.CAVALRY.next());
		assertEquals(LostWave.SQUAD, LostWave.CENTURION.next());
	}
}
