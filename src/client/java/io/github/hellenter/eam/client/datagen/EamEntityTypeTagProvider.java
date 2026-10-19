package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.feeder.FeederModule;
import io.github.hellenter.eam.content.tools.KnifeModule;

import io.github.hellenter.eam.mobs.vanilla.breaking.BlockBreakingModule;
import io.github.hellenter.eam.mobs.vanilla.cows.CowDefenseModule;
import io.github.hellenter.eam.mobs.vanilla.drops.MobDropsModule;
import io.github.hellenter.eam.mobs.vanilla.evolution.MonsterEvolutionModule;
import io.github.hellenter.eam.mobs.vanilla.feud.PillagerFeudModule;
import io.github.hellenter.eam.mobs.vanilla.nemesis.NemesisModule;
import io.github.hellenter.eam.mobs.vanilla.predators.PredatorModule;
import io.github.hellenter.eam.mobs.vanilla.leaves.ThroughLeavesModule;
import io.github.hellenter.eam.mobs.vanilla.noise.NoiseModule;
import io.github.hellenter.eam.mobs.vanilla.running.RunningModule;
import io.github.hellenter.eam.survival.death.DeathModule;
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
		// Mobs that chase the player on foot, and every pillager kind. Creepers have no reason to dig.
		builder(BlockBreakingModule.BREAKERS)
				.add(EntityTypeIds.ZOMBIE, EntityTypeIds.HUSK, EntityTypeIds.DROWNED, EntityTypeIds.ZOMBIE_VILLAGER,
						EntityTypeIds.ZOMBIFIED_PIGLIN, EntityTypeIds.SPIDER, EntityTypeIds.CAVE_SPIDER,
						EntityTypeIds.VINDICATOR, EntityTypeIds.PILLAGER, EntityTypeIds.EVOKER, EntityTypeIds.ILLUSIONER)
				.add(SKELETONS)
				.add(EamEntities.PILLAGER_GUARD.builtInRegistryHolder().key(), EamEntities.ELITE_CROSSBOWMAN.builtInRegistryHolder().key(),
						EamEntities.PILLAGER_KING.builtInRegistryHolder().key(), EamEntities.ZOMBIE_DUERGAR.builtInRegistryHolder().key(),
						EamEntities.DUERGAR_RENEGADE.builtInRegistryHolder().key());
		// Duergar renegades dig through stone with their picks.
		builder(BlockBreakingModule.STONE_BREAKERS).add(EamEntities.DUERGAR_RENEGADE.builtInRegistryHolder().key());
		// The small spiders live in tree crowns and spot a player through the leaves.
		builder(ThroughLeavesModule.SEE_THROUGH).add(SKELETONS)
				.add(EamEntities.WOOD_SPIDER.builtInRegistryHolder().key(), EamEntities.BLACK_WIDOW.builtInRegistryHolder().key());
		builder(BlockBreakingModule.TARGETS).add(EntityTypeIds.PLAYER);
		// Raiders break into houses after the villagers hiding there.
		builder(BlockBreakingModule.RAID_TARGETS).add(EntityTypeIds.VILLAGER, EntityTypeIds.WANDERING_TRADER);
		// Farm animals that eat from feeders.
		builder(FeederModule.LIVESTOCK).add(EntityTypeIds.COW, EntityTypeIds.MOOSHROOM, EntityTypeIds.SHEEP, EntityTypeIds.PIG,
				EntityTypeIds.CHICKEN, EntityTypeIds.GOAT);
		// Predators raid livestock every night: wolves wherever they reach it, zombies only in an open, dark pen.
		builder(PredatorModule.HUNTERS).add(EntityTypeIds.WOLF);
		builder(PredatorModule.RAIDERS).add(EntityTypeIds.ZOMBIE, EntityTypeIds.HUSK, EntityTypeIds.ZOMBIE_VILLAGER);
		builder(MobDropsModule.SMALL_LEATHER).add(EntityTypeIds.SHEEP, EntityTypeIds.PIG);
		// Game and livestock: a knife kill drops more of their meat, leather, wool and feathers.
		builder(KnifeModule.BUTCHERED)
				.add(EntityTypeIds.COW, EntityTypeIds.MOOSHROOM, EntityTypeIds.PIG, EntityTypeIds.SHEEP, EntityTypeIds.CHICKEN,
						EntityTypeIds.RABBIT, EntityTypeIds.GOAT, EntityTypeIds.HORSE, EntityTypeIds.DONKEY, EntityTypeIds.MULE,
						EntityTypeIds.LLAMA, EntityTypeIds.TRADER_LLAMA, EntityTypeIds.CAMEL, EntityTypeIds.POLAR_BEAR,
						EntityTypeIds.TURTLE, EntityTypeIds.PANDA);
		// Melee hostiles run after their target. Creepers and ranged mobs keep their vanilla pace.
		builder(RunningModule.RUNNERS)
				.add(EntityTypeIds.ZOMBIE, EntityTypeIds.HUSK, EntityTypeIds.DROWNED, EntityTypeIds.ZOMBIE_VILLAGER,
						EntityTypeIds.ZOMBIFIED_PIGLIN, EntityTypeIds.SPIDER, EntityTypeIds.CAVE_SPIDER,
						EntityTypeIds.VINDICATOR, EntityTypeIds.SILVERFISH, EntityTypeIds.ENDERMITE)
				.add(EamEntities.PILLAGER_GUARD.builtInRegistryHolder().key());
		// Blood moon: zombies and skeletons put on armor; zombies take up melee weapons, skeletons keep their bows.
		builder(BloodMoonModule.ARMORED)
				.add(EntityTypeIds.ZOMBIE, EntityTypeIds.HUSK, EntityTypeIds.DROWNED, EntityTypeIds.ZOMBIE_VILLAGER)
				.add(SKELETONS);
		// Monster evolution: zombies and skeletons of the overworld put on armor as the world ages; zombies take up weapons.
		builder(MonsterEvolutionModule.ARMORED)
				.add(EntityTypeIds.ZOMBIE, EntityTypeIds.HUSK, EntityTypeIds.DROWNED, EntityTypeIds.ZOMBIE_VILLAGER,
						EntityTypeIds.SKELETON, EntityTypeIds.STRAY, EntityTypeIds.BOGGED, EntityTypeIds.PARCHED);
		// Bosses: a player they kill keeps their things while the game rule is on.
		builder(DeathModule.BOSSES)
				.add(EamEntities.FALLEN_LORD.builtInRegistryHolder().key(), EamEntities.BABA_YAGA.builtInRegistryHolder().key(),
						EamEntities.KOSCHEI.builtInRegistryHolder().key(), EamEntities.PILLAGER_KING.builtInRegistryHolder().key())
				.add(EntityTypeIds.WITHER, EntityTypeIds.ENDER_DRAGON);
		// Bosses keep their own name and fight; they never become a player's nemesis.
		builder(NemesisModule.EXCLUDED).addTag(DeathModule.BOSSES);
		builder(MonsterEvolutionModule.ARMED)
				.add(EntityTypeIds.ZOMBIE, EntityTypeIds.HUSK, EntityTypeIds.DROWNED, EntityTypeIds.ZOMBIE_VILLAGER);
		// Later phases: skeletons carry a blade for close quarters, spiders spit webs.
		builder(MonsterEvolutionModule.SWORDSMEN)
				.add(EntityTypeIds.SKELETON, EntityTypeIds.STRAY, EntityTypeIds.BOGGED, EntityTypeIds.PARCHED);
		builder(MonsterEvolutionModule.WEB_SPINNERS)
				.add(EntityTypeIds.SPIDER, EntityTypeIds.CAVE_SPIDER)
				.add(EamEntities.WOOD_SPIDER.builtInRegistryHolder().key(), EamEntities.BLACK_WIDOW.builtInRegistryHolder().key());
		builder(BloodMoonModule.ARMED).add(EntityTypeIds.ZOMBIE, EntityTypeIds.HUSK, EntityTypeIds.ZOMBIE_VILLAGER);
		builder(BloodMoonModule.HUNTERS)
				.add(EntityTypeIds.ZOMBIE, EntityTypeIds.HUSK, EntityTypeIds.DROWNED, EntityTypeIds.ZOMBIE_VILLAGER,
						EntityTypeIds.SPIDER, EntityTypeIds.CAVE_SPIDER, EntityTypeIds.VINDICATOR)
				.add(SKELETONS)
				.add(EamEntities.PILLAGER_GUARD.builtInRegistryHolder().key());
		// The Fallen Lord drops exactly one black talisman, blood moon or not.
		builder(BloodMoonModule.SINGLE_LOOT).add(EamEntities.FALLEN_LORD.builtInRegistryHolder().key());
		builder(CowDefenseModule.DEFENDERS).add(EntityTypeIds.COW, EntityTypeIds.MOOSHROOM);
		// The Fallen Lord is undead: smite and silver hurt him more.
		builder(EntityTypeTags.UNDEAD).add(EamEntities.FALLEN_LORD.builtInRegistryHolder().key(),
				EamEntities.SKELETON_KNIGHT.builtInRegistryHolder().key(), EamEntities.SWAMP_LURKER.builtInRegistryHolder().key(),
				EamEntities.WIGHT.builtInRegistryHolder().key(), EamEntities.LOST.builtInRegistryHolder().key(), EamEntities.LOST_CENTURION.builtInRegistryHolder().key(),
				EamEntities.LOST_WALL_COMMANDER.builtInRegistryHolder().key(), EamEntities.LOST_LONGBOWMAN.builtInRegistryHolder().key(),
				EamEntities.UNDEAD_WARLOCK.builtInRegistryHolder().key(), EamEntities.LOST_CAVALRYMAN.builtInRegistryHolder().key(),
				EamEntities.LOST_WARHORSE.builtInRegistryHolder().key(), EamEntities.UPYR.builtInRegistryHolder().key(),
				EamEntities.DRAUGR.builtInRegistryHolder().key(), EamEntities.DROWNED_SOLDIER.builtInRegistryHolder().key(),
				EamEntities.ZOMBIE_DUERGAR.builtInRegistryHolder().key());
		// Draugr walk on powder snow like a wearer of leather boots and never freeze.
		builder(EntityTypeTags.POWDER_SNOW_WALKABLE_MOBS).add(EamEntities.DRAUGR.builtInRegistryHolder().key());
		builder(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES).add(EamEntities.DRAUGR.builtInRegistryHolder().key());
		// The swamp lurker and the upyr burn in daylight like a zombie.
		builder(EntityTypeTags.BURN_IN_DAYLIGHT).add(EamEntities.SWAMP_LURKER.builtInRegistryHolder().key(),
				EamEntities.UPYR.builtInRegistryHolder().key());
		// Spiders all the same for Bane of Arthropods.
		builder(EntityTypeTags.ARTHROPOD).add(EamEntities.WOOD_SPIDER.builtInRegistryHolder().key(),
				EamEntities.BLACK_WIDOW.builtInRegistryHolder().key());
		// Monsters and pillagers fight each other on sight.
		builder(PillagerFeudModule.PILLAGERS)
				.add(EntityTypeIds.PILLAGER, EntityTypeIds.VINDICATOR)
				.add(EamEntities.PILLAGER_GUARD.builtInRegistryHolder().key(), EamEntities.ELITE_CROSSBOWMAN.builtInRegistryHolder().key(),
						EamEntities.PILLAGER_KING.builtInRegistryHolder().key());
		// Undead and other monsters; the Fallen Lord and his crypt guards keep to their crypt.
		builder(PillagerFeudModule.ATTACKERS)
				.add(EntityTypeIds.ZOMBIE, EntityTypeIds.HUSK, EntityTypeIds.DROWNED, EntityTypeIds.ZOMBIE_VILLAGER,
						EntityTypeIds.SPIDER, EntityTypeIds.CAVE_SPIDER, EntityTypeIds.CREEPER, EntityTypeIds.WITCH,
						EntityTypeIds.SILVERFISH, EntityTypeIds.ENDERMITE, EntityTypeIds.BLAZE)
				.add(SKELETONS)
				.add(EamEntities.SWAMP_LURKER.builtInRegistryHolder().key(), EamEntities.WIGHT.builtInRegistryHolder().key(),
						EamEntities.WOOD_SPIDER.builtInRegistryHolder().key(), EamEntities.BLACK_WIDOW.builtInRegistryHolder().key(),
						EamEntities.LOST.builtInRegistryHolder().key(), EamEntities.LOST_CENTURION.builtInRegistryHolder().key(), EamEntities.UNDEAD_WARLOCK.builtInRegistryHolder().key(),
						EamEntities.LOST_WALL_COMMANDER.builtInRegistryHolder().key(), EamEntities.LOST_LONGBOWMAN.builtInRegistryHolder().key(),
						EamEntities.LOST_CAVALRYMAN.builtInRegistryHolder().key(), EamEntities.UPYR.builtInRegistryHolder().key(),
						EamEntities.DRAUGR.builtInRegistryHolder().key(), EamEntities.ZOMBIE_DUERGAR.builtInRegistryHolder().key(),
						EamEntities.DUERGAR_RENEGADE.builtInRegistryHolder().key());
		// Wolves hunt him like any skeleton.
		// Pillager guards are illagers and raiders: they join raids and side with the other illagers.
		builder(EntityTypeTags.ILLAGER).add(EamEntities.PILLAGER_GUARD.builtInRegistryHolder().key());
		builder(EntityTypeTags.RAIDERS).add(EamEntities.PILLAGER_GUARD.builtInRegistryHolder().key());
		builder(EntityTypeTags.SKELETONS).add(EamEntities.SKELETON_KNIGHT.builtInRegistryHolder().key());
		// The elite crossbowman is a pillager: he joins raids and counts as an illager.
		builder(EntityTypeTags.ILLAGER).add(EamEntities.ELITE_CROSSBOWMAN.builtInRegistryHolder().key());
		builder(EntityTypeTags.RAIDERS).add(EamEntities.ELITE_CROSSBOWMAN.builtInRegistryHolder().key());
		// Common monsters hear a noisy player. Bosses, squads with their own orders and brain-driven mobs do not.
		builder(NoiseModule.HEARERS)
				.add(EntityTypeIds.ZOMBIE, EntityTypeIds.HUSK, EntityTypeIds.DROWNED, EntityTypeIds.ZOMBIE_VILLAGER,
						EntityTypeIds.ZOMBIFIED_PIGLIN, EntityTypeIds.SPIDER, EntityTypeIds.CAVE_SPIDER, EntityTypeIds.CREEPER,
						EntityTypeIds.WITCH, EntityTypeIds.VINDICATOR, EntityTypeIds.PILLAGER, EntityTypeIds.SILVERFISH)
				.add(SKELETONS)
				.add(EamEntities.PILLAGER_GUARD.builtInRegistryHolder().key(), EamEntities.ZOMBIE_DUERGAR.builtInRegistryHolder().key(),
						EamEntities.DUERGAR_RENEGADE.builtInRegistryHolder().key(), EamEntities.WOOD_SPIDER.builtInRegistryHolder().key(),
						EamEntities.BLACK_WIDOW.builtInRegistryHolder().key(), EamEntities.WIGHT.builtInRegistryHolder().key(),
						EamEntities.LOST.builtInRegistryHolder().key());
	}
}
