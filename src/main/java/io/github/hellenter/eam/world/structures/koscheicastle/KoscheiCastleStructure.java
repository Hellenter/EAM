package io.github.hellenter.eam.world.structures.koscheicastle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.hellenter.eam.dimension.UnderworldLogic;
import io.github.hellenter.eam.world.structures.GroundWork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;

import java.util.Optional;

/**
 * Koschei's castle: Maxim's template on the floor of the underworld's ice caves, in the middle of a patch of them, for
 * {@code chance} of the patches. Its foot is sunk into the highest floor under its footprint, with stone and snow filled in
 * round the foot and banked around it, and clears the ice spires within {@code clearRadius} blocks so none is cut off by its walls. It generates
 * after the spires, so each chunk clears what grew there and what spires of neighbouring chunks reach into it.
 */
public class KoscheiCastleStructure extends Structure {
	public static final MapCodec<KoscheiCastleStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			settingsCodec(instance),
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("chance", 1.0F).forGetter(castle -> castle.chance),
			Codec.intRange(0, 24).optionalFieldOf("clear_radius", 12).forGetter(castle -> castle.clearRadius)
	).apply(instance, KoscheiCastleStructure::new));

	/**
	 * The template's bottom layers that go into the ground. The template leaves out their air, so the ground round the
	 * foot stays; where the ground is lower, stone and snow fill it up to the top of the foot.
	 */
	public static final int BURIED = 4;
	/** Blocks between two floor samples across the footprint. */
	private static final int STEP = 8;
	/** How far an ice spire grown in a neighbouring chunk may reach into this one; each chunk clears this far past its edges. */
	private static final int SPIKE_REACH = 8;
	/** Spires are cleared this far above the castle's top too, so none is left standing on its roof. */
	private static final int CLEAR_ABOVE = 8;

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
				ground = Math.max(ground, floor(context, spot[0] + dx, spot[1] + dz));
			}
		}
		if (ground == Integer.MIN_VALUE) return Optional.empty();
		BlockPos corner = new BlockPos(spot[0] - size.getX() / 2, ground + 1 - BURIED, spot[1] - size.getZ() / 2);
		Rotation rotation = Rotation.getRandom(context.random());
		return Optional.of(new GenerationStub(new BlockPos(spot[0], ground, spot[1]),
				builder -> builder.addPiece(new KoscheiCastlePiece(context.structureTemplateManager(), corner, rotation))));
	}

	/**
	 * The top of the floor of the upper level's lowest cave at {@code x}, {@code z}: in the ice caves, the floor of the
	 * great hall. {@link Integer#MIN_VALUE} if the column is rock all the way up.
	 */
	private static int floor(GenerationContext context, int x, int z) {
		NoiseColumn column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
		for (int y = UnderworldLogic.BAND_TOP + 1; y < context.heightAccessor().getMaxY(); y++) {
			if (column.getBlock(y).isAir() && !column.getBlock(y - 1).isAir()) return y - 1;
		}
		return Integer.MIN_VALUE;
	}

	/** Every chunk the clearing and the bank touch, and those whose spires may reach into them, takes part. */
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
			int ground = footprint.minY() + BURIED - 1;
			clearSpires(level, chunkBox, footprint, ground);
			fillFoot(level, chunkBox, footprint, ground);
			// The floor round the plinth's foot is snow over rock.
			GroundWork.bank(level, chunkBox, footprint, ground, true, Blocks.SNOW_BLOCK.defaultBlockState(), Blocks.STONE.defaultBlockState());
		}
	}

	/**
	 * Removes the ice of the spires, and the snow on them, from the columns round the castle, from a little over its top
	 * down to the ground its foot is sunk into, and over the castle down to its top. The castle's own ice stays, and so
	 * does the hall's floor.
	 */
	private void clearSpires(WorldGenLevel level, BoundingBox chunkBox, BoundingBox footprint, int ground) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int top = footprint.maxY() + CLEAR_ABOVE;
		for (int x = Math.max(chunkBox.minX() - SPIKE_REACH, footprint.minX() - clearRadius); x <= Math.min(chunkBox.maxX() + SPIKE_REACH, footprint.maxX() + clearRadius); x++) {
			for (int z = Math.max(chunkBox.minZ() - SPIKE_REACH, footprint.minZ() - clearRadius); z <= Math.min(chunkBox.maxZ() + SPIKE_REACH, footprint.maxZ() + clearRadius); z++) {
				boolean over = x >= footprint.minX() && x <= footprint.maxX() && z >= footprint.minZ() && z <= footprint.maxZ();
				int bottom = over ? footprint.maxY() + 1 : ground + 1;
				for (int y = top; y >= bottom; y--) {
					pos.set(x, y, z);
					BlockState state = level.getBlockState(pos);
					if (state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE) || state.is(Blocks.SNOW)) {
						level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
		}
	}

	/** Fills the hollows round the sunk foot, inside the footprint, with stone and snow up to {@code ground}. */
	private static void fillFoot(WorldGenLevel level, BoundingBox chunkBox, BoundingBox footprint, int ground) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = Math.max(chunkBox.minX(), footprint.minX()); x <= Math.min(chunkBox.maxX(), footprint.maxX()); x++) {
			for (int z = Math.max(chunkBox.minZ(), footprint.minZ()); z <= Math.min(chunkBox.maxZ(), footprint.maxZ()); z++) {
				for (int y = footprint.minY(); y <= ground; y++) {
					pos.set(x, y, z);
					BlockState state = level.getBlockState(pos);
					if (state.isAir() || state.is(BlockTags.REPLACEABLE)) {
						level.setBlock(pos, (y == ground ? Blocks.SNOW_BLOCK : Blocks.STONE).defaultBlockState(), Block.UPDATE_CLIENTS);
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
