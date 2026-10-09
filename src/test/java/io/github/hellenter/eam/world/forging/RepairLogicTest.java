package io.github.hellenter.eam.world.forging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RepairLogicTest {
	@Test
	void chestplateFromQuarterToFullTakesEightNuggets() {
		int max = 192;
		int damage = max * 3 / 4;
		float perNugget = RepairLogic.perNugget(max, 8);
		assertEquals(8, RepairLogic.itemsUsed(damage, perNugget, 64));
		assertEquals(damage, RepairLogic.repaired(damage, perNugget, 8));
	}

	@Test
	void partialRepairUsesWhatIsThere() {
		float perNugget = RepairLogic.perNugget(120, 3);
		assertEquals(2, RepairLogic.itemsUsed(100, perNugget, 2));
		assertEquals(60, RepairLogic.repaired(100, perNugget, 2));
	}

	@Test
	void smallDamageTakesOneNugget() {
		assertEquals(1, RepairLogic.itemsUsed(1, RepairLogic.perNugget(250, 2), 10));
		assertEquals(0, RepairLogic.itemsUsed(0, RepairLogic.perNugget(250, 2), 10));
	}

	@Test
	void anIngotIsWorthNineNuggets() {
		float perIngot = RepairLogic.perNugget(192, 8) * RepairLogic.NUGGETS_PER_INGOT;
		assertEquals(1, RepairLogic.itemsUsed(144, perIngot, 5));
		assertEquals(144, RepairLogic.repaired(144, perIngot, 1));
	}
}
