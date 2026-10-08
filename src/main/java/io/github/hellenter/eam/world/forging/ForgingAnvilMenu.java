package io.github.hellenter.eam.world.forging;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;

/** Added to the anvil menu by {@code AnvilMenuMixin}. */
public interface ForgingAnvilMenu {
	AnvilForge eam$forge();

	Container eam$inputSlots();

	ResultContainer eam$resultSlots();

	ContainerLevelAccess eam$access();

	/** The hammer slot's container; the hammer goes back to the player when the menu closes. */
	Container eam$hammer();
}
