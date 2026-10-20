package io.github.hellenter.eam.mobs.entity.lost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.List;

class LongbowmanLogicTest {
	@Test
	void marksAtMostOneShotInFour() {
		int shots = Integer.MAX_VALUE / 2;
		int marks = 0;
		StringBuilder pattern = new StringBuilder();
		for (int i = 0; i < 12; i++) {
			boolean marking = LongbowmanLogic.marks(shots, 4, true);
			shots = marking ? 0 : shots + 1;
			marks += marking ? 1 : 0;
			pattern.append(marking ? 'M' : '.');
		}
		assertEquals("M...M...M...", pattern.toString());
		assertEquals(3, marks);
	}

	@Test
	void neverMarksTheMarkedOrAMob() {
		assertFalse(LongbowmanLogic.marks(100, 4, false));
		assertTrue(LongbowmanLogic.marks(3, 4, true));
		assertFalse(LongbowmanLogic.marks(2, 4, true));
	}

	@Test
	void marksEveryOtherShotByDefault() {
		int every = LongbowmanSettings.Mark.DEFAULT.every();
		int shots = Integer.MAX_VALUE / 2;
		StringBuilder pattern = new StringBuilder();
		for (int i = 0; i < 6; i++) {
			boolean marking = LongbowmanLogic.marks(shots, every, true);
			shots = marking ? 0 : shots + 1;
			pattern.append(marking ? 'M' : '.');
		}
		assertEquals("M.M.M.", pattern.toString());
	}

	@Test
	void redArrowHitsHalfAgainHisFullDraw() {
		// Full draw on hard: base about 2.33, flying 1.6 x 2, hurting 2.5 times a squad archer's.
		float full = (float) (2.33 * 1.6 * 2.5);
		assertEquals(full * 1.5F, LongbowmanLogic.redDamage(2.33, 1.6F, 2.5F, 1.5F), 1e-4);
	}

	@Test
	void powerCooldownStaysInItsRange() {
		assertEquals(500, LongbowmanLogic.powerCooldown(500, 800, 0.0F));
		assertEquals(800, LongbowmanLogic.powerCooldown(500, 800, 0.9999F));
		assertEquals(400, LongbowmanLogic.powerCooldown(400, 300, 0.5F));
	}

	@Test
	void markedTakeAFifthMore() {
		assertEquals(12.0F, LongbowmanLogic.markedDamage(10.0F, 0.2F), 1e-6);
	}

	@Test
	void picksTheMarkedThenTheWeakestThenTheNearest() {
		var healthy = new LongbowmanLogic.Candidate<>("healthy", false, 20.0F, 100.0);
		var weak = new LongbowmanLogic.Candidate<>("weak", false, 6.0F, 400.0);
		var weakNear = new LongbowmanLogic.Candidate<>("weakNear", false, 6.0F, 25.0);
		var marked = new LongbowmanLogic.Candidate<>("marked", true, 20.0F, 900.0);
		assertEquals("weak", LongbowmanLogic.pick(List.of(healthy, weak)));
		assertEquals("weakNear", LongbowmanLogic.pick(List.of(weak, healthy, weakNear)));
		assertEquals("marked", LongbowmanLogic.pick(List.of(weak, marked, healthy)));
		assertNull(LongbowmanLogic.pick(List.<LongbowmanLogic.Candidate<String>>of()));
	}
}
