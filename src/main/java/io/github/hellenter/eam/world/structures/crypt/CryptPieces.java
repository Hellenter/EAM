package io.github.hellenter.eam.world.structures.crypt;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.world.structures.crypt.CryptLayout.Piece;
import io.github.hellenter.eam.world.structures.crypt.CryptLayout.Role;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The crypt's room templates, found by name: {@code eam:crypt/<kind>/<n>}. Each room's doors are read from the template
 * itself, as the door places on its sides that are open (air).
 */
public record CryptPieces(List<Piece<Identifier>> rooms, List<Entrance> entrances, List<Stairs> stairs, List<Identifier> bosses) {
	private static final String FOLDER = "crypt/";

	/** An entrance template and where its way into the crypt is: the open middle of its north side, at the floor. */
	public record Entrance(Identifier id, int exitX) {
	}

	/** Stairs between two levels: one cell, two levels high, with doors on both. */
	public record Stairs(Identifier id, int lowerDoors, int upperDoors) {
	}

	public static CryptPieces load(StructureTemplateManager manager) {
		List<Piece<Identifier>> rooms = new ArrayList<>();
		List<Entrance> entrances = new ArrayList<>();
		List<Stairs> stairs = new ArrayList<>();
		List<Identifier> bosses = new ArrayList<>();
		manager.listTemplates()
				.filter(id -> id.getNamespace().equals(Eam.MOD_ID) && id.getPath().startsWith(FOLDER))
				.sorted()
				.forEach(id -> {
					String kind = id.getPath().substring(FOLDER.length()).split("/")[0];
					StructureTemplate template = manager.getOrCreate(id);
					Set<BlockPos> air = air(template);
					Role role = switch (kind) {
						case "corridor" -> Role.CORRIDOR;
						case "fight" -> Role.FIGHT;
						case "treasury" -> Role.TREASURY;
						default -> null;
					};
					Vec3i size = template.getSize();
					boolean oneCell = size.getX() == CryptModule.CELL && size.getZ() == CryptModule.CELL;
					if (role != null && oneCell) {
						int doors = doors(air, CryptModule.CELL, 0);
						if (doors != 0) rooms.add(new Piece<>(id, role, doors));
					} else if (kind.equals("stairs") && oneCell && size.getY() > CryptModule.LEVEL_HEIGHT) {
						int lower = doors(air, CryptModule.CELL, 0);
						int upper = doors(air, CryptModule.CELL, CryptModule.LEVEL_HEIGHT);
						if (lower != 0 && upper != 0) stairs.add(new Stairs(id, lower, upper));
					} else if (kind.equals("boss") && size.getX() == CryptModule.BOSS_SIZE && size.getZ() == CryptModule.BOSS_SIZE) {
						bosses.add(id);
					} else if (kind.equals("entrance")) {
						int exit = entranceExit(air, size.getX());
						if (exit >= 0) entrances.add(new Entrance(id, exit));
					}
				});
		return new CryptPieces(rooms, entrances, stairs, bosses);
	}

	/** Where the template is open: air, plus markers that usually turn into air. */
	private static Set<BlockPos> air(StructureTemplate template) {
		Set<BlockPos> air = new HashSet<>();
		for (var info : template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.AIR)) air.add(info.pos());
		// Skull markers are air most of the time, so a doorway with one still counts as open.
		for (Block marker : new Block[] {Blocks.STAINED_GLASS.red(), Blocks.STAINED_GLASS.yellow()}) {
			for (var info : template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), marker)) air.add(info.pos());
		}
		return air;
	}

	/** A side has a door when most of its 3x3 door place, on the level whose floor is at {@code floor}, is open. */
	static int doors(Set<BlockPos> air, int size, int floor) {
		int last = size - 1;
		int from = CryptModule.doorFrom(size);
		int doors = 0;
		int[][] sides = {{CryptLayout.NORTH, 0}, {CryptLayout.SOUTH, last}, {CryptLayout.WEST, 0}, {CryptLayout.EAST, last}};
		for (int[] side : sides) {
			int open = 0;
			for (int along = from; along < from + CryptModule.DOOR_WIDTH; along++) {
				for (int y = floor + 1; y <= floor + CryptModule.DOOR_HEIGHT; y++) {
					boolean northSouth = side[0] == CryptLayout.NORTH || side[0] == CryptLayout.SOUTH;
					BlockPos pos = northSouth ? new BlockPos(along, y, side[1]) : new BlockPos(side[1], y, along);
					if (air.contains(pos)) open++;
				}
			}
			if (open * 2 > CryptModule.DOOR_WIDTH * CryptModule.DOOR_HEIGHT) doors |= side[0];
		}
		return doors;
	}

	/** The middle of the open stretch on the entrance's north side at floor level, or -1 when it has none. */
	static int entranceExit(Set<BlockPos> air, int width) {
		int from = -1;
		int to = -1;
		for (int x = 0; x < width; x++) {
			if (air.contains(new BlockPos(x, 1, 0)) && air.contains(new BlockPos(x, 2, 0))) {
				if (from < 0) from = x;
				to = x;
			}
		}
		return from < 0 ? -1 : (from + to) / 2;
	}
}
