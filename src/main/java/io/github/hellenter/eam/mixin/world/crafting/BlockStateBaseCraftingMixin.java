package io.github.hellenter.eam.mixin.world.crafting;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import io.github.hellenter.eam.world.crafting.CraftingModule;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** The smithing table extends the crafting table class but stays without a block entity. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseCraftingMixin {
	@Shadow
	public abstract Block getBlock();

	@ModifyReturnValue(method = "hasBlockEntity", at = @At("RETURN"))
	private boolean eam$onlyCraftingTable(boolean original) {
		return CraftingModule.hasBlockEntity(getBlock(), original);
	}
}
