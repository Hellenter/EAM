package io.github.hellenter.eam.mobs.vanilla.cows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.mobs.vanilla.cows.CowDefenseLogic.Phase;
import io.github.hellenter.eam.mobs.vanilla.cows.CowDefenseLogic.State;
import io.github.hellenter.eam.mobs.vanilla.cows.CowDefenseLogic.Step;

import org.junit.jupiter.api.Test;

class CowDefenseLogicTest {
	private static final CowDefenseSettings SETTINGS = CowDefenseSettings.DEFAULT;

	@Test
	void hitsOnceInReachThenRetreats() {
		Step step = CowDefenseLogic.tick(State.START, true, false, SETTINGS);
		assertTrue(step.attack());
		assertEquals(Phase.RETREAT, step.state().phase());
		assertEquals(SETTINGS.attackCooldownTicks(), step.state().cooldown());
	}

	@Test
	void retreatEndsTheFight() {
		State state = new State(Phase.RETREAT, 0, 30);
		for (int i = 0; i < SETTINGS.retreatTicks(); i++) {
			Step step = CowDefenseLogic.tick(state, true, false, SETTINGS);
			assertFalse(step.attack());
			state = step.state();
		}
		assertEquals(Phase.DONE, state.phase());
	}

	@Test
	void givesUpALongChase() {
		State state = State.START;
		for (int i = 0; i < SETTINGS.giveUpTicks(); i++) {
			state = CowDefenseLogic.tick(state, false, false, SETTINGS).state();
		}
		assertEquals(Phase.DONE, state.phase());
	}

	@Test
	void hitAgainWhileRetreatingChargesWithoutSkippingTheCooldown() {
		State state = CowDefenseLogic.tick(new State(Phase.RETREAT, 5, 30), true, true, SETTINGS).state();
		assertEquals(Phase.CHARGE, state.phase());
		Step step = CowDefenseLogic.tick(state, true, false, SETTINGS);
		assertFalse(step.attack());
		for (int i = 0; i < 40 && !step.attack(); i++) {
			step = CowDefenseLogic.tick(step.state(), true, false, SETTINGS);
		}
		assertTrue(step.attack());
	}
}
