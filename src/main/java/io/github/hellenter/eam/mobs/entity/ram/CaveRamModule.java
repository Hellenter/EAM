package io.github.hellenter.eam.mobs.entity.ram;

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
 * Cave rams: shaggy, curl-horned wild sheep of the underworld's rocky and frozen caves. Like boars they leave players
 * alone until hit, then the herd goes for him; they run faster and butt harder. Duergar outcasts ride them. Numbers are
 * in {@code data/eam/mobs/cave_ram.json}; where herds spawn is the biome tag {@code eam:has_cave_rams}, what they eat and
 * breed on the item tag {@code eam:cave_ram_food}.
 */
public final class CaveRamModule {
	public static final TagKey<Biome> SPAWNS_IN = TagKey.create(Registries.BIOME, Eam.id("has_cave_rams"));
	public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, Eam.id("cave_ram_food"));
	/** Spawn weight among the biome's creatures; boars have 10. */
	private static final int SPAWN_WEIGHT = 8;
	private static final int HERD_MIN = 2;
	private static final int HERD_MAX = 4;

	private static volatile CaveRamSettings settings = CaveRamSettings.DEFAULT;

	private CaveRamModule() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(EamEntities.CAVE_RAM, CaveRam.createAttributes());
		SpawnPlacements.register(EamEntities.CAVE_RAM, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				CaveRam::checkCaveRamSpawnRules);
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(CaveRamSettingsLoader.ID, new CaveRamSettingsLoader());
	}

	/** Herds spawning in the underworld; the rams themselves exist whatever the config says. */
	public static void init() {
		BiomeModifications.addSpawn(BiomeSelectors.tag(SPAWNS_IN), MobCategory.CREATURE, EamEntities.CAVE_RAM, SPAWN_WEIGHT, HERD_MIN, HERD_MAX);
	}

	public static CaveRamSettings settings() {
		return settings;
	}

	static void setSettings(CaveRamSettings value) {
		settings = value;
	}
}
