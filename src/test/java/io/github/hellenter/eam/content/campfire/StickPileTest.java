package io.github.hellenter.eam.content.campfire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StickPileTest {
	private static final int FRESH = CampFireBlock.FRESH;
	private static final int SCORCHED = CampFireBlock.SCORCHED;
	private static final int CHARRED = CampFireBlock.CHARRED;

	@Test
	void sticksJoinWhileNothingIsBurnt() {
		StickPile pile = new StickPile(3, 3, FRESH, FRESH).add();
		assertEquals(new StickPile(4, 4, FRESH, FRESH), pile);
	}

	@Test
	void stickPutOnABlackFireStaysFresh() {
		StickPile black = new StickPile(6, 6, CHARRED, FRESH);
		assertTrue(black.startsNewGroup());
		StickPile pile = black.add();
		assertEquals(new StickPile(7, 6, CHARRED, FRESH), pile);
		assertFalse(pile.startsNewGroup());
		assertEquals(new StickPile(8, 6, CHARRED, FRESH), pile.add());
	}

	@Test
	void stickPutOnDarkenedNewSticksStartsAnotherFreshGroup() {
		StickPile pile = new StickPile(7, 5, CHARRED, SCORCHED);
		assertTrue(pile.startsNewGroup());
		assertEquals(new StickPile(8, 7, CHARRED, FRESH), pile.add());
	}

	@Test
	void groupsMergeWhenTheyLookTheSame() {
		StickPile pile = new StickPile(8, 6, CHARRED, FRESH);
		assertEquals(new StickPile(8, 6, CHARRED, SCORCHED), pile.withChar(CHARRED, SCORCHED));
		assertEquals(new StickPile(8, 8, CHARRED, FRESH), pile.withChar(CHARRED, CHARRED));
	}

	@Test
	void oldestBurnsFirstThenNewGroupTakesOver() {
		StickPile pile = new StickPile(3, 2, CHARRED, SCORCHED);
		pile = pile.burn();
		assertEquals(new StickPile(2, 1, CHARRED, SCORCHED), pile);
		assertTrue(pile.burnPromotes());
		pile = pile.burn();
		assertEquals(new StickPile(1, 1, SCORCHED, FRESH), pile);
		assertEquals(StickPile.EMPTY, pile.burn());
	}
}
