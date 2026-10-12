package io.github.hellenter.eam.mobs.entity.crossbowman;

import io.github.hellenter.eam.content.EamBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A net shot by an elite crossbowman: like cobweb, but it only slows whoever stands in it and falls apart after a
 * while. Swords and shears cut it quickly; it never drops itself.
 */
public class NetBlock extends Block {
	/**
	 * Speed left to whoever stands in it. A constant, not data: the player's own client moves the player and must know
	 * it.
	 */
	public static final double SPEED_FACTOR = 0.6;
	private static final Vec3 STUCK = new Vec3(SPEED_FACTOR, SPEED_FACTOR, SPEED_FACTOR);

	public NetBlock(Properties properties) {
		super(properties);
	}

	/** Whether a net can go here: only into air or something as flimsy as grass. */
	static boolean canPlaceAt(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.canBeReplaced() && state.getFluidState().isEmpty();
	}

	/** Puts a net here; returns whether it went in. */
	static boolean place(ServerLevel level, BlockPos pos) {
		if (!canPlaceAt(level, pos)) {
			return false;
		}
		return level.setBlock(pos, EamBlocks.NET.defaultBlockState(), Block.UPDATE_ALL);
	}

	/** However it got here, shot or placed by hand, it falls apart after a while. */
	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!level.isClientSide() && !oldState.is(this)) {
			level.scheduleTick(pos, this, EliteCrossbowmanModule.settings().net().seconds() * 20);
		}
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier,
			boolean isPrecise) {
		entity.makeStuckInBlock(state, STUCK);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		level.destroyBlock(pos, false);
	}
}
