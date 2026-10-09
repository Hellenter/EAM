package io.github.hellenter.eam.content.furnace;

import io.github.hellenter.eam.content.EamBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;

/**
 * A blast furnace lined with obsidian. It smelts everything the blast furnace does, and also what is hot enough only in
 * here: mithril. See {@link io.github.hellenter.eam.world.mithril.MithrilModule}.
 */
public class ObsidianBlastFurnaceBlock extends BlastFurnaceBlock {
	public ObsidianBlastFurnaceBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ObsidianBlastFurnaceBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createFurnaceTicker(level, type, EamBlockEntities.OBSIDIAN_BLAST_FURNACE);
	}

	@Override
	protected void openContainer(Level level, BlockPos pos, Player player) {
		if (level.getBlockEntity(pos) instanceof ObsidianBlastFurnaceBlockEntity furnace) {
			player.openMenu((MenuProvider) furnace);
			player.awardStat(Stats.INTERACT_WITH_BLAST_FURNACE);
		}
	}
}
