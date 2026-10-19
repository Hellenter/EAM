package io.github.hellenter.eam.world.structures.castle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.hellenter.eam.world.structures.GroundWork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;

import java.util.Optional;

/**
 * The Pillager King's castle: the template tiles, turned a random way, on the flattest dry land near the middle of its
 * chunk that is not too steep, standing on the highest ground of the footprint with earth banked under and around it.
 * Like the outpost it generates after the trees and clears them within {@code clearRadius} blocks.
 */
public class CastleStructure extends Structure {
	public static final MapCodec<CastleStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			settingsCodec(instance),
			Codec.intRange(0, 24).optionalFieldOf("clear_radius", 15).forGetter(castle -> castle.clearRadius),
			Codec.intRange(0, 32).optionalFieldOf("max_slope", 10).forGetter(castle -> castle.maxSlope)
	).apply(instance, CastleStructure::new));

	private final int clearRadius;
	private final int maxSlope;

	public CastleStructure(StructureSettings settings, int clearRadius, int maxSlope) {
		super(settings);
		this.clearRadius = clearRadius;
		this.maxSlope = maxSlope;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		Rotation rotation = Rotation.getRandom(context.random());
		Vec3i size = CastleModule.SIZE;
		boolean across = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
		int halfX = CastleLogic.halfSamples(across ? size.getZ() : size.getX());
		int halfZ = CastleLogic.halfSamples(across ? size.getX() : size.getZ());
		int reach = CastleLogic.reach(Math.max(halfX, halfZ));

		ChunkPos chunk = context.chunkPos();
		int width = 2 * reach + 1;
		int[][] ground = new int[width][width];
		boolean[][] wet = new boolean[width][width];
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < width; j++) {
				int x = chunk.getMiddleBlockX() + (i - reach) * CastleLogic.STEP;
				int z = chunk.getMiddleBlockZ() + (j - reach) * CastleLogic.STEP;
				ground[i][j] = surface(context, x, z, Heightmap.Types.WORLD_SURFACE_WG);
				// Dry land only: the moat and the courtyard would flood.
				wet[i][j] = surface(context, x, z, Heightmap.Types.OCEAN_FLOOR_WG) < ground[i][j];
			}
		}
		CastleLogic.Site site = CastleLogic.site(ground, wet, halfX, halfZ, maxSlope);
		if (site == null) return Optional.empty();

		int middleX = chunk.getMiddleBlockX() + site.dx() * CastleLogic.STEP;
		int middleZ = chunk.getMiddleBlockZ() + site.dz() * CastleLogic.STEP;
		// Every tile turns about the middle of the whole castle's floor, which lands on the chosen middle.
		BlockPos middle = new BlockPos(size.getX() / 2, 0, size.getZ() / 2);
		BlockPos corner = new BlockPos(middleX - middle.getX(), site.ground() + 1, middleZ - middle.getZ());
		return Optional.of(new GenerationStub(new BlockPos(middleX, site.ground(), middleZ), builder -> {
			for (CastleModule.Tile tile : CastleModule.TILES) {
				builder.addPiece(new CastlePiece(context.structureTemplateManager(), tile.template(), corner.offset(tile.offset()),
						rotation, middle.subtract(tile.offset())));
			}
		}));
	}

	private static int surface(GenerationContext context, int x, int z, Heightmap.Types heightmap) {
		return context.chunkGenerator().getFirstOccupiedHeight(x, z, heightmap, context.heightAccessor(), context.randomState());
	}

	/** Every chunk the clearing and the leaves reaching into it touch takes part in placing the castle. */
	@Override
	public BoundingBox adjustBoundingBox(BoundingBox box) {
		int reach = clearRadius + GroundWork.LEAF_REACH;
		return super.adjustBoundingBox(box).inflatedBy(reach, 0, reach);
	}

	@Override
	public void afterPlace(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox chunkBox, ChunkPos chunk, PiecesContainer pieces) {
		BoundingBox footprint = null;
		for (StructurePiece piece : pieces.pieces()) {
			if (!(piece instanceof CastlePiece tile)) continue;
			footprint = footprint == null ? tile.templateBox() : footprint.encapsulate(tile.templateBox());
		}
		if (footprint == null) return;
		if (clearRadius > 0) GroundWork.clearTrees(level, chunkBox, footprint, footprint.minY(), clearRadius);
		// The template's bottom layer stands on the ground: the moat and the land around the hill are open grass.
		GroundWork.bank(level, chunkBox, footprint, footprint.minY() - 1, true);
	}

	@Override
	public StructureType<?> type() {
		return CastleModule.CASTLE;
	}
}
