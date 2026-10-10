package io.github.hellenter.eam.mobs.entity.lurker;

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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The swamp lurker: a withered monk-robed body that hides in swamp mud and leaps out at players. Numbers are in
 * {@code data/eam/mobs/swamp_lurker.json}; where it spawns is the biome tag {@code eam:has_swamp_lurkers}, and what it
 * burrows into is the block tag {@code eam:swamp_lurker_burrows_in} (the block turns to mud).
 */
public final class SwampLurkerModule {
	public static final TagKey<Block> BURROWS_IN = TagKey.create(Registries.BLOCK, Eam.id("swamp_lurker_burrows_in"));
	public static final TagKey<Biome> SPAWNS_IN = TagKey.create(Registries.BIOME, Eam.id("has_swamp_lurkers"));
	/** Spawn weight among the swamp's monsters (zombies and skeletons have about 100); the spacing rule thins them further. */
	private static final int SPAWN_WEIGHT = 30;

	private static volatile SwampLurkerSettings settings = SwampLurkerSettings.DEFAULT;

	private SwampLurkerModule() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(EamEntities.SWAMP_LURKER, SwampLurker.createAttributes());
		SpawnPlacements.register(EamEntities.SWAMP_LURKER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				SwampLurker::checkSpawnRules);
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(SwampLurkerSettingsLoader.ID, new SwampLurkerSettingsLoader());
	}

	/** Natural spawning in swamps; the mob itself exists whatever the config says. */
	public static void init() {
		BiomeModifications.addSpawn(BiomeSelectors.tag(SPAWNS_IN), MobCategory.MONSTER, EamEntities.SWAMP_LURKER, SPAWN_WEIGHT, 1, 1);
	}

	static SwampLurkerSettings settings() {
		return settings;
	}

	static void setSettings(SwampLurkerSettings value) {
		settings = value;
	}
}
