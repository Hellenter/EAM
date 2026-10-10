package io.github.hellenter.eam.mobs.entity.wight;

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
 * The wight: a gaunt, hunched body in rags that stalks one player unseen, raids them while they are busy and hunts them
 * down when they are weak. Fire is its weakness. Numbers are in {@code data/eam/mobs/wight.json}; where it spawns is the
 * biome tag {@code eam:has_wights}.
 */
public final class WightModule {
	public static final TagKey<Biome> SPAWNS_IN = TagKey.create(Registries.BIOME, Eam.id("has_wights"));
	/** Spawn weight among the biome's monsters (zombies and skeletons have about 100); the spawn rules thin them a lot more. */
	private static final int SPAWN_WEIGHT = 5;

	private static volatile WightSettings settings = WightSettings.DEFAULT;

	private WightModule() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(EamEntities.WIGHT, Wight.createAttributes());
		SpawnPlacements.register(EamEntities.WIGHT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Wight::checkSpawnRules);
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(WightSettingsLoader.ID, new WightSettingsLoader());
	}

	/** Natural spawning in swamps and jungles; the mob itself exists whatever the config says. */
	public static void init() {
		BiomeModifications.addSpawn(BiomeSelectors.tag(SPAWNS_IN), MobCategory.MONSTER, EamEntities.WIGHT, SPAWN_WEIGHT, 1, 1);
	}

	static WightSettings settings() {
		return settings;
	}

	static void setSettings(WightSettings value) {
		settings = value;
	}
}
