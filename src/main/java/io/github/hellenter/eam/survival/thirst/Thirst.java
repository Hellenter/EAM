package io.github.hellenter.eam.survival.thirst;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The visible water bar: 0..{@code max}, two points per droplet. {@code max} grows with the XP level up to
 * {@link #MAX}. Synced to the owning player for the HUD.
 */
public record Thirst(int water, int max) {
	public static final int MAX = 20;

	public static final Codec<Thirst> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(0, MAX).fieldOf("water").forGetter(Thirst::water),
			Codec.intRange(1, MAX).optionalFieldOf("max", MAX).forGetter(Thirst::max)
	).apply(instance, Thirst::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, Thirst> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Thirst::water,
			ByteBufCodecs.VAR_INT, Thirst::max,
			Thirst::new
	);

	public Thirst {
		max = Math.clamp(max, 1, MAX);
		water = Math.clamp(water, 0, max);
	}

	public Thirst(int water) {
		this(water, MAX);
	}

	public static Thirst full() {
		return new Thirst(MAX, MAX);
	}

	public Thirst add(int amount) {
		return new Thirst(water + amount, max);
	}

	/** Resizes the bar; water above the new size is lost. */
	public Thirst withMax(int newMax) {
		return new Thirst(water, newMax);
	}

	public boolean isFull() {
		return water >= max;
	}
}
