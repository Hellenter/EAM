package io.github.hellenter.eam.world.structures.outpost;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.hellenter.eam.world.structures.GroundWork;

import net.minecraft.core.BlockPos;
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
		int reach = clearRadius + GroundWork.LEAF_REACH;
		return super.adjustBoundingBox(box).inflatedBy(reach, 0, reach);
	}

	@Override
	public void afterPlace(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox chunkBox, ChunkPos chunk, PiecesContainer pieces) {
		for (StructurePiece piece : pieces.pieces()) {
			if (!(piece instanceof OutpostPiece outpost)) continue;
			BoundingBox footprint = outpost.templateBox();
			if (clearRadius > 0) GroundWork.clearTrees(level, chunkBox, footprint, footprint.minY() + OutpostPiece.GROUND, clearRadius);
			GroundWork.bank(level, chunkBox, footprint, footprint.minY() + OutpostPiece.GROUND, false);
		}
	}

	@Override
	public StructureType<?> type() {
		return OutpostModule.OUTPOST;
	}
}
