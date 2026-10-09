package io.github.hellenter.eam.world.forging;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.mixin.world.crafting.AbstractContainerMenuAccessor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;

/**
 * Iron plates for iron armor. An iron ingot smelts into hot iron, the anvil hammers one hot iron from its left and one
 * from its right slot into a plate in five seconds, and hot iron left alone turns back into an ingot a minute after it
 * leaves the furnace. Iron armor takes plates and ingots. Mithril works the same way, but only the obsidian blast
 * furnace heats it and only the mithril anvil forges it. The recipes are datagen files that load only while the {@code iron_plates} flag is on ({@link #ENABLED}); with it off the vanilla ingot armor recipes return.
 */
public final class ForgingModule {
	/** Game time at which a hot iron stack turns back into iron ingots; set when it leaves the furnace. */
	public static final DataComponentType<Long> COOLS_AT = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Eam.id("cools_at"),
			DataComponentType.<Long>builder().persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG).build());

	/** True while the {@code iron_plates} flag is on; the plate recipes load under it, the ingot ones under {@code not}. */
	public static final ResourceCondition ENABLED = new EnabledCondition();

	private static final ResourceConditionType<EnabledCondition> ENABLED_TYPE =
			ResourceConditionType.create(Eam.id("iron_plates_enabled"), MapCodec.unit(EnabledCondition::new));

	private static boolean enabled;
	private static boolean anvilTiers;

	private ForgingModule() {
	}

	/** The component and condition types must exist even with the module off, or saved items and recipe files fail to load. */
	public static void register() {
		ResourceConditions.register(ENABLED_TYPE);
		ForgingTypes.register();
	}

	/** Every anvil gets a block entity for its hammer. */
	public static @Nullable AnvilBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.is(BlockTags.ANVIL) ? new AnvilBlockEntity(pos, state) : null;
	}

	/** Copper and silver anvils work only on items of their tier and below; the iron anvil works on everything. */
	public static void initAnvilTiers() {
		anvilTiers = true;
	}

	/** Server: the tier the menu reports for the anvil block at its position. */
	public static int anvilTier(ForgingAnvilMenu menu) {
		return anvilTiers ? menu.eam$access().evaluate((level, pos) -> AnvilTiers.anvilTier(level.getBlockState(pos)), AnvilTiers.IRON)
				: AnvilTiers.IRON;
	}

	/**
	 * Both sides, before the vanilla anvil result: true when this anvil is too weak for the item in its left slot, which
	 * then gets no result at all, not even a new name.
	 */
	public static boolean tooWeak(ForgingAnvilMenu menu) {
		ItemStack item = menu.eam$inputSlots().getItem(0);
		return !item.isEmpty() && !ForgingLogic.canWorkOn(menu.eam$forge().anvilTier(), AnvilTiers.itemTier(item));
	}

	public static boolean isHammer(ItemStack stack) {
		return stack.is(ForgingTags.HAMMERS);
	}

	/**
	 * Both sides, on a shift-click: a hammer from the inventory goes into the empty hammer slot, and the hammer slot
	 * empties into the inventory. Returns null when the vanilla anvil should handle the click.
	 */
	public static @Nullable ItemStack quickMoveHammer(AbstractContainerMenu menu, int slotIndex) {
		int hammerIndex = menu.slots.size() - 1;
		int inventoryStart = ((ItemCombinerMenu) menu).getResultSlot() + 1;
		if (slotIndex < 0 || slotIndex > hammerIndex || !(menu.slots.get(hammerIndex) instanceof HammerSlot hammerSlot)) {
			return null;
		}
		Slot slot = menu.slots.get(slotIndex);
		ItemStack stack = slot.getItem();
		ItemStack clicked = stack.copy();
		AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) menu;
		boolean moved;
		if (slotIndex == hammerIndex) {
			moved = accessor.eam$moveItemStackTo(stack, inventoryStart, hammerIndex, true);
		} else if (slotIndex >= inventoryStart && isHammer(stack) && !hammerSlot.hasItem()) {
			moved = accessor.eam$moveItemStackTo(stack, hammerIndex, hammerIndex + 1, false);
		} else {
			return null;
		}
		if (!moved) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return clicked;
	}

	public static void init() {
		enabled = true;
		ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(ForgingModule::tick));
	}

	/** Game time at which {@code stack} cools; hot iron still in the furnace output has not started cooling. */
	public static long coolsAt(ItemStack stack) {
		return stack.getOrDefault(COOLS_AT, ForgingLogic.NOT_STARTED);
	}

	/** The plate {@code input} is forged into and the anvil tier that takes, or null for anything else. */
	private static @Nullable Plate plate(ItemStack input) {
		if (input.is(EamItems.HOT_IRON_INGOT)) {
			return new Plate(EamItems.IRON_PLATE, ForgingLogic.PLATE_ANVIL_TIER);
		}
		if (input.is(EamItems.HOT_ANCIENT_METAL_INGOT)) {
			return new Plate(EamItems.ANCIENT_METAL_PLATE, ForgingLogic.PLATE_ANVIL_TIER);
		}
		if (input.is(EamItems.HOT_MITHRIL_INGOT)) {
			return new Plate(EamItems.MITHRIL_PLATE, ForgingLogic.MITHRIL_PLATE_ANVIL_TIER);
		}
		return null;
	}

	/** Both sides: the same hot metal lies in the left and the right slot and can be forged now. */
	private static boolean canForge(ForgingAnvilMenu menu, long now) {
		ItemStack left = menu.eam$inputSlots().getItem(0);
		ItemStack right = menu.eam$inputSlots().getItem(1);
		Plate plate = plate(left);
		return plate != null && plate.equals(plate(right))
				&& ForgingLogic.canForgeOn(menu.eam$forge().anvilTier(), plate.anvilTier())
				&& ForgingLogic.canForge(left.getCount(), right.getCount(),
				ForgingLogic.isCooled(coolsAt(left), now) || ForgingLogic.isCooled(coolsAt(right), now));
	}

	/** Server: a fresh plate for the hot metal in the left slot; only called when {@link #canForge} holds. */
	private static ItemStack newPlate(ForgingAnvilMenu menu) {
		Plate plate = plate(menu.eam$inputSlots().getItem(0));
		return plate == null ? ItemStack.EMPTY : new ItemStack(plate.item());
	}

	/**
	 * Both sides, instead of the vanilla anvil result. Returns true when the anvil is forging: the result slot then
	 * shows the forged plates, else the plate on offer. The client keeps what the server sends it.
	 */
	public static boolean updateResult(ForgingAnvilMenu menu, Player player) {
		AnvilForge forge = menu.eam$forge();
		if (!forge.active()) {
			return false;
		}
		boolean canForge = canForge(menu, player.level().getGameTime());
		if (player.level().isClientSide()) {
			return canForge || forge.hasOutput();
		}
		if (!canForge) {
			forge.stop();
		}
		ItemStack output = forge.output();
		if (output.isEmpty() && !canForge) {
			return false;
		}
		menu.eam$resultSlots().setItem(0, output.isEmpty() ? newPlate(menu) : output.copy());
		resendResult(menu);
		return true;
	}

	/**
	 * Server: send the result slot to the client even if the server thinks the client already has it. The client fills
	 * its result slot itself when the inputs change, and without the synced flags it can take the vanilla path and
	 * clear it. A forged plate looks the same as the offered one, so the server would not send it again and the
	 * plates stay invisible (and the anvil shows its cross) while they can still be taken.
	 */
	private static void resendResult(ForgingAnvilMenu menu) {
		ItemCombinerMenu combiner = (ItemCombinerMenu) menu;
		combiner.setRemoteSlot(combiner.getResultSlot(), ItemStack.EMPTY);
	}

	/**
	 * Both sides, before a click is handled: a click on the offered plate starts forging instead of taking it. Returns
	 * true when the click is used up.
	 */
	public static boolean clickResult(ForgingAnvilMenu menu, int slotIndex, int resultSlot, ContainerInput input, Player player) {
		AnvilForge forge = menu.eam$forge();
		if (slotIndex != resultSlot || !forge.active() || forge.hasOutput() || !canForge(menu, player.level().getGameTime())) {
			return false;
		}
		if (!player.level().isClientSide() && !forge.running() && (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE)) {
			forge.start(forgeTicks(menu, player));
		}
		return true;
	}

	private static int forgeTicks(ForgingAnvilMenu menu, Player player) {
		return player.hasInfiniteMaterials() ? 0 : ForgingLogic.forgeTicks(AnvilTiers.hammerTier(menu.eam$hammer().getItem(0)));
	}

	/** Both sides: only forged plates can be taken from the result slot, and they cost no experience. */
	public static boolean mayTakeResult(ForgingAnvilMenu menu) {
		return menu.eam$forge().active() && menu.eam$forge().hasOutput();
	}

	/** Both sides: forged plates and free repairs can be taken without experience. */
	public static boolean mayTakeFree(ForgingAnvilMenu menu) {
		return mayTakeResult(menu) || menu.eam$forge().repairing() && !menu.eam$resultSlots().getItem(0).isEmpty();
	}

	/**
	 * Both sides, when something is taken from the result slot. Returns true when that was forged plates, which were
	 * paid for already: the vanilla take must not use up the slots, take experience or wear the anvil.
	 */
	public static boolean takeOutput(ForgingAnvilMenu menu, Player player) {
		AnvilForge forge = menu.eam$forge();
		if (!mayTakeResult(menu)) {
			return false;
		}
		// A shift-click into a nearly full inventory leaves some plates in the slot.
		ItemStack left = menu.eam$resultSlots().getItem(0);
		if (!left.isEmpty()) {
			if (!player.level().isClientSide()) {
				forge.setOutput(left.copy());
			}
		} else if (player.level().isClientSide()) {
			forge.outputTakenOnClient();
		} else {
			forge.setOutput(ItemStack.EMPTY);
			if (!updateResult(menu, player)) {
				menu.eam$resultSlots().setItem(0, ItemStack.EMPTY);
			}
		}
		return true;
	}

	/** Server, every tick for the open menu: finish a plate that has been hammered long enough. */
	private static void tick(ServerPlayer player) {
		if (!(player.containerMenu instanceof ForgingAnvilMenu menu) || !menu.eam$forge().due()) {
			return;
		}
		AnvilForge forge = menu.eam$forge();
		long now = player.level().getGameTime();
		if (!canForge(menu, now)) {
			forge.stop();
			return;
		}
		ItemStack plate = newPlate(menu);
		ItemStack output = forge.output();
		if (output.isEmpty()) {
			forge.setOutput(plate);
		} else if (ItemStack.isSameItemSameComponents(output, plate) && output.getCount() < output.getMaxStackSize()) {
			output.grow(1);
		} else {
			AbstractContainerMenuAccessor.eam$dropOrPlaceInInventory(player, plate);
		}
		// Updates the result slot through the vanilla slot change.
		menu.eam$inputSlots().removeItem(0, ForgingLogic.HOT_IRON_PER_SLOT);
		menu.eam$inputSlots().removeItem(1, ForgingLogic.HOT_IRON_PER_SLOT);
		if (canForge(menu, now)) {
			forge.start(forgeTicks(menu, player));
		} else {
			forge.stop();
		}
		menu.eam$access().execute((level, pos) -> level.levelEvent(LevelEvent.SOUND_ANVIL_USED, pos, 0));
	}

	/**
	 * Server, when the menu closes: the plate being hammered is dropped and forged plates go to the player. The hammer
	 * stays in the anvil; only one with no anvil to keep it goes back. Runs whatever the config says, since the hammer
	 * slot is always there.
	 */
	public static void closed(ForgingAnvilMenu menu, Player player) {
		AnvilForge forge = menu.eam$forge();
		if (player.level().isClientSide()) {
			return;
		}
		ItemStack hammer = menu.eam$hammer().takeLoose();
		if (!hammer.isEmpty()) {
			AbstractContainerMenuAccessor.eam$dropOrPlaceInInventory(player, hammer);
		}
		forge.stop();
		ItemStack output = forge.output();
		if (!output.isEmpty()) {
			forge.setOutput(ItemStack.EMPTY);
			menu.eam$resultSlots().setItem(0, ItemStack.EMPTY);
			AbstractContainerMenuAccessor.eam$dropOrPlaceInInventory(player, output);
		}
	}

	/** Whether this server forges plates. The client learns it from the menu's synced flags, not from here. */
	static boolean enabled() {
		return enabled;
	}

	private record Plate(Item item, int anvilTier) {
	}

	private record EnabledCondition() implements ResourceCondition {
		@Override
		public ResourceConditionType<?> getType() {
			return ENABLED_TYPE;
		}

		@Override
		public boolean test(RegistryOps.RegistryInfoLookup registryInfo) {
			return Eam.config().ironPlates();
		}
	}
}
