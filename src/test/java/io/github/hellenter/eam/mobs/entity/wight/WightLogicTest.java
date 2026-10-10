package io.github.hellenter.eam.mobs.entity.wight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.mobs.entity.wight.WightLogic.Plan;
import io.github.hellenter.eam.mobs.entity.wight.WightLogic.Point;

import org.junit.jupiter.api.Test;

class WightLogicTest {
	@Test
	void appearsFromDayTwenty() {
		assertFalse(WightLogic.oldEnough(19, 20));
		assertTrue(WightLogic.oldEnough(20, 20));
	}

	@Test
	void huntsBelowAThird() {
		assertTrue(WightLogic.weak(6.0F, 20.0F, 1.0 / 3.0));
		assertFalse(WightLogic.weak(7.0F, 20.0F, 1.0 / 3.0));
	}

	@Test
	void raidsOnlyBusyPlayersItSeesAfterItsCooldown() {
		assertEquals(Plan.RAID, WightLogic.plan(false, true, true, 0, 0, 0.1F, 0.35F));
		assertEquals(Plan.STALK, WightLogic.plan(false, false, true, 0, 0, 0.1F, 0.35F));
		assertEquals(Plan.STALK, WightLogic.plan(false, true, false, 0, 0, 0.1F, 0.35F));
		assertEquals(Plan.STALK, WightLogic.plan(false, true, true, 5, 0, 0.1F, 0.35F));
		assertEquals(Plan.STALK, WightLogic.plan(false, true, true, 0, 0, 0.5F, 0.35F));
	}

	@Test
	void huntsWeakPlayersWhatTheyDo() {
		assertEquals(Plan.HUNT, WightLogic.plan(true, false, true, 100, 0, 0.9F, 0.35F));
		assertEquals(Plan.STALK, WightLogic.plan(true, true, true, 0, 10, 0.0F, 0.35F));
	}

	@Test
	void behindIsOppositeTheLook() {
		// Yaw 0 looks along +z, so behind is -z.
		Point p = WightLogic.behind(0.0, 0.0, 0.0F, 10.0, 0.0F);
		assertEquals(0.0, p.x(), 1.0E-6);
		assertEquals(-10.0, p.z(), 1.0E-6);
		// Yaw 90 looks along -x, so behind is +x.
		Point q = WightLogic.behind(0.0, 0.0, 90.0F, 10.0, 0.0F);
		assertEquals(10.0, q.x(), 1.0E-6);
		assertEquals(0.0, q.z(), 1.0E-6);
		assertTrue(WightLogic.outOfSight(0.0, 0.0, 0.0F, p.x(), p.z(), 70.0F));
		assertFalse(WightLogic.outOfSight(0.0, 0.0, 0.0F, 0.0, 10.0, 70.0F));
	}

	@Test
	void burningDoublesDamage() {
		assertEquals(8.0F, WightLogic.damageTaken(4.0F, true, 2.0F));
		assertEquals(4.0F, WightLogic.damageTaken(4.0F, false, 2.0F));
	}
}
