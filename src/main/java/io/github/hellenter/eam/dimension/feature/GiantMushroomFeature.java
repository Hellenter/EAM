package io.github.hellenter.eam.dimension.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A mushroom bigger than vanilla's huge ones but built the same way, standing on the floor of a cave: a stem one block
 * thick, or a cross five columns thick for the tallest, under a cap of {@code cap} blocks shaped by {@link Shape}. For
 * a champignon some of the cap itself is {@code light} (glowing patches); for the others a few {@code light} blocks hang
 * under the cap. It fits itself under the ceiling, or is not placed.
 *
 * @param height      stem height
 * @param capRadius   cap radius
 * @param thickAbove  stems at least this tall are five columns thick
 * @param lightChance for a champignon the share of the cap that glows, otherwise the chance for each block under the
 *                    cap to have a glowing block hanging from it
 */
public record GiantMushroomFeature(BlockState cap, BlockState stem, BlockState light, Shape shape, IntProvider height, IntProvider capRadius,
		int thickAbove, float lightChance) implements Feature {
	public static final MapCodec<GiantMushroomFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BlockState.CODEC.fieldOf("cap").forGetter(GiantMushroomFeature::cap),
			BlockState.CODEC.fieldOf("stem").forGetter(GiantMushroomFeature::stem),
			BlockState.CODEC.fieldOf("light").forGetter(GiantMushroomFeature::light),
			Shape.CODEC.fieldOf("shape").forGetter(GiantMushroomFeature::shape),
			IntProviders.codec(2, 32).fieldOf("height").forGetter(GiantMushroomFeature::height),
			IntProviders.codec(1, 10).fieldOf("cap_radius").forGetter(GiantMushroomFeature::capRadius),
			Codec.intRange(1, 64).fieldOf("thick_above").forGetter(GiantMushroomFeature::thickAbove),
			Codec.floatRange(0.0F, 1.0F).fieldOf("light_chance").forGetter(GiantMushroomFeature::lightChance)
	).apply(i, GiantMushroomFeature::new));

	/** The cap's shape. */
	public enum Shape implements StringRepresentable {
		/** Vanilla's brown mushroom: one flat layer, a square with its corners cut. */
		BROWN("brown"),
		/** Vanilla's red mushroom: a flat top and walls hanging down round the stem. */
		RED("red"),
		/** A funnel rising outward from the stem, with a wavy rim. */
		CHANTERELLE("chanterelle"),
		/** A low rounded dome with a short skirt; some of it glows. */
		CHAMPIGNON("champignon");

		public static final Codec<Shape> CODEC = StringRepresentable.fromEnum(Shape::values);
		private final String name;

		Shape(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}
	}

	/** Room left between the cap's top and the ceiling. */
	private static final int HEADROOM = 1;
	private static final int MIN_HEIGHT = 3;

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
		int capTop = capRise(radius);
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
		for (int y = 0; y < height; y++) {
			stemBlocks.add(origin.above(y));
			if (thick && y < height - 1) {
				for (Direction side : Direction.Plane.HORIZONTAL) {
					stemBlocks.add(origin.above(y).relative(side));
				}
			}
		}
		BlockPos top = origin.above(height);
		Set<BlockPos> capBlocks = switch (this.shape) {
			case BROWN -> brownCap(top, radius);
			case RED -> redCap(top, radius);
			case CHANTERELLE -> chanterelleCap(top, radius, random);
			case CHAMPIGNON -> champignonCap(top, radius);
		};
		capBlocks.removeAll(stemBlocks);
		Set<BlockPos> glowing = this.shape == Shape.CHAMPIGNON ? glowPatches(capBlocks, random) : Set.of();

		for (BlockPos pos : stemBlocks) {
			if (replaceable(level, pos)) {
				level.setBlock(pos, faces(this.stem, pos, stemBlocks, Set.of()), 2);
			}
		}
		for (BlockPos pos : capBlocks) {
			if (replaceable(level, pos)) {
				level.setBlock(pos, faces(glowing.contains(pos) ? this.light : this.cap, pos, capBlocks, stemBlocks), 2);
			}
		}
		if (this.shape != Shape.CHAMPIGNON) {
			for (BlockPos pos : capBlocks) {
				BlockPos below = pos.below();
				if (!capBlocks.contains(below) && !stemBlocks.contains(below) && level.getBlockState(below).isAir()
						&& random.nextFloat() < this.lightChance) {
					level.setBlock(below, this.light, 2);
				}
			}
		}
		return true;
	}

	/** How far the cap rises above the stem's top. */
	private int capRise(int radius) {
		return switch (this.shape) {
			case BROWN, RED -> 1;
			case CHANTERELLE -> Mth.ceil(radius * 0.6) + 1;
			case CHAMPIGNON -> Math.max(2, radius * 3 / 5);
		};
	}

	/** Shows the skin where the mushroom does not continue and the pores inside, like vanilla's huge mushrooms. */
	private static BlockState faces(BlockState state, BlockPos pos, Set<BlockPos> same, Set<BlockPos> other) {
		for (Direction direction : Direction.values()) {
			var property = PipeBlock.PROPERTY_BY_DIRECTION.get(direction);
			if (state.hasProperty(property)) {
				BlockPos next = pos.relative(direction);
				state = state.setValue(property, !same.contains(next) && !other.contains(next));
			}
		}
		return state;
	}

	/** One flat layer on the stem: a square with its corners cut, as vanilla's brown mushroom. */
	private static Set<BlockPos> brownCap(BlockPos top, int radius) {
		Set<BlockPos> blocks = new HashSet<>();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (Math.abs(dx) == radius && Math.abs(dz) == radius) {
					continue;
				}
				blocks.add(top.offset(dx, 0, dz));
			}
		}
		return blocks;
	}

	/** A flat top one block narrower than the cap, and walls hanging round the stem, as vanilla's red mushroom. */
	private static Set<BlockPos> redCap(BlockPos top, int radius) {
		Set<BlockPos> blocks = new HashSet<>();
		int hang = radius + 1;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				int ring = Math.max(Math.abs(dx), Math.abs(dz));
				if (ring < radius) {
					blocks.add(top.offset(dx, 0, dz));
				} else if (Math.abs(dx) != Math.abs(dz)) {
					for (int dy = -hang; dy < 0; dy++) {
						blocks.add(top.offset(dx, dy, dz));
					}
				}
			}
		}
		return blocks;
	}

	/** A funnel: low at the stem, rising outward, its rim wavy. */
	private static Set<BlockPos> chanterelleCap(BlockPos top, int radius, RandomSource random) {
		Set<BlockPos> blocks = new HashSet<>();
		float phase = random.nextFloat() * Mth.TWO_PI;
		int waves = 4 + random.nextInt(3);
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				double angle = Math.atan2(dz, dx);
				double edge = radius + 0.5 + 0.7 * Math.sin(angle * waves + phase);
				if (d > edge) {
					continue;
				}
				int rise = (int) Math.round(d * 0.6);
				blocks.add(top.offset(dx, rise, dz));
				// Fill the step down to the ring inside, so the funnel has no holes.
				if (d > 1.0) {
					double inner = d - 1.0;
					int innerRise = (int) Math.round(inner * 0.6);
					for (int y = innerRise + 1; y < rise; y++) {
						blocks.add(top.offset(dx, y, dz));
					}
				}
			}
		}
		return blocks;
	}

	/** A low rounded dome shell with a one-block skirt round its rim. */
	private static Set<BlockPos> champignonCap(BlockPos stemTop, int radius) {
		Set<BlockPos> blocks = new HashSet<>();
		int rise = Math.max(2, radius * 3 / 5);
		for (int dy = -1; dy <= rise; dy++) {
			double fy = dy <= 0 ? 0.0 : (double) dy / rise;
			double ringOuter = (radius + 0.5) * Math.sqrt(Math.max(0.0, 1.0 - fy * fy));
			double ringInner = dy == rise ? -1.0 : (radius - 0.5) * Math.sqrt(Math.max(0.0, 1.0 - Math.pow(Math.min(1.0, (dy + 1.0) / rise), 2)));
			if (dy < 0) {
				ringInner = radius - 0.5;
			} else if (dy == 0) {
				ringInner = -1.0;
			}
			int r = (int) Math.ceil(ringOuter);
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					if (d <= ringOuter && d > ringInner) {
						blocks.add(stemTop.offset(dx, dy, dz));
					}
				}
			}
		}
		return blocks;
	}

	/** Round patches of the cap, about {@code lightChance} of it in all. */
	private Set<BlockPos> glowPatches(Set<BlockPos> capBlocks, RandomSource random) {
		Set<BlockPos> glowing = new HashSet<>();
		if (capBlocks.isEmpty() || this.lightChance <= 0.0F) {
			return glowing;
		}
		List<BlockPos> all = new ArrayList<>(capBlocks);
		int wanted = Math.max(1, Math.round(capBlocks.size() * this.lightChance));
		for (int tries = 0; glowing.size() < wanted && tries < 32; tries++) {
			BlockPos centre = all.get(random.nextInt(all.size()));
			double size = 1.0 + random.nextDouble() * 1.2;
			for (BlockPos pos : all) {
				if (pos.distSqr(centre) <= size * size) {
					glowing.add(pos);
				}
			}
		}
		return glowing;
	}

	private static boolean replaceable(WorldGenLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.isAir() || state.canBeReplaced() && state.getFluidState().isEmpty() || state.is(Blocks.GLOW_LICHEN);
	}
}
