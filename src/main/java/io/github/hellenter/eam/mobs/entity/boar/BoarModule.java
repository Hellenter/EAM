package io.github.hellenter.eam.mobs.entity.boar;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamEntities;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Underground boars: brown, tusked wild pigs of the underworld's lake caves and forests. They leave players alone until
 * one hits them; then the boar and its herd go for him. Numbers are in {@code data/eam/mobs/boar.json}; where herds
 * spawn is the biome tag {@code eam:has_boars}, what they eat and breed on the item tag {@code eam:boar_food}.
 */
public final class BoarModule {
	public static final TagKey<Biome> SPAWNS_IN = TagKey.create(Registries.BIOME, Eam.id("has_boars"));
	public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, Eam.id("boar_food"));
	/** Spawn weight among the biome's creatures; boars are the only ones in the underworld, so it only matters next to a datapack's. */
	private static final int SPAWN_WEIGHT = 10;
	private static final int HERD_MIN = 2;
	private static final int HERD_MAX = 4;

	private static volatile BoarSettings settings = BoarSettings.DEFAULT;

	private BoarModule() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(EamEntities.BOAR, Boar.createAttributes());
		SpawnPlacements.register(EamEntities.BOAR, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Boar::checkBoarSpawnRules);
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(BoarSettingsLoader.ID, new BoarSettingsLoader());
	}

	/** Herds spawning in the underworld; the boars themselves exist whatever the config says. */
	public static void init() {
		BiomeModifications.addSpawn(BiomeSelectors.tag(SPAWNS_IN), MobCategory.CREATURE, EamEntities.BOAR, SPAWN_WEIGHT, HERD_MIN, HERD_MAX);
	}

	static BoarSettings settings() {
		return settings;
	}

	static void setSettings(BoarSettings value) {
		settings = value;
	}
}
