package io.github.hellenter.eam.world.crafting;

import io.github.hellenter.eam.Eam;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * The vanilla crafting menu plus three tool slots. Without a tool from {@link CraftingTags#CRAFTING_TABLE_TOOLS} the
 * table crafts nothing; tools are not used up, but each one makes crafting faster, and metal recipes need a tool of the
 * tier below the metal ({@link CraftingTierLogic}). The tools live in the table's block entity.
 */
public class CraftingTableMenu extends CraftingMenu {
	public static final int TOOL_COUNT = 3;
	/** After the result, the 3x3 grid and the 36 player slots. */
	public static final int FIRST_TOOL_SLOT = 46;
	private static final int INVENTORY_START = 10;
	/** The tool slots sit in a row under the arrow and the result. */
	public static final int TOOL_X = 93;
	public static final int TOOL_Y = 53;
	public static final int TOOL_SPACING = 18;
	private static final Identifier EMPTY_TOOL_ICON = Eam.id("container/slot/knife");

	private final Container tools;

	/** Client side: the tools come in through slot sync. */
	public CraftingTableMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(TOOL_COUNT), ContainerLevelAccess.NULL);
	}

	public CraftingTableMenu(int containerId, Inventory inventory, Container tools, ContainerLevelAccess access) {
		super(containerId, inventory, access);
		this.tools = tools;
		for (int i = 0; i < TOOL_COUNT; i++) {
			addSlot(new ToolSlot(tools, i, TOOL_X + i * TOOL_SPACING, TOOL_Y));
		}
	}

	@Override
	public MenuType<?> getType() {
		return CraftingTypes.MENU;
	}

	public static boolean isToolSlot(int slotIndex) {
		return slotIndex >= FIRST_TOOL_SLOT && slotIndex < FIRST_TOOL_SLOT + TOOL_COUNT;
	}

	public boolean hasTool() {
		return !toolTiers().isEmpty();
	}

	/** Tiers of the tools in the table, one per tool. */
	public List<Integer> toolTiers() {
		return CraftingTiers.toolTiers(tools);
	}

	/** Best tool tier in the table, {@link CraftingTierLogic#NONE} without tools. */
	public int bestToolTier() {
		return toolTiers().stream().mapToInt(Integer::intValue).max().orElse(CraftingTierLogic.NONE);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		Slot slot = slots.get(slotIndex);
		if (slotIndex >= INVENTORY_START && slotIndex < FIRST_TOOL_SLOT && slot.hasItem()
				&& CraftingModule.isCraftingTool(slot.getItem())) {
			ItemStack stack = slot.getItem();
			ItemStack clicked = stack.copy();
			if (moveItemStackTo(stack, FIRST_TOOL_SLOT, FIRST_TOOL_SLOT + TOOL_COUNT, false)) {
				if (stack.isEmpty()) {
					slot.setByPlayer(ItemStack.EMPTY);
				} else {
					slot.setChanged();
				}
				return clicked;
			}
		}
		return super.quickMoveStack(player, slotIndex);
	}

	private final class ToolSlot extends Slot {
		ToolSlot(Container tools, int index, int x, int y) {
			super(tools, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return CraftingModule.isCraftingTool(stack);
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}

		@Override
		public Identifier getNoItemIcon() {
			return EMPTY_TOOL_ICON;
		}

		@Override
		public void setChanged() {
			super.setChanged();
			slotsChanged(craftSlots);
		}
	}
}
