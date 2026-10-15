package io.github.hellenter.eam.world.structures.mine;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;

/** Registers the underworld mine structure type ({@link UnderworldMineStructure}) and its adit ({@link MineAditElement}). */
public final class UnderworldMineModule {
	public static final StructureType<UnderworldMineStructure> TYPE =
			Registry.register(BuiltInRegistries.STRUCTURE_TYPE, Eam.id("underworld_mine"), () -> UnderworldMineStructure.CODEC);
	public static final StructurePoolElementType<MineAditElement> ADIT =
			Registry.register(BuiltInRegistries.STRUCTURE_POOL_ELEMENT, Eam.id("mine_adit"), () -> MineAditElement.CODEC);

	private UnderworldMineModule() {
	}

	/** Loads the structure type. */
	public static void register() {
	}
}
