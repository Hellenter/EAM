package io.github.hellenter.eam.mobs.entity.ghosthorse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GhostHorseLogicTest {
	@Test
	void aFifthFasterThanTheFastestHorse() {
		assertEquals(0.3375, GhostHorseLogic.HORSE_MAX_SPEED, 1.0E-9);
		assertEquals(0.405, GhostHorseLogic.speed(0.2), 1.0E-9);
	}

	@Test
	void jumpsAFifthHigherThanTheBestJumper() {
		double best = GhostHorseLogic.jumpHeight(GhostHorseLogic.HORSE_MAX_JUMP_STRENGTH);
		// The best bred horse clears five blocks and a bit.
		assertTrue(best > 5.0 && best < 6.0, "height " + best);
		double strength = GhostHorseLogic.jumpStrength(0.2);
		assertEquals(best * 1.2, GhostHorseLogic.jumpHeight(strength), 0.05);
		assertTrue(strength > GhostHorseLogic.HORSE_MAX_JUMP_STRENGTH);
	}

	@Test
	void healsToItsOwnHealthAndLosesTheOverflowSlowly() {
		assertEquals(21.0F, GhostHorseLogic.secondLater(20.0F, 30.0F, 1.0F, 0.25F));
		assertEquals(30.0F, GhostHorseLogic.secondLater(29.5F, 30.0F, 1.0F, 0.25F));
		assertEquals(30.0F, GhostHorseLogic.secondLater(30.0F, 30.0F, 1.0F, 0.25F));
		assertEquals(49.75F, GhostHorseLogic.secondLater(50.0F, 30.0F, 1.0F, 0.25F));
		assertEquals(30.0F, GhostHorseLogic.secondLater(30.1F, 30.0F, 1.0F, 0.25F));
	}

	@Test
	void drinksAQuarterOfItsMastersBlowsUpToTheCap() {
		assertEquals(22.0F, GhostHorseLogic.afterMasterHit(20.0F, 50.0F, 8.0F, 0.25F));
		assertEquals(32.0F, GhostHorseLogic.afterMasterHit(30.0F, 50.0F, 8.0F, 0.25F));
		assertEquals(50.0F, GhostHorseLogic.afterMasterHit(49.0F, 50.0F, 40.0F, 0.25F));
		assertEquals(20.0F, GhostHorseLogic.afterMasterHit(20.0F, 50.0F, 0.0F, 0.25F));
	}

	@Test
	void restsInTheHorn() {
		// A minute in the horn heals 60 at 1 a second, never past its own health.
		assertEquals(30.0F, GhostHorseLogic.rested(5.0F, 30.0F, 1.0F, 1200L));
		assertEquals(15.0F, GhostHorseLogic.rested(5.0F, 30.0F, 1.0F, 200L));
		// A horn never blown calls a horse at full health.
		assertEquals(30.0F, GhostHorseLogic.rested(Float.MAX_VALUE, 30.0F, 1.0F, 0L));
	}

	@Test
	void riderTakesLessAndDealsMore() {
		assertEquals(8.0F, GhostHorseLogic.scaled(10.0F, 0.8F), 1.0E-6);
		assertEquals(11.5F, GhostHorseLogic.scaled(10.0F, 1.15F), 1.0E-5);
	}
}
