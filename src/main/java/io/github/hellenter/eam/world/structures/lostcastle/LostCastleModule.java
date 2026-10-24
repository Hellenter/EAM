package io.github.hellenter.eam.world.structures.lostcastle;

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
 * The Lost castle from Maxim's template {@code eam:lost_castle}: a stone brick fort with four towers round a courtyard
 * on the floor of the underworld's stone wastes, held by a garrison of the Lost under a centurion, with longbowmen on
 * the towers and warhorses in the stable. Rare. Found with {@code /locate structure eam:lost_castle} in the underworld.
 */
public final class LostCastleModule {
	public static final Identifier TEMPLATE = Eam.id("lost_castle");

	public static final ResourceKey<Structure> STRUCTURE = ResourceKey.create(Registries.STRUCTURE, Eam.id("lost_castle"));
	public static final ResourceKey<StructureSet> STRUCTURE_SET = ResourceKey.create(Registries.STRUCTURE_SET, Eam.id("lost_castles"));
	public static final TagKey<Biome> HAS_CASTLE = TagKey.create(Registries.BIOME, Eam.id("has_structure/lost_castle"));

	/** Lime stair markers (barrels): provisions, dried food that keeps. */
	public static final ResourceKey<LootTable> PROVISIONS_LOOT = loot("provisions");
	/** Yellow stair markers: the soldiers' belongings. */
	public static final ResourceKey<LootTable> BELONGINGS_LOOT = loot("belongings");
	/** Orange stair markers: arrows and the rest of what the defence uses up. */
	public static final ResourceKey<LootTable> SUPPLIES_LOOT = loot("supplies");
	/** Red stair markers: the treasure, gear, golden apples and gems. */
	public static final ResourceKey<LootTable> TREASURE_LOOT = loot("treasure");

	public static final StructureType<LostCastleStructure> CASTLE =
			Registry.register(BuiltInRegistries.STRUCTURE_TYPE, Eam.id("lost_castle"), () -> LostCastleStructure.CODEC);
	public static final StructurePieceType PIECE =
			Registry.register(BuiltInRegistries.STRUCTURE_PIECE, Eam.id("lost_castle"), LostCastlePiece::new);

	private LostCastleModule() {
	}

	private static ResourceKey<LootTable> loot(String name) {
		return ResourceKey.create(Registries.LOOT_TABLE, Eam.id("chests/lost_castle/" + name));
	}

	/** Loads the structure and piece types. */
	public static void register() {
	}
}
