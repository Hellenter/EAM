package io.github.hellenter.eam.survival.hunger;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Per-player hunger values the client needs: the food bar size for the HUD, eating and sprinting.
 * Recomputed by the server every tick, so it is synced but not saved.
 */
public record HungerStats(int maxFood) {
	public static final StreamCodec<ByteBuf, HungerStats> STREAM_CODEC =
			ByteBufCodecs.VAR_INT.map(HungerStats::new, HungerStats::maxFood);
}
