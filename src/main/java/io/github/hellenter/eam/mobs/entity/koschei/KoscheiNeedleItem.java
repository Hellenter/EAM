package io.github.hellenter.eam.mobs.entity.koschei;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The needle that held Koschei's death, dropped when he dies. Right click: it leaves the hand and does what
 * {@link KoscheiNeedle} does. Get away from it.
 */
public class KoscheiNeedleItem extends Item {
	public KoscheiNeedleItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack needle = player.getItemInHand(hand);
		if (level instanceof ServerLevel serverLevel) {
			Vec3 look = player.getLookAngle();
			Vec3 from = player.getEyePosition().add(look.scale(0.6));
			double speed = KoscheiModule.settings().needle().throwSpeed();
			serverLevel.addFreshEntity(new KoscheiNeedle(serverLevel, from, look.scale(speed).add(0.0, 0.1, 0.0)));
			serverLevel.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,
					1.5F, 0.7F);
			needle.consume(1, player);
		}
		return InteractionResult.SUCCESS;
	}
}
