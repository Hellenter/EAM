package io.github.hellenter.eam.client.survival.spoilage;

import io.github.hellenter.eam.survival.spoilage.Freshness;
import io.github.hellenter.eam.survival.spoilage.SpoilageLogic;
import io.github.hellenter.eam.survival.spoilage.SpoilageModule;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/** Shows how long a perishable stack keeps: one line, or one per group of items when they spoil at different times. */
public final class SpoilageTooltip {
	/** Groups shown before the rest are summed up. */
	private static final int MAX_BATCHES = 4;

	private SpoilageTooltip() {
	}

	public static void register() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			Minecraft minecraft = Minecraft.getInstance();
			Freshness freshness = stack.get(SpoilageModule.FRESHNESS);
			if (freshness == null || freshness.spoilsAt().isEmpty() || minecraft.level == null) return;
			List<SpoilageLogic.Batch> batches = SpoilageLogic.batches(freshness.spoilsAt(), SpoilageModule.now(minecraft.level));
			if (batches.size() == 1) {
				lines.add(Component.translatable("tooltip.eam.spoils_in", time(batches.getFirst().ticksLeft()))
						.withStyle(color(batches.getFirst().ticksLeft())));
				return;
			}
			lines.add(Component.translatable("tooltip.eam.spoils").withStyle(ChatFormatting.GRAY));
			for (int i = 0; i < Math.min(batches.size(), MAX_BATCHES); i++) {
				SpoilageLogic.Batch batch = batches.get(i);
				lines.add(Component.translatable("tooltip.eam.spoils_batch", batch.count(), time(batch.ticksLeft()))
						.withStyle(color(batch.ticksLeft())));
			}
			int rest = 0;
			for (int i = MAX_BATCHES; i < batches.size(); i++) rest += batches.get(i).count();
			if (rest > 0) lines.add(Component.translatable("tooltip.eam.spoils_rest", rest).withStyle(ChatFormatting.GRAY));
		});
	}

	/** Days to a tenth, or whole hours under a day. */
	private static Component time(long ticks) {
		if (ticks < SpoilageLogic.DAY) {
			return Component.translatable("tooltip.eam.hours", Math.max(1L, (ticks + SpoilageLogic.HOUR - 1) / SpoilageLogic.HOUR));
		}
		long tenths = SpoilageLogic.tenths(ticks);
		return Component.translatable("tooltip.eam.days", String.format(Locale.ROOT, "%d.%d", tenths / 10, tenths % 10));
	}

	private static ChatFormatting color(long ticks) {
		if (ticks < SpoilageLogic.DAY) return ChatFormatting.RED;
		return ticks < 3 * SpoilageLogic.DAY ? ChatFormatting.GOLD : ChatFormatting.GREEN;
	}
}
