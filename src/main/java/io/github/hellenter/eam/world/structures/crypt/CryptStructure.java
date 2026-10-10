package io.github.hellenter.eam.world.structures.crypt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.hellenter.eam.world.structures.crypt.CryptLayout.Cell;
import io.github.hellenter.eam.world.structures.crypt.CryptLayout.Fit;
import io.github.hellenter.eam.world.structures.crypt.CryptLayout.Piece;
import io.github.hellenter.eam.world.structures.crypt.CryptLayout.Role;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A crypt: an entrance on the surface whose stairs lead down to the first level of rooms laid out by
 * {@link CryptLayout}; that level's stairs lead to the second, which ends in the boss room. The rooms are Maxim's
 * templates under {@code eam:crypt/}, turned to fit their cells.
 *
 * @param rooms          how many rooms the main path of each level tries to have
 * @param branches       how many side passages each level tries to have
 * @param entranceGround the entrance template's layer that sits at ground level
 * @param deepRooms      room templates that only appear from the second level down
 */
public class CryptStructure extends Structure {
	public static final MapCodec<CryptStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			settingsCodec(instance),
			IntProviders.codec(2, 32).fieldOf("rooms").forGetter(crypt -> crypt.rooms),
			IntProviders.codec(0, 8).fieldOf("branches").forGetter(crypt -> crypt.branches),
			Codec.INT.fieldOf("entrance_ground").forGetter(crypt -> crypt.entranceGround),
			Identifier.CODEC.listOf().optionalFieldOf("deep_rooms", List.of()).forGetter(crypt -> crypt.deepRooms)
	).apply(instance, CryptStructure::new));

	/** How far above a piece's box the world still raises terrain to wall the crypt in. */
	private static final int SHAPE_REACH = 12;

	private final IntProvider rooms;
	private final IntProvider branches;
	private final int entranceGround;
	private final List<Identifier> deepRooms;

	public CryptStructure(StructureSettings settings, IntProvider rooms, IntProvider branches, int entranceGround,
			List<Identifier> deepRooms) {
		super(settings);
		this.rooms = rooms;
		this.branches = branches;
		this.entranceGround = entranceGround;
		this.deepRooms = deepRooms;
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
		CryptLayout first = CryptLayout.generate(random::nextInt, rooms.sample(random), branches.sample(random));
		CryptLayout second = CryptLayout.generateBelow(first, random::nextInt, rooms.sample(random), branches.sample(random));
		return Optional.of(new GenerationStub(origin, builder -> build(builder, context.structureTemplateManager(), pieces,
				entrance, origin, List.of(first, second), random)));
	}

	private static int surface(GenerationContext context, int x, int z, Heightmap.Types heightmap) {
		return context.chunkGenerator().getFirstOccupiedHeight(x + CryptModule.CELL / 2, z + CryptModule.CELL / 2, heightmap,
				context.heightAccessor(), context.randomState());
	}

	private void build(StructurePiecesBuilder builder, StructureTemplateManager manager, CryptPieces pieces,
			CryptPieces.Entrance entrance, BlockPos origin, List<CryptLayout> levels, RandomSource random) {
		builder.addPiece(new CryptRoomPiece(manager, entrance.id(), origin, Rotation.NONE, 0, 0, Math.max(1, entranceGround - SHAPE_REACH)));
		// Cell (0, 0) lies north of the entrance, its south door in line with the entrance's way out.
		BlockPos firstCell = origin.offset(entrance.exitX() - CryptModule.CELL / 2, 0, -CryptModule.CELL);
		// Without a stairs template the crypt keeps to its first level.
		int depth = pieces.stairs().isEmpty() ? 1 : levels.size();
		for (int level = 0; level < depth; level++) {
			CryptLayout layout = levels.get(level);
			boolean deep = level > 0;
			List<Piece<Identifier>> templates = pieces.rooms().stream().filter(piece -> deep || !deepRooms.contains(piece.id())).toList();
			BlockPos levelCorner = firstCell.below(level * CryptModule.LEVEL_HEIGHT);
			for (Map.Entry<Cell, Room> entry : layout.rooms().entrySet()) {
				Cell cell = entry.getKey();
				Room room = entry.getValue();
				BlockPos position = levelCorner.offset(cell.x() * CryptModule.CELL, 0, cell.z() * CryptModule.CELL);
				Role role = room.role();
				if (role == Role.STAIRS) {
					// The stairs are one piece two levels high, put in from the level above.
					if (level + 1 < depth) {
						addStairs(builder, manager, pieces, position.below(CryptModule.LEVEL_HEIGHT),
								levels.get(level + 1).rooms().get(cell).doors(), room.doors(), random);
						continue;
					}
					if (deep) continue;
					role = Role.TREASURY;
				}
				Fit<Identifier> fit = CryptLayout.fit(templates, role, room.doors(), random::nextInt);
				if (fit == null) continue;
				builder.addPiece(new CryptRoomPiece(manager, fit.id(), position, Rotation.values()[fit.quarters()], fit.extraDoors(), 0, 0));
			}
			CryptLayout.Boss boss = layout.boss();
			if (boss != null && !pieces.bosses().isEmpty()) {
				Identifier id = pieces.bosses().get(random.nextInt(pieces.bosses().size()));
				BlockPos position = levelCorner.offset((boss.middle().x() - 1) * CryptModule.CELL, 0, (boss.middle().z() - 1) * CryptModule.CELL);
				builder.addPiece(new CryptRoomPiece(manager, id, position, Rotation.values()[random.nextInt(4)], ~boss.door() & 0xF, 0, 0));
			}
		}
	}

	/** Puts in stairs turned so they have the doors both levels need, with the rest walled up. */
	private static void addStairs(StructurePiecesBuilder builder, StructureTemplateManager manager, CryptPieces pieces,
			BlockPos position, int lowerDoors, int upperDoors, RandomSource random) {
		CryptPieces.Stairs stairs = pieces.stairs().get(random.nextInt(pieces.stairs().size()));
		int first = random.nextInt(4);
		for (int k = 0; k < 4; k++) {
			int quarters = (first + k) % 4;
			int lower = CryptLayout.rotate(stairs.lowerDoors(), quarters);
			int upper = CryptLayout.rotate(stairs.upperDoors(), quarters);
			if ((lower & lowerDoors) != lowerDoors || (upper & upperDoors) != upperDoors) continue;
			builder.addPiece(new CryptRoomPiece(manager, stairs.id(), position, Rotation.values()[quarters],
					lower & ~lowerDoors, upper & ~upperDoors, 0));
			return;
		}
	}

	@Override
	public StructureType<?> type() {
		return CryptModule.CRYPT;
	}
}
