package io.github.hellenter.eam.world.swamp;

import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.swamp.ShelfMushroomBlock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Shelf mushrooms on the tree trunks of one chunk: {@code tries} random columns, and on every trunk found there one to
 * three shelves stacked up one side, low on the trunk.
 *
 * @param tries columns tried per chunk
 */
public record ShelfMushroomFeature(int tries) implements Feature {
	public static final MapCodec<ShelfMushroomFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.intRange(1, 256).fieldOf("tries").forGetter(ShelfMushroomFeature::tries)
	).apply(i, ShelfMushroomFeature::new));
	/** Shelves grow up to this many blocks above the ground. */
	private static final int MAX_HEIGHT = 5;

	@Override
	public MapCodec<ShelfMushroomFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		boolean placed = false;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int i = 0; i < this.tries; i++) {
			int x = (origin.getX() & ~15) + random.nextInt(16);
			int z = (origin.getZ() & ~15) + random.nextInt(16);
			int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
			// The trunk: logs straight up from the ground in this column.
			if (!level.getBlockState(pos.set(x, ground, z)).is(BlockTags.LOGS)) {
				continue;
			}
			Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(random);
			int y = ground + random.nextInt(2);
			int count = 1 + random.nextInt(3);
			for (int n = 0; n < count && y < ground + MAX_HEIGHT; n++, y += 1 + random.nextInt(2)) {
				BlockPos log = new BlockPos(x, y, z);
				BlockPos spot = log.relative(side);
				BlockState shelf = EamBlocks.SHELF_MUSHROOM.defaultBlockState().setValue(ShelfMushroomBlock.FACING, side.getOpposite());
				if (level.getBlockState(log).is(BlockTags.LOGS) && level.isEmptyBlock(spot)) {
					level.setBlock(spot, shelf, 2);
					placed = true;
				}
			}
		}
		return placed;
	}
}
