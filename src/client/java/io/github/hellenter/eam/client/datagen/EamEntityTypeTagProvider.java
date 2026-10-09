package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.mobs.vanilla.breaking.BlockBreakingModule;
import io.github.hellenter.eam.mobs.vanilla.cows.CowDefenseModule;
import io.github.hellenter.eam.mobs.vanilla.drops.MobDropsModule;
import io.github.hellenter.eam.mobs.vanilla.leaves.ThroughLeavesModule;
import io.github.hellenter.eam.mobs.vanilla.running.RunningModule;
import io.github.hellenter.eam.world.bloodmoon.BloodMoonModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EntityTypeTags;
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
		builder(MobDropsModule.SMALL_LEATHER).add(EntityTypeIds.SHEEP, EntityTypeIds.PIG);
		// Melee hostiles run after their target. Creepers and ranged mobs keep their vanilla pace.
		builder(RunningModule.RUNNERS)
				.add(EntityTypeIds.ZOMBIE, EntityTypeIds.HUSK, EntityTypeIds.DROWNED, EntityTypeIds.ZOMBIE_VILLAGER,
						EntityTypeIds.ZOMBIFIED_PIGLIN, EntityTypeIds.SPIDER, EntityTypeIds.CAVE_SPIDER,
						EntityTypeIds.VINDICATOR, EntityTypeIds.SILVERFISH, EntityTypeIds.ENDERMITE);
		// Blood moon: zombies and skeletons put on armor; zombies take up melee weapons, skeletons keep their bows.
		builder(BloodMoonModule.ARMORED)
				.add(EntityTypeIds.ZOMBIE, EntityTypeIds.HUSK, EntityTypeIds.DROWNED, EntityTypeIds.ZOMBIE_VILLAGER)
				.add(SKELETONS);
		builder(BloodMoonModule.ARMED).add(EntityTypeIds.ZOMBIE, EntityTypeIds.HUSK, EntityTypeIds.ZOMBIE_VILLAGER);
		builder(BloodMoonModule.HUNTERS)
				.add(EntityTypeIds.ZOMBIE, EntityTypeIds.HUSK, EntityTypeIds.DROWNED, EntityTypeIds.ZOMBIE_VILLAGER,
						EntityTypeIds.SPIDER, EntityTypeIds.CAVE_SPIDER, EntityTypeIds.VINDICATOR)
				.add(SKELETONS);
		builder(CowDefenseModule.DEFENDERS).add(EntityTypeIds.COW, EntityTypeIds.MOOSHROOM);
		// The Fallen Lord is undead: smite and silver hurt him more.
		builder(EntityTypeTags.UNDEAD).add(EamEntities.FALLEN_LORD.builtInRegistryHolder().key());
	}
}
