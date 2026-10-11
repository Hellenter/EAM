package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.arrows.ArrowRecoveryModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.concurrent.CompletableFuture;

/** Our enchantments. Their effects are code, keyed by the enchantment: see {@link ArrowRecoveryModule}. */
public class EamEnchantmentProvider extends FabricDynamicRegistryProvider {
	public EamEnchantmentProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	public static void bootstrap(BootstrapContext<Enchantment> context) {
		// Bow only, three levels, as rare and as costly as Punch.
		context.register(ArrowRecoveryModule.ARROW_RECOVERY, Enchantment.enchantment(Enchantment.definition(
						context.lookup(Registries.ITEM).getOrThrow(ItemTags.BOW_ENCHANTABLE), 2, 3,
						Enchantment.dynamicCost(12, 20), Enchantment.dynamicCost(37, 20), 4, EquipmentSlotGroup.MAINHAND))
				.build(ArrowRecoveryModule.ARROW_RECOVERY.identifier()));
	}

	@Override
	protected void configure(HolderLookup.Provider registries, Entries entries) {
		entries.add(registries.lookupOrThrow(Registries.ENCHANTMENT), ArrowRecoveryModule.ARROW_RECOVERY);
	}

	@Override
	public String getName() {
		return "EAM enchantments";
	}
}
