package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.mobs.vanilla.breaking.BlockBreakingModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypeIds;

import java.util.concurrent.CompletableFuture;

public class EamEntityTypeTagProvider extends FabricTagsProvider<EntityType<?>> {
	public EamEntityTypeTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, Registries.ENTITY_TYPE, registries);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		// Melee mobs that chase the player on foot. Creepers and archers have no reason to dig.
		builder(BlockBreakingModule.BREAKERS)
				.add(EntityTypeIds.ZOMBIE, EntityTypeIds.HUSK, EntityTypeIds.DROWNED, EntityTypeIds.ZOMBIE_VILLAGER,
						EntityTypeIds.ZOMBIFIED_PIGLIN, EntityTypeIds.SPIDER, EntityTypeIds.CAVE_SPIDER,
						EntityTypeIds.VINDICATOR);
		builder(BlockBreakingModule.TARGETS).add(EntityTypeIds.PLAYER);
	}
}
