package io.github.hellenter.eam.world.structures.yagahut;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Baba Yaga's hut from Maxim's template {@code eam:yaga_hut}: one in the middle of every swampy forest, on ground
 * levelled for 32 blocks around, with Baba Yaga waiting in front of the door. Found with {@code /locate structure eam:yaga_hut}.
 */
public final class YagaHutModule {
	public static final Identifier TEMPLATE = Eam.id("yaga_hut");

	public static final ResourceKey<Structure> STRUCTURE = ResourceKey.create(Registries.STRUCTURE, Eam.id("yaga_hut"));
	/** Her chests: valuables and the potion ingredients of the surface world. */
	public static final ResourceKey<LootTable> CHEST_LOOT = ResourceKey.create(Registries.LOOT_TABLE, Eam.id("chests/yaga_hut"));
	public static final ResourceKey<StructureSet> STRUCTURE_SET = ResourceKey.create(Registries.STRUCTURE_SET, Eam.id("yaga_huts"));

	public static final StructureType<YagaHutStructure> HUT =
			Registry.register(BuiltInRegistries.STRUCTURE_TYPE, Eam.id("yaga_hut"), () -> YagaHutStructure.CODEC);
	public static final StructurePieceType PIECE =
			Registry.register(BuiltInRegistries.STRUCTURE_PIECE, Eam.id("yaga_hut"), YagaHutPiece::new);

	static {
		Registry.register(BuiltInRegistries.STRUCTURE_PLACEMENT, Eam.id("swamp_middle"), YagaHutPlacement.CODEC);
	}

	private YagaHutModule() {
	}

	/** Loads the structure, piece and placement types, and remembers each world's noises for {@code /locate}. */
	public static void register() {
		ServerLevelEvents.LOAD.register((server, level) -> {
			if (level.dimension() == Level.OVERWORLD) {
				YagaHutPlacement.remember(level.getSeed(), level.getChunkSource().randomState());
			}
		});
	}
}
