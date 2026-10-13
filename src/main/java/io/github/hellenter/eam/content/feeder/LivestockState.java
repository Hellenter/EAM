package io.github.hellenter.eam.content.feeder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * How an animal kept at a feeder has been fed. Days are overworld days ({@code clock time / 24000}). Immutable.
 *
 * @param lastMealDay day of its last meal from a feeder
 * @param hunger      days without a meal not yet made up: each missed day adds one, each meal takes one off
 * @param thin        got thin after too many days without a meal; stays thin until {@code hunger} is back to 0
 */
public record LivestockState(long lastMealDay, int hunger, boolean thin) {
	public static final Codec<LivestockState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.LONG.fieldOf("last_meal_day").forGetter(LivestockState::lastMealDay),
			Codec.INT.optionalFieldOf("hunger", 0).forGetter(LivestockState::hunger),
			Codec.BOOL.optionalFieldOf("thin", false).forGetter(LivestockState::thin)
	).apply(instance, LivestockState::new));
}
