package io.github.hellenter.eam.dimension.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * A rock pillar from a cave's floor, or the bottom of a lava lake, all the way up to its ceiling, flaring out where it
 * meets both. Started anywhere in the open: it looks up for the ceiling and down through air and fluid for the floor,
 * and builds nothing unless it finds both. {@code accent} replaces the main block here and there.
 *
 * @param radius      radius in the middle
 * @param accentChance share of the pillar made of {@code accent}
 * @param search      how far up and down to look
 */
public record CavePillarFeature(BlockState block, BlockState accent, IntProvider radius, float accentChance, int search) implements Feature {
	public static final MapCodec<CavePillarFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BlockState.CODEC.fieldOf("block").forGetter(CavePillarFeature::block),
			BlockState.CODEC.fieldOf("accent").forGetter(CavePillarFeature::accent),
			IntProviders.codec(1, 5).fieldOf("radius").forGetter(CavePillarFeature::radius),
			Codec.floatRange(0.0F, 1.0F).fieldOf("accent_chance").forGetter(CavePillarFeature::accentChance),
			Codec.intRange(4, 128).fieldOf("search").forGetter(CavePillarFeature::search)
	).apply(i, CavePillarFeature::new));

	/** How many blocks from the floor and the ceiling the pillar flares, and by how much at most. */
	private static final int FLARE = 4;
	private static final double FLARE_WIDTH = 2.0;

	@Override
	public MapCodec<CavePillarFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		if (!open(level.getBlockState(origin))) {
			return false;
		}
		BlockPos.MutableBlockPos pos = origin.mutable();
		int up = 0;
		while (up < this.search && open(level.getBlockState(pos.move(0, 1, 0)))) {
			up++;
		}
		if (up >= this.search || !level.getBlockState(pos).isSolid()) {
			return false;
		}
		int ceiling = pos.getY();
		pos.set(origin);
		int down = 0;
		while (down < this.search && open(level.getBlockState(pos.move(0, -1, 0)))) {
			down++;
		}
		if (down >= this.search || !level.getBlockState(pos).isSolid()) {
			return false;
		}
		int floor = pos.getY();
		if (ceiling - floor < 4) {
			return false;
		}
		double radius = this.radius.sample(random) + random.nextDouble() * 0.5;
		double wobbleX = random.nextDouble() * 6.0;
		double wobbleZ = random.nextDouble() * 6.0;
		// One block into the floor and the ceiling, so it is anchored even where they are uneven.
		for (int y = floor; y <= ceiling; y++) {
			int fromEnd = Math.min(y - floor, ceiling - y);
			double r = radius + (fromEnd < FLARE ? FLARE_WIDTH * (1.0 - fromEnd / (double) FLARE) : 0.0);
			// A gentle lean so it is not a perfect cylinder.
			double cx = origin.getX() + 0.5 + Math.sin(y * 0.15 + wobbleX) * 0.6;
			double cz = origin.getZ() + 0.5 + Math.sin(y * 0.13 + wobbleZ) * 0.6;
			int reach = (int) Math.ceil(r) + 1;
			for (int dx = -reach; dx <= reach; dx++) {
				for (int dz = -reach; dz <= reach; dz++) {
					int x = origin.getX() + dx;
					int z = origin.getZ() + dz;
					double ox = x + 0.5 - cx;
					double oz = z + 0.5 - cz;
					if (ox * ox + oz * oz > r * r) {
						continue;
					}
					pos.set(x, y, z);
					BlockState there = level.getBlockState(pos);
					if (open(there) || there.is(BlockTags.BASE_STONE_OVERWORLD) && (y == floor || y == ceiling)) {
						level.setBlock(pos, random.nextFloat() < this.accentChance ? this.accent : this.block, 2);
					}
				}
			}
		}
		return true;
	}

	/** Air or a fluid the pillar may stand in. */
	private static boolean open(BlockState state) {
		return state.isAir() || !state.getFluidState().isEmpty() && !state.isSolid() || state.canBeReplaced();
	}
}
