package io.github.hellenter.eam.survival.thirst;

/**
 * Pure thirst rules, free of game objects so they can be unit tested.
 */
public final class ThirstLogic {
	/** Same cap vanilla puts on food exhaustion. */
	static final float MAX_EXHAUSTION = 40.0F;

	private ThirstLogic() {
	}

	/**
	 * Where the player is this tick.
	 *
	 * @param peaceful the difficulty is peaceful: no drain, slow refill
	 * @param temperature base temperature of the biome the player stands in
	 * @param poisoned the player has food poisoning
	 * @param grace    the player just spawned: thirst does not drain and exhaustion is dropped
	 * @param drainFactor water drains at this fraction of the normal rate (a buff below 1)
	 */
	public record Environment(boolean peaceful, float temperature, boolean poisoned, boolean grace, float drainFactor) {
		public Environment(boolean peaceful, float temperature, boolean poisoned, boolean grace) {
			this(peaceful, temperature, poisoned, grace, 1.0F);
		}
	}

	/**
	 * @param dehydrate deal one hit of dehydration damage this tick
	 */
	public record TickResult(Thirst thirst, ThirstCounters counters, boolean dehydrate) {
	}

	/** Adds the thirst share of vanilla food exhaustion (sprinting, jumping, mining, taking damage). */
	public static ThirstCounters addActivity(ThirstCounters counters, float foodExhaustion, ThirstSettings settings) {
		float exhaustion = counters.exhaustion() + foodExhaustion * settings.activityMultiplier();
		return counters.withExhaustion(Math.min(exhaustion, MAX_EXHAUSTION));
	}

	public static TickResult tick(Thirst thirst, ThirstCounters counters, ThirstSettings settings, Environment environment) {
		int sipCooldown = Math.max(counters.sipCooldown() - 1, 0);
		int immunity = environment.poisoned() ? settings.poisoning().immunity() : Math.max(counters.immunity() - 1, 0);

		if (environment.peaceful()) {
			int timer = counters.damageTimer() + 1;
			Thirst next = thirst;
			if (timer >= settings.peacefulRefillInterval()) {
				timer = 0;
				next = thirst.add(1);
			}
			return new TickResult(next, new ThirstCounters(0.0F, timer, sipCooldown, immunity), false);
		}

		if (environment.grace()) {
			return new TickResult(thirst, new ThirstCounters(0.0F, 0, sipCooldown, immunity), false);
		}

		float passive = settings.passiveExhaustion() * climateMultiplier(settings, environment.temperature());
		float exhaustion = Math.min(counters.exhaustion() + passive, MAX_EXHAUSTION);
		Thirst next = thirst;
		float perPoint = settings.exhaustionPerPoint() / environment.drainFactor();
		if (exhaustion >= perPoint) {
			exhaustion -= perPoint;
			// Like food: saturation goes first, the bar only once it is spent.
			next = thirst.saturation() > 0.0F ? thirst.withSaturation(Math.max(thirst.saturation() - 1.0F, 0.0F)) : thirst.add(-1);
		}

		int timer = 0;
		boolean dehydrate = false;
		if (next.water() == 0) {
			timer = counters.damageTimer() + 1;
			if (timer >= settings.dehydrationInterval()) {
				timer = 0;
				dehydrate = true;
			}
		}

		return new TickResult(next, new ThirstCounters(exhaustion, timer, sipCooldown, immunity), dehydrate);
	}

	/** How much faster than usual a biome of this base temperature makes the player thirsty: more in heat, less in cold. */
	public static float climateMultiplier(ThirstSettings settings, float temperature) {
		if (temperature > settings.hotTemperature()) return settings.hotMultiplier();
		if (temperature < settings.coldTemperature()) return settings.coldMultiplier();
		return 1.0F;
	}

	/**
	 * Whether the water in a cauldron is rain water after its level changed from {@code oldLevel} to {@code newLevel}
	 * (0 is no water). Only rain keeps it clean: any other water poured in, even into a clean cauldron, spoils all of it.
	 *
	 * @param rain the change is rain filling the cauldron
	 */
	public static boolean rainWaterAfter(boolean wasRainWater, int oldLevel, int newLevel, boolean rain) {
		if (newLevel == 0) return false;
		if (newLevel > oldLevel) return rain && (oldLevel == 0 || wasRainWater);
		return wasRainWater;
	}

	/**
	 * Heat of a cauldron after its water level changed from {@code oldLevel} to {@code newLevel} (0 is no water). Water
	 * poured in is cold and raw, so boiling starts over; taking water out keeps it boiled.
	 */
	public static int boilHeatAfter(int heat, int oldLevel, int newLevel) {
		return newLevel == 0 || newLevel > oldLevel ? 0 : heat;
	}

	/**
	 * Heat of a water cauldron after {@code ticks} more ticks. Over a heat source it climbs to {@code boilTime} and stays
	 * there; taken off the heat before it boils, the water cools and starts over. Boiled water stays boiled.
	 */
	public static int heatAfter(int heat, boolean heated, int ticks, int boilTime) {
		if (heat >= boilTime) return boilTime;
		return heated ? Math.min(heat + ticks, boilTime) : 0;
	}

	/** Drinking {@code water} points: the bar fills and saturation grows by {@code water} times the saturation ratio. */
	public static Thirst drink(Thirst thirst, int water, ThirstSettings settings) {
		return water > 0 ? thirst.drink(water, water * settings.saturationRatio()) : thirst.add(water);
	}

	/** Adds {@code factor} ticks' worth of passive thirst, for effects that make the player thirsty faster. */
	public static ThirstCounters addPassive(ThirstCounters counters, ThirstSettings settings, float factor) {
		float exhaustion = counters.exhaustion() + settings.passiveExhaustion() * factor;
		return counters.withExhaustion(Math.min(exhaustion, MAX_EXHAUSTION));
	}

	/** Thirst share of one tick of food poisoning. */
	public static ThirstCounters addPoisoning(ThirstCounters counters, ThirstSettings settings) {
		float exhaustion = counters.exhaustion() + settings.poisoning().thirstExhaustion();
		return counters.withExhaustion(Math.min(exhaustion, MAX_EXHAUSTION));
	}

	/**
	 * Whether a sip from a water source poisons the player.
	 *
	 * @param dirtyWater the water is in a biome tagged {@code eam:dirty_water}
	 * @param roll       a uniform random number in [0, 1)
	 */
	public static boolean poisonsOnSip(ThirstCounters counters, ThirstSettings settings, boolean dirtyWater, float roll) {
		PoisoningSettings poisoning = settings.poisoning();
		return counters.immunity() == 0 && roll < (dirtyWater ? poisoning.dirtyWaterChance() : poisoning.chance());
	}

	/** Sprinting needs more than 30% of the water bar; with a full-size bar this is "more than 6", like vanilla food. */
	public static boolean canSprint(Thirst thirst) {
		return thirst.water() * 10 > thirst.max() * 3;
	}

	public static boolean canSip(Thirst thirst, ThirstCounters counters) {
		return !thirst.isFull() && counters.sipCooldown() == 0;
	}
}
