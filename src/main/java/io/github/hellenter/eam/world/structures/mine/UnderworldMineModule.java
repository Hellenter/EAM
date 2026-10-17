package io.github.hellenter.eam.world.structures.mine;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;

/**
 * Registers the underworld mine structure type ({@link UnderworldMineStructure}), its adit ({@link MineAditElement}), its
 * laid-out pieces ({@link MinePieceElement}) and their wing processor ({@link MineWingProcessor}).
 */
public final class UnderworldMineModule {
	public static final StructureType<UnderworldMineStructure> TYPE =
			Registry.register(BuiltInRegistries.STRUCTURE_TYPE, Eam.id("underworld_mine"), () -> UnderworldMineStructure.CODEC);
	public static final StructurePoolElementType<MineAditElement> ADIT =
			Registry.register(BuiltInRegistries.STRUCTURE_POOL_ELEMENT, Eam.id("mine_adit"), () -> MineAditElement.CODEC);
	public static final StructurePoolElementType<MinePieceElement> PIECE =
			Registry.register(BuiltInRegistries.STRUCTURE_POOL_ELEMENT, Eam.id("mine_piece"), () -> MinePieceElement.CODEC);

	static {
		Registry.register(BuiltInRegistries.STRUCTURE_PROCESSOR, Eam.id("mine_wing"), MineWingProcessor.CODEC);
	}

	private UnderworldMineModule() {
	}

	/** Loads the structure type. */
	public static void register() {
	}
}
