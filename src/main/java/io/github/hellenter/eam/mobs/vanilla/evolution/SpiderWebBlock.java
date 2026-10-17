package io.github.hellenter.eam.mobs.vanilla.evolution;

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
 * A web shot by an evolved spider: holds whoever stands in it like cobweb, but falls apart after a few seconds. Swords
 * and shears cut it at once, a bare hand in a moment; it never drops anything.
 */
public class SpiderWebBlock extends Block {
	/** Like cobweb. A constant, not data: the player's own client moves the player and must know it. */
	private static final Vec3 STUCK = new Vec3(0.25, 0.05, 0.25);

	public SpiderWebBlock(Properties properties) {
		super(properties);
	}

	/** Puts a web here if the spot is empty air; returns whether it went in. */
	static boolean place(ServerLevel level, BlockPos pos) {
		if (!level.getBlockState(pos).isAir()) {
			return false;
		}
		return level.setBlock(pos, EamBlocks.SPIDER_WEB.defaultBlockState(), Block.UPDATE_ALL);
	}

	/** However it got here, shot or set by a command, it falls apart after a while. */
	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!level.isClientSide() && !oldState.is(this)) {
			level.scheduleTick(pos, this, MonsterEvolutionModule.settings().web().seconds() * 20);
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
