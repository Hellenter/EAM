package io.github.hellenter.eam.world.swamp;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.mobs.entity.lurker.SwampLurker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

/**
 * The lakes of one chunk: their beds turn to mud, a few tussocks of grass rise out of the shallow water, and now and
 * then a swamp lurker lies hidden in the mud of the bed. Placed once per chunk at the chunk's corner.
 *
 * @param hummocks     how many tussocks to try in the chunk's water
 * @param lurkerChance chance that a chunk with a lake bed gets a lurker lying in it
 */
public record LakeBedFeature(IntProvider hummocks, float lurkerChance) implements Feature {
	public static final MapCodec<LakeBedFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			IntProviders.codec(0, 16).fieldOf("hummocks").forGetter(LakeBedFeature::hummocks),
			Codec.floatRange(0.0F, 1.0F).fieldOf("lurker_chance").forGetter(LakeBedFeature::lurkerChance)
	).apply(i, LakeBedFeature::new));

	@Override
	public MapCodec<LakeBedFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		int minX = origin.getX() & ~15;
		int minZ = origin.getZ() & ~15;
		List<BlockPos> beds = new ArrayList<>();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = minX; x < minX + 16; x++) {
			for (int z = minZ; z < minZ + 16; z++) {
				int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
				int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
				pos.set(x, floor - 1, z);
				if (surface <= floor || !level.getFluidState(pos.above()).is(Fluids.WATER)
						|| !level.getBiome(pos).is(SwampyForestBiomes.IS_SWAMPY_FOREST)) {
					continue;
				}
				BlockState bed = level.getBlockState(pos);
				if (bed.isSolid() && !bed.is(Blocks.MUD)) {
					level.setBlock(pos, Blocks.MUD.defaultBlockState(), 2);
					if (random.nextBoolean() && level.getBlockState(pos.below()).isSolid()) {
						level.setBlock(pos.below(), Blocks.MUD.defaultBlockState(), 2);
					}
				}
				beds.add(pos.immutable());
			}
		}
		if (beds.isEmpty()) {
			return false;
		}
		int hummocks = this.hummocks.sample(random);
		for (int n = 0; n < hummocks; n++) {
			hummock(level, random, beds.get(random.nextInt(beds.size())));
		}
		if (random.nextFloat() < this.lurkerChance) {
			lurker(level, beds.get(random.nextInt(beds.size())));
		}
		return true;
	}

	/** A tussock: mud from the bed up to the water's surface and a grass block one above it, one or two blocks wide. */
	private static void hummock(WorldGenLevel level, RandomSource random, BlockPos bed) {
		int size = 1 + random.nextInt(3);
		for (int n = 0; n < size; n++) {
			BlockPos column = n == 0 ? bed : bed.offset(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
			int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, column.getX(), column.getZ());
			int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, column.getX(), column.getZ());
			// Only in shallow water, and the block above the water must be free.
			if (top - floor > 2 || top <= floor || !level.isEmptyBlock(new BlockPos(column.getX(), top, column.getZ()))) {
				continue;
			}
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(column.getX(), floor, column.getZ());
			for (; pos.getY() < top; pos.move(0, 1, 0)) {
				level.setBlock(pos, Blocks.MUD.defaultBlockState(), 2);
			}
			level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
		}
	}

	/** A swamp lurker standing on the bed; spawned at chunk generation, it at once hides in the mud under it. */
	private static void lurker(WorldGenLevel level, BlockPos bed) {
		if (!level.getBlockState(bed).is(Blocks.MUD)) {
			return;
		}
		SwampLurker lurker = EamEntities.SWAMP_LURKER.create(level.getLevel(), EntitySpawnReason.CHUNK_GENERATION);
		if (lurker == null) {
			return;
		}
		lurker.snapTo(bed.getX() + 0.5, bed.getY() + 1, bed.getZ() + 0.5);
		lurker.finalizeSpawn(level, level.getCurrentDifficultyAt(bed), EntitySpawnReason.CHUNK_GENERATION, null);
		lurker.setPersistenceRequired();
		level.addFreshEntityWithPassengers(lurker);
	}
}
