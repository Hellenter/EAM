package io.github.hellenter.eam.mobs.entity.yaga.grave;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.world.swamp.SwampyForestBiomes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.storage.loot.LootTable;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Baba Yaga's grave: a low mound of rooted earth and moss in her swamp, a crooked dark oak cross at its head, black
 * candles burning on it and a fence of skulls on posts at its corners, as round her hut in the tales. Her chest lies
 * buried under the mound between two bone blocks. It is raised only when a player reads her map.
 */
public final class YagaGrave {
	/** The buried chest's loot. */
	public static final ResourceKey<LootTable> CHEST_LOOT = ResourceKey.create(Registries.LOOT_TABLE, Eam.id("chests/yaga_grave"));
	/** At most this many candidate spots are generated to be looked at closely, so reading the map stays quick. */
	private static final int MAX_CLOSE_LOOKS = 8;
	/** The mound and the cross fit in a square this far from the middle each way, whichever way the grave faces. */
	private static final int CORE = 2;
	private static final int FLAGS = Block.UPDATE_CLIENTS;

	private YagaGrave() {
	}

	/**
	 * Where the grave goes: flat dry ground in the swamp she died in, {@link YagaGraveLogic#NEAR_MIN}-{@link YagaGraveLogic#NEAR_MAX}
	 * blocks from {@code death}. Killed outside a swamp, the nearest swamp within {@link YagaGraveLogic#FAR_MAX}; with no
	 * swamp at all, any flat dry ground near {@code death}, and failing even that, the spot she died at. Returns the first
	 * air block above the ground.
	 */
	public static BlockPos findSite(ServerLevel level, BlockPos death, RandomSource random) {
		boolean inSwamp = isSwamp(level, death.getX(), death.getZ());
		List<int[]> near = YagaGraveLogic.candidates(YagaGraveLogic.NEAR_MIN, YagaGraveLogic.NEAR_MAX, random::nextDouble);
		Util.shuffle(near, random);
		BlockPos site = inSwamp ? search(level, death, near, true) : null;
		if (site == null && !inSwamp) {
			site = search(level, death, YagaGraveLogic.candidates(YagaGraveLogic.RING_STEP, YagaGraveLogic.FAR_MAX, random::nextDouble), true);
		}
		if (site == null) {
			site = search(level, death, near, false);
		}
		return site != null ? site : surface(level, death.getX(), death.getZ());
	}

	/**
	 * The first candidate that is dry land (in the swamp, if {@code swampOnly}) and flat and clear of trees. The terrain
	 * is first judged from the world's noise, which generates nothing; only the few spots that pass are generated and
	 * looked at closely.
	 */
	private static @Nullable BlockPos search(ServerLevel level, BlockPos death, List<int[]> offsets, boolean swampOnly) {
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		RandomState noise = level.getChunkSource().randomState();
		int looks = 0;
		for (int[] offset : offsets) {
			int x = death.getX() + offset[0];
			int z = death.getZ() + offset[1];
			// The biome by the climate alone comes cheapest, then the height of the land by the noise and the biome there.
			if (swampOnly && !isSwamp(level, x, z)) {
				continue;
			}
			int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, noise);
			if (floor != generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, noise)
					|| swampOnly && !level.getBiome(new BlockPos(x, floor, z)).is(SwampyForestBiomes.IS_SWAMPY_FOREST)) {
				continue;
			}
			BlockPos ground = flatSpotNear(level, x, z, swampOnly);
			if (ground != null) {
				return ground;
			}
			if (++looks >= MAX_CLOSE_LOOKS) {
				return null;
			}
		}
		return null;
	}

	/**
	 * The fitting spot nearest to {@code x, z} within its chunk (in the swamp, if {@code swampOnly}), keeping the grave's
	 * core inside that chunk so that no other chunk is generated for it.
	 */
	private static @Nullable BlockPos flatSpotNear(ServerLevel level, int x, int z, boolean swampOnly) {
		int chunkX = x & ~15;
		int chunkZ = z & ~15;
		BlockPos best = null;
		int bestDistance = Integer.MAX_VALUE;
		for (int px = chunkX + CORE; px <= chunkX + 15 - CORE; px++) {
			for (int pz = chunkZ + CORE; pz <= chunkZ + 15 - CORE; pz++) {
				int distance = (px - x) * (px - x) + (pz - z) * (pz - z);
				if (distance < bestDistance) {
					BlockPos ground = surface(level, px, pz);
					if (fits(level, ground) && (!swampOnly || level.getBiome(ground).is(SwampyForestBiomes.IS_SWAMPY_FOREST))) {
						best = ground;
						bestDistance = distance;
					}
				}
			}
		}
		return best;
	}

	/** Whether the swamp is here, judged just over sea level where its land lies. */
	private static boolean isSwamp(ServerLevel level, int x, int z) {
		return level.getBiome(new BlockPos(x, level.getSeaLevel() + 2, z)).is(SwampyForestBiomes.IS_SWAMPY_FOREST);
	}

	/** The first block above the ground (leaves left out), generating the chunk if it has to. */
	private static BlockPos surface(ServerLevel level, int x, int z) {
		return new BlockPos(x, height(level, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
	}

	/** The first free block of a heightmap, generating the chunk if it has to. */
	private static int height(ServerLevel level, Heightmap.Types type, int x, int z) {
		return level.getChunk(x >> 4, z >> 4).getHeight(type, x & 15, z & 15) + 1;
	}

	/**
	 * Earth with no water over it, flat to a block under the mound and the cross, and no tree trunk there. The corner
	 * posts make do with what they find: one that would stand in a trunk is left out.
	 */
	private static boolean fits(ServerLevel level, BlockPos base) {
		if (!level.getBlockState(base.below()).is(BlockTags.SUBSTRATE_OVERWORLD)) {
			return false;
		}
		for (int dx = -CORE; dx <= CORE; dx++) {
			for (int dz = -CORE; dz <= CORE; dz++) {
				int x = base.getX() + dx;
				int z = base.getZ() + dz;
				int top = height(level, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
				if (Math.abs(top - base.getY()) > 1 || !level.getFluidState(new BlockPos(x, top - 1, z)).isEmpty()) {
					return false;
				}
			}
		}
		return true;
	}

	/** Raises the grave on the ground at {@code base} (the first air block), its head towards {@code head}. */
	public static void build(ServerLevel level, BlockPos base, Direction head, RandomSource random) {
		Direction side = head.getClockWise();
		// Plants and grass in the way go; trees and water stay.
		for (int across = -2; across <= 2; across++) {
			for (int along = -3; along <= 3; along++) {
				for (int up = 0; up <= 3; up++) {
					BlockPos pos = at(base, head, side, across, along, up);
					BlockState state = level.getBlockState(pos);
					if (!state.isAir() && state.canBeReplaced() && state.getFluidState().isEmpty()) {
						level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
					}
				}
			}
		}
		// Dug-over earth under and round the mound.
		for (int across = -1; across <= 1; across++) {
			for (int along = -2; along <= 2; along++) {
				level.setBlock(at(base, head, side, across, along, -1), earth(random), FLAGS);
			}
		}
		// The mound: a ridge of earth along the middle, moss on its sides and patches of it on top.
		for (int along = -1; along <= 1; along++) {
			level.setBlock(at(base, head, side, 0, along, 0), earth(random), FLAGS);
			for (int across : new int[] {-1, 1}) {
				level.setBlock(at(base, head, side, across, along, 0), Blocks.MOSS_CARPET.defaultBlockState(), FLAGS);
			}
		}
		if (random.nextBoolean()) {
			level.setBlock(at(base, head, side, 0, 0, 1), Blocks.MOSS_CARPET.defaultBlockState(), FLAGS);
		}
		// Black candles at the foot of the mound, and one at its head.
		level.setBlock(at(base, head, side, 0, -1, 1), Blocks.DYED_CANDLE.pick(DyeColor.BLACK).defaultBlockState()
				.setValue(CandleBlock.CANDLES, 1).setValue(CandleBlock.LIT, true), FLAGS);
		level.setBlock(at(base, head, side, 0, 1, 1), Blocks.DYED_CANDLE.pick(DyeColor.BLACK).defaultBlockState()
				.setValue(CandleBlock.CANDLES, 2 + random.nextInt(2)).setValue(CandleBlock.LIT, true), FLAGS);

		// The cross at the head: a post with a crossbar.
		BlockPos foot = at(base, head, side, 0, -2, 0);
		level.setBlock(foot, Blocks.DARK_OAK_FENCE.defaultBlockState(), FLAGS);
		level.setBlock(foot.above(), fence(Blocks.DARK_OAK_FENCE, side, true, true), FLAGS);
		level.setBlock(foot.above(2), Blocks.DARK_OAK_FENCE.defaultBlockState(), FLAGS);
		level.setBlock(foot.above().relative(side), fence(Blocks.DARK_OAK_FENCE, side, false, true), FLAGS);
		level.setBlock(foot.above().relative(side.getOpposite()), fence(Blocks.DARK_OAK_FENCE, side, true, false), FLAGS);

		// Skulls on posts at the four corners, each turned its own way, on the ground where it stands; none in a tree or water.
		for (int across : new int[] {-2, 2}) {
			for (int along : new int[] {-3, 3}) {
				BlockPos corner = at(base, head, side, across, along, 0);
				BlockPos post = surface(level, corner.getX(), corner.getZ());
				if (Math.abs(post.getY() - base.getY()) <= 1 && free(level, post) && free(level, post.above())
						&& level.getFluidState(post.below()).isEmpty()) {
					level.setBlock(post, Blocks.SPRUCE_FENCE.defaultBlockState(), FLAGS);
					level.setBlock(post.above(), Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, random.nextInt(16)), FLAGS);
				}
			}
		}

		// Her chest, buried under the mound between bones.
		BlockPos chest = at(base, head, side, 0, 0, -3);
		level.setBlock(chest.above(), Blocks.ROOTED_DIRT.defaultBlockState(), FLAGS);
		level.setBlock(chest.below(), Blocks.COARSE_DIRT.defaultBlockState(), FLAGS);
		BlockState bones = Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, side.getAxis());
		level.setBlock(chest.relative(side), bones, FLAGS);
		level.setBlock(chest.relative(side.getOpposite()), bones, FLAGS);
		level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, head), FLAGS);
		RandomizableContainer.setBlockEntityLootTable(level, random, chest, CHEST_LOOT);
	}

	/** {@code base} moved {@code across} to the side, {@code along} towards the foot (the head is negative) and {@code up}. */
	private static BlockPos at(BlockPos base, Direction head, Direction side, int across, int along, int up) {
		return base.relative(side, across).relative(head.getOpposite(), along).above(up);
	}

	private static boolean free(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.isAir() || state.canBeReplaced() && state.getFluidState().isEmpty();
	}

	private static BlockState earth(RandomSource random) {
		return switch (random.nextInt(5)) {
			case 0, 1 -> Blocks.ROOTED_DIRT.defaultBlockState();
			case 2, 3 -> Blocks.COARSE_DIRT.defaultBlockState();
			default -> Blocks.MUD.defaultBlockState();
		};
	}

	/** A fence piece joined towards {@code side} and/or away from it. */
	private static BlockState fence(Block fence, Direction side, boolean towards, boolean away) {
		return fence.defaultBlockState()
				.setValue(CrossCollisionBlock.PROPERTY_BY_DIRECTION.get(side), towards)
				.setValue(CrossCollisionBlock.PROPERTY_BY_DIRECTION.get(side.getOpposite()), away);
	}
}
