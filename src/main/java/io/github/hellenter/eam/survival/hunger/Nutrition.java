package io.github.hellenter.eam.survival.hunger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** A player's protein, fat and carbs, each from 0 to {@link #MAX}. Saved and synced for the inventory screen. */
public record Nutrition(float protein, float fat, float carbs) {
	public static final int MAX = 100;

	private static final Codec<Float> LEVEL = Codec.floatRange(0.0F, MAX);

	public static final Codec<Nutrition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			LEVEL.fieldOf("protein").forGetter(Nutrition::protein),
			LEVEL.fieldOf("fat").forGetter(Nutrition::fat),
			LEVEL.fieldOf("carbs").forGetter(Nutrition::carbs)
	).apply(instance, Nutrition::new));

	public static final StreamCodec<ByteBuf, Nutrition> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, Nutrition::protein,
			ByteBufCodecs.FLOAT, Nutrition::fat,
			ByteBufCodecs.FLOAT, Nutrition::carbs,
			Nutrition::new
	);

	public static Nutrition of(float level) {
		return new Nutrition(level, level, level);
	}
}
