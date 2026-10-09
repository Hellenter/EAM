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
 * side (blocks 6..8 counted from 0), one block above the floor.
 */
public final class CryptModule {
	public static final int CELL = 15;
	public static final int DOOR_FROM = 6;
	public static final int DOOR_WIDTH = 3;
	public static final int DOOR_HEIGHT = 3;

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

	/** Loads the class, which registers the structure and piece types. */
	public static void register() {
	}
}
