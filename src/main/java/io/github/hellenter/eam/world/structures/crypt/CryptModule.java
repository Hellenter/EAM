package io.github.hellenter.eam.world.structures.crypt;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The crypt monument. For now it only exists through {@code /place structure eam:crypt}; it will spawn by abandoned
 * villages later.
 * <p>
 * Rooms sit on a grid of 15x15 cells, one level is 12 blocks high, and doors are 3x3 openings in the middle of a cell
 * side (blocks 6..8 counted from 0), one block above the floor. Stairs lead down to a second level, which ends in the
 * boss room of 3x3 cells with its doors in the middle of its sides.
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

	/** Registers the structure and piece types, and wakes the boss when a player walks into his room. */
	public static void register() {
		CryptBoss.register();
	}
}
