package io.github.hellenter.eam.content.oven;

import io.github.hellenter.eam.content.EamBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Cooks smoking recipes in the smoker's menu, so food only, at the pace of {@link ClayOvenSettings}. */
public class ClayOvenBlockEntity extends AbstractFurnaceBlockEntity {
	private static final Component DEFAULT_NAME = Component.translatable("container.eam.clay_oven");

	public ClayOvenBlockEntity(BlockPos pos, BlockState state) {
		super(EamBlockEntities.CLAY_OVEN, pos, state, RecipeType.SMOKING);
	}

	@Override
	protected int getBurnDuration(ServerLevel level, ItemStack fuelItem) {
		return ClayOvenLogic.burnDuration(super.getBurnDuration(level, fuelItem), ClayOvenModule.settings().fuelTimeMultiplier());
	}

	@Override
	protected float getSpeedMultiplier(ServerLevel level, ItemStack fuelItem) {
		return ClayOvenLogic.speedMultiplier(super.getSpeedMultiplier(level, fuelItem), ClayOvenModule.settings().cookTimeMultiplier());
	}

	@Override
	protected Component getDefaultName() {
		return DEFAULT_NAME;
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new SmokerMenu(containerId, inventory, this, dataAccess);
	}
}
