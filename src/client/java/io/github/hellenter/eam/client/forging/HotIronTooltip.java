package io.github.hellenter.eam.client.forging;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.world.forging.ForgingLogic;
import io.github.hellenter.eam.world.forging.ForgingModule;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Shows how long hot iron stays hot; the game time it cools at is on the item. */
public final class HotIronTooltip {
	private HotIronTooltip() {
	}

	public static void register() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (!stack.is(EamItems.HOT_IRON_INGOT) || minecraft.level == null) {
				return;
			}
			long coolsAt = ForgingModule.coolsAt(stack);
			long now = minecraft.level.getGameTime();
			if (ForgingLogic.isCooled(coolsAt, now)) {
				lines.add(Component.translatable("item.eam.hot_iron_ingot.cooled").withStyle(ChatFormatting.GRAY));
			} else {
				lines.add(Component.translatable("item.eam.hot_iron_ingot.cools_in", ForgingLogic.secondsLeft(coolsAt, now))
						.withStyle(ChatFormatting.GOLD));
				lines.add(Component.translatable("item.eam.hot_iron_ingot.forge").withStyle(ChatFormatting.GRAY));
			}
		});
	}
}
