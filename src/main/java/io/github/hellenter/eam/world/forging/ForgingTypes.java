package io.github.hellenter.eam.world.forging;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamBlocks;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

/** The anvils' block entity, which keeps the hammer. Registered whatever the config says, so client and server match. */
public final class ForgingTypes {
	public static final BlockEntityType<AnvilBlockEntity> ANVIL = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Eam.id("anvil"),
			new BlockEntityType<>(AnvilBlockEntity::new, Set.of(Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL,
					EamBlocks.COPPER_ANVIL, EamBlocks.SILVER_ANVIL, EamBlocks.MITHRIL_ANVIL, EamBlocks.ADAMANTIUM_ANVIL)));

	private ForgingTypes() {
	}

	static void register() {
		// Loads the class, which registers the type above.
	}
}
