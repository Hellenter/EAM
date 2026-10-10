package io.github.hellenter.eam.content.swamp;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.function.Supplier;

/**
 * Berries on a swamp shrub. Most shrubs never bear any ({@link #NONE}); a berry shrub is either picked ({@link #PICKED})
 * or ripe ({@link #RIPE}). Picking a ripe one with an empty hand gives a few berries, and it ripens again over time in
 * the light.
 */
public final class BerryShrub {
	public static final IntegerProperty BERRIES = IntegerProperty.create("berries", 0, 2);
	public static final int NONE = 0;
	public static final int PICKED = 1;
	public static final int RIPE = 2;
	/** One random tick in this many ripens a picked shrub. */
	private static final int RIPEN_ONE_IN = 8;

	private BerryShrub() {
	}

	public static boolean isRandomlyTicking(BlockState state) {
		return state.getValue(BERRIES) == PICKED;
	}

	public static void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(BERRIES) == PICKED && random.nextInt(RIPEN_ONE_IN) == 0 && level.getRawBrightness(pos.above(), 0) >= 9) {
			BlockState ripe = state.setValue(BERRIES, RIPE);
			level.setBlock(pos, ripe, 2);
			level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(ripe));
		}
	}

	/** Picks the berries of a ripe shrub: {@code min} to {@code max} of them drop. */
	public static InteractionResult pick(BlockState state, Level level, BlockPos pos, Player player, Supplier<Item> berry, int min, int max) {
		if (state.getValue(BERRIES) != RIPE) {
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel serverLevel) {
			int count = min + serverLevel.getRandom().nextInt(max - min + 1);
			Block.popResource(serverLevel, pos, new ItemStack(berry.get(), count));
			serverLevel.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F,
					0.8F + serverLevel.getRandom().nextFloat() * 0.4F);
			BlockState picked = state.setValue(BERRIES, PICKED);
			serverLevel.setBlock(pos, picked, 2);
			serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, picked));
		}
		return InteractionResult.SUCCESS;
	}
}
