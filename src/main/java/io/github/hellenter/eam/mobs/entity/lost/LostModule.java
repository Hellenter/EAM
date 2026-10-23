package io.github.hellenter.eam.mobs.entity.lost;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamEntities;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The Lost: undead soldiers in worn ancient metal who roam the underworld in squads of three to five, a leader with
 * sword and shield and his followers with swords, spears and bows. Numbers are in {@code data/eam/mobs/lost.json};
 * where squads spawn is the biome tag {@code eam:has_lost}, where lone stragglers do {@code eam:has_lost_stragglers}.
 */
public final class LostModule {
	/** The longbowman's red power arrow: it goes through a shield, which takes most of the hit itself. */
	public static final ResourceKey<DamageType> RED_ARROW_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Eam.id("red_arrow"));
	public static final TagKey<Biome> SPAWNS_IN = TagKey.create(Registries.BIOME, Eam.id("has_lost"));
	/** Where stragglers roam alone: the stone wastes. */
	public static final TagKey<Biome> STRAGGLERS_SPAWN_IN = TagKey.create(Registries.BIOME, Eam.id("has_lost_stragglers"));
	/** Spawn weight of a squad among the biome's monsters (about 300 in all); a leader spawns, his followers come with him. */
	private static final int SPAWN_WEIGHT = 20;
	/** A straggler's, as often as a squad's; how many roam about is up to his spawn rules. */
	private static final int STRAGGLER_SPAWN_WEIGHT = 20;

	private static volatile LostSettings settings = LostSettings.DEFAULT;

	private LostModule() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(EamEntities.LOST, Lost.createAttributes());
		FabricDefaultAttributeRegistry.register(EamEntities.LOST_CENTURION, LostCenturion.createAttributes());
		FabricDefaultAttributeRegistry.register(EamEntities.LOST_WALL_COMMANDER, Lost.createAttributes());
		FabricDefaultAttributeRegistry.register(EamEntities.LOST_LONGBOWMAN, LostLongbowman.createAttributes());
		FabricDefaultAttributeRegistry.register(EamEntities.LOST_STRAGGLER, LostStraggler.createAttributes());
		LostMark.register();
		SpawnPlacements.register(EamEntities.LOST, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Lost::checkSpawnRules);
		SpawnPlacements.register(EamEntities.LOST_STRAGGLER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				LostStraggler::checkStragglerSpawnRules);
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(LostSettingsLoader.ID, new LostSettingsLoader());
		// Rests are game times of one world: a singleplayer game that opens another world starts afresh.
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> LostSpawnCooldowns.clear());
	}

	/** Natural spawning in the underworld; the mob itself exists whatever the config says. */
	public static void init() {
		BiomeModifications.addSpawn(BiomeSelectors.tag(SPAWNS_IN), MobCategory.MONSTER, EamEntities.LOST, SPAWN_WEIGHT, 1, 1);
		BiomeModifications.addSpawn(BiomeSelectors.tag(STRAGGLERS_SPAWN_IN), MobCategory.MONSTER, EamEntities.LOST_STRAGGLER,
				STRAGGLER_SPAWN_WEIGHT, 1, 1);
	}

	static LostSettings settings() {
		return settings;
	}

	static void setSettings(LostSettings value) {
		settings = value;
	}
}
