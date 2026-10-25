package io.github.hellenter.eam.survival.thirst;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The visible water bar: 0..{@code max}, two points per droplet. {@code max} grows with the XP level up to
 * {@link #MAX}. {@code saturation} works like food saturation: drinking fills it, and thirst spends it before the bar,
 * so the bar holds still for a while after a drink. It never exceeds the water. Synced to the owning player for the HUD.
 */
public record Thirst(int water, int max, float saturation) {
	public static final int MAX = 20;

	public static final Codec<Thirst> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(0, MAX).fieldOf("water").forGetter(Thirst::water),
			Codec.intRange(1, MAX).optionalFieldOf("max", MAX).forGetter(Thirst::max),
			Codec.floatRange(0.0F, MAX).optionalFieldOf("saturation", 0.0F).forGetter(Thirst::saturation)
	).apply(instance, Thirst::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, Thirst> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Thirst::water,
			ByteBufCodecs.VAR_INT, Thirst::max,
			ByteBufCodecs.FLOAT, Thirst::saturation,
			Thirst::new
	);

	public Thirst {
		max = Math.clamp(max, 1, MAX);
		water = Math.clamp(water, 0, max);
		saturation = Math.clamp(saturation, 0.0F, water);
	}

	public Thirst(int water, int max) {
		this(water, max, 0.0F);
	}

	public Thirst(int water) {
		this(water, MAX);
	}

	public static Thirst full() {
		return new Thirst(MAX, MAX);
	}

	/** Changes the water alone; saturation above the new water is lost. */
	public Thirst add(int amount) {
		return new Thirst(water + amount, max, saturation);
	}

	/** A drink: {@code amount} water and {@code saturationGain} saturation, which stops at the new water. */
	public Thirst drink(int amount, float saturationGain) {
		return new Thirst(water + amount, max, saturation + saturationGain);
	}

	public Thirst withSaturation(float value) {
		return new Thirst(water, max, value);
	}

	/** Resizes the bar; water above the new size is lost. */
	public Thirst withMax(int newMax) {
		return new Thirst(water, newMax, saturation);
	}

	public boolean isFull() {
		return water >= max;
	}
}
