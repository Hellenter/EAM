package io.github.hellenter.eam.world.crafting;

/** Added to every crafting menu (inventory grid and crafting table) by {@code AbstractCraftingMenuMixin}. */
public interface TimedCraftingMenu {
	CraftTimer eam$craftTimer();
}
