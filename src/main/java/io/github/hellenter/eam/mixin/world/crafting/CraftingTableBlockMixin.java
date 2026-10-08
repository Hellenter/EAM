package io.github.hellenter.eam.mixin.world.crafting;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import io.github.hellenter.eam.world.crafting.CraftingModule;

import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The vanilla crafting table gets a block entity for its tool and opens the menu with the tool slot. */
@Mixin(CraftingTableBlock.class)
public abstract class CraftingTableBlockMixin implements EntityBlock {
	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return CraftingModule.newBlockEntity(pos, state);
	}

	@ModifyReturnValue(method = "getMenuProvider", at = @At("RETURN"))
	private MenuProvider eam$toolMenu(MenuProvider original, BlockState state, Level level, BlockPos pos) {
		return CraftingModule.tableMenu(original, state, level, pos);
	}
}
