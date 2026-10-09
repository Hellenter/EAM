package io.github.hellenter.eam.mixin.world.mithril;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.github.hellenter.eam.world.mithril.MithrilModule;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

/** Mithril cooks only in the obsidian blast furnace: other furnaces find no recipe for it. */
@Mixin(AbstractFurnaceBlockEntity.class)
abstract class AbstractFurnaceBlockEntityMixin {
	@WrapOperation(method = "serverTick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/crafting/RecipeManager$CachedCheck;getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/server/level/ServerLevel;)Ljava/util/Optional;"))
	private static Optional<RecipeHolder<?>> eam$obsidianFurnaceOnly(RecipeManager.CachedCheck<?, ?> check, RecipeInput input,
			ServerLevel level, Operation<Optional<RecipeHolder<?>>> original, ServerLevel tickLevel, BlockPos pos, BlockState state,
			AbstractFurnaceBlockEntity furnace) {
		return MithrilModule.mayCook(furnace, input.getItem(0)) ? original.call(check, input, level) : Optional.empty();
	}
}
