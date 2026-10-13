package io.github.hellenter.eam.mobs.entity.warg;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamEntities;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Wargs: packs of big wolves led by a bigger one that hunt players at night in the snowy taiga. They circle the player
 * and bite one at a time from behind; kill the leader and the pack runs. Numbers are in {@code data/eam/mobs/warg.json};
 * where packs spawn is the biome tag {@code eam:has_wargs}.
 */
public final class WargModule {
	public static final TagKey<Biome> SPAWNS_IN = TagKey.create(Registries.BIOME, Eam.id("has_wargs"));
	/** Spawn weight of a pack's leader among the biome's monsters (zombies and skeletons have about 100); the spawn rules thin them a lot more. */
	private static final int SPAWN_WEIGHT = 6;

	private static volatile WargSettings settings = WargSettings.DEFAULT;

	private WargModule() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(EamEntities.WARG, Warg.createAttributes());
		FabricDefaultAttributeRegistry.register(EamEntities.WARG_LEADER, Warg.createLeaderAttributes());
		SpawnPlacements.register(EamEntities.WARG_LEADER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				Warg::checkLeaderSpawnRules);
		SpawnPlacements.register(EamEntities.WARG, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				Mob::checkMobSpawnRules);
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(WargSettingsLoader.ID, new WargSettingsLoader());
	}

	/** Packs spawning in the snowy taiga; the wargs themselves exist whatever the config says. Only leaders spawn: each brings his pack. */
	public static void init() {
		BiomeModifications.addSpawn(BiomeSelectors.tag(SPAWNS_IN), MobCategory.MONSTER, EamEntities.WARG_LEADER, SPAWN_WEIGHT, 1, 1);
	}

	static WargSettings settings() {
		return settings;
	}

	static void setSettings(WargSettings value) {
		settings = value;
	}
}
