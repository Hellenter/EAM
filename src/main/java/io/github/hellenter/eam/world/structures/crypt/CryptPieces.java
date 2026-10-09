package io.github.hellenter.eam.world.structures.crypt;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.world.structures.crypt.CryptLayout.Piece;
import io.github.hellenter.eam.world.structures.crypt.CryptLayout.Role;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
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
public record CryptPieces(List<Piece<Identifier>> rooms, List<Entrance> entrances) {
	private static final String FOLDER = "crypt/";

	/** An entrance template and where its way into the crypt is: the open middle of its north side, at the floor. */
	public record Entrance(Identifier id, int exitX) {
	}

	public static CryptPieces load(StructureTemplateManager manager) {
		List<Piece<Identifier>> rooms = new ArrayList<>();
		List<Entrance> entrances = new ArrayList<>();
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
					if (role != null && size.getX() == CryptModule.CELL && size.getZ() == CryptModule.CELL) {
						int doors = doors(air);
						if (doors != 0) rooms.add(new Piece<>(id, role, doors));
					} else if (kind.equals("entrance")) {
						int exit = entranceExit(air, size.getX());
						if (exit >= 0) entrances.add(new Entrance(id, exit));
					}
				});
		return new CryptPieces(rooms, entrances);
	}

	private static Set<BlockPos> air(StructureTemplate template) {
		Set<BlockPos> air = new HashSet<>();
		for (var info : template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.AIR)) air.add(info.pos());
		return air;
	}

	/** A side has a door when most of its 3x3 door place, on the lowest level, is open. */
	static int doors(Set<BlockPos> air) {
		int last = CryptModule.CELL - 1;
		int doors = 0;
		int[][] sides = {{CryptLayout.NORTH, 0}, {CryptLayout.SOUTH, last}, {CryptLayout.WEST, 0}, {CryptLayout.EAST, last}};
		for (int[] side : sides) {
			int open = 0;
			for (int along = CryptModule.DOOR_FROM; along < CryptModule.DOOR_FROM + CryptModule.DOOR_WIDTH; along++) {
				for (int y = 1; y <= CryptModule.DOOR_HEIGHT; y++) {
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
