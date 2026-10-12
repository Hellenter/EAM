package io.github.hellenter.eam.world.structures.outpost;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.Registry;
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

/**
 * The pillager outpost built from Maxim's template {@code eam:pillager_outpost}. It replaces the vanilla outpost in
 * plains and forests, clears the trees around it, and keeps a garrison of pillagers, pillager guards and elite
 * crossbowmen standing where Maxim put wool markers. The red chest holds the map to the Pillager King's castle.
 */
public final class OutpostModule {
	public static final Identifier TEMPLATE = Eam.id("pillager_outpost");

	public static final ResourceKey<Structure> STRUCTURE = ResourceKey.create(Registries.STRUCTURE, Eam.id("pillager_outpost"));
	public static final ResourceKey<StructureSet> STRUCTURE_SET = ResourceKey.create(Registries.STRUCTURE_SET, Eam.id("pillager_outposts"));
	public static final TagKey<Biome> HAS_OUTPOST = TagKey.create(Registries.BIOME, Eam.id("has_structure/pillager_outpost"));
	/** Where the castle map leads. Empty until the castle is built. */
	public static final TagKey<Structure> KING_CASTLE = TagKey.create(Registries.STRUCTURE, Eam.id("pillager_king_castle"));

	public static final ResourceKey<LootTable> ARROWS_LOOT = loot("arrows");
	public static final ResourceKey<LootTable> BASIC_LOOT = loot("basic");
	public static final ResourceKey<LootTable> BETTER_LOOT = loot("better");
	public static final ResourceKey<LootTable> SPECIAL_LOOT = loot("special");
	public static final ResourceKey<LootTable> BARREL_LOOT = loot("barrel");

	public static final StructureType<OutpostStructure> OUTPOST =
			Registry.register(BuiltInRegistries.STRUCTURE_TYPE, Eam.id("pillager_outpost"), () -> OutpostStructure.CODEC);
	public static final StructurePieceType PIECE =
			Registry.register(BuiltInRegistries.STRUCTURE_PIECE, Eam.id("pillager_outpost"), OutpostPiece::new);

	private OutpostModule() {
	}

	private static ResourceKey<LootTable> loot(String name) {
		return ResourceKey.create(Registries.LOOT_TABLE, Eam.id("chests/pillager_outpost/" + name));
	}

	/** Loads the structure and piece types. */
	public static void register() {
	}
}
