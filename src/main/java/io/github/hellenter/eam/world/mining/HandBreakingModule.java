package io.github.hellenter.eam.world.mining;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Player block breaking. Without a tool made for the block in the main hand only blocks in {@link #HAND_BREAKABLE} (and blocks
 * that break instantly) can be broken; everything else gets no break progress at all ({@link HandBreakingLogic}).
 * Blocks in {@link #AXE_ONLY} (logs) break only with an item in {@link #CHOPS_LOGS}, like MITE.
 * Everything breaks slower, and nothing breaks instantly ({@link BreakTimeLogic}).
 */
public final class HandBreakingModule {
	/** Blocks a player can break with an empty hand or a non-tool item. */
	public static final TagKey<Block> HAND_BREAKABLE = TagKey.create(Registries.BLOCK, Eam.id("hand_breakable"));

	/** Logs, stems and hyphae: only an axe breaks them. */
	public static final TagKey<Block> AXE_ONLY = TagKey.create(Registries.BLOCK, Eam.id("axe_only"));
	/** Items that break {@link #AXE_ONLY} blocks: every axe and hatchet. */
	public static final TagKey<Item> CHOPS_LOGS = TagKey.create(Registries.ITEM, Eam.id("chops_logs"));

	private static boolean enabled;

	private HandBreakingModule() {
	}

	public static void init() {
		enabled = true;
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
			if (!level.isClientSide() && !player.isCreative() && !player.isSpectator()
					&& level.getBlockState(pos).is(AXE_ONLY) && !player.getMainHandItem().is(CHOPS_LOGS)) {
				player.sendOverlayMessage(Component.translatable("eam.mining.needs_axe"));
			}
			return InteractionResult.PASS;
		});
	}

	/** Called from the mixin on both sides, so the client does not show cracks the server rejects. */
	public static float destroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos, float original) {
		if (!enabled || original <= 0.0F) {
			return original;
		}
		float progress = HandBreakingLogic.destroyProgress(
				original,
				toolFits(player.getMainHandItem().get(DataComponents.TOOL), state),
				state.is(HAND_BREAKABLE),
				state.getDestroySpeed(level, pos),
				state.is(AXE_ONLY),
				player.getMainHandItem().is(CHOPS_LOGS)
		);
		return BreakTimeLogic.destroyProgress(progress);
	}

	/** A tool counts only on blocks it is made for: an axe does not break stone, a knife does not chop logs. */
	private static boolean toolFits(Tool tool, BlockState state) {
		return tool != null && tool.rules().stream().anyMatch(rule -> rule.speed().isPresent() && state.is(rule.blocks()));
	}
}
