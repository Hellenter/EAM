package io.github.hellenter.eam.world.structures;

import io.github.hellenter.eam.world.structures.outpost.OutpostLogic;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Earthwork for a template that stands on the surface after the trees have grown: clearing the trees around it and
 * banking earth under and around it. Each chunk does its own part; the callers' structures reach far enough beyond the
 * footprint that every chunk with work in it takes part.
 */
public final class GroundWork {
	/**
	 * How far the leaves of a tree growing outside the clearing may reach into it. Each chunk clears this far beyond
	 * its edges, so trees grown in a neighbouring chunk after it are cleared by that chunk, leaves and all. A fancy
	 * oak's branches with their leaves reach about this far from its trunk.
	 */
	public static final int LEAF_REACH = 12;
	/** How far out the bank of earth around a raised template reaches; as far as the steepest site rises. */
	public static final int BANK = 10;
	/** How deep earth is filled under a raised template or its bank at most. */
	private static final int FOUNDATION_DEPTH = 16;

	private GroundWork() {
	}

	/**
	 * Where a template stands above the ground, fills earth under it down to the ground and banks earth around it,
	 * sloping one block down for each block out, so it neither floats nor sits on a cliff.
	 *
	 * @param grass        the level of the grass right beside the footprint
	 * @param grassUnder   whether the top of the fill under the footprint is grass, where the template leaves it open
	 */
	public static void bank(WorldGenLevel level, BoundingBox chunkBox, BoundingBox footprint, int grass, boolean grassUnder) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = Math.max(chunkBox.minX(), footprint.minX() - BANK); x <= Math.min(chunkBox.maxX(), footprint.maxX() + BANK); x++) {
			for (int z = Math.max(chunkBox.minZ(), footprint.minZ() - BANK); z <= Math.min(chunkBox.maxZ(), footprint.maxZ() + BANK); z++) {
				int distance = OutpostLogic.distance(footprint.minX(), footprint.minZ(), footprint.maxX(), footprint.maxZ(), x, z);
				if (distance > BANK) continue;
				int top = distance == 0 ? footprint.minY() - 1 : OutpostLogic.bankTop(grass, distance);
				boolean grassTop = distance > 0 || grassUnder;
				for (int y = top; y > top - FOUNDATION_DEPTH; y--) {
					pos.set(x, y, z);
					BlockState state = level.getBlockState(pos);
					// The bank stops at water: a lake beside the template keeps its shore.
					if (!state.getFluidState().isEmpty()) break;
					if (!state.isAir() && !state.is(BlockTags.REPLACEABLE)) {
						// Grass buried under the bank turns to dirt.
						if (y < top && state.is(Blocks.GRASS_BLOCK)) level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
						break;
					}
					BlockState fill = grassTop && y == top ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState();
					level.setBlock(pos, fill, Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	/**
	 * Clears every tree, its leaves and the forest's huge mushrooms within {@code clearRadius} blocks of the footprint,
	 * and the leaves that trees of neighbouring chunks put into the footprint, down to {@code ground}.
	 */
	public static void clearTrees(WorldGenLevel level, BoundingBox chunkBox, BoundingBox footprint, int ground, int clearRadius) {
		int fromX = Math.max(chunkBox.minX() - LEAF_REACH, footprint.minX() - clearRadius);
		int toX = Math.min(chunkBox.maxX() + LEAF_REACH, footprint.maxX() + clearRadius);
		int fromZ = Math.max(chunkBox.minZ() - LEAF_REACH, footprint.minZ() - clearRadius);
		int toZ = Math.min(chunkBox.maxZ() + LEAF_REACH, footprint.maxZ() + clearRadius);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = fromX; x <= toX; x++) {
			for (int z = fromZ; z <= toZ; z++) {
				if (!OutpostLogic.inClearing(footprint.minX(), footprint.minZ(), footprint.maxX(), footprint.maxZ(), clearRadius, x, z)) continue;
				boolean inFootprint = x >= footprint.minX() && x <= footprint.maxX() && z >= footprint.minZ() && z <= footprint.maxZ();
				if (inFootprint) clearFootprintColumn(level, pos, x, z, ground, footprint.maxY());
				else clearColumn(level, pos, x, z);
			}
		}
	}

	/** Removes the tree blocks of one column from the top down to the ground, which keeps grass where a trunk stood. */
	private static void clearColumn(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z) {
		boolean trunk = false;
		for (int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1; y >= level.getMinY(); y--) {
			pos.set(x, y, z);
			BlockState state = level.getBlockState(pos);
			if (isTreePart(state)) {
				trunk = state.is(BlockTags.LOGS);
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			} else if (state.isAir() || state.is(BlockTags.REPLACEABLE_BY_TREES)) {
				trunk = false;
			} else {
				if (trunk && state.is(Blocks.DIRT)) level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
				return;
			}
		}
	}

	/**
	 * Removes the tree blocks of one column over the template down to its ground. A tree grown in a neighbouring chunk
	 * after the template was placed puts its leaves into the template's air, beside walls and under roofs, so the whole
	 * height is searched. Logs and placed (persistent) leaves inside the template's box stay: the template is built of
	 * them, and trees from other chunks only reach in with their own leaves.
	 */
	private static void clearFootprintColumn(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z, int ground, int top) {
		for (int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1; y >= ground; y--) {
			pos.set(x, y, z);
			BlockState state = level.getBlockState(pos);
			if (isTreePart(state) && !(y <= top && (state.is(BlockTags.LOGS) || isPlacedLeaves(state)))) {
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
	}

	private static boolean isPlacedLeaves(BlockState state) {
		return state.is(BlockTags.LEAVES) && state.getValueOrElse(LeavesBlock.PERSISTENT, false);
	}

	private static boolean isTreePart(BlockState state) {
		return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(Blocks.VINE) || state.is(Blocks.BEE_NEST)
				|| state.is(Blocks.COCOA) || state.is(Blocks.BROWN_MUSHROOM_BLOCK) || state.is(Blocks.RED_MUSHROOM_BLOCK)
				|| state.is(Blocks.MUSHROOM_STEM);
	}
}
