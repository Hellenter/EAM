package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.world.structures.mine.UnderworldMine;

import com.mojang.datafixers.util.Pair;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProtectedBlockProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.RandomBlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * The underworld mine ({@link UnderworldMine}): a jigsaw structure like vanilla's ancient city, built from Maxim-editable
 * templates. It starts from a hall between y 60 and 120 and grows up to seven pieces deep, never below y 56 (the deep
 * bedrock band is under it) nor into the bedrock roof.
 */
public class EamUnderworldMineProvider extends FabricDynamicRegistryProvider {
	private static final String TEMPLATES = "eam:underworld_mine/";

	public EamUnderworldMineProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	/**
	 * Floor markers (brown wool) bridge caves and lakes with dark oak planks and otherwise keep the rock; a few rails,
	 * torches and beams are missing, cobwebs hang here and there, and bedrock (the deep bedrock band too) stays whole.
	 */
	public static void bootstrapProcessors(BootstrapContext<StructureProcessorList> context) {
		HolderGetter<Block> blocks = context.lookup(Registries.BLOCK);
		context.register(UnderworldMine.PROCESSORS, new StructureProcessorList(List.of(
				new ProtectedBlockProcessor(blocks.getOrThrow(BlockTags.FEATURES_CANNOT_REPLACE)),
				new RuleProcessor(List.of(
						new ProcessorRule(new BlockMatchTest(Blocks.WOOL.brown()), new TagMatchTest(BlockTags.REPLACEABLE),
								Blocks.DARK_OAK_PLANKS.defaultBlockState()),
						new ProcessorRule(new RandomBlockMatchTest(Blocks.RAIL, 0.25F), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
						new ProcessorRule(new RandomBlockMatchTest(Blocks.WALL_TORCH, 0.5F), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
						new ProcessorRule(new RandomBlockMatchTest(Blocks.LANTERN, 0.3F), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
						new ProcessorRule(new RandomBlockMatchTest(Blocks.DARK_OAK_PLANKS, 0.1F), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
						new ProcessorRule(new RandomBlockMatchTest(Blocks.AIR, 0.015F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()))),
				new BlockIgnoreProcessor(List.of(Blocks.WOOL.brown())))));
	}

	public static void bootstrapPools(BootstrapContext<StructureTemplatePool> context) {
		HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
		Holder<StructureProcessorList> processors = context.lookup(Registries.PROCESSOR_LIST).getOrThrow(UnderworldMine.PROCESSORS);
		Holder<StructureTemplatePool> empty = pools.getOrThrow(Pools.EMPTY);
		context.register(UnderworldMine.START_POOL, new StructureTemplatePool(empty, List.of(
				piece("hub/1", processors, 1)), StructureTemplatePool.Projection.RIGID));
		context.register(UnderworldMine.TUNNEL_POOL, new StructureTemplatePool(pools.getOrThrow(UnderworldMine.END_POOL), List.of(
				piece("corridor/1", processors, 10),
				piece("corridor/2", processors, 4),
				piece("corridor/3", processors, 3),
				piece("corridor/4", processors, 4),
				piece("crossing/1", processors, 4),
				piece("crossing/2", processors, 4),
				piece("crossing/3", processors, 2),
				piece("stairs/1", processors, 3),
				piece("room/1", processors, 2)), StructureTemplatePool.Projection.RIGID));
		context.register(UnderworldMine.END_POOL, new StructureTemplatePool(empty, List.of(
				piece("end/1", processors, 1)), StructureTemplatePool.Projection.RIGID));
	}

	public static void bootstrap(BootstrapContext<Structure> context) {
		context.register(UnderworldMine.STRUCTURE, new JigsawStructure(
				new Structure.StructureSettings(context.lookup(Registries.BIOME).getOrThrow(UnderworldMine.HAS_MINE), Map.of(),
						GenerationStep.Decoration.UNDERGROUND_STRUCTURES, TerrainAdjustment.NONE),
				context.lookup(Registries.TEMPLATE_POOL).getOrThrow(UnderworldMine.START_POOL), Optional.empty(), 7,
				UniformHeight.of(VerticalAnchor.absolute(60), VerticalAnchor.absolute(120)), false, Optional.empty(),
				new JigsawStructure.MaxDistance(80, 48), List.of(), new DimensionPadding(56, 8), LiquidSettings.IGNORE_WATERLOGGING));
	}

	public static void bootstrapSets(BootstrapContext<StructureSet> context) {
		context.register(UnderworldMine.STRUCTURE_SET, new StructureSet(
				context.lookup(Registries.STRUCTURE).getOrThrow(UnderworldMine.STRUCTURE),
				new RandomSpreadStructurePlacement(12, 4, RandomSpreadType.LINEAR, 20261013)));
	}

	private static Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer> piece(
			String name, Holder<StructureProcessorList> processors, int weight) {
		return Pair.of(StructurePoolElement.single(TEMPLATES + name, processors, LiquidSettings.IGNORE_WATERLOGGING), weight);
	}

	@Override
	protected void configure(HolderLookup.Provider registries, Entries entries) {
		entries.add(registries.lookupOrThrow(Registries.PROCESSOR_LIST), UnderworldMine.PROCESSORS);
		entries.add(registries.lookupOrThrow(Registries.TEMPLATE_POOL), UnderworldMine.START_POOL);
		entries.add(registries.lookupOrThrow(Registries.TEMPLATE_POOL), UnderworldMine.TUNNEL_POOL);
		entries.add(registries.lookupOrThrow(Registries.TEMPLATE_POOL), UnderworldMine.END_POOL);
		entries.add(registries.lookupOrThrow(Registries.STRUCTURE), UnderworldMine.STRUCTURE);
		entries.add(registries.lookupOrThrow(Registries.STRUCTURE_SET), UnderworldMine.STRUCTURE_SET);
	}

	@Override
	public String getName() {
		return "EAM underworld mine";
	}
}
