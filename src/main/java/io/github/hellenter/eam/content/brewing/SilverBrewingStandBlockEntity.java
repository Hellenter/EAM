package io.github.hellenter.eam.content.brewing;

import io.github.hellenter.eam.content.EamBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.BrewingInput;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.crafting.PotionIngredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * The vanilla brewing stand's loop with the silver stand's own recipes ({@link BrewingModule#RECIPE_TYPE}) and fuel
 * ({@link BrewingModule#SILVER_BREWING_FUEL}). Saves under vanilla's keys, so stands placed from structure templates
 * keep their bottles.
 */
public class SilverBrewingStandBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
	public static final int INGREDIENT_SLOT = 3;
	public static final int FUEL_SLOT = 4;
	private static final int[] SLOTS_FOR_UP = {INGREDIENT_SLOT};
	private static final int[] SLOTS_FOR_DOWN = {0, 1, 2, INGREDIENT_SLOT};
	private static final int[] SLOTS_FOR_SIDES = {0, 1, 2, FUEL_SLOT};
	private static final Component DEFAULT_NAME = Component.translatable("container.eam.silver_brewing_stand");

	private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
	private int brewTime;
	private int fuel;
	private int totalFuel = SilverBrewingLogic.MIX_FUEL_USES;
	private @Nullable Item ingredient;
	private boolean @Nullable [] lastPotionBits;
	private final RecipeManager.CachedCheck<BrewingInput, BrewingRecipe> quickCheck = RecipeManager.createCheck(BrewingModule.RECIPE_TYPE);

	/** Same layout as vanilla's, so the vanilla brewing screen draws it. */
	private final ContainerData dataAccess = new ContainerData() {
		@Override
		public int get(int id) {
			return switch (id) {
				case 0 -> brewTime;
				case 1 -> fuel;
				case 2 -> SilverBrewingLogic.BREW_TICKS;
				case 3 -> totalFuel;
				default -> 0;
			};
		}

		@Override
		public void set(int id, int value) {
			switch (id) {
				case 0 -> brewTime = value;
				case 1 -> fuel = value;
				case 3 -> totalFuel = value;
				default -> {
				}
			}
		}

		@Override
		public int getCount() {
			return 4;
		}
	};

	public SilverBrewingStandBlockEntity(BlockPos pos, BlockState state) {
		super(EamBlockEntities.SILVER_BREWING_STAND, pos, state);
	}

	public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, SilverBrewingStandBlockEntity stand) {
		ItemStack fuelStack = stand.items.get(FUEL_SLOT);
		Integer uses = fuelStack.get(BrewingModule.SILVER_BREWING_FUEL);
		if (stand.fuel <= 0 && uses != null) {
			stand.fuel = uses;
			stand.totalFuel = uses;
			fuelStack.shrink(1);
			setChanged(level, pos, state);
		}

		boolean brewable = stand.isBrewable(level);
		ItemStack ingredientStack = stand.items.get(INGREDIENT_SLOT);
		if (stand.brewTime > 0) {
			stand.brewTime--;
			if (stand.brewTime == 0 && brewable) {
				stand.brew(level, pos);
			} else if (!brewable || !ingredientStack.is(stand.ingredient)) {
				stand.brewTime = 0;
			}
			setChanged(level, pos, state);
		} else if (brewable && stand.fuel > 0) {
			stand.fuel--;
			stand.brewTime = SilverBrewingLogic.BREW_TICKS;
			stand.ingredient = ingredientStack.getItem();
			setChanged(level, pos, state);
		}

		boolean[] bits = stand.potionBits();
		if (!java.util.Arrays.equals(bits, stand.lastPotionBits)) {
			stand.lastPotionBits = bits;
			BlockState newState = state;
			for (int i = 0; i < BrewingStandBlock.HAS_BOTTLE.length; i++) {
				newState = newState.setValue(BrewingStandBlock.HAS_BOTTLE[i], bits[i]);
			}
			level.setBlock(pos, newState, 2);
		}
	}

	private boolean[] potionBits() {
		boolean[] bits = new boolean[3];
		for (int i = 0; i < 3; i++) {
			bits[i] = !items.get(i).isEmpty();
		}
		return bits;
	}

	private boolean isBrewable(ServerLevel level) {
		ItemStack reagent = items.get(INGREDIENT_SLOT);
		if (reagent.isEmpty()) {
			return false;
		}
		for (int i = 0; i < 3; i++) {
			ItemStack bottle = items.get(i);
			if (!bottle.isEmpty() && quickCheck.getRecipeFor(new BrewingInput(bottle, reagent), level).isPresent()) {
				return true;
			}
		}
		return false;
	}

	private void brew(ServerLevel level, BlockPos pos) {
		ItemStack reagent = items.get(INGREDIENT_SLOT);
		for (int i = 0; i < 3; i++) {
			BrewingInput input = new BrewingInput(items.get(i), reagent);
			Optional<RecipeHolder<BrewingRecipe>> recipe = quickCheck.getRecipeFor(input, level);
			if (recipe.isPresent()) {
				items.set(i, recipe.get().value().assemble(input));
			}
		}
		ItemStackTemplate remainder = reagent.getItem().getCraftingRemainder();
		reagent.shrink(1);
		if (remainder != null) {
			if (reagent.isEmpty()) {
				reagent = remainder.create();
			} else {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), remainder.create());
			}
		}
		items.set(INGREDIENT_SLOT, reagent);
		level.levelEvent(1035, pos, 0);
	}

	public static boolean isFuel(ItemStack stack) {
		return stack.has(BrewingModule.SILVER_BREWING_FUEL);
	}

	public static boolean isReagent(ItemStack stack) {
		return stack.is(BrewingModule.REAGENTS);
	}

	@Override
	protected Component getDefaultName() {
		return DEFAULT_NAME;
	}

	@Override
	public int getContainerSize() {
		return items.size();
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		brewTime = input.getIntOr("BrewTime", 0);
		if (brewTime > 0) {
			ingredient = items.get(INGREDIENT_SLOT).getItem();
		}
		fuel = input.getIntOr("Fuel", 0);
		totalFuel = input.getIntOr("total_fuel", SilverBrewingLogic.MIX_FUEL_USES);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("BrewTime", brewTime);
		ContainerHelper.saveAllItems(output, items);
		output.putInt("Fuel", fuel);
		output.putInt("total_fuel", totalFuel);
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot == FUEL_SLOT) {
			return isFuel(stack);
		}
		if (slot == INGREDIENT_SLOT) {
			return isReagent(stack);
		}
		return level != null && PotionIngredient.isPotionInput(stack, level.recipeAccess()) && getItem(slot).isEmpty();
	}

	@Override
	public int[] getSlotsForFace(Direction direction) {
		return direction == Direction.UP ? SLOTS_FOR_UP : direction == Direction.DOWN ? SLOTS_FOR_DOWN : SLOTS_FOR_SIDES;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
		return slot != INGREDIENT_SLOT || stack.is(Items.GLASS_BOTTLE);
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new SilverBrewingStandMenu(containerId, inventory, this, dataAccess);
	}
}
