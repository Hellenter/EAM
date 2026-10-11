package io.github.hellenter.eam.dimension.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import java.util.HashSet;
import java.util.Set;

/**
 * A mushroom far bigger than vanilla's huge ones, standing on the floor of a cave: a stem one block thick, or a cross
 * five columns thick for the tallest, under a cap of {@code cap} blocks. A {@code dome} cap is a hollow half-ball that
 * hangs down around the stem, like a red mushroom; otherwise the cap is a wide flat disc with a drooping rim, like a
 * brown one. Some blocks under the cap glow ({@code light}). It fits itself under the ceiling, or is not placed.
 *
 * @param height     stem height
 * @param capRadius  cap radius
 * @param thickAbove stems at least this tall are five columns thick
 * @param lightChance chance for each block under the cap to have a glowing block hanging from it
 */
public record GiantMushroomFeature(BlockState cap, BlockState stem, BlockState light, boolean dome, IntProvider height, IntProvider capRadius,
		int thickAbove, float lightChance) implements Feature {
	public static final MapCodec<GiantMushroomFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BlockState.CODEC.fieldOf("cap").forGetter(GiantMushroomFeature::cap),
			BlockState.CODEC.fieldOf("stem").forGetter(GiantMushroomFeature::stem),
			BlockState.CODEC.fieldOf("light").forGetter(GiantMushroomFeature::light),
			Codec.BOOL.fieldOf("dome").forGetter(GiantMushroomFeature::dome),
			IntProviders.codec(3, 48).fieldOf("height").forGetter(GiantMushroomFeature::height),
			IntProviders.codec(2, 12).fieldOf("cap_radius").forGetter(GiantMushroomFeature::capRadius),
			Codec.intRange(1, 64).fieldOf("thick_above").forGetter(GiantMushroomFeature::thickAbove),
			Codec.floatRange(0.0F, 1.0F).fieldOf("light_chance").forGetter(GiantMushroomFeature::lightChance)
	).apply(i, GiantMushroomFeature::new));

	/** Room left between the cap's top and the ceiling. */
	private static final int HEADROOM = 1;
	private static final int MIN_HEIGHT = 4;

	@Override
	public MapCodec<GiantMushroomFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		BlockState ground = level.getBlockState(origin.below());
		if (!ground.isFaceSturdy(level, origin.below(), Direction.UP) || !level.getBlockState(origin).isAir()) {
			return false;
		}
		int radius = this.capRadius.sample(random);
		int wanted = this.height.sample(random);
		int capTop = this.dome ? Math.max(2, radius * 2 / 3) : 1;
		// How much open air is above: the mushroom shrinks to fit under the ceiling.
		int clearance = 0;
		while (clearance < wanted + capTop + HEADROOM && level.getBlockState(origin.above(clearance)).isAir()
				&& !level.isOutsideBuildHeight(origin.above(clearance))) {
			clearance++;
		}
		int height = Math.min(wanted, clearance - capTop - HEADROOM);
		if (height < MIN_HEIGHT) {
			return false;
		}
		boolean thick = height >= this.thickAbove;

		Set<BlockPos> stemBlocks = new HashSet<>();
		for (int y = 0; y < height + (this.dome ? 1 : 0); y++) {
			stemBlocks.add(origin.above(y));
			if (thick && y < height - 1) {
				for (Direction side : Direction.Plane.HORIZONTAL) {
					stemBlocks.add(origin.above(y).relative(side));
				}
			}
		}
		Set<BlockPos> capBlocks = this.dome ? domeCap(origin.above(height), radius, capTop) : flatCap(origin.above(height), radius);
		capBlocks.removeAll(stemBlocks);

		for (BlockPos pos : stemBlocks) {
			if (replaceable(level, pos)) {
				level.setBlock(pos, this.stem, 2);
			}
		}
		for (BlockPos pos : capBlocks) {
			if (!replaceable(level, pos)) {
				continue;
			}
			BlockState state = this.cap;
			for (Direction direction : Direction.values()) {
				var property = PipeBlock.PROPERTY_BY_DIRECTION.get(direction);
				if (state.hasProperty(property)) {
					// The outer face shows where the cap does not continue; inside faces show the pores.
					BlockPos next = pos.relative(direction);
					state = state.setValue(property, !capBlocks.contains(next) && !stemBlocks.contains(next));
				}
			}
			level.setBlock(pos, state, 2);
		}
		for (BlockPos pos : capBlocks) {
			BlockPos below = pos.below();
			if (!capBlocks.contains(below) && !stemBlocks.contains(below) && level.getBlockState(below).isAir() && random.nextFloat() < this.lightChance) {
				level.setBlock(below, this.light, 2);
			}
		}
		return true;
	}

	/** A flat disc, one layer, with a rim one block lower round the edge for caps of radius 4 and more. */
	private static Set<BlockPos> flatCap(BlockPos top, int radius) {
		Set<BlockPos> blocks = new HashSet<>();
		double outer = (radius + 0.5) * (radius + 0.5);
		double inner = (radius - 0.5) * (radius - 0.5);
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				double d = dx * dx + dz * dz;
				if (d > outer) {
					continue;
				}
				blocks.add(top.offset(dx, 0, dz));
				if (radius >= 4 && d > inner) {
					blocks.add(top.offset(dx, -1, dz));
				}
			}
		}
		return blocks;
	}

	/** A hollow half-ball shell one block thick, hanging {@code radius / 2} blocks below the stem's top. */
	private static Set<BlockPos> domeCap(BlockPos stemTop, int radius, int rise) {
		Set<BlockPos> blocks = new HashSet<>();
		int hang = Math.max(1, radius / 2);
		for (int dy = -hang; dy <= rise; dy++) {
			// The ball is squashed: rise blocks up from the stem's top to its crown.
			double fy = dy <= 0 ? 0.0 : (double) dy / rise * radius;
			double ringOuter = Math.sqrt(Math.max(0.0, (radius + 0.5) * (radius + 0.5) - fy * fy));
			double ringInner = dy == rise ? -1.0 : Math.sqrt(Math.max(0.0, (radius - 0.5) * (radius - 0.5) - fy * fy));
			if (dy < 0) {
				// The hanging skirt: only the outer ring.
				ringInner = radius - 0.5;
			}
			int r = (int) Math.ceil(ringOuter);
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					if (d <= ringOuter && (d > ringInner || dy == rise)) {
						blocks.add(stemTop.offset(dx, dy, dz));
					}
				}
			}
		}
		return blocks;
	}

	private static boolean replaceable(WorldGenLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.isAir() || state.canBeReplaced() && state.getFluidState().isEmpty() || state.is(Blocks.GLOW_LICHEN);
	}
}
