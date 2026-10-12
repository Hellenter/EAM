package io.github.hellenter.eam.world.structures.outpost;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;

import java.util.Optional;

/**
 * Maxim's pillager outpost: one template set into the ground in the middle of its chunk, on dry land that is not too
 * steep. It generates after the trees, and clears every tree, its leaves and the forest's huge mushrooms within
 * {@code clearRadius} blocks of its footprint.
 */
public class OutpostStructure extends Structure {
	public static final MapCodec<OutpostStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			settingsCodec(instance),
			Codec.intRange(0, 24).optionalFieldOf("clear_radius", 15).forGetter(outpost -> outpost.clearRadius),
			Codec.intRange(0, 32).optionalFieldOf("max_slope", 10).forGetter(outpost -> outpost.maxSlope)
	).apply(instance, OutpostStructure::new));

	/**
	 * How far the leaves of a tree growing outside the clearing may reach into it. Each chunk clears this far beyond
	 * its edges, so trees grown in a neighbouring chunk after it are cleared by that chunk, leaves and all.
	 */
	private static final int LEAF_REACH = 8;

	private final int clearRadius;
	private final int maxSlope;

	public OutpostStructure(StructureSettings settings, int clearRadius, int maxSlope) {
		super(settings);
		this.clearRadius = clearRadius;
		this.maxSlope = maxSlope;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		var size = context.structureTemplateManager().getOrCreate(OutpostModule.TEMPLATE).getSize();
		ChunkPos chunk = context.chunkPos();
		int minX = chunk.getMiddleBlockX() - size.getX() / 2;
		int minZ = chunk.getMiddleBlockZ() - size.getZ() / 2;
		int lowest = Integer.MAX_VALUE;
		int highest = Integer.MIN_VALUE;
		for (int[] spot : new int[][] {{0, 0}, {size.getX() - 1, 0}, {0, size.getZ() - 1}, {size.getX() - 1, size.getZ() - 1}, {size.getX() / 2, size.getZ() / 2}}) {
			int x = minX + spot[0];
			int z = minZ + spot[1];
			int ground = surface(context, x, z, Heightmap.Types.WORLD_SURFACE_WG);
			// Dry land only: in a river or a lake the cellar would flood.
			if (surface(context, x, z, Heightmap.Types.OCEAN_FLOOR_WG) < ground) return Optional.empty();
			lowest = Math.min(lowest, ground);
			highest = Math.max(highest, ground);
		}
		if (highest - lowest > maxSlope) return Optional.empty();
		int ground = surface(context, minX + size.getX() / 2, minZ + size.getZ() / 2, Heightmap.Types.WORLD_SURFACE_WG);
		BlockPos corner = new BlockPos(minX, ground - OutpostPiece.GROUND, minZ);
		Rotation rotation = Rotation.getRandom(context.random());
		return Optional.of(new GenerationStub(new BlockPos(chunk.getMiddleBlockX(), ground, chunk.getMiddleBlockZ()),
				builder -> builder.addPiece(new OutpostPiece(context.structureTemplateManager(), corner, rotation))));
	}

	private static int surface(GenerationContext context, int x, int z, Heightmap.Types heightmap) {
		return context.chunkGenerator().getFirstOccupiedHeight(x, z, heightmap, context.heightAccessor(), context.randomState());
	}

	/** Every chunk the clearing and the leaves reaching into it touch takes part in placing the outpost. */
	@Override
	public BoundingBox adjustBoundingBox(BoundingBox box) {
		int reach = clearRadius + LEAF_REACH;
		return super.adjustBoundingBox(box).inflatedBy(reach, 0, reach);
	}

	@Override
	public void afterPlace(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox chunkBox, ChunkPos chunk, PiecesContainer pieces) {
		if (clearRadius <= 0) return;
		for (StructurePiece piece : pieces.pieces()) {
			if (piece instanceof OutpostPiece outpost) clearTrees(level, chunkBox, outpost.templateBox());
		}
	}

	private void clearTrees(WorldGenLevel level, BoundingBox chunkBox, BoundingBox footprint) {
		int fromX = Math.max(chunkBox.minX() - LEAF_REACH, footprint.minX() - clearRadius);
		int toX = Math.min(chunkBox.maxX() + LEAF_REACH, footprint.maxX() + clearRadius);
		int fromZ = Math.max(chunkBox.minZ() - LEAF_REACH, footprint.minZ() - clearRadius);
		int toZ = Math.min(chunkBox.maxZ() + LEAF_REACH, footprint.maxZ() + clearRadius);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = fromX; x <= toX; x++) {
			for (int z = fromZ; z <= toZ; z++) {
				if (!OutpostLogic.inClearing(footprint.minX(), footprint.minZ(), footprint.maxX(), footprint.maxZ(), clearRadius, x, z)) continue;
				boolean inFootprint = x >= footprint.minX() && x <= footprint.maxX() && z >= footprint.minZ() && z <= footprint.maxZ();
				clearColumn(level, pos, x, z, inFootprint, footprint.maxY());
			}
		}
	}

	/** Removes the tree blocks of one column from the top down to the ground, which keeps grass where a trunk stood. */
	private static void clearColumn(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z, boolean inFootprint, int templateTop) {
		boolean trunk = false;
		for (int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1; y >= level.getMinY(); y--) {
			if (!OutpostLogic.mayClear(inFootprint, y, templateTop)) return;
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

	private static boolean isTreePart(BlockState state) {
		return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(Blocks.VINE) || state.is(Blocks.BEE_NEST)
				|| state.is(Blocks.COCOA) || state.is(Blocks.BROWN_MUSHROOM_BLOCK) || state.is(Blocks.RED_MUSHROOM_BLOCK)
				|| state.is(Blocks.MUSHROOM_STEM);
	}

	@Override
	public StructureType<?> type() {
		return OutpostModule.OUTPOST;
	}
}
