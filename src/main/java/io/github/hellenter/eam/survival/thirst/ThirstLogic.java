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
	 */
	public record Environment(boolean peaceful, boolean hot) {
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

		if (environment.peaceful()) {
			int timer = counters.damageTimer() + 1;
			Thirst next = thirst;
			if (timer >= settings.peacefulRefillInterval()) {
				timer = 0;
				next = thirst.add(1);
			}
			return new TickResult(next, new ThirstCounters(0.0F, timer, sipCooldown), false);
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

		return new TickResult(next, new ThirstCounters(exhaustion, timer, sipCooldown), dehydrate);
	}

	public static boolean canSprint(Thirst thirst, ThirstSettings settings) {
		return thirst.water() > settings.sprintThreshold();
	}

	public static boolean canSip(Thirst thirst, ThirstCounters counters) {
		return !thirst.isFull() && counters.sipCooldown() == 0;
	}
}
