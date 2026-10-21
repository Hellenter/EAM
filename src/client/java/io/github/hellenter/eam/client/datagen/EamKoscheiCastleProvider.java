package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.world.structures.koscheicastle.KoscheiCastleModule;
import io.github.hellenter.eam.world.structures.koscheicastle.KoscheiCastlePlacement;
import io.github.hellenter.eam.world.structures.koscheicastle.KoscheiCastleStructure;

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
 * Koschei's castle: Maxim's template in the middle of every patch of the underworld's ice caves, placed after the ice
 * spires so it can clear them within 12 blocks.
 */
public class EamKoscheiCastleProvider extends FabricDynamicRegistryProvider {
	public EamKoscheiCastleProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	public static void bootstrap(BootstrapContext<Structure> context) {
		context.register(KoscheiCastleModule.STRUCTURE, new KoscheiCastleStructure(
				new Structure.StructureSettings(context.lookup(Registries.BIOME).getOrThrow(KoscheiCastleModule.HAS_CASTLE), Map.of(),
						GenerationStep.Decoration.TOP_LAYER_MODIFICATION, TerrainAdjustment.BEARD_THIN),
				1.0F, 12));
	}

	public static void bootstrapSets(BootstrapContext<StructureSet> context) {
		context.register(KoscheiCastleModule.STRUCTURE_SET, new StructureSet(
				context.lookup(Registries.STRUCTURE).getOrThrow(KoscheiCastleModule.STRUCTURE), KoscheiCastlePlacement.INSTANCE));
	}

	@Override
	protected void configure(HolderLookup.Provider registries, Entries entries) {
		entries.add(registries.lookupOrThrow(Registries.STRUCTURE), KoscheiCastleModule.STRUCTURE);
		entries.add(registries.lookupOrThrow(Registries.STRUCTURE_SET), KoscheiCastleModule.STRUCTURE_SET);
	}

	@Override
	public String getName() {
		return "EAM Koschei castle";
	}
}
