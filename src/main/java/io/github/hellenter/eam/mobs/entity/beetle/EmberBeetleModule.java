package io.github.hellenter.eam.mobs.entity.beetle;

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
 * Ember beetles: glowing coal-shelled beetles of the underworld's burnt forest and lava caves that eat coal and are
 * living fuel. Numbers are in {@code data/eam/mobs/ember_beetle.json}; where swarms spawn is the biome tag
 * {@code eam:has_ember_beetles}, what they eat the item tag {@code eam:ember_beetle_food}, and which weapons crack their
 * shell {@code eam:crushes_shells}.
 */
public final class EmberBeetleModule {
	public static final TagKey<Biome> SPAWNS_IN = TagKey.create(Registries.BIOME, Eam.id("has_ember_beetles"));
	public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, Eam.id("ember_beetle_food"));
	public static final TagKey<Item> CRUSHES_SHELLS = TagKey.create(Registries.ITEM, Eam.id("crushes_shells"));
	/** Spawn weight among the biome's monsters; a Lost has 20. */
	private static final int SPAWN_WEIGHT = 12;
	private static final int SWARM_MIN = 2;
	private static final int SWARM_MAX = 4;

	private static volatile EmberBeetleSettings settings = EmberBeetleSettings.DEFAULT;

	private EmberBeetleModule() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(EamEntities.EMBER_BEETLE, EmberBeetle.createAttributes());
		SpawnPlacements.register(EamEntities.EMBER_BEETLE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				EmberBeetle::checkEmberBeetleSpawnRules);
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(EmberBeetleSettingsLoader.ID, new EmberBeetleSettingsLoader());
	}

	/** Swarms spawning in the underworld; the beetles themselves exist whatever the config says. */
	public static void init() {
		BiomeModifications.addSpawn(BiomeSelectors.tag(SPAWNS_IN), MobCategory.MONSTER, EamEntities.EMBER_BEETLE, SPAWN_WEIGHT, SWARM_MIN, SWARM_MAX);
	}

	public static EmberBeetleSettings settings() {
		return settings;
	}

	static void setSettings(EmberBeetleSettings value) {
		settings = value;
	}
}
