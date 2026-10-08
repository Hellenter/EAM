package io.github.hellenter.eam.mixin.world.structures;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import io.github.hellenter.eam.world.structures.StructureRemovalModule;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import org.spongepowered.asm.mixin.Mixin;

/** Removed structures never start. */
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {
	@WrapMethod(method = "tryGenerateStructure")
	private boolean eam$removedStructures(StructureSet.StructureSelectionEntry selected, StructureManager structureManager,
			RegistryAccess registryAccess, RandomState randomState, StructureTemplateManager structureTemplateManager, long seed,
			ChunkAccess centerChunk, ChunkPos sourceChunkPos, ResourceKey<Level> level, Climate.Sampler climateSampler,
			Operation<Boolean> original) {
		return !StructureRemovalModule.isRemoved(selected.structure()) && original.call(selected, structureManager, registryAccess,
				randomState, structureTemplateManager, seed, centerChunk, sourceChunkPos, level, climateSampler);
	}
}
