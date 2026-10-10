package io.github.hellenter.eam.client.bucket;

import io.github.hellenter.eam.content.bucket.BucketContent;
import io.github.hellenter.eam.content.bucket.BucketLogic;
import io.github.hellenter.eam.content.bucket.BucketMeltChancesPayload;
import io.github.hellenter.eam.content.bucket.MetalBuckets;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.util.Map;

/** Shows on every bucket how likely it is to melt when it scoops lava, with the server's chances. */
public final class BucketMeltTooltip {
	/** The server's chances by empty bucket; empty until it sends them, so nothing shows on servers without the mod. */
	private static Map<Item, Float> chances = Map.of();

	private BucketMeltTooltip() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(BucketMeltChancesPayload.TYPE, (payload, context) -> chances = Map.copyOf(payload.chances()));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> chances = Map.of());
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (MetalBuckets.contentOf(stack.getItem()) == null) return;
			// Filled buckets show the chance of their empty bucket, the one that scoops.
			Item empty = MetalBuckets.withContent(stack.getItem(), BucketContent.EMPTY);
			float chance = chances.getOrDefault(empty, 0.0F);
			if (chance <= 0.0F) return;
			lines.add(Component.translatable("item.eam.bucket.lava_melt_chance", BucketLogic.percent(chance)).withStyle(ChatFormatting.GRAY));
		});
	}
}
