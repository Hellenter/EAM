package io.github.hellenter.eam.world.structures.yagahut;

import io.github.hellenter.eam.world.swamp.SwampyForestBiomes;
import io.github.hellenter.eam.world.swamp.SwampyForestLogic;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.placement.AbstractSpreadingStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One hut in the middle of every swamp. Swamps are laid out one per {@link SwampyForestLogic#cellSize()}-block cell, so
 * this is a random spread with one cell per swamp cell whose chosen chunk is the one holding the swamp's middle. It stays
 * a random spread so {@code /locate} searches it like any other structure.
 */
public class YagaHutPlacement extends RandomSpreadStructurePlacement {
	public static final YagaHutPlacement INSTANCE = new YagaHutPlacement();
	public static final MapCodec<RandomSpreadStructurePlacement> CODEC = MapCodec.unit(INSTANCE);

	/** The overworld's noises for each world seed, to find a swamp's middle from the seed alone, as {@code /locate} does. */
	private static final Map<Long, RandomState> STATES = new ConcurrentHashMap<>();
	/** Each cell's hut spot, worked out once: searching the swamp for it reads the climate a few hundred times. */
	private static final Map<CellKey, Optional<int[]>> SPOTS = new ConcurrentHashMap<>();
	private static final int MAX_CACHED = 4096;

	private record CellKey(long seed, int cellX, int cellZ) {
	}

	private YagaHutPlacement() {
		super(Vec3i.ZERO, AbstractSpreadingStructurePlacement.FrequencyReductionMethod.DEFAULT, 1.0F, 20261015, Optional.empty(),
				SwampyForestLogic.cellSize() / 16, 0, RandomSpreadType.LINEAR);
	}

	/** Remembers a world's overworld noises, for {@code /locate} before any new chunk has been made. */
	public static void remember(long seed, RandomState state) {
		STATES.put(seed, state);
	}

	/** The hut's {x, z} in the swamp cell holding {@code chunk}, or null when that cell has no swamp. */
	public static int @Nullable [] spot(long seed, RandomState state, ChunkPos chunk) {
		int cell = SwampyForestLogic.cellSize();
		int cellX = Math.floorDiv(chunk.getMinBlockX(), cell);
		int cellZ = Math.floorDiv(chunk.getMinBlockZ(), cell);
		CellKey key = new CellKey(seed, cellX, cellZ);
		Optional<int[]> spot = SPOTS.get(key);
		if (spot == null) {
			if (SPOTS.size() > MAX_CACHED) SPOTS.clear();
			Climate.Sampler sampler = state.createClimateSampler(SamplerContext.EMPTY_UNCACHED);
			spot = Optional.ofNullable(SwampyForestLogic.hutSpot(SwampyForestBiomes.climate(sampler), cellX, cellZ));
			SPOTS.put(key, spot);
		}
		return spot.orElse(null);
	}

	@Override
	public ChunkPos getPotentialStructureChunk(long seed, int sourceX, int sourceZ) {
		RandomState state = STATES.get(seed);
		if (state != null) {
			int[] spot = spot(seed, state, new ChunkPos(sourceX, sourceZ));
			if (spot != null) {
				return ChunkPos.containing(new BlockPos(spot[0], 0, spot[1]));
			}
		}
		return super.getPotentialStructureChunk(seed, sourceX, sourceZ);
	}

	@Override
	protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int sourceX, int sourceZ) {
		remember(state.getLevelSeed(), state.randomState());
		return super.isPlacementChunk(state, sourceX, sourceZ);
	}

	@Override
	public MapCodec<RandomSpreadStructurePlacement> codec() {
		return CODEC;
	}
}
