package io.github.hellenter.eam.content.brewing;

import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PotionIngredient;

/**
 * The vanilla brewing menu with the silver stand's reagent and fuel slots, under its own menu type so the client
 * builds the same slots. The client draws it with the vanilla brewing screen.
 */
public class SilverBrewingStandMenu extends BrewingStandMenu {
	private static final Identifier EMPTY_SLOT_FUEL = Identifier.withDefaultNamespace("container/slot/brewing_fuel");
	private static final int INGREDIENT_SLOT = SilverBrewingStandBlockEntity.INGREDIENT_SLOT;
	private static final int FUEL_SLOT = SilverBrewingStandBlockEntity.FUEL_SLOT;
	private static final int INV_SLOT_START = 5;
	private static final int USE_ROW_SLOT_START = 32;
	private static final int USE_ROW_SLOT_END = 41;

	public SilverBrewingStandMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(5), new SimpleContainerData(4));
	}

	public SilverBrewingStandMenu(int containerId, Inventory inventory, Container stand, ContainerData data) {
		super(containerId, inventory, stand, data);
		replaceSlot(new Slot(stand, INGREDIENT_SLOT, 79, 17) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return SilverBrewingStandBlockEntity.isReagent(stack);
			}
		});
		replaceSlot(new Slot(stand, FUEL_SLOT, 17, 17) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return SilverBrewingStandBlockEntity.isFuel(stack);
			}

			@Override
			public Identifier getNoItemIcon() {
				return EMPTY_SLOT_FUEL;
			}
		});
	}

	private void replaceSlot(Slot slot) {
		slot.index = slot.getContainerSlot();
		slots.set(slot.index, slot);
	}

	@Override
	public MenuType<?> getType() {
		return BrewingModule.MENU;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		Slot slot = slots.get(slotIndex);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack clicked = stack.copy();
		if (slotIndex >= INV_SLOT_START) {
			boolean moved;
			if (SilverBrewingStandBlockEntity.isFuel(stack)) {
				moved = moveItemStackTo(stack, FUEL_SLOT, FUEL_SLOT + 1, false);
			} else if (SilverBrewingStandBlockEntity.isReagent(stack)) {
				moved = moveItemStackTo(stack, INGREDIENT_SLOT, INGREDIENT_SLOT + 1, false);
			} else if (PotionIngredient.isPotionInput(clicked, player.level().recipeAccess())) {
				moved = moveItemStackTo(stack, 0, 3, false);
			} else if (slotIndex < USE_ROW_SLOT_START) {
				moved = moveItemStackTo(stack, USE_ROW_SLOT_START, USE_ROW_SLOT_END, false);
			} else {
				moved = moveItemStackTo(stack, INV_SLOT_START, USE_ROW_SLOT_START, false);
			}
			if (!moved) {
				return ItemStack.EMPTY;
			}
		} else {
			if (!moveItemStackTo(stack, INV_SLOT_START, USE_ROW_SLOT_END, true)) {
				return ItemStack.EMPTY;
			}
			slot.onQuickCraft(stack, clicked);
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (stack.getCount() == clicked.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(player, clicked);
		return clicked;
	}
}
