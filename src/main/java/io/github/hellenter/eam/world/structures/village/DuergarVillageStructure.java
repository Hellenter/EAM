package io.github.hellenter.eam.world.structures.village;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntUnaryOperator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * A duergar village: a jigsaw village that stands on the floor of a cave instead of at a random height. It looks for a
 * wide enough stretch of floor near its chunk, builds the village there, then sets every piece down onto the floor
 * under it, at most {@code max_step} blocks above or below the piece it joins, so the streets climb and fall with the
 * cave like a surface village's paths. Where the floor rises faster than that, the street stops at the foot of the rise
 * rather than tunnel on under it. A site where too many pieces would hang over open air is given up; a smaller gap under
 * a piece is filled with a footing once it is built, and water that would run out of a pond or moat is filled in too.
 */
public class DuergarVillageStructure extends Structure {
	public static final MapCodec<DuergarVillageStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			settingsCodec(instance),
			StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(village -> village.startPool),
			Codec.intRange(0, 20).fieldOf("size").forGetter(village -> village.size),
			Codec.INT.fieldOf("floor_min_y").forGetter(village -> village.floorMinY),
			Codec.INT.fieldOf("floor_max_y").forGetter(village -> village.floorMaxY),
			Codec.intRange(16, 100).optionalFieldOf("max_distance", 56).forGetter(village -> village.maxDistance),
			Codec.intRange(0, 4).optionalFieldOf("max_step", 1).forGetter(village -> village.maxStep),
			Codec.intRange(4, 32).optionalFieldOf("site_radius", 14).forGetter(village -> village.siteRadius),
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("max_hanging", 0.15F).forGetter(village -> village.maxHanging),
			BlockState.CODEC.fieldOf("foundation").forGetter(village -> village.foundation),
			Codec.intRange(0, 16).optionalFieldOf("foundation_depth", 6).forGetter(village -> village.foundationDepth)
	).apply(instance, DuergarVillageStructure::new));

	/** How far above or below its expected height a piece looks for the floor. */
	private static final int SEARCH = 6;
	/** How many sites are tried; the one that keeps the most pieces is built. */
	private static final int TRIES = 3;
	/** How far apart the columns tried as the village's centre are. */
	private static final int GRID = 8;

	private final Holder<StructureTemplatePool> startPool;
	private final int size;
	private final int floorMinY;
	private final int floorMaxY;
	private final int maxDistance;
	private final int maxStep;
	private final int siteRadius;
	private final float maxHanging;
	private final BlockState foundation;
	private final int foundationDepth;

	public DuergarVillageStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int size, int floorMinY,
			int floorMaxY, int maxDistance, int maxStep, int siteRadius, float maxHanging, BlockState foundation, int foundationDepth) {
		super(settings);
		this.startPool = startPool;
		this.size = size;
		this.floorMinY = floorMinY;
		this.floorMaxY = floorMaxY;
		this.maxDistance = maxDistance;
		this.maxStep = maxStep;
		this.siteRadius = siteRadius;
		this.maxHanging = maxHanging;
		this.foundation = foundation;
		this.foundationDepth = foundationDepth;
	}

	@Override
	public StructureType<?> type() {
		return DuergarVillageModule.TYPE;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		Columns columns = new Columns(context);
		List<BlockPos> sites = sites(context, columns);
		RandomSource random = context.random();
		Settled best = null;
		BlockPos bestPosition = null;
		for (int i = 0; i < TRIES && !sites.isEmpty(); i++) {
			BlockPos site = sites.remove(random.nextInt(Math.min(sites.size(), 4)));
			Optional<GenerationStub> stub = JigsawPlacement.addPieces(context, startPool, Optional.empty(), size, site, false,
					Optional.empty(), new JigsawStructure.MaxDistance(maxDistance, 24), PoolAliasLookup.EMPTY,
					DimensionPadding.ZERO, LiquidSettings.IGNORE_WATERLOGGING);
			if (stub.isEmpty()) {
				continue;
			}
			Settled village = settle(stub.get().getPiecesBuilder().build().pieces(), columns, context.structureTemplateManager());
			if (village != null && (best == null || village.pieces().size() > best.pieces().size())) {
				best = village;
				bestPosition = stub.get().position();
			}
		}
		if (best == null) {
			return Optional.empty();
		}
		StructurePiecesBuilder settled = new StructurePiecesBuilder();
		best.pieces().forEach(settled::addPiece);
		return Optional.of(new GenerationStub(bestPosition.above(best.startMove()), Either.right(settled)));
	}

	/**
	 * Fills the air under each piece's floor down to the ground with {@code foundation}, at most {@code foundation_depth}
	 * blocks, like a woodland mansion's footing. The floor is set from the bare terrain a step at a time, so a street can
	 * end up a block or two above the ground; this closes that gap, under the water of a pond or moat too. A drop deeper
	 * than the footing is left open; the site search keeps it rare. Then the village's own water is kept in: a pond or
	 * moat block that would run out, over open ground beside it or down into a lower piece, is filled with the footing.
	 */
	@Override
	public void afterPlace(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
			BoundingBox chunkBox, ChunkPos chunkPos, PiecesContainer pieces) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (StructurePiece piece : pieces.pieces()) {
			BoundingBox box = piece.getBoundingBox();
			int floor = box.minY();
			for (int x = Math.max(box.minX(), chunkBox.minX()); x <= Math.min(box.maxX(), chunkBox.maxX()); x++) {
				for (int z = Math.max(box.minZ(), chunkBox.minZ()); z <= Math.min(box.maxZ(), chunkBox.maxZ()); z++) {
					if (level.getBlockState(pos.set(x, floor, z)).isAir()) {
						continue;
					}
					int gap = 0;
					while (gap < foundationDepth && level.getBlockState(pos.set(x, floor - gap - 1, z)).isAir()) {
						gap++;
					}
					if (gap == 0 || gap == foundationDepth && level.getBlockState(pos.set(x, floor - gap - 1, z)).isAir()) {
						continue;
					}
					for (int y = floor - gap; y < floor; y++) {
						level.setBlock(pos.set(x, y, z), foundation, Block.UPDATE_CLIENTS);
					}
				}
			}
		}
		// A piece in a neighbouring chunk may have been built after this chunk's water was checked and opened a way out
		// for it, so the water along this chunk's edges is looked at again.
		BoundingBox around = chunkBox.inflatedBy(1, 0, 1);
		BlockPos.MutableBlockPos near = new BlockPos.MutableBlockPos();
		for (StructurePiece piece : pieces.pieces()) {
			BoundingBox box = piece.getBoundingBox();
			int floor = box.minY();
			for (int x = Math.max(box.minX(), around.minX()); x <= Math.min(box.maxX(), around.maxX()); x++) {
				for (int z = Math.max(box.minZ(), around.minZ()); z <= Math.min(box.maxZ(), around.maxZ()); z++) {
					BlockState state = level.getBlockState(pos.set(x, floor, z));
					if (state.getFluidState().isSource() && state.getFluidState().is(FluidTags.WATER)
							&& leaks(level, pos, near)) {
						level.setBlock(pos, foundation, Block.UPDATE_CLIENTS);
					}
				}
			}
		}
	}

	/** Whether the water at {@code pos} could run off: down, or sideways into a block that is neither water nor a wall. */
	private static boolean leaks(WorldGenLevel level, BlockPos pos, BlockPos.MutableBlockPos near) {
		for (Direction direction : Direction.values()) {
			if (direction == Direction.UP) {
				continue;
			}
			BlockState there = level.getBlockState(near.setWithOffset(pos, direction));
			FluidState fluid = there.getFluidState();
			if (fluid.is(FluidTags.WATER) ? !fluid.isSource() : !there.getCollisionShape(level, near).equals(Shapes.block())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Floors around the chunk wide enough for a plaza: most columns on a ring around them have a floor within a few
	 * blocks of the same height. Best first.
	 */
	private List<BlockPos> sites(GenerationContext context, Columns columns) {
		int x0 = context.chunkPos().getMinBlockX();
		int z0 = context.chunkPos().getMinBlockZ();
		List<BlockPos> sites = new ArrayList<>();
		List<Integer> scores = new ArrayList<>();
		for (int dx = -16; dx < 32; dx += GRID) {
			for (int dz = -16; dz < 32; dz += GRID) {
				int x = x0 + dx;
				int z = z0 + dz;
				for (int y : DuergarVillageLogic.floors(columns.at(x, z), floorMinY, floorMaxY)) {
					int score = 0;
					for (int a = 0; a < 8; a++) {
						double angle = a * Math.PI / 4.0;
						int rx = x + (int) Math.round(Math.cos(angle) * siteRadius);
						int rz = z + (int) Math.round(Math.sin(angle) * siteRadius);
						if (DuergarVillageLogic.floorNear(columns.at(rx, rz), y, 3) != DuergarVillageLogic.NONE) {
							score++;
						}
					}
					if (score >= 7) {
						int at = 0;
						while (at < scores.size() && scores.get(at) >= score) {
							at++;
						}
						sites.add(at, new BlockPos(x, y, z));
						scores.add(at, score);
					}
				}
			}
		}
		return sites;
	}

	/**
	 * Moves each piece down or up onto the floor under it. A piece the floor still walls in after its step (the cave
	 * floor rises faster than the streets may climb) is left out together with everything joined on beyond it, so a
	 * street ends at the foot of the rise instead of running on under the ground. A stair that climbs where the floor
	 * drops (or the other way round) is turned round first, so the streets can go down as well as up. {@code null} when
	 * too many pieces would hang in the air or fewer than a third are left.
	 */
	private Settled settle(List<StructurePiece> built, Columns columns, StructureTemplateManager templates) {
		List<StructurePiece> all = new ArrayList<>(built);
		int n = all.size();
		int[] moves = new int[n];
		// How far the pieces joined on beyond each piece move; more than the piece itself for a stair that was turned.
		int[] carried = new int[n];
		boolean[] dropped = new boolean[n];
		int hanging = 0;
		int kept = 0;
		for (int i = 0; i < n; i++) {
			BoundingBox box = all.get(i).getBoundingBox();
			int parent = parent(all, i);
			if (parent >= 0 && dropped[parent]) {
				dropped[i] = true;
				continue;
			}
			int base = parent < 0 ? 0 : carried[parent];
			int rise = parent < 0 ? 0 : turnStair(all, i, all.get(parent).getBoundingBox(), base, columns, templates);
			// The near end of a turned stair is where its far end was, rise blocks higher or lower.
			base -= rise;
			int[][] samples = samples(box);
			int[] found = new int[samples.length];
			int count = 0;
			for (int[] sample : samples) {
				int floor = DuergarVillageLogic.floorNear(columns.at(sample[0], sample[1]), box.minY() + base, SEARCH);
				if (floor != DuergarVillageLogic.NONE) {
					found[count++] = floor - box.minY();
				}
			}
			int wanted = count == 0 ? base : DuergarVillageLogic.median(Arrays.copyOf(found, count));
			moves[i] = parent < 0 ? wanted : DuergarVillageLogic.step(wanted, base, maxStep);
			carried[i] = moves[i] - rise;
			int over = 0;
			int under = 0;
			for (int[] sample : samples) {
				IntUnaryOperator column = columns.at(sample[0], sample[1]);
				if (DuergarVillageLogic.hangs(column, box.minY() + moves[i])) {
					over++;
				}
				if (DuergarVillageLogic.buried(column, box.minY() + moves[i])) {
					under++;
				}
			}
			if (parent >= 0 && under * 2 > samples.length) {
				dropped[i] = true;
				continue;
			}
			kept++;
			if (over * 2 > samples.length) {
				hanging++;
			}
		}
		if (hanging > maxHanging * kept || kept * 3 < n) {
			return null;
		}
		List<StructurePiece> pieces = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			if (!dropped[i]) {
				all.get(i).move(0, moves[i], 0);
				pieces.add(all.get(i));
			}
		}
		// The joins between pieces smooth the ground (terrain adaptation); keep them at the pieces' new heights, and
		// forget the joins into pieces that were left out.
		for (StructurePiece piece : pieces) {
			if (piece instanceof PoolElementStructurePiece pool) {
				List<JigsawJunction> junctions = pool.getJunctions();
				for (int j = junctions.size() - 1; j >= 0; j--) {
					JigsawJunction junction = junctions.get(j);
					int target = containing(all, junction.getSourceX(), junction.getSourceZ(), piece);
					if (target >= 0 && dropped[target]) {
						junctions.remove(j);
					} else if (target >= 0 && moves[target] != 0) {
						junctions.set(j, new JigsawJunction(junction.getSourceX(), junction.getSourceGroundY() + moves[target],
								junction.getSourceZ(), junction.getDeltaY(), junction.getDestProjection()));
					}
				}
			}
		}
		return new Settled(pieces, moves[0]);
	}

	/**
	 * Turns piece {@code index} round in place if it is a stair (two joins at different heights) that climbs away from
	 * {@code parent} where the floor under it drops, or the other way round. Returns how many blocks it climbed away
	 * from its parent before it was turned, or zero when it is left as it is.
	 */
	private static int turnStair(List<StructurePiece> pieces, int index, BoundingBox parent, int base, Columns columns,
			StructureTemplateManager templates) {
		if (!(pieces.get(index) instanceof PoolElementStructurePiece piece)) {
			return 0;
		}
		List<StructureTemplate.JigsawBlockInfo> joins = piece.getElement().getShuffledJigsawBlocks(templates, piece.getPosition(),
				piece.getRotation(), RandomSource.create(0));
		if (joins.size() != 2 || joins.get(0).pos().getY() == joins.get(1).pos().getY()) {
			return 0;
		}
		BoundingBox near = parent.inflatedBy(1);
		BlockPos from = near.isInside(joins.get(0).pos()) ? joins.get(0).pos() : joins.get(1).pos();
		BlockPos to = from == joins.get(0).pos() ? joins.get(1).pos() : joins.get(0).pos();
		if (!near.isInside(from) || near.isInside(to)) {
			return 0;
		}
		int fromFloor = DuergarVillageLogic.floorNear(columns.at(from.getX(), from.getZ()), from.getY() + base, SEARCH);
		int toFloor = DuergarVillageLogic.floorNear(columns.at(to.getX(), to.getZ()), to.getY() + base, SEARCH);
		int rise = to.getY() - from.getY();
		if (fromFloor == DuergarVillageLogic.NONE || toFloor == DuergarVillageLogic.NONE
				|| !DuergarVillageLogic.turnStair(rise, toFloor - fromFloor)) {
			return 0;
		}
		// Half a turn swaps the two ends; the box is kept where it was.
		BoundingBox box = piece.getBoundingBox();
		Rotation turned = piece.getRotation().getRotated(Rotation.CLOCKWISE_180);
		BoundingBox moved = piece.getElement().getBoundingBox(templates, piece.getPosition(), turned);
		BlockPos position = piece.getPosition().offset(box.minX() - moved.minX(), 0, box.minZ() - moved.minZ());
		PoolElementStructurePiece flipped = new PoolElementStructurePiece(templates, piece.getElement(), position,
				piece.getGroundLevelDelta(), turned, box, LiquidSettings.IGNORE_WATERLOGGING);
		piece.getJunctions().forEach(flipped::addJunction);
		pieces.set(index, flipped);
		return rise;
	}

	/** The pieces that stay, set down onto the floor, and how far the first one moved. */
	private record Settled(List<StructurePiece> pieces, int startMove) {
	}

	/** The piece this one was joined to: the earlier piece with a junction pointing into it. */
	private static int parent(List<StructurePiece> pieces, int index) {
		BoundingBox box = pieces.get(index).getBoundingBox();
		for (int j = 0; j < index; j++) {
			if (pieces.get(j) instanceof PoolElementStructurePiece pool) {
				for (JigsawJunction junction : pool.getJunctions()) {
					if (box.minX() <= junction.getSourceX() && junction.getSourceX() <= box.maxX()
							&& box.minZ() <= junction.getSourceZ() && junction.getSourceZ() <= box.maxZ()) {
						return j;
					}
				}
			}
		}
		return -1;
	}

	private static int containing(List<StructurePiece> pieces, int x, int z, StructurePiece except) {
		for (int i = 0; i < pieces.size(); i++) {
			BoundingBox box = pieces.get(i).getBoundingBox();
			if (pieces.get(i) != except && box.minX() <= x && x <= box.maxX() && box.minZ() <= z && z <= box.maxZ()) {
				return i;
			}
		}
		return -1;
	}

	/** The middle of a piece and four points just inside its corners. */
	private static int[][] samples(BoundingBox box) {
		int x0 = Math.min(box.minX() + 1, box.maxX());
		int x1 = Math.max(box.maxX() - 1, box.minX());
		int z0 = Math.min(box.minZ() + 1, box.maxZ());
		int z1 = Math.max(box.maxZ() - 1, box.minZ());
		return new int[][] {{(box.minX() + box.maxX()) / 2, (box.minZ() + box.maxZ()) / 2}, {x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}};
	}

	/** The bare noise columns read so far, each kept as block kinds. */
	private static final class Columns {
		private final GenerationContext context;
		private final Map<Long, IntUnaryOperator> cache = new HashMap<>();

		Columns(GenerationContext context) {
			this.context = context;
		}

		IntUnaryOperator at(int x, int z) {
			return cache.computeIfAbsent(BlockPos.asLong(x, 0, z), key -> {
				NoiseColumn column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
				return y -> kind(column.getBlock(y));
			});
		}

		private static int kind(BlockState state) {
			if (state.isAir()) {
				return DuergarVillageLogic.AIR;
			}
			if (!state.getFluidState().isEmpty()) {
				return state.getFluidState().is(FluidTags.WATER) ? DuergarVillageLogic.WATER : DuergarVillageLogic.OTHER;
			}
			return DuergarVillageLogic.SOLID;
		}
	}
}
