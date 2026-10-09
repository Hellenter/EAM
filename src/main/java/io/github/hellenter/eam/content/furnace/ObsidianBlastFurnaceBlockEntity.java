package io.github.hellenter.eam.content.furnace;

import io.github.hellenter.eam.content.EamBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Cooks blasting recipes like the blast furnace, in the blast furnace's menu. */
public class ObsidianBlastFurnaceBlockEntity extends AbstractFurnaceBlockEntity {
	private static final Component DEFAULT_NAME = Component.translatable("container.eam.obsidian_blast_furnace");

	public ObsidianBlastFurnaceBlockEntity(BlockPos pos, BlockState state) {
		super(EamBlockEntities.OBSIDIAN_BLAST_FURNACE, pos, state, RecipeType.BLASTING);
	}

	@Override
	protected Component getDefaultName() {
		return DEFAULT_NAME;
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new BlastFurnaceMenu(containerId, inventory, this, dataAccess);
	}
}
