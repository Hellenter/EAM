package io.github.hellenter.eam.mixin.world.crafting;

import net.minecraft.world.inventory.ResultSlot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ResultSlot.class)
public interface ResultSlotAccessor {
	@Accessor("removeCount")
	void eam$setRemoveCount(int count);
}
