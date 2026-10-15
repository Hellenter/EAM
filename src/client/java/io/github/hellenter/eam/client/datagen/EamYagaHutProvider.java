package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.world.structures.yagahut.YagaHutModule;
import io.github.hellenter.eam.world.structures.yagahut.YagaHutPlacement;
import io.github.hellenter.eam.world.structures.yagahut.YagaHutStructure;
import io.github.hellenter.eam.world.swamp.SwampyForestBiomes;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Baba Yaga's hut: Maxim's template in the middle of every swampy forest, placed after the trees so it can level the
 * ground around it and clear them.
 */
public class EamYagaHutProvider extends FabricDynamicRegistryProvider {
	public EamYagaHutProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	public static void bootstrap(BootstrapContext<Structure> context) {
		context.register(YagaHutModule.STRUCTURE, new YagaHutStructure(
				new Structure.StructureSettings(context.lookup(Registries.BIOME).getOrThrow(SwampyForestBiomes.IS_SWAMPY_FOREST), Map.of(),
						GenerationStep.Decoration.TOP_LAYER_MODIFICATION, TerrainAdjustment.NONE)));
	}

	public static void bootstrapSets(BootstrapContext<StructureSet> context) {
		context.register(YagaHutModule.STRUCTURE_SET, new StructureSet(
				context.lookup(Registries.STRUCTURE).getOrThrow(YagaHutModule.STRUCTURE), YagaHutPlacement.INSTANCE));
	}

	@Override
	protected void configure(HolderLookup.Provider registries, Entries entries) {
		entries.add(registries.lookupOrThrow(Registries.STRUCTURE), YagaHutModule.STRUCTURE);
		entries.add(registries.lookupOrThrow(Registries.STRUCTURE_SET), YagaHutModule.STRUCTURE_SET);
	}

	@Override
	public String getName() {
		return "EAM Baba Yaga hut";
	}
}
