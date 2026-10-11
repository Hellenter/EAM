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
	 * @param hot      the player stands in a hot biome
	 * @param poisoned the player has food poisoning
	 * @param grace    the player just spawned: thirst does not drain and exhaustion is dropped
	 */
	public record Environment(boolean peaceful, boolean hot, boolean poisoned, boolean grace) {
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

		float passive = settings.passiveExhaustion() * (environment.hot() ? settings.hotMultiplier() : 1.0F);
		float exhaustion = Math.min(counters.exhaustion() + passive, MAX_EXHAUSTION);
		Thirst next = thirst;
		if (exhaustion >= settings.exhaustionPerPoint()) {
			exhaustion -= settings.exhaustionPerPoint();
			next = thirst.add(-1);
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
