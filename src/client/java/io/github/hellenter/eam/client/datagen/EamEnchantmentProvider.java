package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.arrows.ArrowRecoveryModule;
import io.github.hellenter.eam.world.shield.ShieldModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.MultiplyValue;

import java.util.concurrent.CompletableFuture;

/** Our enchantments. Their effects are code, keyed by the enchantment: see {@link ArrowRecoveryModule} and {@link ShieldModule}. */
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
		// Shield only, three levels, as common and as cheap as Unbreaking. Each level takes 10% off the time the shield
		// stays disabled, counted from the full time: 90%, 80%, 70%.
		context.register(ShieldModule.LIGHT_AS_A_FEATHER, Enchantment.enchantment(Enchantment.definition(
						context.lookup(Registries.ITEM).getOrThrow(ShieldModule.ENCHANTABLE), 5, 3,
						Enchantment.dynamicCost(5, 8), Enchantment.dynamicCost(55, 8), 2, EquipmentSlotGroup.HAND))
				.withSpecialEffect(ShieldModule.DISABLE_TIME, new MultiplyValue(LevelBasedValue.perLevel(0.9F, -0.1F)))
				.build(ShieldModule.LIGHT_AS_A_FEATHER.identifier()));
	}

	@Override
	protected void configure(HolderLookup.Provider registries, Entries entries) {
		entries.add(registries.lookupOrThrow(Registries.ENCHANTMENT), ArrowRecoveryModule.ARROW_RECOVERY);
		entries.add(registries.lookupOrThrow(Registries.ENCHANTMENT), ShieldModule.LIGHT_AS_A_FEATHER);
	}

	@Override
	public String getName() {
		return "EAM enchantments";
	}
}
