package io.github.hellenter.eam.mobs.entity.frozen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FrozenCorpseLogicTest {
	@Test
	void firstHitGivesSlownessOne() {
		assertEquals(0, FrozenCorpseLogic.nextLevel(-1, 2));
	}

	@Test
	void eachHitDeepensItUpToTheCap() {
		assertEquals(1, FrozenCorpseLogic.nextLevel(0, 2));
		assertEquals(2, FrozenCorpseLogic.nextLevel(1, 2));
		assertEquals(2, FrozenCorpseLogic.nextLevel(2, 2));
		// A deeper slowness from elsewhere: the corpse adds no more than its own cap.
		assertEquals(2, FrozenCorpseLogic.nextLevel(4, 2));
	}
}
