package io.github.hellenter.eam.world.structures.crypt;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The crypt monument. {@code eam:crypt} generates by abandoned villages (see {@link CryptStructure.VillageRule});
 * {@code eam:crypt_test} is the same crypt for {@code /place structure} anywhere.
 * <p>
 * Rooms sit on a grid of 15x15 cells, one level is 12 blocks high, and doors are 3x3 openings in the middle of a cell
 * side (blocks 6..8 counted from 0), one block above the floor. Big rooms take 2x2 cells with a door in the middle of
 * each cell side. Stairs lead down level by level; the last level ends in the boss room of 3x3 cells with its doors in
 * the middle of its sides.
 */
public final class CryptModule {
	public static final int CELL = 15;
	public static final int DOOR_WIDTH = 3;
	public static final int DOOR_HEIGHT = 3;
	public static final int LEVEL_HEIGHT = 12;
	/** The boss room takes 3x3 cells. */
	public static final int BOSS_SIZE = 3 * CELL;
	/** Boss room templates are named {@code eam:crypt/boss/<n>}. */
	public static final String BOSS_TEMPLATES = Eam.MOD_ID + ":crypt/boss/";

	public static final ResourceKey<Structure> STRUCTURE = ResourceKey.create(Registries.STRUCTURE, Eam.id("crypt"));
	/** The same crypt without the village rule, for {@code /place structure}. */
	public static final ResourceKey<Structure> TEST_STRUCTURE = ResourceKey.create(Registries.STRUCTURE, Eam.id("crypt_test"));
	/** Places crypts on the same grid as villages, so a crypt start can look for its village in the same chunk. */
	public static final ResourceKey<StructureSet> STRUCTURE_SET = ResourceKey.create(Registries.STRUCTURE_SET, Eam.id("crypts"));
	public static final Identifier TIER_1_CHEST = Eam.id("chests/crypt/tier_1");
	public static final Identifier TIER_2_CHEST = Eam.id("chests/crypt/tier_2");
	public static final ResourceKey<LootTable> TIER_1_LOOT = ResourceKey.create(Registries.LOOT_TABLE, TIER_1_CHEST);
	public static final ResourceKey<LootTable> TIER_2_LOOT = ResourceKey.create(Registries.LOOT_TABLE, TIER_2_CHEST);

	public static final StructureType<CryptStructure> CRYPT =
			Registry.register(BuiltInRegistries.STRUCTURE_TYPE, Eam.id("crypt"), () -> CryptStructure.CODEC);
	public static final StructurePieceType ROOM_PIECE =
			Registry.register(BuiltInRegistries.STRUCTURE_PIECE, Eam.id("crypt_room"), CryptRoomPiece::new);

	private CryptModule() {
	}

	/** Where the door place starts along a side {@code size} blocks long: doors sit in the middle of it. */
	public static int doorFrom(int size) {
		return size / 2 - DOOR_WIDTH / 2;
	}

	/**
	 * Where the door place of a big room's door starts along its side, for a door bit of {@link CryptLayout.Big#doors}:
	 * in the middle of the side of one of its cells.
	 */
	public static int bigDoorFrom(int door) {
		int side = CryptLayout.bigDoorSide(door);
		// Clockwise, the far half of the north and east sides lies further along x or z; of the south and west, nearer.
		boolean far = CryptLayout.bigDoorFarHalf(door) == (side == CryptLayout.NORTH || side == CryptLayout.EAST);
		return (far ? CELL : 0) + doorFrom(CELL);
	}

	/**
	 * A block on the {@code side} of a square room whose last block is {@code last}, {@code along} blocks along that side
	 * (along x for the north and south sides, along z for the others), {@code y} above the room's floor.
	 */
	public static BlockPos sidePos(int side, int along, int y, int last) {
		return switch (side) {
			case CryptLayout.NORTH -> new BlockPos(along, y, 0);
			case CryptLayout.SOUTH -> new BlockPos(along, y, last);
			case CryptLayout.WEST -> new BlockPos(0, y, along);
			default -> new BlockPos(last, y, along);
		};
	}

	/** Registers the structure and piece types, and wakes the boss when a player walks into his room. */
	public static void register() {
		CryptBoss.register();
	}
}
