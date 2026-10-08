package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.mobs.vanilla.breaking.BlockBreakingModule;
import io.github.hellenter.eam.mobs.vanilla.leaves.ThroughLeavesModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypeIds;

import java.util.concurrent.CompletableFuture;

public class EamEntityTypeTagProvider extends FabricTagsProvider<EntityType<?>> {
	@SuppressWarnings("unchecked")
	private static final ResourceKey<EntityType<?>>[] SKELETONS = new ResourceKey[] {
			EntityTypeIds.SKELETON, EntityTypeIds.STRAY, EntityTypeIds.BOGGED, EntityTypeIds.PARCHED, EntityTypeIds.WITHER_SKELETON
	};

	public EamEntityTypeTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, Registries.ENTITY_TYPE, registries);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		// Mobs that chase the player on foot. Creepers have no reason to dig.
		builder(BlockBreakingModule.BREAKERS)
				.add(EntityTypeIds.ZOMBIE, EntityTypeIds.HUSK, EntityTypeIds.DROWNED, EntityTypeIds.ZOMBIE_VILLAGER,
						EntityTypeIds.ZOMBIFIED_PIGLIN, EntityTypeIds.SPIDER, EntityTypeIds.CAVE_SPIDER,
						EntityTypeIds.VINDICATOR)
				.add(SKELETONS);
		builder(ThroughLeavesModule.SEE_THROUGH).add(SKELETONS);
		builder(BlockBreakingModule.TARGETS).add(EntityTypeIds.PLAYER);
	}
}
