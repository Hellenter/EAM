package io.github.hellenter.eam.mixin.content.heart;

import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets the heart in a jar show the vampire heart item on its flying display entity. */
@Mixin(Display.ItemDisplay.class)
public interface ItemDisplayInvoker {
	@Invoker("setItemStack")
	void eam$setItemStack(ItemStack stack);
}
