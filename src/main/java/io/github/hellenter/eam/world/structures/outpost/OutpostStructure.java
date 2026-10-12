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
 * Maxim's pillager outpost: one template on the flattest dry land near the middle of its chunk that is not too steep,
 * standing on the highest ground of its footprint with its gate toward the lowest land around, and earth banked under
 * and around it. It generates after the trees, and clears every tree, its leaves and the forest's huge mushrooms within
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
	/** How far out the bank of earth around a raised outpost reaches; as far as the steepest site rises. */
	private static final int BANK = 10;
	/** How deep earth is filled under a raised outpost or its bank at most. */
	private static final int FOUNDATION_DEPTH = 16;

	private final int clearRadius;
	private final int maxSlope;

	public OutpostStructure(StructureSettings settings, int clearRadius, int maxSlope) {
		super(settings);
		this.clearRadius = clearRadius;
		this.maxSlope = maxSlope;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		ChunkPos chunk = context.chunkPos();
		int width = 2 * OutpostLogic.REACH + 1;
		int[][] ground = new int[width][width];
		boolean[][] wet = new boolean[width][width];
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < width; j++) {
				int x = chunk.getMiddleBlockX() + (i - OutpostLogic.REACH) * OutpostLogic.STEP;
				int z = chunk.getMiddleBlockZ() + (j - OutpostLogic.REACH) * OutpostLogic.STEP;
				ground[i][j] = surface(context, x, z, Heightmap.Types.WORLD_SURFACE_WG);
				// Dry land only: in a river or a lake the cellar would flood.
				wet[i][j] = surface(context, x, z, Heightmap.Types.OCEAN_FLOOR_WG) < ground[i][j];
			}
		}
		OutpostLogic.Site site = OutpostLogic.site(ground, wet, maxSlope, context.random().nextInt(4));
		if (site == null) return Optional.empty();
		var size = context.structureTemplateManager().getOrCreate(OutpostModule.TEMPLATE).getSize();
		int middleX = chunk.getMiddleBlockX() + site.dx() * OutpostLogic.STEP;
		int middleZ = chunk.getMiddleBlockZ() + site.dz() * OutpostLogic.STEP;
		BlockPos corner = new BlockPos(middleX - size.getX() / 2, site.ground() - OutpostPiece.GROUND, middleZ - size.getZ() / 2);
		// The template's gate is on its north side; each turn moves it one side clockwise.
		Rotation rotation = Rotation.BY_ID.apply(site.gate());
		return Optional.of(new GenerationStub(new BlockPos(middleX, site.ground(), middleZ),
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
		for (StructurePiece piece : pieces.pieces()) {
			if (!(piece instanceof OutpostPiece outpost)) continue;
			if (clearRadius > 0) clearTrees(level, chunkBox, outpost.templateBox());
			bank(level, chunkBox, outpost.templateBox());
		}
	}

	/**
	 * Where the outpost stands above the ground, fills earth under it down to the ground and banks earth around it,
	 * sloping one block down for each block out, so it neither floats nor sits on a cliff.
	 */
	private static void bank(WorldGenLevel level, BoundingBox chunkBox, BoundingBox footprint) {
		int grass = footprint.minY() + OutpostPiece.GROUND;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = Math.max(chunkBox.minX(), footprint.minX() - BANK); x <= Math.min(chunkBox.maxX(), footprint.maxX() + BANK); x++) {
			for (int z = Math.max(chunkBox.minZ(), footprint.minZ() - BANK); z <= Math.min(chunkBox.maxZ(), footprint.maxZ() + BANK); z++) {
				int distance = OutpostLogic.distance(footprint.minX(), footprint.minZ(), footprint.maxX(), footprint.maxZ(), x, z);
				if (distance > BANK) continue;
				int top = distance == 0 ? footprint.minY() - 1 : OutpostLogic.bankTop(grass, distance);
				for (int y = top; y > top - FOUNDATION_DEPTH; y--) {
					pos.set(x, y, z);
					BlockState state = level.getBlockState(pos);
					// The bank stops at water: a lake beside the outpost keeps its shore.
					if (!state.getFluidState().isEmpty()) break;
					if (!state.isAir() && !state.is(BlockTags.REPLACEABLE)) {
						// Grass buried under the bank turns to dirt.
						if (y < top && state.is(Blocks.GRASS_BLOCK)) level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
						break;
					}
					BlockState fill = distance > 0 && y == top ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState();
					level.setBlock(pos, fill, Block.UPDATE_CLIENTS);
				}
			}
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
