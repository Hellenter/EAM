package io.github.hellenter.eam.content.longbow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LongbowLogicTest {
	@Test
	void fullDrawTakesTwoSeconds() {
		assertEquals(1.0F, LongbowLogic.powerForTime(40));
		assertTrue(LongbowLogic.powerForTime(39) < 1.0F);
		// Half a draw of the longbow is as strong as half a draw of the bow.
		assertEquals((0.25F + 1.0F) / 3.0F, LongbowLogic.powerForTime(20), 1e-6);
	}

	@Test
	void fullDrawFliesTwiceAsFastAsABow() {
		assertEquals(6.0F, LongbowLogic.speed(1.0F), 1e-6);
	}

	@Test
	void hitsTwoAndAHalfTimesAsHard() {
		// Vanilla damage on hit is base damage times speed.
		double bow = LongbowLogic.ARROW_BASE_DAMAGE * 3.0;
		double longbow = LongbowLogic.baseDamage(LongbowLogic.ARROW_BASE_DAMAGE, LongbowLogic.DAMAGE_MULTIPLIER, LongbowLogic.SPEED_MULTIPLIER)
				* LongbowLogic.speed(1.0F);
		assertEquals(2.5, longbow / bow, 1e-6);
	}
}
