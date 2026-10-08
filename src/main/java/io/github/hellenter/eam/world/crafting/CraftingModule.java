package io.github.hellenter.eam.world.crafting;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;

/**
 * Crafting takes time: the result waits in its slot until the craft has run (by hand at full time, on a crafting
 * table at {@link CraftTimeRules#craftingTableMultiplier()}). The crafter block is not touched and crafts at once.
 * The crafting table needs a tool in its own slot, kept in the table between uses.
 */
public final class CraftingModule {
	private static final Component CRAFTING_TITLE = Component.translatable("container.crafting");

	private static volatile CraftTimeRules rules = CraftTimeRules.DEFAULT;
	private static boolean enabled;

	private CraftingModule() {
	}

	/** The block entity and menu exist whatever the config says: the vanilla table always has the block entity. */
	public static void register() {
		CraftingTypes.register();
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(CraftTimeRulesLoader.ID, new CraftTimeRulesLoader());
	}

	public static void init() {
		enabled = true;
	}

	static void setRules(CraftTimeRules value) {
		rules = value;
	}

	public static boolean isCraftingTool(ItemStack stack) {
		return stack.is(CraftingTags.CRAFTING_TABLE_TOOLS);
	}

	/** Only the crafting table itself keeps a block entity, not the smithing table that extends its class. */
	public static boolean hasBlockEntity(Block block, boolean original) {
		// Runs for every block state, also while blocks are still being created: compare classes, not registry entries.
		return original && (!(block instanceof CraftingTableBlock) || block.getClass() == CraftingTableBlock.class);
	}

	public static @Nullable CraftingTableBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.is(Blocks.CRAFTING_TABLE) ? new CraftingTableBlockEntity(pos, state) : null;
	}

	/** The crafting table opens the menu with the tool slot. */
	public static MenuProvider tableMenu(MenuProvider original, BlockState state, Level level, BlockPos pos) {
		if (!enabled || !state.is(Blocks.CRAFTING_TABLE) || !(level.getBlockEntity(pos) instanceof CraftingTableBlockEntity table)) {
			return original;
		}
		return new SimpleMenuProvider((containerId, inventory, player) ->
				new CraftingTableMenu(containerId, inventory, table.tools(), ContainerLevelAccess.create(level, pos)), CRAFTING_TITLE);
	}

	/** Server: what the result slot shows. A crafting table without a tool crafts nothing. */
	public static ItemStack result(AbstractContainerMenu menu, ItemStack result) {
		return menu instanceof CraftingTableMenu table && !table.hasTool() ? ItemStack.EMPTY : result;
	}

	/** Server: the grid has a new result, so the craft starts over. */
	public static void resultChanged(AbstractContainerMenu menu, Player player, CraftingContainer grid, ResultContainer resultSlots) {
		if (!(menu instanceof TimedCraftingMenu timed)) {
			return;
		}
		ItemStack result = resultSlots.getItem(0);
		int ticks = !enabled || result.isEmpty() || player.hasInfiniteMaterials()
				? 0
				: rules.ticks(result, ingredients(grid), menu instanceof CraftingTableMenu);
		timed.eam$craftTimer().restart(player.level().getGameTime(), ticks);
	}

	private static int ingredients(CraftingContainer grid) {
		int count = 0;
		for (ItemStack stack : grid.getItems()) {
			if (!stack.isEmpty()) {
				count++;
			}
		}
		return count;
	}

	/** Both sides: the result can be taken once the craft has run, and on a table only with a tool in it. */
	public static boolean mayTakeResult(Player player) {
		AbstractContainerMenu menu = player.containerMenu;
		if (menu instanceof CraftingTableMenu table && !table.hasTool()) {
			return false;
		}
		return !(menu instanceof TimedCraftingMenu timed) || timed.eam$craftTimer().ready();
	}

	/** Both sides: a result was taken. The next craft starts from zero and the table's tool wears down. */
	public static void crafted(Player player) {
		AbstractContainerMenu menu = player.containerMenu;
		if (menu instanceof TimedCraftingMenu timed) {
			timed.eam$craftTimer().crafted();
		}
		if (menu instanceof CraftingTableMenu table) {
			table.damageTool(player);
		}
	}

	/** Shift-click on the result crafts again only while the slot can still be taken from: one craft per wait. */
	public static boolean keepQuickMoving(AbstractContainerMenu menu, Player player, int slotIndex) {
		return menu.getSlot(slotIndex).mayPickup(player);
	}
}
