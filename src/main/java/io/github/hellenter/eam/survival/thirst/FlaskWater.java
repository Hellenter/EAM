package io.github.hellenter.eam.survival.thirst;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Item component {@code eam:flask_water}: water inside a flask, in half droplets.
 *
 * @param amount       half droplets of water, 0..{@link FlaskLogic#MAX_CAPACITY}
 * @param contaminated the water was dirty when the flask was filled; drinking it poisons unless the filter works
 */
public record FlaskWater(int amount, boolean contaminated) {
	public static final FlaskWater EMPTY = new FlaskWater(0, false);

	public static final Codec<FlaskWater> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(0, FlaskLogic.MAX_CAPACITY).fieldOf("amount").forGetter(FlaskWater::amount),
			Codec.BOOL.optionalFieldOf("contaminated", false).forGetter(FlaskWater::contaminated)
	).apply(instance, FlaskWater::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, FlaskWater> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, FlaskWater::amount,
			ByteBufCodecs.BOOL, FlaskWater::contaminated,
			FlaskWater::new);

	public FlaskWater {
		amount = Math.clamp(amount, 0, FlaskLogic.MAX_CAPACITY);
		contaminated = contaminated && amount > 0;
	}

	public boolean isEmpty() {
		return amount == 0;
	}

	public boolean isFull(int capacity) {
		return amount >= capacity;
	}
}
