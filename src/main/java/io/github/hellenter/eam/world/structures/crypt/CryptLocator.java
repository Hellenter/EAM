package io.github.hellenter.eam.world.structures.crypt;

import com.mojang.datafixers.util.Pair;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import org.jspecify.annotations.Nullable;

import java.util.Optional;

/** Finds the nearest crypt entrance for the fermented ender eye, the way the ender eye finds a stronghold. */
public final class CryptLocator {
	/** Structures the fermented ender eye flies to. */
	public static final TagKey<Structure> LOCATED_BY_EYE = TagKey.create(Registries.STRUCTURE, Eam.id("fermented_ender_eye_located"));

	/** Search radius in chunks, as for the ender eye. */
	private static final int SEARCH_RADIUS = 100;

	private CryptLocator() {
	}

	/** The middle of the nearest crypt's entrance, or {@code null} when no crypt is within reach. */
	public static @Nullable BlockPos nearestEntrance(ServerLevel level, BlockPos origin) {
		Optional<HolderSet.Named<Structure>> crypts = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(LOCATED_BY_EYE);
		if (crypts.isEmpty()) return null;
		Pair<BlockPos, Holder<Structure>> nearest = level.getChunkSource().getGenerator()
				.findNearestMapStructure(level, crypts.get(), origin, SEARCH_RADIUS, false);
		if (nearest == null) return null;
		BlockPos found = nearest.getFirst();
		// The entrance is the start's first piece; the located position is only the corner of its chunk.
		StructureStart start = level.structureManager().getStartForStructure(nearest.getSecond().value(),
				level.getChunk(found.getX() >> 4, found.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS));
		if (start == null || !start.isValid() || start.getPieces().isEmpty()) return found;
		return start.getPieces().getFirst().getBoundingBox().getCenter();
	}
}
