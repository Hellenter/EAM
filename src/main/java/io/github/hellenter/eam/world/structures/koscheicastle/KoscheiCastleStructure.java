package io.github.hellenter.eam.world.structures.koscheicastle;

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
 * Koschei's castle: Maxim's template in the middle of an ice spikes patch, for {@code chance} of the patches. It stands
 * on the highest ground of its footprint with snow banked under and around it, and clears the ice spikes within
 * {@code clearRadius} blocks so none is cut off by its walls. It generates after the spikes, so each chunk clears what
 * grew there and what spikes of neighbouring chunks reach into it.
 */
public class KoscheiCastleStructure extends Structure {
	public static final MapCodec<KoscheiCastleStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			settingsCodec(instance),
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("chance", 0.5F).forGetter(castle -> castle.chance),
			Codec.intRange(0, 24).optionalFieldOf("clear_radius", 12).forGetter(castle -> castle.clearRadius)
	).apply(instance, KoscheiCastleStructure::new));

	/** Blocks between two ground samples across the footprint. */
	private static final int STEP = 8;
	/** How far an ice spike grown in a neighbouring chunk may reach into this one; each chunk clears this far past its edges. */
	private static final int SPIKE_REACH = 8;

	private final float chance;
	private final int clearRadius;

	public KoscheiCastleStructure(StructureSettings settings, float chance, int clearRadius) {
		super(settings);
		this.chance = chance;
		this.clearRadius = clearRadius;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		KoscheiCastleLogic.Patch patch = KoscheiCastlePlacement.patch(context.seed(), context.chunkPos());
		if (patch == null || !KoscheiCastleLogic.chosen(context.seed(), patch.middleX(), patch.middleZ(), chance)) return Optional.empty();
		int[] spot = patch.block();
		if (!context.chunkPos().contains(new BlockPos(spot[0], 0, spot[1]))) return Optional.empty();
		Vec3i size = context.structureTemplateManager().getOrCreate(KoscheiCastleModule.TEMPLATE).getSize();
		int ground = Integer.MIN_VALUE;
		for (int dx = -size.getX() / 2; dx <= size.getX() / 2; dx += STEP) {
			for (int dz = -size.getZ() / 2; dz <= size.getZ() / 2; dz += STEP) {
				ground = Math.max(ground, context.chunkGenerator().getFirstOccupiedHeight(spot[0] + dx, spot[1] + dz,
						Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState()));
			}
		}
		BlockPos corner = new BlockPos(spot[0] - size.getX() / 2, ground + 1, spot[1] - size.getZ() / 2);
		Rotation rotation = Rotation.getRandom(context.random());
		return Optional.of(new GenerationStub(new BlockPos(spot[0], ground, spot[1]),
				builder -> builder.addPiece(new KoscheiCastlePiece(context.structureTemplateManager(), corner, rotation))));
	}

	/** Every chunk the clearing and the bank touch, and those whose spikes may reach into them, takes part. */
	@Override
	public BoundingBox adjustBoundingBox(BoundingBox box) {
		int reach = Math.max(clearRadius, GroundWork.BANK) + SPIKE_REACH;
		return super.adjustBoundingBox(box).inflatedBy(reach, 0, reach);
	}

	@Override
	public void afterPlace(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox chunkBox, ChunkPos chunk, PiecesContainer pieces) {
		for (StructurePiece piece : pieces.pieces()) {
			if (!(piece instanceof KoscheiCastlePiece castle)) continue;
			BoundingBox footprint = castle.templateBox();
			clearSpikes(level, chunkBox, footprint);
			// The plinth's foot stands on the ground: the land round it is open snow.
			GroundWork.bank(level, chunkBox, footprint, footprint.minY() - 1, true, Blocks.SNOW_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState());
		}
	}

	/**
	 * Removes the packed ice of the spikes, and the snow on them, from the columns round the castle down to its foot,
	 * and over the castle down to its top. The castle's own ice stays, and so does the ice of the ground below its foot.
	 */
	private void clearSpikes(WorldGenLevel level, BoundingBox chunkBox, BoundingBox footprint) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = Math.max(chunkBox.minX() - SPIKE_REACH, footprint.minX() - clearRadius); x <= Math.min(chunkBox.maxX() + SPIKE_REACH, footprint.maxX() + clearRadius); x++) {
			for (int z = Math.max(chunkBox.minZ() - SPIKE_REACH, footprint.minZ() - clearRadius); z <= Math.min(chunkBox.maxZ() + SPIKE_REACH, footprint.maxZ() + clearRadius); z++) {
				boolean over = x >= footprint.minX() && x <= footprint.maxX() && z >= footprint.minZ() && z <= footprint.maxZ();
				int bottom = over ? footprint.maxY() + 1 : footprint.minY();
				for (int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1; y >= bottom; y--) {
					pos.set(x, y, z);
					BlockState state = level.getBlockState(pos);
					if (state.is(Blocks.PACKED_ICE) || state.is(Blocks.SNOW) || state.isAir()) {
						if (!state.isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					} else {
						break;
					}
				}
			}
		}
	}

	@Override
	public StructureType<?> type() {
		return KoscheiCastleModule.CASTLE;
	}
}
