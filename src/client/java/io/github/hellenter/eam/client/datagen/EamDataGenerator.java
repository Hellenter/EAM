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
		pack.addProvider(EamMobEffectTagProvider::new);
		pack.addProvider(EamEnchantmentProvider::new);
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
		pack.addProvider(EamOutpostProvider::new);
		pack.addProvider(EamYagaHutProvider::new);
		pack.addProvider(EamUnderworldMineProvider::new);
		pack.addProvider(EamUnderworldMineLootProvider::new);
		pack.addProvider(EamDuergarVillageProvider::new);
		pack.addProvider(EamDuergarTradeProvider::new);
		pack.addProvider(EamDuergarVillageLootProvider::new);
		pack.addProvider(EamOutpostLootProvider::new);
		pack.addProvider(EamCryptLootProvider::new);
		pack.addProvider(EamYagaGraveLootProvider::new);
		pack.addProvider(EamCryptSpawnerProvider::new);
		pack.addProvider(EamCryptGearProvider::new);
		pack.addProvider(EamEarlyLootProvider::new);
		pack.addProvider(EamModelProvider::new);
		pack.addProvider(EamClientItemProvider::new);
		pack.addProvider(EamEquipmentAssetProvider::new);
		pack.addProvider(EamThirstSettingsProvider::new);
		pack.addProvider(EamCampFireSettingsProvider::new);
		pack.addProvider(EamFoodDryerSettingsProvider::new);
		pack.addProvider(EamRainDousingSettingsProvider::new);
		pack.addProvider(EamTimelineProvider::new);
		pack.addProvider(EamTimelineTagProvider::new);
		pack.addProvider(EamSieveSettingsProvider::new);
		pack.addProvider(EamSieveLootProvider::new);
		pack.addProvider(EamFeederSettingsProvider::new);
		pack.addProvider(EamVillagerWorkProvider::new);
		pack.addProvider(EamGuardSettingsProvider::new);
		pack.addProvider(EamRepairSettingsProvider::new);
		pack.addProvider(EamFoodSettingsProvider::new);
		pack.addProvider(EamButcherSettingsProvider::new);
		pack.addProvider(EamClericSettingsProvider::new);
		pack.addProvider(EamAlarmSettingsProvider::new);
		pack.addProvider(EamElderSettingsProvider::new);
		pack.addProvider(EamSpoilageSettingsProvider::new);
		pack.addProvider(EamEntityLootProvider::new);
		pack.addProvider(EamCraftTimesProvider::new);
		pack.addProvider(EamSoundsProvider::new);
		pack.addProvider(EamAdvancementProvider::new);
		pack.addProvider(EamLanguageProviders.English::new);
		pack.addProvider(EamLanguageProviders.Russian::new);
	}

	@Override
	public void buildRegistry(RegistrySetBuilder registryBuilder) {
		registryBuilder.add(Registries.DAMAGE_TYPE, EamDamageTypeProvider::bootstrap);
		registryBuilder.add(Registries.ENCHANTMENT, EamEnchantmentProvider::bootstrap);
		registryBuilder.add(Registries.FEATURE, EamWorldgenProvider::bootstrapFeatures);
		registryBuilder.add(Registries.PLACED_FEATURE, EamWorldgenProvider::bootstrapPlacedFeatures);
		registryBuilder.add(Registries.BIOME, EamWorldgenProvider::bootstrapBiomes);
		registryBuilder.add(Registries.NOISE, UnderworldWorldgen::bootstrapNoises);
		registryBuilder.add(Registries.DENSITY_FUNCTION, UnderworldWorldgen::bootstrapDensityFunctions);
		registryBuilder.add(Registries.MATERIAL_RULE, UnderworldWorldgen::bootstrapMaterialRules);
		registryBuilder.add(Registries.NOISE_SETTINGS, UnderworldWorldgen::bootstrapNoiseSettings);
		registryBuilder.add(Registries.DIMENSION_TYPE, UnderworldWorldgen::bootstrapDimensionTypes);
		registryBuilder.add(Registries.TIMELINE, EamTimelineProvider::bootstrap);
		registryBuilder.add(Registries.PROCESSOR_LIST, context -> {
			EamUnderworldMineProvider.bootstrapProcessors(context);
			EamDuergarVillageProvider.bootstrapProcessors(context);
		});
		registryBuilder.add(Registries.TEMPLATE_POOL, context -> {
			EamUnderworldMineProvider.bootstrapPools(context);
			EamDuergarVillageProvider.bootstrapPools(context);
		});
		registryBuilder.add(Registries.STRUCTURE, context -> {
			EamCryptProvider.bootstrap(context);
			EamOutpostProvider.bootstrap(context);
			EamYagaHutProvider.bootstrap(context);
			EamUnderworldMineProvider.bootstrap(context);
			EamDuergarVillageProvider.bootstrap(context);
		});
		registryBuilder.add(Registries.STRUCTURE_SET, context -> {
			EamCryptProvider.bootstrapSets(context);
			EamOutpostProvider.bootstrapSets(context);
			EamYagaHutProvider.bootstrapSets(context);
			EamUnderworldMineProvider.bootstrapSets(context);
			EamDuergarVillageProvider.bootstrapSets(context);
		});
		registryBuilder.add(Registries.VILLAGER_TRADE, EamDuergarTradeProvider::bootstrapTrades);
		registryBuilder.add(Registries.TRADE_SET, EamDuergarTradeProvider::bootstrapTradeSets);
		registryBuilder.add(Registries.TRIAL_SPAWNER_CONFIG, EamCryptSpawnerProvider::bootstrap);
	}
}
