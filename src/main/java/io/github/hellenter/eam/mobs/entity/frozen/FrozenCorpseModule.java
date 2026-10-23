package io.github.hellenter.eam.mobs.entity.frozen;

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
 * The frozen corpse: a traveller who froze to death in the underworld's ice and walks again. It spawns in the frozen
 * and ice caves (biome tag {@code eam:has_frozen_corpses}) and its hits slow the victim, deeper with each hit. Numbers
 * are in {@code data/eam/mobs/frozen_corpse.json}.
 */
public final class FrozenCorpseModule {
	public static final TagKey<Biome> SPAWNS_IN = TagKey.create(Registries.BIOME, Eam.id("has_frozen_corpses"));
	/** Spawn weight among the biome's monsters: the commonest of the cold caves, next to the Lost squads (20). */
	private static final int SPAWN_WEIGHT = 30;
	private static final int GROUP_MIN = 1;
	private static final int GROUP_MAX = 2;

	private static volatile FrozenCorpseSettings settings = FrozenCorpseSettings.DEFAULT;

	private FrozenCorpseModule() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(EamEntities.FROZEN_CORPSE, FrozenCorpse.createAttributes());
		SpawnPlacements.register(EamEntities.FROZEN_CORPSE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				FrozenCorpse::checkSpawnRules);
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(FrozenCorpseSettingsLoader.ID, new FrozenCorpseSettingsLoader());
	}

	/** Natural spawning in the cold caves; the mob itself exists whatever the config says. */
	public static void init() {
		BiomeModifications.addSpawn(BiomeSelectors.tag(SPAWNS_IN), MobCategory.MONSTER, EamEntities.FROZEN_CORPSE, SPAWN_WEIGHT, GROUP_MIN,
				GROUP_MAX);
	}

	static FrozenCorpseSettings settings() {
		return settings;
	}

	static void setSettings(FrozenCorpseSettings value) {
		settings = value;
	}
}
