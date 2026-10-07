package io.github.hellenter.eam.content;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.campfire.CampFireBlockEntity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

/** Block entity types added by EAM. Registered after {@link EamBlocks}. */
public final class EamBlockEntities {
	public static final BlockEntityType<CampFireBlockEntity> CAMP_FIRE = register("camp_fire",
			new BlockEntityType<>(CampFireBlockEntity::new, Set.of(EamBlocks.CAMP_FIRE)));

	private EamBlockEntities() {
	}

	public static void register() {
		// Loads the class, which registers the types above.
	}

	private static <T extends BlockEntity> BlockEntityType<T> register(String name, BlockEntityType<T> type) {
		return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Eam.id(name), type);
	}
}
