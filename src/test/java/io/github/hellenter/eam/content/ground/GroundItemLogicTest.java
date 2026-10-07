package io.github.hellenter.eam.content.ground;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GroundItemLogicTest {
	@Test
	void sixteenPositionsOnAFourByFourGrid() {
		assertEquals(16, GroundItemLogic.POSITIONS);
		assertEquals(0, GroundItemLogic.positionAt(0.0, 0.0));
		assertEquals(3, GroundItemLogic.positionAt(0.99, 0.0));
		assertEquals(12, GroundItemLogic.positionAt(0.0, 0.99));
		assertEquals(15, GroundItemLogic.positionAt(0.99, 0.99));
		assertEquals(5, GroundItemLogic.positionAt(0.3, 0.3));
	}

	@Test
	void clicksOnTheEdgeStayOnTheBlock() {
		assertEquals(15, GroundItemLogic.positionAt(1.0, 1.0));
		assertEquals(0, GroundItemLogic.positionAt(-0.01, -0.01));
	}

	@Test
	void itemSitsInTheMiddleOfItsCell() {
		assertEquals(0.5, GroundItemLogic.minX(0, 3));
		assertEquals(12.5, GroundItemLogic.minX(3, 3));
		assertEquals(1.0, GroundItemLogic.minZ(0, 2));
		assertEquals(13.0, GroundItemLogic.minZ(15, 2));
	}
}
