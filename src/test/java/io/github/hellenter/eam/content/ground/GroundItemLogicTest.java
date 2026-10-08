package io.github.hellenter.eam.content.ground;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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

	@Test
	void stickLiesAlongXOrZAcrossTheWholeBlock() {
		GroundItemLogic.Shape stick = GroundItemLogic.stick(2, 1);
		assertEquals(new GroundItemLogic.Box(0, 1, 16, 3, 1), stick.at(0));
		assertEquals(new GroundItemLogic.Box(1, 0, 3, 16, 1), stick.at(1));
		assertEquals(new GroundItemLogic.Box(0, 13, 16, 15, 1), stick.at(12));
		assertEquals(new GroundItemLogic.Box(13, 0, 15, 16, 1), stick.at(13));
	}

	@Test
	void stickLiesDiagonallyBothWays() {
		GroundItemLogic.Shape stick = GroundItemLogic.stick(2, 1);
		assertEquals(45, stick.at(2).angle());
		assertEquals(-45, stick.at(3).angle());
		assertEquals(new GroundItemLogic.Box(2, 3.25, 14, 5.25, 1, 45), stick.at(2));
		assertEquals(new GroundItemLogic.Box(2, 10.75, 14, 12.75, 1, -45), stick.at(15));
	}

	@Test
	void turnedStickOutlineStaysInsideTheBlock() {
		GroundItemLogic.Shape stick = GroundItemLogic.stick(2, 1);
		for (int position = 0; position < GroundItemLogic.POSITIONS; position++) {
			for (GroundItemLogic.Box box : stick.at(position).outline()) {
				assertTrue(box.minX() >= 0 && box.minZ() >= 0 && box.maxX() <= 16 && box.maxZ() <= 16, "position " + position);
			}
		}
	}

	@Test
	void turnedOutlineFollowsTheModelRotation() {
		// +45 degrees about y turns the +x end of a centred stick towards -z.
		GroundItemLogic.Box stick = new GroundItemLogic.Box(2, 7, 14, 9, 1, 45);
		List<GroundItemLogic.Box> outline = stick.outline();
		assertEquals(GroundItemLogic.OUTLINE_SEGMENTS, outline.size());
		GroundItemLogic.Box last = outline.getLast();
		assertTrue(last.minX() > 8 && last.maxZ() < 8);
	}
}
