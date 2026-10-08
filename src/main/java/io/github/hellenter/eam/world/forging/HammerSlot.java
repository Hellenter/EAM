package io.github.hellenter.eam.world.forging;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The anvil's hammer slot, over the hammer drawn in the corner of the anvil screen. */
public class HammerSlot extends Slot {
	public static final int X = 23;
	public static final int Y = 14;
	private static final Identifier EMPTY_ICON = Eam.id("container/slot/hammer");

	public HammerSlot(Container hammer) {
		super(hammer, 0, X, Y);
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return ForgingModule.isHammer(stack);
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	public Identifier getNoItemIcon() {
		return EMPTY_ICON;
	}
}
