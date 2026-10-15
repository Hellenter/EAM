package io.github.hellenter.eam.content.feeder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * How an animal kept at a feeder has been fed. Days are overworld days ({@code clock time / 24000}). Immutable.
 *
 * @param lastMealDay day of its last meal from a feeder
 * @param hunger      days without a meal not yet made up: each missed day adds one, each meal takes one off
 * @param thin        got thin after too many days without a meal; stays thin until {@code hunger} is back to 0
 * @param biteDay     day of the last grass bite that has not yet made up a meal
 * @param bites       grass bites taken on {@code biteDay}; two make a meal
 * @param feederTime  overworld clock time of its last meal from a feeder, {@link #NEVER} if it has not had one. Within a
 *                    day of it the animal is kept at that feeder and grows up and breeds faster.
 */
public record LivestockState(long lastMealDay, int hunger, boolean thin, long biteDay, int bites, long feederTime) {
	/** {@link #feederTime} of an animal that never ate from a feeder. */
	public static final long NEVER = Long.MIN_VALUE;

	public LivestockState(long lastMealDay, int hunger, boolean thin) {
		this(lastMealDay, hunger, thin, 0L, 0, NEVER);
	}

	public LivestockState(long lastMealDay, int hunger, boolean thin, long biteDay, int bites) {
		this(lastMealDay, hunger, thin, biteDay, bites, NEVER);
	}

	public LivestockState withFeederTime(long time) {
		return new LivestockState(lastMealDay, hunger, thin, biteDay, bites, time);
	}

	public static final Codec<LivestockState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.LONG.fieldOf("last_meal_day").forGetter(LivestockState::lastMealDay),
			Codec.INT.optionalFieldOf("hunger", 0).forGetter(LivestockState::hunger),
			Codec.BOOL.optionalFieldOf("thin", false).forGetter(LivestockState::thin),
			Codec.LONG.optionalFieldOf("bite_day", 0L).forGetter(LivestockState::biteDay),
			Codec.INT.optionalFieldOf("bites", 0).forGetter(LivestockState::bites),
			Codec.LONG.optionalFieldOf("feeder_time", NEVER).forGetter(LivestockState::feederTime)
	).apply(instance, LivestockState::new));
}
