package io.github.hellenter.eam.content.heart;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Heart in a jar: right click squeezes the jar until it shatters and the heart flies into the player. Works at once,
 * with no eating delay. See {@link HeartInJarModule}.
 */
public class HeartInJarItem extends Item {
	public HeartInJarItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack jar = player.getItemInHand(hand);
		if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
			HeartInJarModule.use(serverLevel, serverPlayer, hand);
			player.getCooldowns().addCooldown(jar, HeartInJarLogic.COOLDOWN_TICKS);
			jar.consume(1, player);
		}
		return InteractionResult.SUCCESS;
	}
}
