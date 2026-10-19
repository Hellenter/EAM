package io.github.hellenter.eam.world.darkness;

import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.campfire.CampFireBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;

import org.jspecify.annotations.Nullable;

/**
 * Rain puts out torches, campfires and camp fires that stand under the open sky. Soul and copper torches and soul
 * campfires keep burning. Torches and campfires get random ticks for this; see {@code BlockBehaviourRainMixin}.
 */
public final class RainDousing {
	private static volatile boolean enabled;
	private static volatile RainDousingSettings settings = RainDousingSettings.DEFAULT;

	private RainDousing() {
	}

	static void enable() {
		enabled = true;
	}

	static void setSettings(RainDousingSettings value) {
		settings = value;
	}

	/**
	 * Blocks that need random ticks so the rain can reach them. Called once per block state while blocks load, at the
	 * end of {@code Blocks}' static init for vanilla blocks, so the vanilla fields are set and EamBlocks is not touched.
	 */
	public static boolean isDousable(BlockBehaviour block) {
		return block == Blocks.TORCH || block == Blocks.WALL_TORCH || block == Blocks.CAMPFIRE || block instanceof CampFireBlock;
	}

	public static void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!enabled) {
			return;
		}
		float chance = chance(state);
		// The block above, because a campfire blocks motion and so stands under its own heightmap.
		if (chance <= 0.0F || !DarknessLogic.douses(level.isRainingAt(pos.above()), chance, random.nextFloat())) {
			return;
		}
		BlockState doused = doused(state);
		if (doused == null) {
			return;
		}
		level.setBlockAndUpdate(pos, doused);
		level.levelEvent(LevelEvent.LAVA_FIZZ, pos, 0);
		level.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
	}

	private static float chance(BlockState state) {
		if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)) {
			return settings.torchChance();
		}
		if ((state.is(Blocks.CAMPFIRE) || state.is(EamBlocks.CAMP_FIRE)) && state.getValue(BlockStateProperties.LIT)) {
			return settings.campfireChance();
		}
		return 0.0F;
	}

	private static @Nullable BlockState doused(BlockState state) {
		if (state.is(Blocks.TORCH)) {
			return EamBlocks.UNLIT_TORCH.defaultBlockState();
		}
		if (state.is(Blocks.WALL_TORCH)) {
			return EamBlocks.UNLIT_WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, state.getValue(WallTorchBlock.FACING));
		}
		if (state.hasProperty(BlockStateProperties.LIT)) {
			return state.setValue(BlockStateProperties.LIT, false);
		}
		return null;
	}
}
