package io.github.hellenter.eam.world.structures.lostcastle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.hellenter.eam.world.structures.GroundWork;
import io.github.hellenter.eam.world.structures.village.DuergarVillageLogic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntUnaryOperator;

/**
 * The Lost castle: Maxim's template on a wide stretch of cave floor near its chunk, between {@code floor_min_y} and
 * {@code floor_max_y}, turned a random way. It stands on the highest floor under its footprint, with stone filled in
 * under it and banked round it, so it neither floats nor sits on a ledge. The template leaves out the columns round
 * the castle, so the cave beside its walls stays as it was.
 */
public class LostCastleStructure extends Structure {
	public static final MapCodec<LostCastleStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			settingsCodec(instance),
			Codec.INT.fieldOf("floor_min_y").forGetter(castle -> castle.floorMinY),
			Codec.INT.fieldOf("floor_max_y").forGetter(castle -> castle.floorMaxY)
	).apply(instance, LostCastleStructure::new));

	/** How far apart the columns tried as the castle's middle are, and how far from the chunk middle they go. */
	private static final int GRID = 8;
	private static final int SEARCH = 16;

	private final int floorMinY;
	private final int floorMaxY;

	public LostCastleStructure(StructureSettings settings, int floorMinY, int floorMaxY) {
		super(settings);
		this.floorMinY = floorMinY;
		this.floorMaxY = floorMaxY;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		int middleX = context.chunkPos().getMiddleBlockX();
		int middleZ = context.chunkPos().getMiddleBlockZ();
		// Reading the floor takes many noise columns: first a cheap look whether the chunk is in the right biome at all.
		if (!context.validBiome().test(context.biomeSource().createUncachedResolver(context.randomState()).getNoiseBiome(
				QuartPos.fromBlock(middleX), QuartPos.fromBlock((floorMinY + floorMaxY) / 2), QuartPos.fromBlock(middleZ)))) {
			return Optional.empty();
		}
		Vec3i size = context.structureTemplateManager().getOrCreate(LostCastleModule.TEMPLATE).getSize();
		int half = Math.max(size.getX(), size.getZ()) / 2;
		Map<Long, IntUnaryOperator> cache = new HashMap<>();
		LostCastleLogic.Columns columns = (x, z) -> cache.computeIfAbsent(BlockPos.asLong(x, 0, z), key -> {
			NoiseColumn column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
			return y -> kind(column.getBlock(y));
		});
		LostCastleLogic.Site best = null;
		for (int dx = -SEARCH; dx <= SEARCH; dx += GRID) {
			for (int dz = -SEARCH; dz <= SEARCH; dz += GRID) {
				int x = middleX + dx;
				int z = middleZ + dz;
				for (int floor : DuergarVillageLogic.floors(columns.at(x, z), floorMinY, floorMaxY)) {
					LostCastleLogic.Site site = LostCastleLogic.site(columns, x, z, floor, half);
					if (site != null && LostCastleLogic.better(site, best, middleX, middleZ)) best = site;
				}
			}
		}
		if (best == null) return Optional.empty();
		BlockPos corner = new BlockPos(best.x() - size.getX() / 2, best.ground() + 1, best.z() - size.getZ() / 2);
		Rotation rotation = Rotation.getRandom(context.random());
		return Optional.of(new GenerationStub(new BlockPos(best.x(), best.ground(), best.z()),
				builder -> builder.addPiece(new LostCastlePiece(context.structureTemplateManager(), corner, rotation))));
	}

	private static int kind(BlockState state) {
		if (state.isAir()) return DuergarVillageLogic.AIR;
		if (!state.getFluidState().isEmpty()) return DuergarVillageLogic.OTHER;
		return DuergarVillageLogic.SOLID;
	}

	/** Every chunk the stone bank round the castle touches takes part. */
	@Override
	public BoundingBox adjustBoundingBox(BoundingBox box) {
		return super.adjustBoundingBox(box).inflatedBy(GroundWork.BANK, 0, GroundWork.BANK);
	}

	@Override
	public void afterPlace(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox chunkBox, ChunkPos chunk, PiecesContainer pieces) {
		for (StructurePiece piece : pieces.pieces()) {
			if (!(piece instanceof LostCastlePiece castle)) continue;
			BoundingBox footprint = castle.templateBox();
			BlockState stone = Blocks.STONE.defaultBlockState();
			// Stone under the castle down to the floor, and banked round it out to where the floor is.
			GroundWork.bank(level, chunkBox, footprint, footprint.minY() - 1, true, stone, stone);
		}
	}

	@Override
	public StructureType<?> type() {
		return LostCastleModule.CASTLE;
	}
}
