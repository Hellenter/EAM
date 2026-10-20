package io.github.hellenter.eam.mobs.entity.koschei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.mobs.entity.koschei.KoscheiLogic.Attack;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiLogic.Choice;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiLogic.StaffAttack;

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

	@Test
	void hopInLandsShortOfTheTarget() {
		assertEquals(2.0, KoscheiLogic.hopInLength(8.0, 2.0, 3.5, 0.0), 1.0E-9);
		assertEquals(3.5, KoscheiLogic.hopInLength(8.0, 2.0, 3.5, 1.0), 1.0E-9);
		assertEquals(2.5, KoscheiLogic.hopInLength(4.5, 2.0, 3.5, 1.0), 1.0E-9);
		assertEquals(0.0, KoscheiLogic.hopInLength(3.5, 2.0, 3.5, 0.5), 1.0E-9);
	}

	@Test
	void afterKnockingAShieldOutTheBoltWaitsTillHalfASecondBeforeItIsBack() {
		assertTrue(KoscheiLogic.boltBlocked(100, 200, 10));
		assertTrue(KoscheiLogic.boltBlocked(189, 200, 10));
		assertFalse(KoscheiLogic.boltBlocked(190, 200, 10));
		assertNotEquals(Attack.BOLT, KoscheiLogic.choose(8.0, false, 0, 0, ready(), null, true, false, ONE, ALWAYS).attack());
	}

	@Test
	void hungryForTheBoltHeShootsFromCloseAndLeavesARaisedShieldAlone() {
		assertEquals(Attack.BOLT, KoscheiLogic.choose(3.0, true, 0, 0, ready(), null, false, true, ONE, ALWAYS).attack());
		int[] noBolt = cooling(Attack.BOLT);
		assertNotEquals(Attack.LUNGE, KoscheiLogic.choose(4.0, true, 0, 0, noBolt, null, false, true, ONE, ALWAYS).attack());
		assertNotEquals(Attack.KICK, KoscheiLogic.choose(1.5, true, 100, 0, noBolt, null, false, true, ONE, ALWAYS).attack());
		for (double roll = 0.0; roll < 1.0; roll += 0.05) {
			double r = roll;
			assertNotEquals(Attack.ORANGE, KoscheiLogic.melee(false, false, ready(), null, ONE, () -> r));
		}
	}

	@Test
	void clickingThroughTheOverheadWindupCostsTheParry() {
		assertFalse(KoscheiLogic.windupSpammed(2, 2));
		assertTrue(KoscheiLogic.windupSpammed(3, 2));
	}

	@Test
	void theRavensAreRareAndNeverCalledWhileAFlockIsAbout() {
		KoscheiSettings.Ravens ravens = KoscheiSettings.DEFAULT.ravens();
		assertTrue(KoscheiLogic.callsRavens(5.0, 0, ravens, 0.0));
		assertFalse(KoscheiLogic.callsRavens(5.0, 0, ravens, ravens.chance()));
		assertFalse(KoscheiLogic.callsRavens(5.0, 1, ravens, 0.0));
		assertFalse(KoscheiLogic.callsRavens(ravens.maxRange() + 0.1, 0, ravens, 0.0));
	}

	@Test
	void aTargetThatKeepsBackingOffGetsTheBoltOrHimBehindIt() {
		KoscheiSettings.Bolt bolt = ONE.bolt();
		assertEquals(Attack.BOLT, KoscheiLogic.answerRetreat(8.0, true, 0, false, bolt));
		assertNull(KoscheiLogic.answerRetreat(8.0, false, 0, false, bolt));
		assertNull(KoscheiLogic.answerRetreat(8.0, true, 1, false, bolt));
		assertNull(KoscheiLogic.answerRetreat(8.0, true, 0, true, bolt));
		assertNull(KoscheiLogic.answerRetreat(bolt.minRange() - 0.5, true, 0, false, bolt));
	}

	@Test
	void aTargetUpOnAPillarFindsHimByItElseIsThrownOffElseGetsTheBolt() {
		KoscheiSettings.Height height = KoscheiSettings.DEFAULT.height();
		assertFalse(KoscheiLogic.outOfReachAbove(2.0, 100, height));
		assertTrue(KoscheiLogic.outOfReachAbove(4.0, height.after(), height));
		assertEquals(KoscheiLogic.HeightAnswer.TELEPORT, KoscheiLogic.answerHeight(true, true, true, true, true, true));
		// Alone on a one-block pillar: no room to stand by it.
		assertEquals(KoscheiLogic.HeightAnswer.FORCE, KoscheiLogic.answerHeight(true, false, true, true, true, true));
		assertEquals(KoscheiLogic.HeightAnswer.BOLT, KoscheiLogic.answerHeight(true, false, true, false, true, true));
		assertNull(KoscheiLogic.answerHeight(false, true, false, true, true, false));
	}

	@Test
	void withTheStaffHeSweepsAwayOneCloseElseRaisesTheDeadNowAndThenElseShoots() {
		PhaseTwoSettings two = PhaseTwoSettings.DEFAULT;
		int[] ready = new int[StaffAttack.values().length];
		assertEquals(StaffAttack.STRIKE, staffAttack(two.strike().reach(), true, ready, 0, false, false, two, () -> 0.99));
		// The rare ball of lightning comes first, but never a second while one is about.
		assertEquals(StaffAttack.BALL, staffAttack(8.0, true, ready, 0, false, false, two, () -> 0.0));
		assertEquals(StaffAttack.SOULS, staffAttack(8.0, true, ready, 0, false, true, two, () -> 0.0));
		// At his cap of draugr he raises no more.
		assertEquals(StaffAttack.ICE_RAVENS, staffAttack(8.0, true, ready, two.thralls().cap(), false, true, two, () -> 0.0));
		assertEquals(StaffAttack.BOLT, staffAttack(8.0, true, ready, 0, false, false, two, () -> 0.99));
		assertNull(staffAttack(8.0, false, ready, 0, false, false, two, () -> 0.99));
		int[] soulsCooling = new int[StaffAttack.values().length];
		soulsCooling[StaffAttack.SOULS.ordinal()] = 1;
		assertEquals(StaffAttack.TRAP, staffAttack(8.0, true, soulsCooling, 0, false, true, two, () -> 0.0));
		int[] boltCooling = new int[StaffAttack.values().length];
		boltCooling[StaffAttack.BOLT.ordinal()] = 1;
		assertNull(staffAttack(8.0, true, boltCooling, 0, false, false, two, () -> 0.99));
	}

	@Test
	void aTargetRunningAtHimMayFindAWallOfIce() {
		PhaseTwoSettings two = PhaseTwoSettings.DEFAULT;
		int[] ready = new int[StaffAttack.values().length];
		assertEquals(StaffAttack.ICE_WALL, staffAttack(6.0, true, ready, 0, true, false, two, () -> 0.0));
		assertEquals(StaffAttack.BALL, staffAttack(6.0, true, ready, 0, false, false, two, () -> 0.0));
		assertEquals(StaffAttack.BALL, staffAttack(two.iceWall().maxDistance() + 1.0, true, ready, 0, true, false, two, () -> 0.0));
	}

	private static StaffAttack staffAttack(double distance, boolean sees, int[] cooldowns, int thralls, boolean rushing, boolean ballAbout,
			PhaseTwoSettings two, java.util.function.DoubleSupplier rolls) {
		// Copies about and the shield bearers cooling down: the attacks of the earlier tests.
		int[] cooling = cooldowns.clone();
		cooling[StaffAttack.TURTLE.ordinal()] = 1;
		return KoscheiLogic.chooseStaff(new KoscheiLogic.StaffSituation(distance, sees, thralls, rushing, ballAbout, true, false, false), cooling,
				two, rolls);
	}

	@Test
	void behindHisShieldBearersHeOnlyShootsAndHurtHeMayDrainOne() {
		PhaseTwoSettings two = PhaseTwoSettings.DEFAULT;
		int[] ready = new int[StaffAttack.values().length];
		assertEquals(StaffAttack.BOLT, KoscheiLogic.chooseStaff(new KoscheiLogic.StaffSituation(8.0, true, 3, false, false, false, true, false),
				ready, two, () -> 0.0));
		assertEquals(StaffAttack.DRAIN, KoscheiLogic.chooseStaff(new KoscheiLogic.StaffSituation(8.0, true, 3, false, false, false, true, true),
				ready, two, () -> 0.0));
		// With no ball to call, ice copies come next, then the shield bearers while there is room for enough of them.
		assertEquals(StaffAttack.ICE_COPIES, KoscheiLogic.chooseStaff(new KoscheiLogic.StaffSituation(8.0, true, 0, false, true, false, false,
				false), ready, two, () -> 0.0));
		assertEquals(StaffAttack.TURTLE, KoscheiLogic.chooseStaff(new KoscheiLogic.StaffSituation(8.0, true, 0, false, true, true, false,
				false), ready, two, () -> 0.0));
		int tooMany = two.thralls().cap() - two.turtle().minRoom() + 1;
		assertEquals(StaffAttack.SOULS, KoscheiLogic.chooseStaff(new KoscheiLogic.StaffSituation(8.0, true, tooMany, false, true, true, false,
				false), ready, two, () -> 0.0));
	}

	@Test
	void theDrainHealsLessEachSecondDownToItsLeast() {
		PhaseTwoSettings.Drain drain = PhaseTwoSettings.DEFAULT.drain();
		assertEquals(10.0F, KoscheiLogic.drainHeal(0, drain));
		assertEquals(9.0F, KoscheiLogic.drainHeal(1, drain));
		assertEquals(7.0F, KoscheiLogic.drainHeal(3, drain));
		assertEquals(7.0F, KoscheiLogic.drainHeal(6, drain));
		assertFalse(KoscheiLogic.mayDrain(100.0F, 120.0F, 0, drain));
		assertTrue(KoscheiLogic.mayDrain(80.0F, 120.0F, 1, drain));
		assertFalse(KoscheiLogic.mayDrain(80.0F, 120.0F, drain.uses(), drain));
	}

	@Test
	void hisNeedleBoresThroughAllButPlainBedrock() {
		assertTrue(KoscheiLogic.needlePierces(1.5F, false));
		assertTrue(KoscheiLogic.needlePierces(50.0F, false));
		assertFalse(KoscheiLogic.needlePierces(-1.0F, false));
		assertTrue(KoscheiLogic.needlePierces(-1.0F, true));
	}

	@Test
	void theSnowStormCatchesOnlyThoseInFrontOfHim() {
		assertTrue(KoscheiLogic.inCone(5.0, 0.0, 1.0, 0.0, 9.0, 35.0F));
		assertTrue(KoscheiLogic.inCone(5.0, 3.0, 1.0, 0.0, 9.0, 35.0F));
		assertFalse(KoscheiLogic.inCone(5.0, 4.0, 1.0, 0.0, 9.0, 35.0F));
		assertFalse(KoscheiLogic.inCone(-5.0, 0.0, 1.0, 0.0, 9.0, 35.0F));
		assertFalse(KoscheiLogic.inCone(10.0, 0.0, 1.0, 0.0, 9.0, 35.0F));
	}

	@Test
	void eachIceRavenAddsALevelOfSlowness() {
		assertEquals(0, KoscheiLogic.stackedSlowness(-1, 4));
		assertEquals(2, KoscheiLogic.stackedSlowness(1, 4));
		assertEquals(4, KoscheiLogic.stackedSlowness(4, 4));
	}

	@Test
	void heNeverRaisesMoreDraugrThanHisCap() {
		assertEquals(3, KoscheiLogic.thrallsToRaise(3, 0, 5));
		assertEquals(2, KoscheiLogic.thrallsToRaise(3, 3, 5));
		assertEquals(0, KoscheiLogic.thrallsToRaise(3, 5, 5));
		assertEquals(0, KoscheiLogic.thrallsToRaise(3, 7, 5));
	}

	@Test
	void hisHealthFillsThroughTheTransformation() {
		assertEquals(1.0F, KoscheiLogic.transformHealth(0, 60, 1.0F, 120.0F));
		assertEquals(60.5F, KoscheiLogic.transformHealth(30, 60, 1.0F, 120.0F), 1.0E-4F);
		assertEquals(120.0F, KoscheiLogic.transformHealth(60, 60, 1.0F, 120.0F));
		assertEquals(120.0F, KoscheiLogic.transformHealth(90, 60, 1.0F, 120.0F));
	}

	@Test
	void hitsCloseTogetherCountUpAndALateOneStartsAgain() {
		assertEquals(1, KoscheiLogic.hitStreak(0, 0L, 1000L, 60));
		assertEquals(2, KoscheiLogic.hitStreak(1, 1000L, 1060L, 60));
		assertEquals(1, KoscheiLogic.hitStreak(1, 1000L, 1061L, 60));
	}

	@Test
	void blizzardFrostBuildsUpToItsCapAndNeverThaws() {
		assertEquals(6, KoscheiLogic.frost(0, 6, 130));
		assertEquals(130, KoscheiLogic.frost(127, 6, 130));
		assertEquals(140, KoscheiLogic.frost(140, 6, 130));
	}

	@Test
	void inTheThirdPhaseHeCutsInReachAndAttacksFromAfarWhenReady() {
		PhaseThreeSettings three = PhaseThreeSettings.DEFAULT;
		int[] ready = new int[KoscheiLogic.DaggerAttack.values().length];
		int[] raiseDown = ready.clone();
		raiseDown[KoscheiLogic.DaggerAttack.RAISE.ordinal()] = 100;
		raiseDown[KoscheiLogic.DaggerAttack.FEINT.ordinal()] = 100;
		assertEquals(KoscheiLogic.DaggerAttack.RAISE, KoscheiLogic.chooseDagger(2.0, true, 3, ready, three, () -> 0.0));
		assertEquals(KoscheiLogic.DaggerAttack.FEINT, KoscheiLogic.chooseDagger(2.0, true, 0, ready, three, () -> 0.0));
		assertEquals(KoscheiLogic.DaggerAttack.COMBO, KoscheiLogic.chooseDagger(2.0, true, 0, raiseDown, three, () -> 0.9));
		assertEquals(KoscheiLogic.DaggerAttack.ORANGE, KoscheiLogic.chooseDagger(2.0, true, 0, raiseDown, three, () -> 0.1));
		assertEquals(KoscheiLogic.DaggerAttack.CHAINS, KoscheiLogic.chooseDagger(6.0, true, 3, raiseDown, three, () -> 0.0));
		int[] chainsDown = raiseDown.clone();
		chainsDown[KoscheiLogic.DaggerAttack.CHAINS.ordinal()] = 100;
		assertEquals(KoscheiLogic.DaggerAttack.LEAP, KoscheiLogic.chooseDagger(6.0, true, 3, chainsDown, three, () -> 0.0));
		assertEquals(KoscheiLogic.DaggerAttack.DASH, KoscheiLogic.chooseDagger(8.0, true, 3, chainsDown, three, () -> 0.0));
		assertNull(KoscheiLogic.chooseDagger(6.0, false, 3, raiseDown, three, () -> 0.0));
		assertNull(KoscheiLogic.chooseDagger(6.0, true, 3, raiseDown, three, () -> 0.99));
		assertNull(KoscheiLogic.chooseDagger(20.0, true, 3, raiseDown, three, () -> 0.0));
	}

	@Test
	void hisLeapIsStoppedOnlyByAShieldRaisedAsHeHangsAtTheTop() {
		// The top of the leap is 6 ticks before it lands; the shield may go up 3 before it to 2 after it.
		assertTrue(KoscheiLogic.leapBlockedInTime(6, 6, 3, 2));
		assertTrue(KoscheiLogic.leapBlockedInTime(9, 6, 3, 2));
		assertTrue(KoscheiLogic.leapBlockedInTime(4, 6, 3, 2));
		assertFalse(KoscheiLogic.leapBlockedInTime(10, 6, 3, 2));
		assertFalse(KoscheiLogic.leapBlockedInTime(3, 6, 3, 2));
		assertFalse(KoscheiLogic.leapBlockedInTime(80, 6, 3, 2));
	}

	@Test
	void blockedCutsCountUpOnlyWhileTheyComeCloseTogether() {
		assertEquals(1, KoscheiLogic.blockedInRow(0, -1000, 0, 60));
		assertEquals(2, KoscheiLogic.blockedInRow(1, 0, 4, 60));
		assertEquals(3, KoscheiLogic.blockedInRow(2, 4, 8, 60));
		assertEquals(1, KoscheiLogic.blockedInRow(2, 4, 100, 60));
	}

	@Test
	void theLeapPeaksHalfwayAndLandsOnTheTarget() {
		assertEquals(0.0, KoscheiLogic.leapHeight(0.0, 1.0, 2.5), 1.0E-9);
		assertEquals(3.0, KoscheiLogic.leapHeight(0.5, 1.0, 2.5), 1.0E-9);
		assertEquals(1.0, KoscheiLogic.leapHeight(1.0, 1.0, 2.5), 1.0E-9);
	}

	@Test
	void heSpeedsUpOnlyAfterTheTargetKeepsAwayAWhile() {
		assertEquals(1.0, KoscheiLogic.chaseSpeed(0, 60, 100, 1.7), 1.0E-9);
		assertEquals(1.0, KoscheiLogic.chaseSpeed(60, 60, 100, 1.7), 1.0E-9);
		assertEquals(1.35, KoscheiLogic.chaseSpeed(110, 60, 100, 1.7), 1.0E-9);
		assertEquals(1.7, KoscheiLogic.chaseSpeed(500, 60, 100, 1.7), 1.0E-9);
	}

	@Test
	void hisFuryShortensThePauseBetweenAttacksDownToItsFloor() {
		assertEquals(14, KoscheiLogic.furyPause(14, 0, 0.18F, 2));
		assertEquals(9, KoscheiLogic.furyPause(14, 2, 0.18F, 2));
		assertEquals(2, KoscheiLogic.furyPause(14, 5, 0.18F, 2));
	}
}
