package io.github.hellenter.eam.mobs.entity.duergar;

import io.github.hellenter.eam.content.EamEntities;

import io.github.hellenter.eam.Eam;

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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Duergar: grey dwarves of the underworld. Zombie duergar are the miners who died in the old mines and still haunt them;
 * they spawn only inside the mines ({@code spawn_overrides} of the structure {@code eam:underworld_mine}). Renegades roam
 * the dark caves in armored squads, where the biome tag {@code eam:has_duergar_renegades} lets them; other outcasts ride
 * cave rams with tomahawks and nets, where {@code eam:has_ram_riders} lets them. Numbers are in
 * {@code data/eam/mobs/zombie_duergar.json}, {@code duergar_renegade.json} and {@code duergar_ram_rider.json}. Village duergar live in the clan villages
 * ({@code eam:duergar_village_*} structures) and trade; both other kinds hunt them.
 */
public final class DuergarModule {
	public static final TagKey<Biome> RENEGADES_SPAWN_IN = TagKey.create(Registries.BIOME, Eam.id("has_duergar_renegades"));
	public static final TagKey<Biome> RAM_RIDERS_SPAWN_IN = TagKey.create(Registries.BIOME, Eam.id("has_ram_riders"));
	/** Spawn weight of a squad among the biome's monsters (the Lost have 20); the leader spawns, his followers come with him. */
	private static final int RENEGADE_SPAWN_WEIGHT = 8;
	/** Spawn weight of a ram rider squad; rarer than the renegades. */
	private static final int RAM_RIDER_SPAWN_WEIGHT = 4;

	private static volatile DuergarRenegadeSettings renegadeSettings = DuergarRenegadeSettings.DEFAULT;
	private static volatile ZombieDuergarSettings zombieSettings = ZombieDuergarSettings.DEFAULT;
	private static volatile RamRiderSettings riderSettings = RamRiderSettings.DEFAULT;

	private DuergarModule() {
	}

	/** The mobs exist whatever the config says, so the mine structure always loads. */
	public static void register() {
		FabricDefaultAttributeRegistry.register(EamEntities.ZOMBIE_DUERGAR, ZombieDuergar.createAttributes());
		SpawnPlacements.register(EamEntities.ZOMBIE_DUERGAR, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				Monster::checkMonsterSpawnRules);
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(ZombieDuergarSettingsLoader.ID, new ZombieDuergarSettingsLoader());
		FabricDefaultAttributeRegistry.register(EamEntities.DUERGAR_RENEGADE, DuergarRenegade.createAttributes());
		SpawnPlacements.register(EamEntities.DUERGAR_RENEGADE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				DuergarRenegade::checkSpawnRules);
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(DuergarRenegadeSettingsLoader.ID, new DuergarRenegadeSettingsLoader());
		FabricDefaultAttributeRegistry.register(EamEntities.DUERGAR_VILLAGER, DuergarVillager.createAttributes());
		FabricDefaultAttributeRegistry.register(EamEntities.DUERGAR_RAM_RIDER, DuergarRamRider.createAttributes());
		SpawnPlacements.register(EamEntities.DUERGAR_RAM_RIDER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				DuergarRamRider::checkSpawnRules);
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(RamRiderSettingsLoader.ID, new RamRiderSettingsLoader());
	}

	/** Natural spawning of renegade squads; the mobs themselves exist whatever the config says. */
	public static void init() {
		BiomeModifications.addSpawn(BiomeSelectors.tag(RENEGADES_SPAWN_IN), MobCategory.MONSTER, EamEntities.DUERGAR_RENEGADE,
				RENEGADE_SPAWN_WEIGHT, 1, 1);
		BiomeModifications.addSpawn(BiomeSelectors.tag(RAM_RIDERS_SPAWN_IN), MobCategory.MONSTER, EamEntities.DUERGAR_RAM_RIDER,
				RAM_RIDER_SPAWN_WEIGHT, 1, 1);
	}

	static DuergarRenegadeSettings renegadeSettings() {
		return renegadeSettings;
	}

	static void setRenegadeSettings(DuergarRenegadeSettings value) {
		renegadeSettings = value;
	}

	static RamRiderSettings riderSettings() {
		return riderSettings;
	}

	static void setRiderSettings(RamRiderSettings value) {
		riderSettings = value;
	}

	static ZombieDuergarSettings zombieSettings() {
		return zombieSettings;
	}

	static void setZombieSettings(ZombieDuergarSettings value) {
		zombieSettings = value;
	}
}
