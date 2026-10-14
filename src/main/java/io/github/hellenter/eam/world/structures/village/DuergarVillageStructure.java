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
import net.minecraft.core.Holder;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

/**
 * A duergar village: a jigsaw village that stands on the floor of a cave instead of at a random height. It looks for a
 * wide enough stretch of floor near its chunk, builds the village there, then sets every piece down onto the floor
 * under it, at most {@code max_step} blocks above or below the piece it joins, so the streets climb and fall with the
 * cave like a surface village's paths. A site where too many pieces would hang over open air is given up.
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
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("max_hanging", 0.15F).forGetter(village -> village.maxHanging)
	).apply(instance, DuergarVillageStructure::new));

	/** How far above or below its expected height a piece looks for the floor. */
	private static final int SEARCH = 6;
	/** How many sites are tried before the village is given up for this placement. */
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

	public DuergarVillageStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int size, int floorMinY,
			int floorMaxY, int maxDistance, int maxStep, int siteRadius, float maxHanging) {
		super(settings);
		this.startPool = startPool;
		this.size = size;
		this.floorMinY = floorMinY;
		this.floorMaxY = floorMaxY;
		this.maxDistance = maxDistance;
		this.maxStep = maxStep;
		this.siteRadius = siteRadius;
		this.maxHanging = maxHanging;
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
		for (int i = 0; i < TRIES && !sites.isEmpty(); i++) {
			BlockPos site = sites.remove(random.nextInt(Math.min(sites.size(), 4)));
			Optional<GenerationStub> stub = JigsawPlacement.addPieces(context, startPool, Optional.empty(), size, site, false,
					Optional.empty(), new JigsawStructure.MaxDistance(maxDistance, 24), PoolAliasLookup.EMPTY,
					DimensionPadding.ZERO, LiquidSettings.IGNORE_WATERLOGGING);
			if (stub.isEmpty()) {
				continue;
			}
			StructurePiecesBuilder builder = stub.get().getPiecesBuilder();
			List<StructurePiece> pieces = new ArrayList<>(builder.build().pieces());
			int[] moves = settle(pieces, columns);
			if (moves == null) {
				continue;
			}
			StructurePiecesBuilder settled = new StructurePiecesBuilder();
			pieces.forEach(settled::addPiece);
			return Optional.of(new GenerationStub(stub.get().position().above(moves[0]), Either.right(settled)));
		}
		return Optional.empty();
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
	 * Moves each piece down or up onto the floor under it, and returns how far each moved; {@code null} when too many
	 * of them would still hang in the air.
	 */
	private int[] settle(List<StructurePiece> pieces, Columns columns) {
		int n = pieces.size();
		int[] moves = new int[n];
		int hanging = 0;
		for (int i = 0; i < n; i++) {
			BoundingBox box = pieces.get(i).getBoundingBox();
			int parent = parent(pieces, i);
			int base = parent < 0 ? 0 : moves[parent];
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
			int over = 0;
			for (int[] sample : samples) {
				if (DuergarVillageLogic.hangs(columns.at(sample[0], sample[1]), box.minY() + moves[i])) {
					over++;
				}
			}
			if (over * 2 > samples.length) {
				hanging++;
			}
		}
		if (hanging > maxHanging * n) {
			return null;
		}
		for (int i = 0; i < n; i++) {
			pieces.get(i).move(0, moves[i], 0);
		}
		// The joins between pieces smooth the ground (terrain adaptation); keep them at the pieces' new heights.
		for (StructurePiece piece : pieces) {
			if (piece instanceof PoolElementStructurePiece pool) {
				List<JigsawJunction> junctions = pool.getJunctions();
				for (int j = 0; j < junctions.size(); j++) {
					JigsawJunction junction = junctions.get(j);
					int target = containing(pieces, junction.getSourceX(), junction.getSourceZ(), piece);
					if (target >= 0 && moves[target] != 0) {
						junctions.set(j, new JigsawJunction(junction.getSourceX(), junction.getSourceGroundY() + moves[target],
								junction.getSourceZ(), junction.getDeltaY(), junction.getDestProjection()));
					}
				}
			}
		}
		return moves;
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
