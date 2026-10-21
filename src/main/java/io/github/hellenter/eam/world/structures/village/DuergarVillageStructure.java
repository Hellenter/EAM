package io.github.hellenter.eam.world.structures.village;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
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
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
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
 * cave like a surface village's paths. A stair climbs one block, or as many as the floor climbs over it (up to four) when
 * the {@code stairs} pool has the taller ones. Where the floor rises faster than that, the street stops at the foot of the
 * rise rather than tunnel on under it. A site where too many pieces would hang over open air is given up; a smaller gap under
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
			Codec.intRange(0, 16).optionalFieldOf("foundation_depth", 6).forGetter(village -> village.foundationDepth),
			StructureTemplatePool.CODEC.optionalFieldOf("stairs").forGetter(village -> village.stairs)
	).apply(instance, DuergarVillageStructure::new));

	/** How far above or below its expected height a piece looks for the floor. */
	private static final int SEARCH = 6;
	/** How many sites are tried; the one that keeps the most pieces is built. */
	private static final int TRIES = 3;
	/** How far apart the columns tried as the village's centre are. */
	private static final int GRID = 8;
	/** What {@link #fitStair} says of a piece that is no stair. */
	private static final int NOT_A_STAIR = Integer.MIN_VALUE;

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
	private final Optional<Holder<StructureTemplatePool>> stairs;

	public DuergarVillageStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int size, int floorMinY,
			int floorMaxY, int maxDistance, int maxStep, int siteRadius, float maxHanging, BlockState foundation, int foundationDepth,
			Optional<Holder<StructureTemplatePool>> stairs) {
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
		this.stairs = stairs;
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
	 * street ends at the foot of the rise instead of running on under the ground. A stair is swapped first for the one
	 * that climbs (or goes down) as far as the floor under it, so the streets go up and down hills of any height a
	 * flight at a time. {@code null} when too many pieces would hang in the air or fewer than a third are left.
	 */
	private Settled settle(List<StructurePiece> built, Columns columns, StructureTemplateManager templates) {
		List<StructurePiece> all = new ArrayList<>(built);
		int n = all.size();
		Map<Integer, StructurePoolElement> flights = flights(templates);
		int[] moves = new int[n];
		// How far the pieces joined on beyond each piece move; not the piece's own move for a stair that was swapped.
		int[] carried = new int[n];
		boolean[] dropped = new boolean[n];
		int hanging = 0;
		int kept = 0;
		for (int i = 0; i < n; i++) {
			int parent = parent(all, i);
			if (parent >= 0 && dropped[parent]) {
				dropped[i] = true;
				continue;
			}
			int base = parent < 0 ? 0 : carried[parent];
			int extra = parent < 0 ? NOT_A_STAIR : fitStair(all, i, all.get(parent).getBoundingBox(), base, columns, templates, flights);
			if (extra != NOT_A_STAIR) {
				// A stair meets the piece it joins exactly and its far end stands on the floor; the pieces beyond it
				// follow its far end, extra blocks higher or lower than they were built.
				moves[i] = base;
				carried[i] = base + extra;
				kept++;
				continue;
			}
			BoundingBox box = all.get(i).getBoundingBox();
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
			carried[i] = moves[i];
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
	 * Swaps piece {@code index}, if it is a stair (two joins at different heights), for the flight that climbs or goes
	 * down as far as the floor under it, one block a step: its near end where the old one's was, its far end on the floor
	 * there. Returns how many blocks higher (or lower) its far end now stands than the old one's, zero when the old one
	 * already fits, or {@link #NOT_A_STAIR}.
	 */
	private static int fitStair(List<StructurePiece> pieces, int index, BoundingBox parent, int base, Columns columns,
			StructureTemplateManager templates, Map<Integer, StructurePoolElement> flights) {
		if (!(pieces.get(index) instanceof PoolElementStructurePiece piece)) {
			return NOT_A_STAIR;
		}
		List<StructureTemplate.JigsawBlockInfo> joins = piece.getElement().getShuffledJigsawBlocks(templates, piece.getPosition(),
				piece.getRotation(), RandomSource.create(0));
		if (joins.size() != 2 || joins.get(0).pos().getY() == joins.get(1).pos().getY()) {
			return NOT_A_STAIR;
		}
		BoundingBox near = parent.inflatedBy(1);
		BlockPos from = near.isInside(joins.get(0).pos()) ? joins.get(0).pos() : joins.get(1).pos();
		BlockPos to = from == joins.get(0).pos() ? joins.get(1).pos() : joins.get(0).pos();
		if (!near.isInside(from) || near.isInside(to)) {
			return NOT_A_STAIR;
		}
		int rise = to.getY() - from.getY();
		int toFloor = DuergarVillageLogic.floorNear(columns.at(to.getX(), to.getZ()), to.getY() + base, SEARCH);
		if (toFloor == DuergarVillageLogic.NONE) {
			return 0;
		}
		int maxRise = flights.keySet().stream().mapToInt(Integer::intValue).max().orElse(Math.abs(rise));
		int wanted = DuergarVillageLogic.stairRise(toFloor - (from.getY() + base), rise, maxRise);
		StructurePoolElement flight = flights.getOrDefault(Math.abs(wanted), wanted == -rise ? piece.getElement() : null);
		if (wanted == rise || flight == null) {
			return 0;
		}
		// The turn that lays the flight along the old one, climbing the way it should.
		for (Rotation rotation : Rotation.values()) {
			List<StructureTemplate.JigsawBlockInfo> ends = flight.getShuffledJigsawBlocks(templates, BlockPos.ZERO, rotation,
					RandomSource.create(0));
			if (ends.size() != 2) {
				return 0;
			}
			BlockPos low = ends.get(0).pos().getY() < ends.get(1).pos().getY() ? ends.get(0).pos() : ends.get(1).pos();
			BlockPos high = low == ends.get(0).pos() ? ends.get(1).pos() : ends.get(0).pos();
			BlockPos start = wanted > 0 ? low : high;
			BlockPos end = wanted > 0 ? high : low;
			if (end.getX() - start.getX() != to.getX() - from.getX() || end.getZ() - start.getZ() != to.getZ() - from.getZ()) {
				continue;
			}
			BlockPos position = from.subtract(start);
			BoundingBox box = flight.getBoundingBox(templates, position, rotation);
			BoundingBox old = piece.getBoundingBox();
			if (box.minX() != old.minX() || box.maxX() != old.maxX() || box.minZ() != old.minZ() || box.maxZ() != old.maxZ()) {
				return 0;
			}
			PoolElementStructurePiece swapped = new PoolElementStructurePiece(templates, flight, position,
					piece.getGroundLevelDelta(), rotation, box, LiquidSettings.IGNORE_WATERLOGGING);
			piece.getJunctions().forEach(swapped::addJunction);
			pieces.set(index, swapped);
			return wanted - rise;
		}
		return 0;
	}

	/** The flights of the {@code stairs} pool by how many blocks each climbs. */
	private Map<Integer, StructurePoolElement> flights(StructureTemplateManager templates) {
		Map<Integer, StructurePoolElement> flights = new HashMap<>();
		stairs.ifPresent(pool -> {
			for (Pair<StructurePoolElement, Integer> entry : pool.value().getTemplates()) {
				List<StructureTemplate.JigsawBlockInfo> ends = entry.getFirst().getShuffledJigsawBlocks(templates, BlockPos.ZERO,
						Rotation.NONE, RandomSource.create(0));
				if (ends.size() == 2 && ends.get(0).pos().getY() != ends.get(1).pos().getY()) {
					flights.put(Math.abs(ends.get(0).pos().getY() - ends.get(1).pos().getY()), entry.getFirst());
				}
			}
		});
		return flights;
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
