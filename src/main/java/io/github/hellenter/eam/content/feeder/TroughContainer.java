package io.github.hellenter.eam.content.feeder;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** The feeders of one trough as one container, one block's slots after another, as a double chest joins two chests. */
class TroughContainer implements Container {
	private final List<FeederBlockEntity> feeders;

	TroughContainer(List<FeederBlockEntity> feeders) {
		this.feeders = List.copyOf(feeders);
	}

	/** Slots of the first block, which sets the menu's row width. */
	int slotsPerBlock() {
		return feeders.getFirst().getContainerSize();
	}

	@Override
	public int getContainerSize() {
		int size = 0;
		for (FeederBlockEntity feeder : feeders) size += feeder.getContainerSize();
		return size;
	}

	@Override
	public boolean isEmpty() {
		for (FeederBlockEntity feeder : feeders) {
			if (!feeder.isEmpty()) return false;
		}
		return true;
	}

	/** Runs {@code action} on the feeder holding trough slot {@code index}, with the slot index inside that feeder. */
	private <T> T at(int index, SlotAction<T> action) {
		for (FeederBlockEntity feeder : feeders) {
			if (index < feeder.getContainerSize()) return action.apply(feeder, index);
			index -= feeder.getContainerSize();
		}
		throw new IndexOutOfBoundsException(index);
	}

	@Override
	public ItemStack getItem(int index) {
		return at(index, FeederBlockEntity::getItem);
	}

	@Override
	public ItemStack removeItem(int index, int count) {
		return at(index, (feeder, slot) -> feeder.removeItem(slot, count));
	}

	@Override
	public ItemStack removeItemNoUpdate(int index) {
		return at(index, FeederBlockEntity::removeItemNoUpdate);
	}

	@Override
	public void setItem(int index, ItemStack stack) {
		at(index, (feeder, slot) -> {
			feeder.setItem(slot, stack);
			return null;
		});
	}

	@Override
	public boolean canPlaceItem(int index, ItemStack stack) {
		return stack.is(FeederModule.FEED);
	}

	@Override
	public void setChanged() {
		for (FeederBlockEntity feeder : feeders) feeder.setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		for (FeederBlockEntity feeder : feeders) {
			if (feeder.isRemoved() || !feeder.stillValid(player)) return false;
		}
		return true;
	}

	@Override
	public void clearContent() {
		for (FeederBlockEntity feeder : feeders) feeder.clearContent();
	}

	@FunctionalInterface
	private interface SlotAction<T> {
		T apply(FeederBlockEntity feeder, int slot);
	}
}
