package io.github.hellenter.eam.content.feeder;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A whole trough's inventory, like a chest: each feeder block adds its slots, one block's slots after another. The
 * slots take only feed. Also shows the meals the trough gave today out of its heads.
 */
public class FeederMenu extends AbstractContainerMenu {
	/** Inventory slots in the menu: the trough's slots, then the player's. */
	private final Container trough;
	private final Layout layout;
	/** Meals given today and heads the trough feeds. */
	private final ContainerData meals;

	/** Client side: an empty copy the server fills. */
	public FeederMenu(int containerId, Inventory inventory, Layout layout) {
		this(containerId, inventory, layout, new SimpleContainer(layout.slots()), new SimpleContainerData(2));
	}

	FeederMenu(int containerId, Inventory inventory, Layout layout, Container trough, ContainerData meals) {
		super(FeederModule.MENU, containerId);
		this.trough = trough;
		this.layout = layout;
		this.meals = meals;
		checkContainerSize(trough, layout.slots());
		trough.startOpen(inventory.player);
		for (int i = 0; i < layout.slots(); i++) {
			addSlot(new Slot(trough, i, layout.slotX(i), layout.slotY(i)) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return stack.is(FeederModule.FEED);
				}
			});
		}
		addStandardInventorySlots(inventory, 8, layout.inventoryTop());
		addDataSlots(meals);
	}

	public Layout layout() {
		return layout;
	}

	public int mealsToday() {
		return meals.get(0);
	}

	public int heads() {
		return meals.get(1);
	}

	@Override
	public boolean stillValid(Player player) {
		return trough.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		ItemStack clicked = ItemStack.EMPTY;
		Slot slot = slots.get(slotIndex);
		if (slot.hasItem()) {
			ItemStack stack = slot.getItem();
			clicked = stack.copy();
			int size = trough.getContainerSize();
			if (slotIndex < size) {
				if (!moveItemStackTo(stack, size, slots.size(), true)) return ItemStack.EMPTY;
			} else if (!moveItemStackTo(stack, 0, size, false)) {
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
		trough.stopOpen(player);
	}

	/**
	 * How the menu lays out: {@code slots} in rows of {@code columns}, centered in a chest-wide panel.
	 */
	public record Layout(int slots, int columns) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Layout> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Layout::slots, ByteBufCodecs.VAR_INT, Layout::columns, Layout::new);
		private static final int SLOT = 18;
		private static final int MAX_COLUMNS = 9;
		/** Below the title. */
		private static final int TOP = 18;

		/** One row per block, or two when a block has more slots than fit in a chest row. */
		static Layout of(TroughContainer trough) {
			int perBlock = trough.slotsPerBlock();
			int columns = perBlock <= MAX_COLUMNS ? perBlock : (perBlock + 1) / 2;
			return new Layout(trough.getContainerSize(), Math.min(columns, MAX_COLUMNS));
		}

		public int rows() {
			return (slots + columns - 1) / columns;
		}

		/** Left edge of the first column; slot positions are of the item inside the 18-pixel frame. */
		public int left() {
			return 8 + (MAX_COLUMNS - columns) * SLOT / 2;
		}

		public int slotX(int index) {
			return left() + index % columns * SLOT;
		}

		public int slotY(int index) {
			return TOP + index / columns * SLOT;
		}

		/** Height of the part above the player's inventory, as a chest with as many rows draws it. */
		public int topHeight() {
			return TOP - 1 + rows() * SLOT;
		}

		public int inventoryTop() {
			return topHeight() + 14;
		}
	}

	/** Opens the trough made of {@code feeders}, from its negative end. */
	record Provider(Component title, ServerLevel level, List<FeederBlockEntity> feeders)
			implements ExtendedMenuProvider<Layout> {
		@Override
		public Component getDisplayName() {
			return title;
		}

		@Override
		public Layout getScreenOpeningData(ServerPlayer player) {
			return Layout.of(new TroughContainer(feeders));
		}

		@Override
		public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
			ContainerData meals = new ContainerData() {
				@Override
				public int get(int dataId) {
					int sum = 0;
					for (FeederBlockEntity feeder : feeders) sum += dataId == 0 ? feeder.mealsToday(level) : feeder.heads();
					return sum;
				}

				@Override
				public void set(int dataId, int value) {
				}

				@Override
				public int getCount() {
					return 2;
				}
			};
			TroughContainer trough = new TroughContainer(feeders);
			return new FeederMenu(containerId, inventory, Layout.of(trough), trough, meals);
		}
	}
}
