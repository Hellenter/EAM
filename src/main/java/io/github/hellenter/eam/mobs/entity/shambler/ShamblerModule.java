package io.github.hellenter.eam.mobs.entity.shambler;

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
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Mushroom shamblers: dead men overgrown with mushrooms that stand still among the mushrooms of the underworld's mushroom
 * forest, looking like one, until a player comes close. Hit, they puff out sickening spores. The smoldering kin of the
 * burnt forest throw sparks instead and set what they hit on fire. Numbers are in {@code data/eam/mobs/mushroom_shambler.json};
 * where each kind spawns is the biome tags {@code eam:has_mushroom_shamblers} and {@code eam:has_smoldering_shamblers}.
 */
public final class ShamblerModule {
	public static final TagKey<Biome> SPAWNS_IN = TagKey.create(Registries.BIOME, Eam.id("has_mushroom_shamblers"));
	public static final TagKey<Biome> SMOLDERING_SPAWNS_IN = TagKey.create(Registries.BIOME, Eam.id("has_smoldering_shamblers"));
	/** Spawn weight among the biome's monsters (a Lost squad has 20); the spawn rules thin them further. */
	private static final int SPAWN_WEIGHT = 25;
	private static final int GROUP_MIN = 1;
	private static final int GROUP_MAX = 2;

	private static volatile ShamblerSettings settings = ShamblerSettings.DEFAULT;

	private ShamblerModule() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(EamEntities.MUSHROOM_SHAMBLER, MushroomShambler.createAttributes());
		FabricDefaultAttributeRegistry.register(EamEntities.SMOLDERING_SHAMBLER, MushroomShambler.createAttributes());
		SpawnPlacements.register(EamEntities.MUSHROOM_SHAMBLER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				MushroomShambler::checkSpawnRules);
		SpawnPlacements.register(EamEntities.SMOLDERING_SHAMBLER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				MushroomShambler::checkSpawnRules);
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(ShamblerSettingsLoader.ID, new ShamblerSettingsLoader());
	}

	/** Natural spawning in the two mushroom forests; the mobs themselves exist whatever the config says. */
	public static void init() {
		BiomeModifications.addSpawn(BiomeSelectors.tag(SPAWNS_IN), MobCategory.MONSTER, EamEntities.MUSHROOM_SHAMBLER, SPAWN_WEIGHT,
				GROUP_MIN, GROUP_MAX);
		BiomeModifications.addSpawn(BiomeSelectors.tag(SMOLDERING_SPAWNS_IN), MobCategory.MONSTER, EamEntities.SMOLDERING_SHAMBLER,
				SPAWN_WEIGHT, GROUP_MIN, GROUP_MAX);
	}

	public static ShamblerSettings settings() {
		return settings;
	}

	static void setSettings(ShamblerSettings value) {
		settings = value;
	}
}
