package io.github.hellenter.eam.content.brewing;

import io.github.hellenter.eam.content.EamBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jspecify.annotations.Nullable;

/** Looks and opens like the vanilla brewing stand; brews with {@link SilverBrewingStandBlockEntity}. */
public class SilverBrewingStandBlock extends BrewingStandBlock {
	public SilverBrewingStandBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SilverBrewingStandBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level instanceof ServerLevel serverLevel
				? createTickerHelper(type, EamBlockEntities.SILVER_BREWING_STAND,
						(l, pos, s, stand) -> SilverBrewingStandBlockEntity.serverTick(serverLevel, pos, s, stand))
				: null;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SilverBrewingStandBlockEntity stand) {
			player.openMenu(stand);
			player.awardStat(Stats.INTERACT_WITH_BREWINGSTAND);
		}
		return InteractionResult.SUCCESS;
	}
}
