package io.github.hellenter.eam.world.structures.village;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.StructureType;

/** Registers the duergar village structure type ({@link DuergarVillageStructure}) and {@link OutsideStructureFilter}. */
public final class DuergarVillageModule {
	public static final StructureType<DuergarVillageStructure> TYPE =
			Registry.register(BuiltInRegistries.STRUCTURE_TYPE, Eam.id("duergar_village"), () -> DuergarVillageStructure.CODEC);

	static {
		Registry.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, Eam.id("outside_structure"), OutsideStructureFilter.CODEC);
	}

	private DuergarVillageModule() {
	}

	/** Loads the structure and placement filter types. */
	public static void register() {
	}
}
