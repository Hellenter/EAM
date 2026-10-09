package io.github.hellenter.eam.world.forging;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

/**
 * The anvil menu's hammer slot. On the server it is the hammer kept in the anvil's block entity, looked up on every use
 * so it follows an iron anvil that wears into the next block. On the client, or with no block entity, it is a plain
 * slot whose hammer goes back to the player when the menu closes.
 */
public final class AnvilHammer implements Container {
	private final ContainerLevelAccess access;
	private final SimpleContainer loose = new SimpleContainer(1);

	public AnvilHammer(ContainerLevelAccess access) {
		this.access = access;
	}

	private Container target() {
		return access.evaluate((level, pos) -> level.getBlockEntity(pos) instanceof AnvilBlockEntity anvil ? anvil.hammer() : loose,
				loose);
	}

	/** Server, when the menu closes: a hammer not kept by an anvil goes back to the player. */
	public ItemStack takeLoose() {
		return loose.removeItemNoUpdate(0);
	}

	@Override
	public int getContainerSize() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return target().isEmpty();
	}

	@Override
	public ItemStack getItem(int slot) {
		return target().getItem(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		return target().removeItem(slot, count);
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return target().removeItemNoUpdate(slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		target().setItem(slot, stack);
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	public void setChanged() {
		target().setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {
		target().clearContent();
	}
}
