package io.github.hellenter.eam.mobs.entity.duergar;

import io.github.hellenter.eam.content.EamEntities;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Duergar: grey dwarves of the underworld. Zombie duergar are the miners who died in the old mines and still haunt them;
 * they spawn only inside the mines ({@code spawn_overrides} of the structure {@code eam:underworld_mine}). Numbers are in
 * {@code data/eam/mobs/zombie_duergar.json}.
 */
public final class DuergarModule {
	private static volatile ZombieDuergarSettings zombieSettings = ZombieDuergarSettings.DEFAULT;

	private DuergarModule() {
	}

	/** The mobs exist whatever the config says, so the mine structure always loads. */
	public static void register() {
		FabricDefaultAttributeRegistry.register(EamEntities.ZOMBIE_DUERGAR, ZombieDuergar.createAttributes());
		SpawnPlacements.register(EamEntities.ZOMBIE_DUERGAR, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				Monster::checkMonsterSpawnRules);
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(ZombieDuergarSettingsLoader.ID, new ZombieDuergarSettingsLoader());
	}

	static ZombieDuergarSettings zombieSettings() {
		return zombieSettings;
	}

	static void setZombieSettings(ZombieDuergarSettings value) {
		zombieSettings = value;
	}
}
