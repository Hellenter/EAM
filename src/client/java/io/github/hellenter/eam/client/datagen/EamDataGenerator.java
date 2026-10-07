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
		pack.addProvider(EamThirstSettingsProvider::new);
		pack.addProvider(EamLanguageProviders.English::new);
		pack.addProvider(EamLanguageProviders.Russian::new);
	}

	@Override
	public void buildRegistry(RegistrySetBuilder registryBuilder) {
		registryBuilder.add(Registries.DAMAGE_TYPE, EamDamageTypeProvider::bootstrap);
	}
}
