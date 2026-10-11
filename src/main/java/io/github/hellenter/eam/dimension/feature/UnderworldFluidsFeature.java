package io.github.hellenter.eam.dimension.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.Fluids;

/**
 * Turns the underground lakes of a whole chunk into what their biome holds. The terrain generator fills every lake with
 * water; in {@code lavaBiomes} it becomes lava, with magma in the rock right around it, and in {@code frozenBiomes} the
 * top of the water freezes, mostly to ice, some of it packed or blue. Place it once per chunk, with no placement.
 *
 * @param magmaChance chance that a stone block touching a lava lake turns to magma
 * @param packedIceChance chance that a frozen surface block is packed ice instead of ice
 * @param blueIceChance chance that it is blue ice
 */
public record UnderworldFluidsFeature(TagKey<Biome> lavaBiomes, TagKey<Biome> frozenBiomes, float magmaChance, float packedIceChance,
		float blueIceChance) implements Feature {
	public static final MapCodec<UnderworldFluidsFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			TagKey.codec(Registries.BIOME).fieldOf("lava_biomes").forGetter(UnderworldFluidsFeature::lavaBiomes),
			TagKey.codec(Registries.BIOME).fieldOf("frozen_biomes").forGetter(UnderworldFluidsFeature::frozenBiomes),
			Codec.floatRange(0.0F, 1.0F).fieldOf("magma_chance").forGetter(UnderworldFluidsFeature::magmaChance),
			Codec.floatRange(0.0F, 1.0F).fieldOf("packed_ice_chance").forGetter(UnderworldFluidsFeature::packedIceChance),
			Codec.floatRange(0.0F, 1.0F).fieldOf("blue_ice_chance").forGetter(UnderworldFluidsFeature::blueIceChance)
	).apply(i, UnderworldFluidsFeature::new));

	private static final int HOT = 1;
	private static final int COLD = 2;

	@Override
	public MapCodec<UnderworldFluidsFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		int minX = origin.getX() & ~15;
		int minZ = origin.getZ() & ~15;
		int minY = level.getMinY();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos near = new BlockPos.MutableBlockPos();
		boolean changed = false;
		for (int dx = 0; dx < 16; dx++) {
			for (int dz = 0; dz < 16; dz++) {
				int x = minX + dx;
				int z = minZ + dz;
				int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
				int lastQuart = Integer.MIN_VALUE;
				int kind = 0;
				for (int y = minY; y < top; y++) {
					pos.set(x, y, z);
					BlockState state = level.getBlockState(pos);
					if (!state.is(Blocks.WATER)) {
						continue;
					}
					int quart = QuartPos.fromBlock(y);
					if (quart != lastQuart) {
						lastQuart = quart;
						Holder<Biome> biome = level.getBiome(pos);
						kind = biome.is(this.lavaBiomes) ? HOT : biome.is(this.frozenBiomes) ? COLD : 0;
					}
					if (kind == HOT) {
						level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 2);
						scorch(level, random, pos, near, minX, minZ);
						changed = true;
					} else if (kind == COLD && level.getBlockState(near.setWithOffset(pos, Direction.UP)).isAir()) {
						level.setBlock(pos, ice(random), 2);
						changed = true;
					}
				}
			}
		}
		return changed;
	}

	/** The rock around a lava block, inside this chunk, turns to magma here and there; lava next to air is set flowing. */
	private void scorch(WorldGenLevel level, RandomSource random, BlockPos lava, BlockPos.MutableBlockPos near, int minX, int minZ) {
		boolean open = false;
		for (Direction direction : Direction.values()) {
			if (direction == Direction.UP) {
				continue;
			}
			near.setWithOffset(lava, direction);
			if (near.getX() < minX || near.getX() > minX + 15 || near.getZ() < minZ || near.getZ() > minZ + 15) {
				continue;
			}
			BlockState there = level.getBlockState(near);
			if (there.isAir()) {
				open = true;
			} else if (there.is(BlockTags.BASE_STONE_OVERWORLD) && random.nextFloat() < this.magmaChance) {
				level.setBlock(near, Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
			}
		}
		if (open) {
			level.scheduleTick(lava, Fluids.LAVA, 1);
		}
	}

	private BlockState ice(RandomSource random) {
		float roll = random.nextFloat();
		if (roll < this.blueIceChance) {
			return Blocks.BLUE_ICE.defaultBlockState();
		}
		if (roll < this.blueIceChance + this.packedIceChance) {
			return Blocks.PACKED_ICE.defaultBlockState();
		}
		return Blocks.ICE.defaultBlockState();
	}
}
