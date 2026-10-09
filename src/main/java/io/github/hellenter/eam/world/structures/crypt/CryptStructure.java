package io.github.hellenter.eam.world.structures.crypt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.hellenter.eam.world.structures.crypt.CryptLayout.Cell;
import io.github.hellenter.eam.world.structures.crypt.CryptLayout.Fit;
import io.github.hellenter.eam.world.structures.crypt.CryptLayout.Room;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Map;
import java.util.Optional;

/**
 * A crypt: an entrance on the surface whose stairs lead down to one level of rooms laid out by {@link CryptLayout}.
 * The rooms are Maxim's templates under {@code eam:crypt/}, turned to fit their cells.
 *
 * @param rooms          how many rooms the main path tries to have
 * @param branches       how many side passages it tries to have
 * @param entranceGround the entrance template's layer that sits at ground level
 */
public class CryptStructure extends Structure {
	public static final MapCodec<CryptStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			settingsCodec(instance),
			IntProviders.codec(2, 32).fieldOf("rooms").forGetter(crypt -> crypt.rooms),
			IntProviders.codec(0, 8).fieldOf("branches").forGetter(crypt -> crypt.branches),
			Codec.INT.fieldOf("entrance_ground").forGetter(crypt -> crypt.entranceGround)
	).apply(instance, CryptStructure::new));

	/** How far above a piece's box the world still raises terrain to wall the crypt in. */
	private static final int SHAPE_REACH = 12;

	private final IntProvider rooms;
	private final IntProvider branches;
	private final int entranceGround;

	public CryptStructure(StructureSettings settings, IntProvider rooms, IntProvider branches, int entranceGround) {
		super(settings);
		this.rooms = rooms;
		this.branches = branches;
		this.entranceGround = entranceGround;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		CryptPieces pieces = CryptPieces.load(context.structureTemplateManager());
		if (pieces.entrances().isEmpty() || pieces.rooms().isEmpty()) return Optional.empty();
		ChunkPos chunk = context.chunkPos();
		RandomSource random = context.random();
		CryptPieces.Entrance entrance = pieces.entrances().get(random.nextInt(pieces.entrances().size()));
		int x = chunk.getMinBlockX();
		int z = chunk.getMinBlockZ();
		int ground = surface(context, x, z, Heightmap.Types.WORLD_SURFACE_WG);
		// The entrance stands on dry land: under water the stairs would flood the whole crypt.
		if (surface(context, x, z, Heightmap.Types.OCEAN_FLOOR_WG) < ground) return Optional.empty();
		BlockPos origin = new BlockPos(x, ground - entranceGround, z);
		CryptLayout layout = CryptLayout.generate(random::nextInt, rooms.sample(random), branches.sample(random));
		return Optional.of(new GenerationStub(origin, builder -> build(builder, context.structureTemplateManager(), pieces,
				entrance, origin, layout, random, entranceGround)));
	}

	private static int surface(GenerationContext context, int x, int z, Heightmap.Types heightmap) {
		return context.chunkGenerator().getFirstOccupiedHeight(x + CryptModule.CELL / 2, z + CryptModule.CELL / 2, heightmap,
				context.heightAccessor(), context.randomState());
	}

	private static void build(StructurePiecesBuilder builder, StructureTemplateManager manager, CryptPieces pieces,
			CryptPieces.Entrance entrance, BlockPos origin, CryptLayout layout, RandomSource random, int entranceGround) {
		builder.addPiece(new CryptRoomPiece(manager, entrance.id(), origin, Rotation.NONE, 0, Math.max(1, entranceGround - SHAPE_REACH)));
		// Cell (0, 0) lies north of the entrance, its south door in line with the entrance's way out.
		BlockPos firstCell = origin.offset(entrance.exitX() - CryptModule.CELL / 2, 0, -CryptModule.CELL);
		for (Map.Entry<Cell, Room> entry : layout.rooms().entrySet()) {
			Room room = entry.getValue();
			Fit<Identifier> fit = CryptLayout.fit(pieces.rooms(), room.role(), room.doors(), random::nextInt);
			if (fit == null) continue;
			Cell cell = entry.getKey();
			BlockPos position = firstCell.offset(cell.x() * CryptModule.CELL, 0, cell.z() * CryptModule.CELL);
			builder.addPiece(new CryptRoomPiece(manager, fit.id(), position, Rotation.values()[fit.quarters()], fit.extraDoors(), 0));
		}
	}

	@Override
	public StructureType<?> type() {
		return CryptModule.CRYPT;
	}
}
