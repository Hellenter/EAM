package io.github.hellenter.eam.world.tools;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.Optional;

class GearLogicTest {
	// Sets are helmet, chestplate, leggings, boots.

	@Test
	void fullIronSetGivesTwelve() {
		assertArrayEquals(new int[] {2, 5, 4, 1}, GearLogic.scaleSet(new int[] {2, 6, 5, 2}, GearLogic.ARMOR_FACTOR));
	}

	@Test
	void otherSetsLoseAboutAFifth() {
		assertArrayEquals(new int[] {1, 2, 2, 1}, GearLogic.scaleSet(new int[] {1, 3, 2, 1}, GearLogic.ARMOR_FACTOR));
		assertArrayEquals(new int[] {2, 3, 2, 1}, GearLogic.scaleSet(new int[] {2, 4, 3, 1}, GearLogic.ARMOR_FACTOR));
		assertArrayEquals(new int[] {2, 4, 3, 1}, GearLogic.scaleSet(new int[] {2, 5, 4, 1}, GearLogic.ARMOR_FACTOR));
		assertArrayEquals(new int[] {2, 4, 2, 1}, GearLogic.scaleSet(new int[] {2, 5, 3, 1}, GearLogic.ARMOR_FACTOR));
		assertArrayEquals(new int[] {3, 6, 5, 2}, GearLogic.scaleSet(new int[] {3, 8, 6, 3}, GearLogic.ARMOR_FACTOR));
	}

	@Test
	void rustySetsArePointBelowTheirOriginals() {
		// Rusty iron ends like chainmail, rusty chainmail two points lower.
		assertArrayEquals(new int[] {2, 4, 3, 1}, GearLogic.scaleSet(new int[] {2, 5, 4, 2}, GearLogic.ARMOR_FACTOR));
		assertArrayEquals(new int[] {2, 3, 2, 1}, GearLogic.scaleSet(new int[] {2, 4, 3, 1}, GearLogic.ARMOR_FACTOR));
	}

	@Test
	void chainmailSetsArePointAboveThePlateOfTheTierBelow() {
		// Copper and silver 7 (leather 6), iron 9 (copper 8), ancient metal 13 (iron 12), mithril 15 (ancient metal 14).
		assertArrayEquals(new int[] {1, 3, 2, 1}, GearLogic.scaleSet(new int[] {1, 4, 3, 1}, GearLogic.ARMOR_FACTOR));
		assertArrayEquals(new int[] {2, 4, 2, 1}, GearLogic.scaleSet(new int[] {2, 5, 3, 1}, GearLogic.ARMOR_FACTOR));
		assertArrayEquals(new int[] {2, 5, 4, 2}, GearLogic.scaleSet(new int[] {3, 6, 5, 2}, GearLogic.ARMOR_FACTOR));
		assertArrayEquals(new int[] {2, 6, 5, 2}, GearLogic.scaleSet(new int[] {3, 7, 6, 3}, GearLogic.ARMOR_FACTOR));
	}

	@Test
	void pieceWithArmorKeepsAPoint() {
		assertArrayEquals(new int[] {1, 0, 0, 0}, GearLogic.scaleSet(new int[] {1, 0, 0, 0}, 0.3));
	}

	@Test
	void toolDurability() {
		assertEquals(Optional.of(24), GearLogic.toolDurability("copper_pickaxe"));
		assertEquals(Optional.of(250), GearLogic.toolDurability("iron_sword"));
		assertEquals(Optional.empty(), GearLogic.toolDurability("golden_pickaxe"));
		// Netherite lasts three times as long as adamantium, which keeps vanilla netherite's 2031.
		assertEquals(Optional.of(6093), GearLogic.toolDurability("netherite_pickaxe"));
		assertEquals(Optional.empty(), GearLogic.toolDurability("adamantium_pickaxe"));
	}

	@Test
	void copperArmorHalved() {
		assertEquals(88, GearLogic.copperArmorDurability(176));
	}

	@Test
	void netheriteArmorTripled() {
		assertEquals(1221, GearLogic.netheriteArmorDurability(407));
	}

	@Test
	void fullArmorSetsKeepVanillaPoints() {
		// Adamantium and netherite: vanilla netherite's 3/8/6/3 = 20.
		assertArrayEquals(new int[] {3, 8, 6, 3}, GearLogic.scaleSet(new int[] {3, 8, 6, 3}, 1.0));
	}
}
