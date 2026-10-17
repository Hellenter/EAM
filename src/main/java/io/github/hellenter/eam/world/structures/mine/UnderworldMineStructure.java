package io.github.hellenter.eam.world.structures.mine;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * An underworld mine: the jigsaw mine of {@link UnderworldMine}, which always has a way in from the cave. Once the
 * pieces are laid out, one of the mine's dead ends is replaced by an adit ({@link MineAditElement}) dug straight on to
 * the nearest open cave floor. When no dead end leads to a cave close enough, the mine is laid out again at another
 * height, and given up for this spot after a few tries. The pieces then become {@link MinePieceElement}s: a wing may have
 * caved in or flooded ({@link MineWingLogic}), and chest minecarts stand on some of the rails.
 */
public class UnderworldMineStructure extends Structure {
	public static final MapCodec<UnderworldMineStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			settingsCodec(instance),
			StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(mine -> mine.startPool),
			Codec.intRange(0, 20).fieldOf("size").forGetter(mine -> mine.size),
			HeightProvider.CODEC.fieldOf("start_height").forGetter(mine -> mine.startHeight),
			JigsawStructure.MaxDistance.CODEC.fieldOf("max_distance_from_center").forGetter(mine -> mine.maxDistance),
			DimensionPadding.CODEC.optionalFieldOf("dimension_padding", DimensionPadding.ZERO).forGetter(mine -> mine.padding),
			Codec.intRange(4, 64).optionalFieldOf("max_adit_length", 40).forGetter(mine -> mine.maxAditLength),
			Codec.intRange(1, 8).optionalFieldOf("tries", 4).forGetter(mine -> mine.tries),
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("collapsed_wing_chance", 0.0F).forGetter(mine -> mine.collapsedWingChance),
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("flooded_wing_chance", 0.0F).forGetter(mine -> mine.floodedWingChance),
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("minecart_chance", 0.0F).forGetter(mine -> mine.minecartChance)
	).apply(instance, UnderworldMineStructure::new));

	/** Templates of the cave-ins that close the mine's tunnels; the adit replaces one of them. */
	private static final String DEAD_ENDS = "underworld_mine/end/";

	private final Holder<StructureTemplatePool> startPool;
	private final int size;
	private final HeightProvider startHeight;
	private final JigsawStructure.MaxDistance maxDistance;
	private final DimensionPadding padding;
	private final int maxAditLength;
	private final int tries;
	private final float collapsedWingChance;
	private final float floodedWingChance;
	private final float minecartChance;

	public UnderworldMineStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int size,
			HeightProvider startHeight, JigsawStructure.MaxDistance maxDistance, DimensionPadding padding, int maxAditLength, int tries,
			float collapsedWingChance, float floodedWingChance, float minecartChance) {
		super(settings);
		this.startPool = startPool;
		this.size = size;
		this.startHeight = startHeight;
		this.maxDistance = maxDistance;
		this.padding = padding;
		this.maxAditLength = maxAditLength;
		this.tries = tries;
		this.collapsedWingChance = collapsedWingChance;
		this.floodedWingChance = floodedWingChance;
		this.minecartChance = minecartChance;
	}

	@Override
	public StructureType<?> type() {
		return UnderworldMineModule.TYPE;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		Columns columns = new Columns(context);
		RandomSource random = context.random();
		int minFloor = context.heightAccessor().getMinY() + padding.bottom();
		int maxFloor = context.heightAccessor().getMaxY() - padding.top() - UnderworldMineLogic.HEIGHT;
		for (int i = 0; i < tries; i++) {
			int height = startHeight.sample(random, new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor()));
			BlockPos start = new BlockPos(context.chunkPos().getMinBlockX(), height, context.chunkPos().getMinBlockZ());
			Optional<GenerationStub> stub = JigsawPlacement.addPieces(context, startPool, Optional.empty(), size, start, false,
					Optional.empty(), maxDistance, PoolAliasLookup.EMPTY, padding, LiquidSettings.IGNORE_WATERLOGGING);
			if (stub.isEmpty()) {
				continue;
			}
			List<StructurePiece> pieces = new ArrayList<>(stub.get().getPiecesBuilder().build().pieces());
			List<Integer> ends = new ArrayList<>();
			for (int p = 0; p < pieces.size(); p++) {
				if (isDeadEnd(pieces.get(p))) {
					ends.add(p);
				}
			}
			while (!ends.isEmpty()) {
				int end = ends.remove(random.nextInt(ends.size()));
				UnderworldMineLogic.Adit adit = adit(pieces, end, columns, minFloor, maxFloor);
				if (adit != null) {
					List<StructurePiece> laidOut = withWings(context, pieces, end);
					PoolElementStructurePiece deadEnd = (PoolElementStructurePiece) laidOut.remove(end);
					MineAditElement element = new MineAditElement(adit);
					BlockPos at = new BlockPos(adit.startX(), adit.startFloor(), adit.startZ());
					laidOut.add(new PoolElementStructurePiece(context.structureTemplateManager(), element, at, 0,
							deadEnd.getRotation(), element.box(), LiquidSettings.IGNORE_WATERLOGGING));
					StructurePiecesBuilder builder = new StructurePiecesBuilder();
					laidOut.forEach(builder::addPiece);
					return Optional.of(new GenerationStub(stub.get().position(), Either.right(builder)));
				}
			}
		}
		return Optional.empty();
	}

	/**
	 * The mine's pieces as {@link MinePieceElement}s: maybe one wing caved in and another flooded (neither on the way in
	 * from dead end {@code keep}, where the adit comes), and every piece with its chance of a chest minecart.
	 */
	private List<StructurePiece> withWings(GenerationContext context, List<StructurePiece> pieces, int keep) {
		RandomSource random = context.random();
		StructureTemplateManager templates = context.structureTemplateManager();
		int[] parent = new int[pieces.size()];
		BlockPos[] entrance = new BlockPos[pieces.size()];
		join(templates, pieces, parent, entrance);
		List<Integer> candidates = MineWingLogic.candidates(parent, keep, MineWingLogic.MIN_PIECES,
				Math.max(MineWingLogic.MIN_PIECES, pieces.size() / 2));
		int collapsed = -1;
		if (!candidates.isEmpty() && random.nextFloat() < collapsedWingChance) {
			collapsed = candidates.get(random.nextInt(candidates.size()));
		}
		int flooded = -1;
		if (random.nextFloat() < floodedWingChance) {
			// The deepest wings first, so the water fills more than the end of a tunnel.
			List<Integer> wet = new ArrayList<>();
			int deepest = 0;
			for (int root : candidates) {
				int depth = entrance[root].getY() - lowest(pieces, parent, root);
				if ((collapsed < 0 || MineWingLogic.disjoint(parent, root, collapsed))
						&& MineWingLogic.floods(lowest(pieces, parent, root), entrance[root].getY()) && depth >= deepest) {
					if (depth > deepest) {
						wet.clear();
						deepest = depth;
					}
					wet.add(root);
				}
			}
			if (!wet.isEmpty()) {
				flooded = wet.get(random.nextInt(wet.size()));
			}
		}
		long collapseSeed = random.nextLong();
		long floodSeed = random.nextLong();
		long cartSeed = random.nextLong();
		Holder<StructureProcessorList> processors = context.registryAccess().lookupOrThrow(Registries.PROCESSOR_LIST)
				.getOrThrow(UnderworldMine.PROCESSORS);
		List<StructurePiece> result = new ArrayList<>(pieces.size());
		for (int p = 0; p < pieces.size(); p++) {
			StructurePiece piece = pieces.get(p);
			if (!(piece instanceof PoolElementStructurePiece pool) || !(pool.getElement() instanceof SinglePoolElement single)) {
				result.add(piece);
				continue;
			}
			Optional<MineWingProcessor> wing = Optional.empty();
			if (collapsed >= 0 && MineWingLogic.inWing(parent, collapsed, p)) {
				wing = Optional.of(MineWingProcessor.collapsed(p == collapsed ? Optional.of(entrance[p]) : Optional.empty(), collapseSeed));
			} else if (flooded >= 0 && MineWingLogic.inWing(parent, flooded, p)) {
				wing = Optional.of(MineWingProcessor.flooded(entrance[flooded].getY(), floodSeed));
			}
			MinePieceElement element = new MinePieceElement(single.getTemplateLocation(), processors, single.getProjection(), wing,
					minecartChance, cartSeed);
			PoolElementStructurePiece laid = new PoolElementStructurePiece(templates, element, pool.getPosition(),
					pool.getGroundLevelDelta(), pool.getRotation(), pool.getBoundingBox(), LiquidSettings.IGNORE_WATERLOGGING);
			pool.getJunctions().forEach(laid::addJunction);
			result.add(laid);
		}
		return result;
	}

	/**
	 * The tree of the mine's pieces from the hall (piece 0): the {@code parent} of every piece ({@code -1} for the hall
	 * and for pieces not joined to it) and its {@code entrance}, the floor block where it joins its parent. Two pieces are
	 * joined where their jigsaws face each other.
	 */
	private static void join(StructureTemplateManager templates, List<StructurePiece> pieces, int[] parent, BlockPos[] entrance) {
		List<List<StructureTemplate.JigsawBlockInfo>> jigsaws = new ArrayList<>(pieces.size());
		Map<BlockPos, Integer> owner = new HashMap<>();
		for (int p = 0; p < pieces.size(); p++) {
			List<StructureTemplate.JigsawBlockInfo> own = pieces.get(p) instanceof PoolElementStructurePiece pool
					? pool.getElement().getShuffledJigsawBlocks(templates, pool.getPosition(), pool.getRotation(), RandomSource.create(0L))
					: List.of();
			jigsaws.add(own);
			for (StructureTemplate.JigsawBlockInfo jigsaw : own) {
				owner.put(jigsaw.pos(), p);
			}
		}
		Arrays.fill(parent, -1);
		boolean[] reached = new boolean[pieces.size()];
		ArrayDeque<Integer> queue = new ArrayDeque<>();
		reached[0] = true;
		queue.add(0);
		while (!queue.isEmpty()) {
			int p = queue.poll();
			for (StructureTemplate.JigsawBlockInfo jigsaw : jigsaws.get(p)) {
				BlockPos facing = jigsaw.pos().relative(JigsawBlock.getFrontFacing(jigsaw.state()));
				Integer child = owner.get(facing);
				if (child != null && !reached[child]) {
					reached[child] = true;
					parent[child] = p;
					entrance[child] = facing;
					queue.add(child);
				}
			}
		}
	}

	/** The lowest block of the wing that starts at {@code root}. */
	private static int lowest(List<StructurePiece> pieces, int[] parent, int root) {
		int lowest = Integer.MAX_VALUE;
		for (int p = 0; p < pieces.size(); p++) {
			if (MineWingLogic.inWing(parent, root, p)) {
				lowest = Math.min(lowest, pieces.get(p).getBoundingBox().minY());
			}
		}
		return lowest;
	}

	private static boolean isDeadEnd(StructurePiece piece) {
		return piece instanceof PoolElementStructurePiece pool && pool.getElement() instanceof SinglePoolElement single
				&& single.getTemplateLocation().getPath().startsWith(DEAD_ENDS);
	}

	/**
	 * The adit that replaces dead end {@code end}: it starts in the dead end's doorway, at the tunnel's floor, and heads
	 * away from the piece the dead end closes.
	 */
	private UnderworldMineLogic.Adit adit(List<StructurePiece> pieces, int end, Columns columns, int minFloor, int maxFloor) {
		PoolElementStructurePiece deadEnd = (PoolElementStructurePiece) pieces.get(end);
		List<JigsawJunction> junctions = deadEnd.getJunctions();
		if (junctions.isEmpty()) {
			return null;
		}
		BoundingBox box = deadEnd.getBoundingBox();
		int floor = box.minY();
		JigsawJunction join = junctions.getFirst();
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			int x = join.getSourceX() + direction.getStepX();
			int z = join.getSourceZ() + direction.getStepZ();
			if (box.isInside(x, floor, z)) {
				List<UnderworldMineLogic.Box> others = new ArrayList<>();
				for (int p = 0; p < pieces.size(); p++) {
					BoundingBox other = pieces.get(p).getBoundingBox();
					if (p != end) {
						others.add(new UnderworldMineLogic.Box(other.minX(), other.minY(), other.minZ(), other.maxX(), other.maxY(), other.maxZ()));
					}
				}
				return UnderworldMineLogic.find(columns, others, x, floor, z, direction.getStepX(), direction.getStepZ(),
						maxAditLength, minFloor, maxFloor);
			}
		}
		return null;
	}

	/** The bare noise columns read so far, each kept as block kinds. */
	private static final class Columns implements UnderworldMineLogic.Terrain {
		private final GenerationContext context;
		private final Map<Long, NoiseColumn> cache = new HashMap<>();

		Columns(GenerationContext context) {
			this.context = context;
		}

		@Override
		public int kind(int x, int y, int z) {
			NoiseColumn column = cache.computeIfAbsent(BlockPos.asLong(x, 0, z),
					key -> context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState()));
			return kind(column.getBlock(y));
		}

		private static int kind(BlockState state) {
			if (state.isAir()) {
				return UnderworldMineLogic.AIR;
			}
			if (!state.getFluidState().isEmpty()) {
				return state.getFluidState().is(FluidTags.WATER) ? UnderworldMineLogic.WATER : UnderworldMineLogic.OTHER;
			}
			return UnderworldMineLogic.SOLID;
		}
	}
}
