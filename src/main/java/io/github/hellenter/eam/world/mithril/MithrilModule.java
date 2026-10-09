package io.github.hellenter.eam.world.mithril;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.furnace.ObsidianBlastFurnaceBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mithril and the obsidian blast furnace. Obsidian is softer and mined with an iron pickaxe (the block tags are data),
 * which makes the obsidian blast furnace reachable. Its smelting recipes are ordinary blasting recipes; what is tagged
 * {@link #OBSIDIAN_FURNACE_ONLY} (raw mithril, mithril ores, mithril ingots) cooks only in it, not in the vanilla blast
 * furnace. Mithril tools, armor, anvil and plates are items and data. Flag: {@code mithril}.
 */
public final class MithrilModule {
	/** Furnace inputs that only the obsidian blast furnace is hot enough for. */
	public static final TagKey<Item> OBSIDIAN_FURNACE_ONLY = TagKey.create(Registries.ITEM, Eam.id("obsidian_furnace_only"));

	private static boolean enabled;

	private MithrilModule() {
	}

	public static void init() {
		enabled = true;
	}

	/** Called from the block state mixin on both sides, so the client predicts the same break time as the server. */
	public static float destroySpeed(BlockState state, float original) {
		return enabled ? MithrilLogic.destroySpeed(state.is(Blocks.OBSIDIAN), original) : original;
	}

	/** Server, when a furnace looks up a recipe for its input: false keeps a vanilla blast furnace off mithril. */
	public static boolean mayCook(AbstractFurnaceBlockEntity furnace, ItemStack input) {
		return !enabled || MithrilLogic.mayCook(input.is(OBSIDIAN_FURNACE_ONLY), furnace instanceof ObsidianBlastFurnaceBlockEntity);
	}
}
