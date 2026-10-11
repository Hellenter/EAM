package io.github.hellenter.eam.mobs.entity.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.UUID;

class ParryPenaltyTest {
	private static final ParryPenalty PENALTY = ParryPenalty.DEFAULT;

	@Test
	void secondParryWithinEightSecondsIsPenalized() {
		ParryStreak streak = new ParryStreak();
		UUID player = UUID.randomUUID();
		assertFalse(streak.penalized(player, 100, PENALTY));
		streak.parried(player, 100);
		assertTrue(streak.penalized(player, 260, PENALTY));
		assertFalse(streak.penalized(player, 261, PENALTY));
		assertFalse(streak.penalized(UUID.randomUUID(), 150, PENALTY));
	}

	@Test
	void penaltyShortensWindowAndCutsBonus() {
		assertEquals(11, PENALTY.window(11, false));
		assertEquals(9, PENALTY.window(11, true));
		assertEquals(1, PENALTY.window(1, true));
		assertEquals(1.5F, PENALTY.multiplier(1.5F, false), 1.0E-6);
		assertEquals(1.25F, PENALTY.multiplier(1.5F, true), 1.0E-6);
		assertEquals(1.0F, PENALTY.multiplier(1.1F, true), 1.0E-6);
	}
}
