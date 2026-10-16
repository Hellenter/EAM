package io.github.hellenter.eam.mobs.entity.koschei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.mobs.entity.koschei.KoscheiLogic.Attack;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiLogic.Choice;

import org.junit.jupiter.api.Test;

import java.util.function.DoubleSupplier;

class KoscheiLogicTest {
	private static final KoscheiSettings.PhaseOne ONE = KoscheiSettings.DEFAULT.phaseOne();
	private static final DoubleSupplier ALWAYS = () -> 0.0;
	private static final DoubleSupplier NEVER = () -> 0.999;

	private static int[] ready() {
		return new int[Attack.values().length];
	}

	private static int[] cooling(Attack... attacks) {
		int[] cooldowns = ready();
		for (Attack attack : attacks) {
			cooldowns[attack.ordinal()] = 100;
		}
		return cooldowns;
	}

	@Test
	void theBoltIsNeverShotFromCloserThanSixBlocks() {
		assertEquals(Attack.BOLT, KoscheiLogic.choose(6.0, true, 0, 0, ready(), null, ONE, ALWAYS).attack());
		assertEquals(Attack.BOLT, KoscheiLogic.choose(15.0, true, 0, 0, ready(), null, ONE, ALWAYS).attack());
		assertNotEquals(Attack.BOLT, KoscheiLogic.choose(5.9, true, 0, 0, ready(), null, ONE, ALWAYS).attack());
		assertNotEquals(Attack.BOLT, KoscheiLogic.choose(2.0, true, 0, 0, ready(), null, ONE, ALWAYS).attack());
	}

	@Test
	void heLungesFromAboutFourBlocksAndWalksInFromFurther() {
		int[] noBolt = cooling(Attack.BOLT);
		assertEquals(Attack.LUNGE, KoscheiLogic.choose(4.0, true, 0, 0, noBolt, null, ONE, ALWAYS).attack());
		assertNull(KoscheiLogic.choose(7.0, true, 0, 0, noBolt, null, ONE, ALWAYS).attack());
		assertNull(KoscheiLogic.choose(4.0, true, 0, 0, cooling(Attack.BOLT, Attack.LUNGE), null, ONE, ALWAYS).attack());
	}

	@Test
	void aLungeWantedAtCloseRangeFailsAndAfterTwoHeMakesRoom() {
		int[] noForce = cooling(Attack.BOLT, Attack.FORCE);
		Choice close = KoscheiLogic.choose(2.0, true, 0, 0, noForce, null, ONE, ALWAYS);
		assertTrue(close.lungeFailed());
		assertNotEquals(Attack.LUNGE, close.attack());
		assertNotEquals(Attack.MAKE_ROOM, KoscheiLogic.choose(2.0, true, 0, ONE.lunge().maxFails(), noForce, null, ONE, ALWAYS).attack());
		assertEquals(Attack.MAKE_ROOM, KoscheiLogic.choose(2.0, true, 0, ONE.lunge().maxFails() + 1, noForce, null, ONE, ALWAYS).attack());
	}

	@Test
	void theOrangeBlowNeedsAShieldThatIsNotKnockedOut() {
		int[] onlyOrange = cooling(Attack.DOUBLE, Attack.RED, Attack.OVERHEAD);
		KoscheiSettings.PhaseOne noStrike = new KoscheiSettings.PhaseOne(ONE.reach(), new KoscheiSettings.Blow(8.0F, 0.0F, 0),
				ONE.doubleStrike(), ONE.orange(), ONE.red(), ONE.overhead(), ONE.lunge(), ONE.kick(), ONE.force(), ONE.bolt(), ONE.parry(), 1.0F);
		assertEquals(Attack.ORANGE, KoscheiLogic.melee(true, false, onlyOrange, null, noStrike, ALWAYS));
		assertEquals(Attack.STRIKE, KoscheiLogic.melee(false, false, onlyOrange, null, noStrike, ALWAYS));
	}

	@Test
	void heKicksAShieldHeldUpCloseAndNeverFromAfar() {
		int[] noBolt = cooling(Attack.BOLT, Attack.FORCE);
		int held = ONE.kick().shieldTicks();
		assertEquals(Attack.KICK, KoscheiLogic.choose(1.5, true, held, 0, noBolt, null, ONE, NEVER).attack());
		assertNotEquals(Attack.KICK, KoscheiLogic.choose(1.5, true, held - 1, 0, noBolt, null, ONE, ALWAYS).attack());
		assertNotEquals(Attack.KICK, KoscheiLogic.choose(3.0, true, held, 0, noBolt, null, ONE, ALWAYS).attack());
		assertNotEquals(Attack.KICK, KoscheiLogic.choose(1.5, true, held, 0, cooling(Attack.BOLT, Attack.FORCE, Attack.KICK), null, ONE, ALWAYS)
				.attack());
		int[] onlyKick = cooling(Attack.STRIKE, Attack.DOUBLE, Attack.ORANGE, Attack.RED, Attack.OVERHEAD);
		assertEquals(Attack.KICK, KoscheiLogic.melee(true, true, onlyKick, null, ONE, ALWAYS));
		assertEquals(Attack.STRIKE, KoscheiLogic.melee(true, false, onlyKick, null, ONE, ALWAYS));
	}

	@Test
	void blowsOnCooldownAreNotPicked() {
		int[] all = cooling(Attack.DOUBLE, Attack.ORANGE, Attack.RED, Attack.OVERHEAD);
		assertEquals(Attack.STRIKE, KoscheiLogic.melee(true, false, all, null, ONE, NEVER));
	}

	@Test
	void onlyTheLungeAndTheOverheadBlowCanBeParriedJustBeforeTheyLand() {
		assertTrue(KoscheiLogic.parries(KoscheiAction.OVERHEAD, KoscheiAction.OVERHEAD_HIT - 1, 7));
		assertTrue(KoscheiLogic.parries(KoscheiAction.OVERHEAD, KoscheiAction.OVERHEAD_DESCENT, 7));
		assertFalse(KoscheiLogic.parries(KoscheiAction.OVERHEAD, KoscheiAction.OVERHEAD_DESCENT - 1, 7));
		assertFalse(KoscheiLogic.parries(KoscheiAction.OVERHEAD, KoscheiAction.OVERHEAD_HIT, 7));
		assertTrue(KoscheiLogic.parries(KoscheiAction.LUNGE, KoscheiAction.LUNGE_STAB - 1, 7));
		assertFalse(KoscheiLogic.parries(KoscheiAction.LUNGE, KoscheiAction.LUNGE_STAB, 7));
		for (KoscheiAction action : new KoscheiAction[] {KoscheiAction.STRIKE, KoscheiAction.RED, KoscheiAction.ORANGE, KoscheiAction.NONE}) {
			for (int t = 0; t < 30; t++) {
				assertFalse(KoscheiLogic.parries(action, t, 7));
			}
		}
	}

	@Test
	void resistanceShieldAndBoltNumbers() {
		assertEquals(8.0F, KoscheiLogic.resisted(10.0F, true, 0.2F), 1.0E-4F);
		assertEquals(10.0F, KoscheiLogic.resisted(10.0F, false, 0.2F), 1.0E-4F);
		assertEquals(6.0F, KoscheiLogic.shielded(12.0F, true, 0.5F), 1.0E-4F);
		assertEquals(12.0F, KoscheiLogic.shielded(12.0F, false, 0.5F), 1.0E-4F);
		// Four hearts a second for a second and a half: twelve health in six pulses of two.
		float pulse = KoscheiLogic.boltPulse(8.0F, 5);
		int pulses = (KoscheiAction.BOLT_END - KoscheiAction.BOLT_RELEASE) / 5;
		assertEquals(12.0F, pulse * pulses, 1.0E-4F);
	}

	@Test
	void thePushCarriesTheTargetTheWholeDistance() {
		double speed = KoscheiLogic.pushSpeed(10.0, 13);
		double travelled = 0.0;
		double v = speed;
		for (int i = 0; i < 13; i++) {
			travelled += v;
			v *= 0.91;
		}
		assertEquals(10.0, travelled, 1.0E-6);
		assertTrue(KoscheiLogic.pushSpeed(6.0, 13) < speed);
	}

	@Test
	void theFirstTeleportAngleIsWithinTheLimitAndAllRoundAreTriedAfter() {
		float[] angles = KoscheiLogic.teleportAngles(70.0F, () -> 0.9);
		assertTrue(Math.abs(angles[0]) <= 70.0F);
		assertEquals(-angles[0], angles[1], 1.0E-4F);
		assertEquals(180.0F, angles[angles.length - 1], 1.0E-4F);
	}

	@Test
	void attackPauseFollowsTheAttackSpeed() {
		assertEquals(11, KoscheiSettings.Stats.DEFAULT.attackPause());
	}
}
