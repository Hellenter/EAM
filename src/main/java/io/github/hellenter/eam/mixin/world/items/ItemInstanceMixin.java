package io.github.hellenter.eam.mixin.world.items;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import io.github.hellenter.eam.world.items.StackSizeModule;

import net.minecraft.world.item.ItemInstance;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Per-stack max stack size: water bottles share the {@code potion} item with every other potion. */
@Mixin(ItemInstance.class)
public interface ItemInstanceMixin {
	@ModifyReturnValue(method = "getMaxStackSize", at = @At("RETURN"))
	private int eam$maxStackSize(int original) {
		return StackSizeModule.maxStackSize((ItemInstance) this, original);
	}
}
