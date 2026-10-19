package io.github.hellenter.eam.mobs.vanilla.villager;

import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A villager's gear as one container: armor, the main hand (only what a player gave him; his own work item shows in its
 * own slot), the off hand, his work item and his inventory. The gear slots show what he wears or, if he has put it away,
 * what he keeps for them. Server side only; the client menu uses a plain container.
 */
final class VillagerGearContainer implements Container {
	static final int HEAD = 0;
	static final int CHEST = 1;
	static final int LEGS = 2;
	static final int FEET = 3;
	static final int MAIN_HAND = 4;
	static final int OFF_HAND = 5;
	static final int WORK_ITEM = 6;
	static final int INVENTORY = 7;
	static final int INVENTORY_SIZE = 8;
	static final int SIZE = INVENTORY + INVENTORY_SIZE;
	static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	private final Villager villager;

	VillagerGearContainer(Villager villager) {
		this.villager = villager;
	}

	Villager villager() {
		return villager;
	}

	@Override
	public int getContainerSize() {
		return SIZE;
	}

	@Override
	public boolean isEmpty() {
		for (int i = 0; i < SIZE; i++) {
			if (!getItem(i).isEmpty()) return false;
		}
		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		if (slot < MAIN_HAND) return VillagerMilitiaModule.gear(villager, ARMOR[slot]);
		if (slot == MAIN_HAND) return VillagerMilitiaModule.gear(villager, EquipmentSlot.MAINHAND);
		if (slot == OFF_HAND) return VillagerMilitiaModule.gear(villager, EquipmentSlot.OFFHAND);
		if (slot == WORK_ITEM) return VillagerGearModule.workItem(villager);
		return villager.getInventory().getItem(slot - INVENTORY);
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		ItemStack stack = getItem(slot);
		if (stack.isEmpty() || slot == WORK_ITEM) return ItemStack.EMPTY;
		ItemStack taken = stack.split(count);
		setItem(slot, stack);
		return taken;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack stack = getItem(slot);
		if (stack.isEmpty() || slot == WORK_ITEM) return ItemStack.EMPTY;
		setItem(slot, ItemStack.EMPTY);
		return stack;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		if (slot < MAIN_HAND) {
			give(ARMOR[slot], stack);
		} else if (slot == MAIN_HAND) {
			give(EquipmentSlot.MAINHAND, stack);
		} else if (slot == OFF_HAND) {
			give(EquipmentSlot.OFFHAND, stack);
		} else if (slot >= INVENTORY) {
			villager.getInventory().setItem(slot - INVENTORY, stack);
		}
	}

	/** Gear a player puts in or takes out replaces both what the villager wears and what he put away. */
	private void give(EquipmentSlot slot, ItemStack stack) {
		VillagerMilitiaModule.clearStowed(villager, slot);
		VillagerGearModule.give(villager, slot, stack);
	}

	@Override
	public void setChanged() {
	}

	@Override
	public boolean stillValid(Player player) {
		return villager.isAlive() && !villager.isBaby() && player.isWithinEntityInteractionRange(villager, 4.0);
	}

	@Override
	public void clearContent() {
	}
}
