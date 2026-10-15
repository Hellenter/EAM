package io.github.hellenter.eam.survival.spoilage;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code eam:freshness} component of a perishable stack: when each of its items spoils, in overworld clock ticks,
 * oldest first. A stack without it has not been seen by the server yet and is fresh.
 */
public record Freshness(List<Long> spoilsAt) {
	public static final Codec<Freshness> CODEC = Codec.LONG.listOf().xmap(Freshness::of, Freshness::spoilsAt);
	public static final StreamCodec<ByteBuf, Freshness> STREAM_CODEC = ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list())
			.map(Freshness::of, Freshness::spoilsAt);

	public Freshness {
		spoilsAt = List.copyOf(spoilsAt);
	}

	/** Sorted, so any list read from disk or the network keeps the oldest-first order. */
	public static Freshness of(List<Long> times) {
		List<Long> sorted = new ArrayList<>(times);
		sorted.sort(null);
		return new Freshness(sorted);
	}
}
