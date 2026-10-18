package io.github.hellenter.eam.world.structures.castle;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;

/**
 * The Pillager King's castle, read out of Maxim's world: a keep on a hill with a bridge over a dry moat to a gatehouse,
 * held by pillagers, pillager guards and elite crossbowmen, with the King in his hall. It stands in plains and forests,
 * far rarer than outposts, whose red chests hold the map to it. Found with {@code /locate structure eam:pillager_king_castle}.
 */
public final class CastleModule {
	/** The castle is too big for one structure block, so it is kept as tiles, each with its corner in the whole castle. */
	public record Tile(Identifier template, Vec3i offset) {
	}

	/** The whole castle's size: 80 blocks east to west (its gate on the east), 73 high, 59 north to south. */
	public static final Vec3i SIZE = new Vec3i(80, 73, 59);
	public static final List<Tile> TILES = List.of(
			tile(0, 0, 0, 0), tile(0, 1, 0, 30),
			tile(1, 0, 27, 0), tile(1, 1, 27, 30),
			tile(2, 0, 54, 0), tile(2, 1, 54, 30));

	public static final ResourceKey<Structure> STRUCTURE = ResourceKey.create(Registries.STRUCTURE, Eam.id("pillager_king_castle"));
	public static final ResourceKey<StructureSet> STRUCTURE_SET = ResourceKey.create(Registries.STRUCTURE_SET, Eam.id("pillager_king_castles"));
	public static final TagKey<Biome> HAS_CASTLE = TagKey.create(Registries.BIOME, Eam.id("has_structure/pillager_king_castle"));

	/** Lime stair markers: barrels of food. */
	public static final ResourceKey<LootTable> FOOD_LOOT = loot("food");
	/** Yellow stair markers: arrows, potions and shields. */
	public static final ResourceKey<LootTable> ARMS_LOOT = loot("arms");
	/** Green stair markers: the King's treasure. */
	public static final ResourceKey<LootTable> TREASURE_LOOT = loot("treasure");

	public static final StructureType<CastleStructure> CASTLE =
			Registry.register(BuiltInRegistries.STRUCTURE_TYPE, Eam.id("pillager_king_castle"), () -> CastleStructure.CODEC);
	public static final StructurePieceType PIECE =
			Registry.register(BuiltInRegistries.STRUCTURE_PIECE, Eam.id("pillager_king_castle"), CastlePiece::new);

	static {
		Registry.register(BuiltInRegistries.STRUCTURE_PLACEMENT, Eam.id("castle_spread"), CastlePlacement.CODEC);
	}

	private CastleModule() {
	}

	private static Tile tile(int column, int row, int x, int z) {
		return new Tile(Eam.id("pillager_king_castle/part_" + column + "_" + row), new Vec3i(x, 0, z));
	}

	private static ResourceKey<LootTable> loot(String name) {
		return ResourceKey.create(Registries.LOOT_TABLE, Eam.id("chests/pillager_king_castle/" + name));
	}

	/** Loads the structure, piece and placement types. */
	public static void register() {
	}
}
