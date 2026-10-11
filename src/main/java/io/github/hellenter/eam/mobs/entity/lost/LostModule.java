package io.github.hellenter.eam.mobs.entity.lost;

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
 * The Lost: undead soldiers in worn ancient metal who roam the underworld in squads of three to five, a leader with
 * sword and shield and his followers with swords, spears and bows. Numbers are in {@code data/eam/mobs/lost.json};
 * where squads spawn is the biome tag {@code eam:has_lost}.
 */
public final class LostModule {
	public static final TagKey<Biome> SPAWNS_IN = TagKey.create(Registries.BIOME, Eam.id("has_lost"));
	/** Spawn weight of a squad among the biome's monsters (about 300 in all); a leader spawns, his followers come with him. */
	private static final int SPAWN_WEIGHT = 20;

	private static volatile LostSettings settings = LostSettings.DEFAULT;

	private LostModule() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(EamEntities.LOST, Lost.createAttributes());
		SpawnPlacements.register(EamEntities.LOST, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Lost::checkSpawnRules);
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(LostSettingsLoader.ID, new LostSettingsLoader());
	}

	/** Natural spawning in the underworld; the mob itself exists whatever the config says. */
	public static void init() {
		BiomeModifications.addSpawn(BiomeSelectors.tag(SPAWNS_IN), MobCategory.MONSTER, EamEntities.LOST, SPAWN_WEIGHT, 1, 1);
	}

	static LostSettings settings() {
		return settings;
	}

	static void setSettings(LostSettings value) {
		settings = value;
	}
}
