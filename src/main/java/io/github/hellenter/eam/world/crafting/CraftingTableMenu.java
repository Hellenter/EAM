package io.github.hellenter.eam.world.crafting;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.world.forging.ForgingTags;

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

import java.util.List;

/**
 * The vanilla crafting menu plus three tool slots for knives, saws and hammers ({@link CraftingTags#CRAFTING_TABLE_TOOLS})
 * in any mix. Without a knife the table crafts nothing; each tool makes its crafts faster, tools below iron wear a little
 * with each craft they help, and metal recipes need a knife of the tier below the metal ({@link CraftingTierLogic}). The tools live in the table's
 * block entity.
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

	/** The table crafts only with a knife in it. */
	public boolean hasKnife() {
		return !knifeTiers().isEmpty();
	}

	/** Tiers of the knives in the table, one per knife. */
	public List<Integer> knifeTiers() {
		return CraftingTiers.toolTiers(tools, CraftingTags.KNIFE_TIERS);
	}

	/** Best knife tier in the table, {@link CraftingTierLogic#NONE} without knives. */
	public int bestKnifeTier() {
		return knifeTiers().stream().mapToInt(Integer::intValue).max().orElse(CraftingTierLogic.NONE);
	}

	/**
	 * Server, when a craft of {@code result} is done and before its ingredients are used up: every tool that helped it
	 * (knives always, saws on wooden and decorative results, hammers on metalwork) and is below {@code wearFreeTier}
	 * loses a point of durability.
	 */
	public void wearTools(ServerPlayer player, ItemStack result, int wearFreeTier) {
		boolean sawCraft = result.is(CraftingTags.SAW_RESULTS);
		boolean metal = CraftingTiers.hasMetal(craftSlots);
		for (int i = 0; i < tools.getContainerSize(); i++) {
			ItemStack tool = tools.getItem(i);
			if (tool.isDamageableItem() && CraftingTierLogic.wears(helpingTier(tool, sawCraft, metal), wearFreeTier)) {
				tool.hurtAndBreak(1, player.level(), player, item -> player.level().playSound(null, player.getX(), player.getY(),
						player.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 1.0F));
			}
		}
		tools.setChanged();
	}

	/** Tier of a tool that helps this craft, {@link CraftingTierLogic#NONE} when it does not. */
	private static int helpingTier(ItemStack tool, boolean sawCraft, boolean metal) {
		int knife = CraftingTiers.tier(tool, CraftingTags.KNIFE_TIERS);
		if (knife > CraftingTierLogic.NONE) {
			return knife;
		}
		if (sawCraft) {
			int saw = CraftingTiers.tier(tool, CraftingTags.SAW_TIERS);
			if (saw > CraftingTierLogic.NONE) {
				return saw;
			}
		}
		return metal ? CraftingTiers.tier(tool, ForgingTags.HAMMER_TIERS) : CraftingTierLogic.NONE;
	}

	/** What the tools in the table do for a craft from {@code grid}. */
	public TableTools tools(Container grid) {
		return new TableTools(knifeTiers(), CraftingTiers.toolTiers(tools, CraftingTags.SAW_TIERS),
				CraftingTiers.toolTiers(tools, ForgingTags.HAMMER_TIERS), CraftingTiers.hasMetal(grid));
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
