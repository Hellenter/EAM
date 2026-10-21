package io.github.hellenter.eam.world.structures.koscheicastle;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.placement.AbstractSpreadingStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One castle spot per {@link KoscheiCastleLogic#CELL}-sample cell: the middle of the biggest ice spikes patch whose
 * middle lies in the cell. It stays a random spread with one cell per cell, so {@code /locate} searches it like any
 * other structure; a cell without such a patch falls back to a random chunk, where the castle finds no spot and
 * generates nothing.
 */
public class KoscheiCastlePlacement extends RandomSpreadStructurePlacement {
	public static final KoscheiCastlePlacement INSTANCE = new KoscheiCastlePlacement();
	public static final MapCodec<RandomSpreadStructurePlacement> CODEC = MapCodec.unit(INSTANCE);

	/** The height the biome is read at: the surface of the low, flat ice spikes land. */
	private static final int SAMPLE_Y = 72;
	private static final int MAX_CACHED = 4096;

	/** The overworld's biomes and noises for each world seed, to find a patch's middle from the seed alone, as {@code /locate} does. */
	private static final Map<Long, World> WORLDS = new ConcurrentHashMap<>();
	/** Each cell's spot, worked out once: following a patch reads the climate a few hundred times. */
	private static final Map<CellKey, Optional<KoscheiCastleLogic.Patch>> PATCHES = new ConcurrentHashMap<>();

	private record World(BiomeSource biomes, RandomState state) {
	}

	private record CellKey(long seed, int cellX, int cellZ) {
	}

	private KoscheiCastlePlacement() {
		super(Vec3i.ZERO, AbstractSpreadingStructurePlacement.FrequencyReductionMethod.DEFAULT, 1.0F, 20261021, Optional.empty(),
				KoscheiCastleLogic.CELL * KoscheiCastleLogic.SAMPLE / 16, 0, RandomSpreadType.LINEAR);
	}

	/** Remembers a world's overworld biomes and noises, for {@code /locate} before any new chunk has been made. */
	public static void remember(long seed, BiomeSource biomes, RandomState state) {
		WORLDS.put(seed, new World(biomes, state));
	}

	/** The patch whose middle is the castle's spot in the cell holding {@code chunk}, or null when the cell has none. */
	public static KoscheiCastleLogic.@Nullable Patch patch(long seed, ChunkPos chunk) {
		World world = WORLDS.get(seed);
		if (world == null) return null;
		int cell = KoscheiCastleLogic.CELL * KoscheiCastleLogic.SAMPLE;
		CellKey key = new CellKey(seed, Math.floorDiv(chunk.getMinBlockX(), cell), Math.floorDiv(chunk.getMinBlockZ(), cell));
		Optional<KoscheiCastleLogic.Patch> patch = PATCHES.get(key);
		if (patch == null) {
			if (PATCHES.size() > MAX_CACHED) PATCHES.clear();
			BiomeResolver biomes = world.biomes().createUncachedResolver(world.state());
			patch = Optional.ofNullable(KoscheiCastleLogic.patch((x, z) -> biomes.getNoiseBiome(
					QuartPos.fromBlock(x * KoscheiCastleLogic.SAMPLE + KoscheiCastleLogic.SAMPLE / 2), QuartPos.fromBlock(SAMPLE_Y),
					QuartPos.fromBlock(z * KoscheiCastleLogic.SAMPLE + KoscheiCastleLogic.SAMPLE / 2)).is(KoscheiCastleModule.HAS_CASTLE),
					key.cellX(), key.cellZ()));
			PATCHES.put(key, patch);
		}
		return patch.orElse(null);
	}

	@Override
	public ChunkPos getPotentialStructureChunk(long seed, int sourceX, int sourceZ) {
		KoscheiCastleLogic.Patch patch = patch(seed, new ChunkPos(sourceX, sourceZ));
		if (patch != null) {
			int[] spot = patch.block();
			return ChunkPos.containing(new BlockPos(spot[0], 0, spot[1]));
		}
		return super.getPotentialStructureChunk(seed, sourceX, sourceZ);
	}

	@Override
	public MapCodec<RandomSpreadStructurePlacement> codec() {
		return CODEC;
	}
}
