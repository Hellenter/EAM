package io.github.hellenter.eam.survival.hunger;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Per-player hunger values the client needs: the food bar size for the HUD, eating and sprinting, and the
 * nutrient level that counts as a balanced diet for the inventory screen. Recomputed by the server every tick,
 * so it is synced but not saved.
 */
public record HungerStats(int maxFood, int balanceThreshold) {
	public static final StreamCodec<ByteBuf, HungerStats> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, HungerStats::maxFood,
			ByteBufCodecs.VAR_INT, HungerStats::balanceThreshold,
			HungerStats::new
	);
}
