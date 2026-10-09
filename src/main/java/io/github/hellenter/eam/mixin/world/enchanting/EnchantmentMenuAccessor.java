package io.github.hellenter.eam.mixin.world.enchanting;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EnchantmentMenu.class)
public interface EnchantmentMenuAccessor {
	@Accessor("enchantSlots")
	Container eam$getEnchantSlots();

	@Accessor("access")
	ContainerLevelAccess eam$getAccess();

	@Accessor("enchantmentSeed")
	DataSlot eam$getEnchantmentSeed();
}
