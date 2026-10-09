package io.github.hellenter.eam.client.forging;

import io.github.hellenter.eam.world.forging.ForgingLogic;
import io.github.hellenter.eam.world.forging.ForgingModule;
import io.github.hellenter.eam.world.forging.HotIronItem;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Shows how long hot iron or hot mithril stays hot; the game time it cools at is on the item. */
public final class HotIronTooltip {
	private HotIronTooltip() {
	}

	public static void register() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (!(stack.getItem() instanceof HotIronItem) || minecraft.level == null) {
				return;
			}
			// Keys like item.eam.hot_iron_ingot.cooled, one set per hot metal.
			String key = stack.getItem().getDescriptionId();
			long coolsAt = ForgingModule.coolsAt(stack);
			long now = minecraft.level.getGameTime();
			if (ForgingLogic.isCooled(coolsAt, now)) {
				lines.add(Component.translatable(key + ".cooled").withStyle(ChatFormatting.GRAY));
			} else {
				lines.add(Component.translatable(key + ".cools_in", ForgingLogic.secondsLeft(coolsAt, now))
						.withStyle(ChatFormatting.GOLD));
				lines.add(Component.translatable(key + ".forge").withStyle(ChatFormatting.GRAY));
			}
		});
	}
}
