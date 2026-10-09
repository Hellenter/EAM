package io.github.hellenter.eam.client.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;

public class EamDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		FabricDataGenerator.Pack pack = generator.createPack();
		pack.addProvider(EamDamageTypeProvider::new);
		pack.addProvider(EamDamageTypeTagProvider::new);
		pack.addProvider(EamEnchantmentTagProvider::new);
		pack.addProvider(EamBiomeTagProvider::new);
		pack.addProvider(EamBlockTagProvider::new);
		pack.addProvider(EamEntityTypeTagProvider::new);
		pack.addProvider(EamItemTagProvider::new);
		pack.addProvider(EamStructureTagProvider::new);
		pack.addProvider(EamTemplatePoolTagProvider::new);
		pack.addProvider(EamProcessorListTagProvider::new);
		pack.addProvider(EamRecipeProvider::new);
		pack.addProvider(EamBlockLootProvider::new);
		pack.addProvider(EamWorldgenProvider::new);
		pack.addProvider(EamCryptProvider::new);
		pack.addProvider(EamCryptLootProvider::new);
		pack.addProvider(EamModelProvider::new);
		pack.addProvider(EamClientItemProvider::new);
		pack.addProvider(EamEquipmentAssetProvider::new);
		pack.addProvider(EamThirstSettingsProvider::new);
		pack.addProvider(EamCampFireSettingsProvider::new);
		pack.addProvider(EamSieveSettingsProvider::new);
		pack.addProvider(EamSieveLootProvider::new);
		pack.addProvider(EamEntityLootProvider::new);
		pack.addProvider(EamCraftTimesProvider::new);
		pack.addProvider(EamSoundsProvider::new);
		pack.addProvider(EamLanguageProviders.English::new);
		pack.addProvider(EamLanguageProviders.Russian::new);
	}

	@Override
	public void buildRegistry(RegistrySetBuilder registryBuilder) {
		registryBuilder.add(Registries.DAMAGE_TYPE, EamDamageTypeProvider::bootstrap);
		registryBuilder.add(Registries.FEATURE, EamWorldgenProvider::bootstrapFeatures);
		registryBuilder.add(Registries.PLACED_FEATURE, EamWorldgenProvider::bootstrapPlacedFeatures);
		registryBuilder.add(Registries.STRUCTURE, EamCryptProvider::bootstrap);
	}
}
