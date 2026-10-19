package io.github.hellenter.eam.mobs.entity.koschei;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Koschei's ice: the clear blue ice of the wall he raises. It breaks like ice and drops nothing, and melts away on its own
 * once the tick he scheduled for it comes.
 */
public class KoscheiIceBlock extends HalfTransparentBlock {
	public KoscheiIceBlock(Properties properties) {
		super(properties);
	}

	/** Melts away. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		level.removeBlock(pos, false);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), pos.getX() + 0.5,
				pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.05);
		level.sendParticles(ParticleTypes.FALLING_WATER, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 4, 0.3, 0.1, 0.3, 0.0);
		if (random.nextInt(3) == 0) {
			level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.5F, 1.2F);
		}
	}
}
