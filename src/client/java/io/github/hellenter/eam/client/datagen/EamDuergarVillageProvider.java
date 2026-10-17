package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.world.structures.EmptyContainerLoot;
import io.github.hellenter.eam.world.structures.village.DuergarVillage;
import io.github.hellenter.eam.world.structures.village.DuergarVillage.Style;
import io.github.hellenter.eam.world.structures.village.DuergarVillageStructure;

import com.mojang.datafixers.util.Pair;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProtectedBlockProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Duergar villages ({@link DuergarVillage}): one jigsaw structure per style, each in its own biome, all in one structure
 * set so two villages never stand close. A village stands on a wide cave floor between y 58 and 150, each piece set down
 * onto the floor under it ({@link DuergarVillageStructure}); its pieces clear only the room they need and the ground
 * under them is filled in gently (beard_thin), so it follows its cave. No monsters spawn inside.
 */
public class EamDuergarVillageProvider extends FabricDynamicRegistryProvider {
	private static final String TEMPLATES = "eam:duergar_village/";
	public static final ResourceKey<StructureProcessorList> PROCESSORS =
			ResourceKey.create(Registries.PROCESSOR_LIST, Eam.id("duergar_village"));

	public EamDuergarVillageProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	/** Bedrock and the like stay whole where a village hall reaches them. */
	public static void bootstrapProcessors(BootstrapContext<StructureProcessorList> context) {
		HolderGetter<net.minecraft.world.level.block.Block> blocks = context.lookup(Registries.BLOCK);
		context.register(PROCESSORS, new StructureProcessorList(List.of(
				new ProtectedBlockProcessor(blocks.getOrThrow(BlockTags.FEATURES_CANNOT_REPLACE)),
				// Barrels Maxim left empty hold a few potions.
				new EmptyContainerLoot(Blocks.BARREL, EmptyContainerLoot.LIGHT_POTIONS))));
	}

	public static void bootstrapPools(BootstrapContext<StructureTemplatePool> context) {
		HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
		Holder<StructureProcessorList> processors = context.lookup(Registries.PROCESSOR_LIST).getOrThrow(PROCESSORS);
		Holder<StructureTemplatePool> empty = pools.getOrThrow(Pools.EMPTY);
		for (Style style : Style.values()) {
			String dir = style.id() + "/";
			context.register(style.pool("start"), new StructureTemplatePool(empty, List.of(
					piece(dir + "plaza", processors, 1)), StructureTemplatePool.Projection.RIGID));
			// Straight streets, turns, forks, ramps up or down and bridges, so a village follows its cave.
			context.register(style.pool("streets"), new StructureTemplatePool(pools.getOrThrow(style.pool("ends")), List.of(
					piece(dir + "street/1", processors, 5),
					piece(dir + "street/2", processors, 2),
					piece(dir + "street/3", processors, 2),
					piece(dir + "street/4", processors, 3),
					piece(dir + "street/5", processors, 3),
					piece(dir + "ramp", processors, 3),
					piece(dir + "corner", processors, 3),
					piece(dir + "fork", processors, 2),
					piece(dir + "crossing", processors, 1),
					piece(dir + "bridge", processors, 1)), StructureTemplatePool.Projection.RIGID));
			context.register(style.pool("ends"), new StructureTemplatePool(empty, List.of(
					piece(dir + "end/1", processors, 2),
					piece(dir + "end/2", processors, 1)), StructureTemplatePool.Projection.RIGID));
			// Ten kinds of building and a yard; small homes most often, one drinking hall at most now and then.
			context.register(style.pool("houses"), new StructureTemplatePool(empty, List.of(
					piece(dir + "house/1", processors, 4),
					piece(dir + "house/2", processors, 4),
					piece(dir + "house/3", processors, 3),
					piece(dir + "house/4", processors, 2),
					piece(dir + "house/5", processors, 2),
					piece(dir + "house/6", processors, 1),
					piece(dir + "house/7", processors, 2),
					piece(dir + "house/8", processors, 2),
					piece(dir + "house/9", processors, 2),
					piece(dir + "house/10", processors, 1),
					piece(dir + "house/yard", processors, 2)), StructureTemplatePool.Projection.RIGID));
		}
	}

	/** Lit halls with their own folk in them: no monster spawns inside a village. */
	public static void bootstrap(BootstrapContext<Structure> context) {
		Map<MobCategory, StructureSpawnOverride> spawns = Map.of(MobCategory.MONSTER, new StructureSpawnOverride(
				StructureSpawnOverride.BoundingBoxType.STRUCTURE, WeightedList.of()));
		for (Style style : Style.values()) {
			context.register(style.structure(), new DuergarVillageStructure(
					new Structure.StructureSettings(context.lookup(Registries.BIOME).getOrThrow(style.biomes()), spawns,
							GenerationStep.Decoration.UNDERGROUND_STRUCTURES, TerrainAdjustment.BEARD_THIN),
					context.lookup(Registries.TEMPLATE_POOL).getOrThrow(style.pool("start")), 6, 58, 150, 56, 1, 14, 0.15F));
		}
	}

	public static void bootstrapSets(BootstrapContext<StructureSet> context) {
		HolderGetter<Structure> structures = context.lookup(Registries.STRUCTURE);
		List<StructureSet.StructureSelectionEntry> entries = new ArrayList<>();
		for (Style style : Style.values()) {
			entries.add(StructureSet.entry(structures.getOrThrow(style.structure())));
		}
		context.register(DuergarVillage.STRUCTURE_SET, new StructureSet(entries,
				new RandomSpreadStructurePlacement(32, 12, RandomSpreadType.LINEAR, 20261014)));
	}

	private static Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer> piece(
			String name, Holder<StructureProcessorList> processors, int weight) {
		return Pair.of(StructurePoolElement.single(TEMPLATES + name, processors, LiquidSettings.IGNORE_WATERLOGGING), weight);
	}

	@Override
	protected void configure(HolderLookup.Provider registries, Entries entries) {
		entries.add(registries.lookupOrThrow(Registries.PROCESSOR_LIST), PROCESSORS);
		for (Style style : Style.values()) {
			for (String kind : List.of("start", "streets", "ends", "houses")) {
				entries.add(registries.lookupOrThrow(Registries.TEMPLATE_POOL), style.pool(kind));
			}
			entries.add(registries.lookupOrThrow(Registries.STRUCTURE), style.structure());
		}
		entries.add(registries.lookupOrThrow(Registries.STRUCTURE_SET), DuergarVillage.STRUCTURE_SET);
	}

	@Override
	public String getName() {
		return "EAM duergar villages";
	}
}
