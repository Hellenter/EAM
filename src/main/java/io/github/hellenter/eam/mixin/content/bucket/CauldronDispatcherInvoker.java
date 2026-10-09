package io.github.hellenter.eam.mixin.content.bucket;

import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.item.Item;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets metal buckets add their cauldron interactions; vanilla's {@code put} is package-private. */
@Mixin(CauldronInteraction.Dispatcher.class)
public interface CauldronDispatcherInvoker {
	@Invoker("put")
	void eam$put(Item item, CauldronInteraction interaction);
}
