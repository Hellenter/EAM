package io.github.hellenter.eam.world.crafting;

import io.github.hellenter.eam.Eam;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

/**
 * The vanilla crafting menu plus a tool slot. Without a tool from {@link CraftingTags#CRAFTING_TABLE_TOOLS} the table
 * crafts nothing; every craft costs the tool one durability. The tool lives in the table's block entity.
 */
public class CraftingTableMenu extends CraftingMenu {
	/** After the result, the 3x3 grid and the 36 player slots. */
	public static final int TOOL_SLOT = 46;
	private static final int INVENTORY_START = 10;
	public static final int TOOL_X = 93;
	public static final int TOOL_Y = 53;
	private static final Identifier EMPTY_TOOL_ICON = Eam.id("container/slot/knife");

	private final Container tools;

	/** Client side: the tool comes in through slot sync. */
	public CraftingTableMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(1), ContainerLevelAccess.NULL);
	}

	public CraftingTableMenu(int containerId, Inventory inventory, Container tools, ContainerLevelAccess access) {
		super(containerId, inventory, access);
		this.tools = tools;
		addSlot(new ToolSlot(tools, TOOL_X, TOOL_Y));
	}

	@Override
	public MenuType<?> getType() {
		return CraftingTypes.MENU;
	}

	public boolean hasTool() {
		return CraftingModule.isCraftingTool(tools.getItem(0));
	}

	/** One craft finished: wear the tool down, and stop crafting if it broke. */
	void damageTool(Player player) {
		ItemStack tool = tools.getItem(0);
		if (tool.isEmpty() || !(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		tool.hurtAndBreak(1, serverPlayer.level(), serverPlayer, broken -> serverPlayer.level().playSound(null,
				serverPlayer.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.8F, 1.0F));
		tools.setChanged();
		slotsChanged(craftSlots);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		Slot slot = slots.get(slotIndex);
		Slot toolSlot = slots.get(TOOL_SLOT);
		if (slotIndex >= INVENTORY_START && slotIndex < TOOL_SLOT && slot.hasItem()
				&& toolSlot.mayPlace(slot.getItem()) && !toolSlot.hasItem()) {
			ItemStack stack = slot.getItem();
			ItemStack clicked = stack.copy();
			if (moveItemStackTo(stack, TOOL_SLOT, TOOL_SLOT + 1, false)) {
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
		ToolSlot(Container tools, int x, int y) {
			super(tools, 0, x, y);
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
