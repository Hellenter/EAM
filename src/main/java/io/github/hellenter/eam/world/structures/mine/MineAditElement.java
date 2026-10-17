package io.github.hellenter.eam.world.structures.mine;

import io.github.hellenter.eam.content.EamBlocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * The adit that leads from a dead end of an underworld mine out to the cave floor ({@link UnderworldMineLogic.Adit}):
 * dug three wide and three high, a poplar stair per block of rise, planks where it crosses open air and a timber frame
 * every few cells under rock. An adit that comes out high in a cave wall ends in a small lattice tower with a ladder in
 * the middle down to the cave floor. It is a pool element so that it is saved with the mine, and it uses the terrain
 * matching projection so the terrain does not close in around it the way it does around the mine's pieces.
 */
public class MineAditElement extends StructurePoolElement {
	public static final MapCodec<MineAditElement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			BlockPos.CODEC.fieldOf("start").forGetter(element -> new BlockPos(element.adit.startX(), element.adit.startFloor(), element.adit.startZ())),
			Direction.CODEC.fieldOf("direction").forGetter(MineAditElement::direction),
			Codec.INT.fieldOf("end_floor").forGetter(element -> element.adit.endFloor()),
			Codec.INT.fieldOf("length").forGetter(element -> element.adit.length()),
			Codec.INT.optionalFieldOf("tower_bottom", UnderworldMineLogic.NO_TOWER).forGetter(element -> element.adit.towerBottom()),
			projectionCodec()
	).apply(instance, (start, direction, endFloor, length, towerBottom, projection) -> new MineAditElement(new UnderworldMineLogic.Adit(
			start.getX(), start.getY(), start.getZ(), direction.getStepX(), direction.getStepZ(), endFloor, length, towerBottom),
			projection)));

	private final UnderworldMineLogic.Adit adit;

	public MineAditElement(UnderworldMineLogic.Adit adit, StructureTemplatePool.Projection projection) {
		super(projection);
		this.adit = adit;
	}

	public MineAditElement(UnderworldMineLogic.Adit adit) {
		this(adit, StructureTemplatePool.Projection.TERRAIN_MATCHING);
	}

	private Direction direction() {
		return Direction.getNearest(adit.stepX(), 0, adit.stepZ(), Direction.NORTH);
	}

	/**
	 * Every block the adit touches: its cells side to side, from the lowest floor (or the foot of the tower) to the top
	 * of the highest frame.
	 */
	public BoundingBox box() {
		int minY = adit.hasTower() ? adit.towerBottom() : Math.min(adit.startFloor(), adit.endFloor());
		int maxY = Math.max(adit.startFloor(), adit.endFloor()) + UnderworldMineLogic.HEIGHT;
		int last = adit.hasTower() ? adit.length() + UnderworldMineLogic.TOWER_SIZE : adit.length();
		int x0 = adit.x(0, -1);
		int z0 = adit.z(0, -1);
		int x1 = adit.x(last, 1);
		int z1 = adit.z(last, 1);
		return new BoundingBox(Math.min(x0, x1), minY, Math.min(z0, z1), Math.max(x0, x1), maxY, Math.max(z0, z1));
	}

	@Override
	public Vec3i getSize(StructureTemplateManager structureTemplateManager, Rotation rotation) {
		return box().getLength();
	}

	@Override
	public List<StructureTemplate.JigsawBlockInfo> getShuffledJigsawBlocks(StructureTemplateManager structureTemplateManager,
			BlockPos position, Rotation rotation, RandomSource random) {
		return List.of();
	}

	@Override
	public BoundingBox getBoundingBox(StructureTemplateManager structureTemplateManager, BlockPos position, Rotation rotation) {
		return box();
	}

	@Override
	public boolean place(StructureTemplateManager structureTemplateManager, WorldGenLevel level, StructureManager structureManager,
			ChunkGenerator generator, BlockPos position, BlockPos referencePos, Rotation rotation, BoundingBox chunkBB,
			RandomSource random, LiquidSettings liquidSettings, boolean keepJigsaws) {
		Direction along = direction();
		Direction.Axis across = along.getClockWise().getAxis();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int i = 0; i <= adit.length(); i++) {
			if (!touches(chunkBB, i)) {
				continue;
			}
			int floor = adit.floor(i);
			int stair = UnderworldMineLogic.stair(adit, i);
			boolean frame = UnderworldMineLogic.frame(adit, i)
					&& level.getBlockState(pos.set(adit.x(i, 0), floor + UnderworldMineLogic.HEIGHT + 1, adit.z(i, 0))).isSolid();
			for (int side = -1; side <= 1; side++) {
				int x = adit.x(i, side);
				int z = adit.z(i, side);
				if (stair != 0) {
					set(level, chunkBB, pos.set(x, floor, z), Blocks.POPLAR_STAIRS.defaultBlockState()
							.setValue(StairBlock.FACING, stair > 0 ? along : along.getOpposite()));
				} else if (chunkBB.isInside(pos.set(x, floor, z)) && !level.getBlockState(pos).isSolid()) {
					set(level, chunkBB, pos, Blocks.POPLAR_PLANKS.defaultBlockState());
				}
				Direction out = side > 0 ? along.getClockWise() : along.getCounterClockWise();
				for (int h = 1; h <= UnderworldMineLogic.HEIGHT; h++) {
					BlockState state = Blocks.AIR.defaultBlockState();
					if (frame && h == UnderworldMineLogic.HEIGHT) {
						state = log(across);
					} else if (frame && side != 0) {
						pos.set(x, floor + h, z);
						state = fence(level.getBlockState(pos.relative(out)).isSolid() ? out : null, null);
					}
					set(level, chunkBB, pos.set(x, floor + h, z), state);
				}
			}
		}
		if (adit.hasTower()) {
			placeTower(level, chunkBB, along);
		}
		return true;
	}

	/**
	 * The tower at the adit's mouth, three by three: stripped poplar posts in the corners, a post in the middle of the
	 * far side with the ladder on it in the very middle, rails between the posts at the top and a ring of beams every
	 * four blocks down to the cave floor. The near side stays open at the bottom and at the top, where the ladder is
	 * entered. Every column is read down from the top and stops on the first solid block, so it stands on whatever the
	 * cave floor became.
	 */
	private void placeTower(WorldGenLevel level, BoundingBox chunkBB, Direction along) {
		int top = adit.startFloor();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int k = 0; k < UnderworldMineLogic.TOWER_SIZE; k++) {
			int i = adit.length() + 1 + k;
			for (int side = -1; side <= 1; side++) {
				int x = adit.x(i, side);
				int z = adit.z(i, side);
				if (x < chunkBB.minX() || x > chunkBB.maxX() || z < chunkBB.minZ() || z > chunkBB.maxZ()) {
					continue;
				}
				int ground = adit.towerBottom() - 1;
				for (int y = top - 1; y >= adit.towerBottom(); y--) {
					if (level.getBlockState(pos.set(x, y, z)).isSolid()) {
						ground = y;
						break;
					}
				}
				for (int y = ground + 1; y <= top + 2; y++) {
					BlockState state = towerBlock(k, side, y - top, y - ground, along);
					if (state != null) {
						set(level, chunkBB, pos.set(x, y, z), state);
					}
				}
			}
		}
	}

	/**
	 * The tower's block in row {@code k} (0 nearest the adit), at {@code side}, {@code dy} blocks above the adit's floor
	 * and {@code above} blocks above the cave floor; {@code null} leaves the cave as it is.
	 */
	private static BlockState towerBlock(int k, int side, int dy, int above, Direction along) {
		Direction.Axis alongAxis = along.getAxis();
		Direction.Axis acrossAxis = along.getClockWise().getAxis();
		boolean corner = k != 1 && side != 0;
		boolean ring = dy < 0 && -dy % 4 == 0;
		if (dy == 2) {
			return k == 2 && side == 0 ? Blocks.LANTERN.defaultBlockState() : null;
		}
		if (corner) {
			return log(Direction.Axis.Y);
		}
		if (k == 2) {
			// The post the ladder hangs on, up to the rail.
			return log(Direction.Axis.Y);
		}
		if (k == 1 && side == 0) {
			return dy <= 0 ? Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, along.getOpposite()) : null;
		}
		if (dy == 0) {
			return Blocks.POPLAR_PLANKS.defaultBlockState();
		}
		if (k == 1) {
			// Between the near and far corners on either side: a rail on top, beams and fence below.
			return ring ? log(alongAxis) : fence(along, along.getOpposite());
		}
		// The near side's middle: open at the top and at the foot, where the ladder is reached.
		return ring && above > 3 ? log(acrossAxis) : null;
	}

	/** Whether any block of cell {@code i} lies in the chunk being placed; only then is the terrain around it read. */
	private boolean touches(BoundingBox chunkBB, int i) {
		for (int side = -1; side <= 1; side++) {
			int x = adit.x(i, side);
			int z = adit.z(i, side);
			if (x >= chunkBB.minX() && x <= chunkBB.maxX() && z >= chunkBB.minZ() && z <= chunkBB.maxZ()) {
				return true;
			}
		}
		return false;
	}

	/** A stripped poplar fence post, its rails reaching towards {@code a} and {@code b} (either may be {@code null}). */
	private static BlockState fence(Direction a, Direction b) {
		BlockState state = EamBlocks.STRIPPED_POPLAR_FENCE.defaultBlockState();
		for (Direction direction : new Direction[] {a, b}) {
			if (direction != null) {
				state = state.setValue(CrossCollisionBlock.PROPERTY_BY_DIRECTION.get(direction), true);
			}
		}
		return state;
	}

	private static BlockState log(Direction.Axis axis) {
		return Blocks.STRIPPED_POPLAR_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
	}

	private static void set(WorldGenLevel level, BoundingBox chunkBB, BlockPos pos, BlockState state) {
		if (chunkBB.isInside(pos) && !level.getBlockState(pos).is(BlockTags.FEATURES_CANNOT_REPLACE)) {
			level.setBlock(pos, state, Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public StructurePoolElementType<?> getType() {
		return UnderworldMineModule.ADIT;
	}

	@Override
	public String toString() {
		return "MineAdit[" + adit + "]";
	}
}
