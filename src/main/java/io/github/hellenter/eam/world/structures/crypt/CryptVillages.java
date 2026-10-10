package io.github.hellenter.eam.world.structures.crypt;

import io.github.hellenter.eam.world.structures.VillageModule;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.List;
import java.util.Optional;

/**
 * Finds the village a crypt goes with. Crypts sit on the same grid as villages ({@link CryptModule#STRUCTURE_SET}), so
 * the village of a crypt's chunk starts in that same chunk; laying it out again here gives the very village the world
 * builds, since both draw from the same seeded random.
 */
final class CryptVillages {
	private CryptVillages() {
	}

	/** The boxes of the abandoned village starting in the context's chunk, or empty when there is none. */
	static Optional<List<BoundingBox>> abandonedVillage(Structure.GenerationContext context) {
		Optional<Holder.Reference<StructureSet>> villages =
				context.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).get(BuiltinStructureSets.VILLAGES);
		if (villages.isEmpty()) return Optional.empty();
		// Village kinds want different biomes, so the first one that generates here is the one the world picks.
		for (StructureSet.StructureSelectionEntry entry : villages.get().value().structures()) {
			Holder<Structure> village = entry.structure();
			StructureStart start = village.value().generate(village, Level.OVERWORLD, context.registryAccess(),
					context.chunkGenerator(), context.biomeSource(), context.climateSampler(), context.randomState(),
					context.structureTemplateManager(), context.seed(), context.chunkPos(), 0, context.heightAccessor(),
					village.value().biomes()::contains);
			if (!start.isValid()) continue;
			if (!VillageModule.isAbandoned(start)) return Optional.empty();
			return Optional.of(start.getPieces().stream().map(StructurePiece::getBoundingBox).toList());
		}
		return Optional.empty();
	}
}
