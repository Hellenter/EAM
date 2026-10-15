package io.github.hellenter.eam.content.fridge;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A fridge's inventory, laid out like a chest: the food rows, then one row with the ice slots at its right end. Shows
 * how long the ice it has keeps it cold.
 */
public class FridgeMenu extends AbstractContainerMenu {
	private static final int SLOT = 18;
	private static final int COLUMNS = 9;
	/** Below the title. */
	private static final int TOP = 18;

	private final Container fridge;
	private final Size size;
	private final ContainerData data;

	/** Client side: an empty copy the server fills. */
	public FridgeMenu(int containerId, Inventory inventory, Size size) {
		this(containerId, inventory, size, new SimpleContainer(size.slots()), new SimpleContainerData(1));
	}

	FridgeMenu(int containerId, Inventory inventory, Size size, Container fridge, ContainerData data) {
		super(FridgeModule.MENU, containerId);
		this.fridge = fridge;
		this.size = size;
		this.data = data;
		checkContainerSize(fridge, size.slots());
		checkContainerDataCount(data, 1);
		fridge.startOpen(inventory.player);
		for (int i = 0; i < size.ice(); i++) {
			addSlot(new Slot(fridge, i, iceX(i), iceY()) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return stack.is(FridgeModule.ICE);
				}
			});
		}
		for (int i = 0; i < size.food(); i++) {
			addSlot(new Slot(fridge, size.ice() + i, 8 + i % COLUMNS * SLOT, TOP + i / COLUMNS * SLOT));
		}
		addStandardInventorySlots(inventory, 8, inventoryTop());
		addDataSlots(data);
	}

	public Size size() {
		return size;
	}

	/** Hours the ice in the fridge keeps it cold. */
	public int hoursOfCold() {
		return data.get(0);
	}

	/** Rows of food slots. */
	public int foodRows() {
		return (size.food() + COLUMNS - 1) / COLUMNS;
	}

	public int iceX(int index) {
		return 8 + (COLUMNS - size.ice() + index) * SLOT;
	}

	public int iceY() {
		return TOP + foodRows() * SLOT;
	}

	/** Height of the part above the player's inventory, as a chest with the food rows and the ice row draws it. */
	public int topHeight() {
		return TOP - 1 + (foodRows() + 1) * SLOT;
	}

	public int inventoryTop() {
		return topHeight() + 14;
	}

	@Override
	public boolean stillValid(Player player) {
		return fridge.stillValid(player);
	}

	/** Ice goes to the ice slots first, everything else to the food slots; the fridge's items go to the player. */
	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		ItemStack clicked = ItemStack.EMPTY;
		Slot slot = slots.get(slotIndex);
		if (slot.hasItem()) {
			ItemStack stack = slot.getItem();
			clicked = stack.copy();
			int size = fridge.getContainerSize();
			int ice = this.size.ice();
			if (slotIndex < size) {
				if (!moveItemStackTo(stack, size, slots.size(), true)) return ItemStack.EMPTY;
			} else if (stack.is(FridgeModule.ICE) && moveItemStackTo(stack, 0, ice, false)) {
				if (!stack.isEmpty() && !moveItemStackTo(stack, ice, size, false)) return ItemStack.EMPTY;
			} else if (!moveItemStackTo(stack, ice, size, false)) {
				return ItemStack.EMPTY;
			}
			if (stack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}
		return clicked;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		fridge.stopOpen(player);
	}

	/**
	 * A fridge's slots.
	 *
	 * @param ice   ice slots
	 * @param food  food slots, in rows of nine
	 * @param large the iron fridge
	 */
	public record Size(int ice, int food, boolean large) {
		public static final Size SMALL = new Size(2, 18, false);
		public static final Size LARGE = new Size(4, 36, true);
		public static final StreamCodec<RegistryFriendlyByteBuf, Size> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Size::ice, ByteBufCodecs.VAR_INT, Size::food, ByteBufCodecs.BOOL, Size::large, Size::new);

		public int slots() {
			return ice + food;
		}
	}
}
