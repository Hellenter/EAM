package io.github.hellenter.eam.world.structures.yagahut;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
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
 * Baba Yaga's hut: Maxim's template in the middle of every swampy forest. The ground within
 * {@link YagaHutLogic#FLAT_RADIUS} blocks of it is made level with the hut's floor (hills cut, dips filled, lakes
 * drained, trees cleared), sloping back to the land's own height over {@link YagaHutLogic#BLEND} more. It generates after
 * the trees, so each chunk clears what grew there and what neighbouring trees spill in.
 */
public class YagaHutStructure extends Structure {
	public static final MapCodec<YagaHutStructure> CODEC = simpleCodec(YagaHutStructure::new);

	/** How far the leaves of a tree outside the levelled ground may reach into it; each chunk clears this far past its edges. */
	private static final int LEAF_REACH = 12;
	private static final int FLAGS = Block.UPDATE_CLIENTS;

	public YagaHutStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		int[] spot = YagaHutPlacement.spot(context.seed(), context.randomState(), context.chunkPos());
		if (spot == null || !context.chunkPos().contains(new BlockPos(spot[0], 0, spot[1]))) return Optional.empty();
		Vec3i size = context.structureTemplateManager().getOrCreate(YagaHutModule.TEMPLATE).getSize();
		int[] tops = new int[25];
		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < 5; j++) {
				int x = spot[0] + (i - 2) * size.getX() / 5;
				int z = spot[1] + (j - 2) * size.getZ() / 5;
				tops[i * 5 + j] = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
						context.heightAccessor(), context.randomState()) - 1;
			}
		}
		int ground = YagaHutLogic.groundLevel(tops, context.chunkGenerator().getSeaLevel());
		BlockPos corner = new BlockPos(spot[0] - size.getX() / 2, ground, spot[1] - size.getZ() / 2);
		Rotation rotation = Rotation.getRandom(context.random());
		return Optional.of(new GenerationStub(new BlockPos(spot[0], ground, spot[1]),
				builder -> builder.addPiece(new YagaHutPiece(context.structureTemplateManager(), corner, rotation))));
	}

	/** Every chunk of the levelled ground, and those whose trees may reach into it, takes part. */
	@Override
	public BoundingBox adjustBoundingBox(BoundingBox box) {
		BlockPos middle = box.getCenter();
		int reach = YagaHutLogic.FLAT_RADIUS + YagaHutLogic.BLEND + LEAF_REACH;
		return new BoundingBox(middle.getX() - reach, box.minY(), middle.getZ() - reach, middle.getX() + reach, box.maxY(), middle.getZ() + reach);
	}

	@Override
	public void afterPlace(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox chunkBox, ChunkPos chunk, PiecesContainer pieces) {
		for (StructurePiece piece : pieces.pieces()) {
			if (piece instanceof YagaHutPiece hut) {
				level(level, generator, chunkBox, hut);
				clearTrees(level, chunkBox, hut);
			}
		}
	}

	/** Levels the columns of this chunk around the hut. */
	private static void level(WorldGenLevel level, ChunkGenerator generator, BoundingBox chunkBox, YagaHutPiece hut) {
		BlockPos middle = hut.middle();
		BoundingBox footprint = hut.templateBox();
		int ground = hut.groundY();
		int seaLevel = generator.getSeaLevel();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = chunkBox.minX(); x <= chunkBox.maxX(); x++) {
			for (int z = chunkBox.minZ(); z <= chunkBox.maxZ(); z++) {
				boolean inFootprint = x >= footprint.minX() && x <= footprint.maxX() && z >= footprint.minZ() && z <= footprint.maxZ();
				double distance = YagaHutLogic.distance(middle.getX(), middle.getZ(), x, z);
				if (inFootprint) {
					// The template already put its own ground and air here; earth under it, nothing over it.
					clearAbove(level, pos, x, z, footprint.maxY());
					fillBelow(level, pos, x, z, ground);
					continue;
				}
				if (!YagaHutLogic.inReach(distance)) continue;
				Column column = column(level, pos, x, z);
				int target = YagaHutLogic.targetTop(ground, column.groundTop, distance);
				if (column.wet && YagaHutLogic.keepsWater(distance, target, seaLevel)) continue;
				clearAbove(level, pos, x, z, target);
				pos.set(x, target, z);
				level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), FLAGS);
				fillBelow(level, pos, x, z, target);
			}
		}
	}

	/** A column's own ground: its top solid block under trees, plants and water, and whether water stands over it. */
	private record Column(int groundTop, boolean wet) {
	}

	private static Column column(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z) {
		boolean wet = false;
		for (int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1; y > level.getMinY(); y--) {
			pos.set(x, y, z);
			BlockState state = level.getBlockState(pos);
			if (!state.getFluidState().isEmpty()) {
				wet = true;
				if (!state.isSolid()) continue;
			}
			if (isGround(state)) return new Column(y, wet);
		}
		return new Column(level.getMinY(), wet);
	}

	/** Solid ground: not air, water, a plant, or part of a tree. */
	private static boolean isGround(BlockState state) {
		return !state.isAir() && state.getFluidState().isEmpty() && !state.is(BlockTags.REPLACEABLE) && !state.is(BlockTags.REPLACEABLE_BY_TREES)
				&& !isTreePart(state) && state.isSolid();
	}

	/** Removes everything over {@code top} in a column: hills, trees, plants and water. */
	private static void clearAbove(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z, int top) {
		for (int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1; y > top; y--) {
			pos.set(x, y, z);
			if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
		}
	}

	/** Fills earth under {@code top} down to the ground, through water and plants; grass found buried turns to dirt. */
	private static void fillBelow(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z, int top) {
		for (int y = top - 1; y >= top - YagaHutLogic.FILL_DEPTH && y > level.getMinY(); y--) {
			pos.set(x, y, z);
			BlockState state = level.getBlockState(pos);
			if (isGround(state)) {
				if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM) || state.is(Blocks.PODZOL)) {
					level.setBlock(pos, Blocks.DIRT.defaultBlockState(), FLAGS);
				}
				return;
			}
			level.setBlock(pos, Blocks.DIRT.defaultBlockState(), FLAGS);
		}
	}

	/**
	 * Removes trees reaching over the levelled ground from this chunk and a little past it: trees decorated later in a
	 * neighbouring chunk spill their leaves in.
	 */
	private static void clearTrees(WorldGenLevel level, BoundingBox chunkBox, YagaHutPiece hut) {
		BlockPos middle = hut.middle();
		BoundingBox footprint = hut.templateBox();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = chunkBox.minX() - LEAF_REACH; x <= chunkBox.maxX() + LEAF_REACH; x++) {
			for (int z = chunkBox.minZ() - LEAF_REACH; z <= chunkBox.maxZ() + LEAF_REACH; z++) {
				double distance = YagaHutLogic.distance(middle.getX(), middle.getZ(), x, z);
				if (distance > YagaHutLogic.FLAT_RADIUS + YagaHutLogic.BLEND / 2.0) continue;
				boolean inFootprint = x >= footprint.minX() && x <= footprint.maxX() && z >= footprint.minZ() && z <= footprint.maxZ();
				for (int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1; y > hut.groundY(); y--) {
					pos.set(x, y, z);
					BlockState state = level.getBlockState(pos);
					// Inside the hut's box logs stay: the hut is built of them, and trees from other chunks only reach in with leaves.
					if (isTreePart(state) && !(inFootprint && y <= footprint.maxY() && state.is(BlockTags.LOGS))) {
						level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
					} else if (!inFootprint && isGround(state)) {
						break;
					}
				}
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
		return YagaHutModule.HUT;
	}
}
