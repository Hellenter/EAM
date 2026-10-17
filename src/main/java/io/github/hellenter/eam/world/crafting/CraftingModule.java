package io.github.hellenter.eam.world.crafting;

import io.github.hellenter.eam.mixin.world.crafting.AbstractContainerMenuAccessor;
import io.github.hellenter.eam.mixin.world.crafting.ResultSlotAccessor;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;

/**
 * Crafting takes time: a click on the offered result starts the craft, and when it has run (by hand at full time, on
 * a crafting table at {@link CraftTimeRules#craftingTableMultiplier()}) the ingredients are used up and the result
 * waits in the slot; the next craft follows while the grid still makes it. The crafter block is not touched.
 * The crafting table needs a knife in one of its three tool slots, which also take saws and hammers and keep them in
 * the table between uses; they make crafting faster, and those below iron wear a point per craft they help. Crafting
 * with metal needs a knife of the tier below it in the table, so the inventory grid cannot craft with metal at all
 * ({@link CraftingTierLogic}).
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
		ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(CraftingModule::tick));
	}

	static void setRules(CraftTimeRules value) {
		rules = value;
	}

	/** Whether this server runs craft times. The client learns it from the menu's synced flags, not from here. */
	static boolean enabled() {
		return enabled;
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

	/**
	 * Server: what the result slot shows: finished crafts, else what the grid makes. A table without a knife makes
	 * nothing, and metal needs a knife of the tier below it.
	 */
	public static ItemStack result(AbstractContainerMenu menu, ItemStack result, CraftingContainer grid) {
		ItemStack preview = mayCraft(menu, result, grid) ? result : ItemStack.EMPTY;
		return menu instanceof TimedCraftingMenu timed ? timed.eam$craftTimer().updatePreview(preview) : preview;
	}

	private static boolean mayCraft(AbstractContainerMenu menu, ItemStack result, CraftingContainer grid) {
		if (menu instanceof CraftingTableMenu table) {
			// The table menu exists only while the module is on.
			return table.hasKnife() && CraftingTierLogic.canCraft(CraftingTiers.materialTier(grid, result), table.bestKnifeTier());
		}
		return !enabled || !(menu instanceof TimedCraftingMenu)
				|| CraftingTierLogic.canCraft(CraftingTiers.materialTier(grid, result), CraftingTierLogic.NONE);
	}

	/** Server: the grid was evaluated again; the craft goes on, starts over or stops. */
	public static void resultChanged(AbstractContainerMenu menu, Player player, CraftingContainer grid) {
		if (!(menu instanceof TimedCraftingMenu timed)) {
			return;
		}
		CraftTimer timer = timed.eam$craftTimer();
		ItemStack preview = timer.preview();
		int ticks = !enabled || preview.isEmpty() || player.hasInfiniteMaterials()
				? 0
				: rules.ticks(preview, ingredients(grid), menu instanceof CraftingTableMenu table ? table.tools(grid) : null);
		timer.afterPreview(ticks);
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

	/**
	 * Both sides, before a click is handled: a click on the offered result starts the craft instead of taking it.
	 * Returns true when the click is used up.
	 */
	public static boolean clickResult(AbstractContainerMenu menu, int slotIndex, ContainerInput input, Player player) {
		if (!(menu instanceof AbstractCraftingMenu crafting) || !(menu instanceof TimedCraftingMenu timed)
				|| slotIndex < 0 || slotIndex >= menu.slots.size() || menu.getSlot(slotIndex) != crafting.getResultSlot()) {
			return false;
		}
		CraftTimer timer = timed.eam$craftTimer();
		if (!timer.active() || timer.hasOutput()) {
			return false;
		}
		if (!player.level().isClientSide() && (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE)) {
			timer.start();
		}
		return true;
	}

	/** Both sides: only finished crafts can be taken from the result slot. */
	public static boolean mayTakeResult(Player player) {
		return !(player.containerMenu instanceof TimedCraftingMenu timed) || !timed.eam$craftTimer().active()
				|| timed.eam$craftTimer().hasOutput();
	}

	/**
	 * Both sides, when something is taken from the result slot. Returns true when that was finished crafts, which were
	 * paid for already: the vanilla take must not use up ingredients again.
	 */
	public static boolean takeOutput(Player player, ResultSlot slot) {
		if (!(player.containerMenu instanceof TimedCraftingMenu timed)) {
			return false;
		}
		CraftTimer timer = timed.eam$craftTimer();
		if (!timer.active() || timer.completing()) {
			return false;
		}
		((ResultSlotAccessor) slot).eam$setRemoveCount(0);
		if (player.level().isClientSide()) {
			timer.outputTakenOnClient();
		} else {
			timer.setOutput(ItemStack.EMPTY);
			timed.eam$resultSlots().setItem(0, timer.preview());
		}
		return true;
	}

	/** Server, every tick for the open menu: finish a craft whose time has run. */
	public static void tick(ServerPlayer player) {
		if (!(player.containerMenu instanceof AbstractCraftingMenu menu) || !(menu instanceof TimedCraftingMenu timed)) {
			return;
		}
		CraftTimer timer = timed.eam$craftTimer();
		if (!timer.due()) {
			return;
		}
		ItemStack crafted = timer.preview().copy();
		ResultSlot slot = (ResultSlot) menu.getResultSlot();
		timer.setCompleting(true);
		try {
			ItemStack output = timer.output();
			if (output.isEmpty()) {
				timer.setOutput(crafted.copy());
			} else if (ItemStack.isSameItemSameComponents(output, crafted)
					&& output.getCount() + crafted.getCount() <= output.getMaxStackSize()) {
				output.grow(crafted.getCount());
			} else {
				AbstractContainerMenuAccessor.eam$dropOrPlaceInInventory(player, crafted.copy());
			}
			if (menu instanceof CraftingTableMenu table && !player.hasInfiniteMaterials()) {
				table.wearTools(player, crafted, rules.wearFreeTier());
			}
			// Vanilla take: stats, recipe unlocks, ingredients used up, remainders; the grid is evaluated again.
			((ResultSlotAccessor) slot).eam$setRemoveCount(crafted.getCount());
			slot.onTake(player, crafted);
		} finally {
			timer.setCompleting(false);
		}
		timed.eam$resultSlots().setItem(0, timer.output().isEmpty() ? timer.preview() : timer.output());
	}

	/** Server, when the menu closes: the craft in progress is dropped and finished crafts go to the player. */
	public static void closed(AbstractContainerMenu menu, Player player) {
		if (!(menu instanceof TimedCraftingMenu timed) || player.level().isClientSide()) {
			return;
		}
		CraftTimer timer = timed.eam$craftTimer();
		timer.stop();
		ItemStack output = timer.output();
		if (!output.isEmpty()) {
			timer.setOutput(ItemStack.EMPTY);
			timed.eam$resultSlots().setItem(0, ItemStack.EMPTY);
			AbstractContainerMenuAccessor.eam$dropOrPlaceInInventory(player, output);
		}
	}

	/** Shift-click on the result takes the finished crafts once, not again and again. */
	public static boolean keepQuickMoving(AbstractContainerMenu menu, Player player, int slotIndex) {
		return menu.getSlot(slotIndex).mayPickup(player);
	}
}
