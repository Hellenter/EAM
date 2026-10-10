package io.github.hellenter.eam.content.talisman;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.PortalShape;

import java.util.Optional;

/**
 * Used on an empty nether portal frame like flint and steel: it flies out, lights the portal and comes back. See
 * {@link TalismanModule}.
 */
public class BlackTalismanItem extends Item {
	public BlackTalismanItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		if (!TalismanModule.canOpenPortalIn(level)) {
			return InteractionResult.PASS;
		}
		// The frame block that was clicked, or the space in front of it.
		BlockPos inside = context.getClickedPos().relative(context.getClickedFace());
		Optional<PortalShape> shape = TalismanModule.findFrame(level, inside, context.getHorizontalDirection());
		if (shape.isEmpty() || context.getPlayer() == null) {
			return InteractionResult.PASS;
		}
		ItemStack talisman = context.getItemInHand();
		if (context.getPlayer() instanceof ServerPlayer player && level instanceof ServerLevel serverLevel) {
			TalismanModule.start(serverLevel, player, context.getHand(), talisman, shape.get());
			player.getCooldowns().addCooldown(talisman, TalismanLogic.ACTIVATION_TICKS);
		}
		return InteractionResult.SUCCESS;
	}
}
