package io.github.hellenter.eam.survival.thirst;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The visible water bar: 0..20, two points per droplet. Synced to the owning player for the HUD.
 */
public record Thirst(int water) {
	public static final int MAX = 20;
	/**
	 * At or below this much water the player cannot sprint, like vanilla food. A constant, not data: the client
	 * predicts sprinting and only the server reads datapacks.
	 */
	public static final int SPRINT_THRESHOLD = 6;

	public static final Codec<Thirst> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(0, MAX).fieldOf("water").forGetter(Thirst::water)
	).apply(instance, Thirst::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, Thirst> STREAM_CODEC =
			ByteBufCodecs.VAR_INT.map(Thirst::new, Thirst::water).cast();

	public Thirst {
		water = Math.clamp(water, 0, MAX);
	}

	public static Thirst full() {
		return new Thirst(MAX);
	}

	public Thirst add(int amount) {
		return new Thirst(water + amount);
	}

	public boolean isFull() {
		return water >= MAX;
	}
}
