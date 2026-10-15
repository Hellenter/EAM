package io.github.hellenter.eam.world.structures.mine;

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
 * every few cells under rock. It is a pool element so that it is saved with the mine, and it uses the terrain matching
 * projection so the terrain does not close in around it the way it does around the mine's pieces.
 */
public class MineAditElement extends StructurePoolElement {
	public static final MapCodec<MineAditElement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			BlockPos.CODEC.fieldOf("start").forGetter(element -> new BlockPos(element.adit.startX(), element.adit.startFloor(), element.adit.startZ())),
			Direction.CODEC.fieldOf("direction").forGetter(MineAditElement::direction),
			Codec.INT.fieldOf("end_floor").forGetter(element -> element.adit.endFloor()),
			Codec.INT.fieldOf("length").forGetter(element -> element.adit.length()),
			projectionCodec()
	).apply(instance, (start, direction, endFloor, length, projection) -> new MineAditElement(new UnderworldMineLogic.Adit(
			start.getX(), start.getY(), start.getZ(), direction.getStepX(), direction.getStepZ(), endFloor, length), projection)));

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

	/** Every block the adit touches: its cells side to side, from the lowest floor to the top of the highest frame. */
	public BoundingBox box() {
		int minY = Math.min(adit.startFloor(), adit.endFloor());
		int maxY = Math.max(adit.startFloor(), adit.endFloor()) + UnderworldMineLogic.HEIGHT;
		int x0 = adit.x(0, -1);
		int z0 = adit.z(0, -1);
		int x1 = adit.x(adit.length(), 1);
		int z1 = adit.z(adit.length(), 1);
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
				for (int h = 1; h <= UnderworldMineLogic.HEIGHT; h++) {
					BlockState state = Blocks.AIR.defaultBlockState();
					if (frame && h == UnderworldMineLogic.HEIGHT) {
						state = log(across);
					} else if (frame && side != 0) {
						state = log(Direction.Axis.Y);
					}
					set(level, chunkBB, pos.set(x, floor + h, z), state);
				}
			}
		}
		return true;
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
