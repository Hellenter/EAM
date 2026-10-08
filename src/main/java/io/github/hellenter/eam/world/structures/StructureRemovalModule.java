package io.github.hellenter.eam.world.structures;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Structures in {@link #REMOVED} never start in newly generated chunks; the ones already in the world stay. The tag
 * holds trial chambers for now.
 */
public final class StructureRemovalModule {
	public static final TagKey<Structure> REMOVED = TagKey.create(Registries.STRUCTURE, Eam.id("removed_structures"));

	private static volatile boolean enabled;

	private StructureRemovalModule() {
	}

	public static void init() {
		enabled = true;
	}

	public static boolean isRemoved(Holder<Structure> structure) {
		return enabled && structure.is(REMOVED);
	}
}
