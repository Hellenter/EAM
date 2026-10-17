package io.github.hellenter.eam.survival.hunger;

/**
 * Pure hunger formulas, free of Minecraft state so they can be unit tested.
 */
public final class HungerLogic {
	/** Vanilla caps stored exhaustion at this value. */
	static final float MAX_EXHAUSTION = 40.0F;

	private HungerLogic() {
	}

	/** Snapshot of a player's {@code FoodData}. */
	public record FoodState(int food, float saturation, float exhaustion, int tickTimer) {
	}

	/**
	 * What the tick needs to know about the player and the world. While {@code paused} (grace period after spawning,
	 * creative or spectator) hunger does not drain: all exhaustion is dropped.
	 */
	public record TickContext(int maxFood, boolean hurt, boolean naturalRegen, boolean peaceful, boolean balanced, boolean paused,
			Deficiencies deficiencies, float drainFactor, float regenFactor) {
		/** No buffs: hunger drains and regeneration runs at the normal rate. */
		public TickContext(int maxFood, boolean hurt, boolean naturalRegen, boolean peaceful, boolean balanced, boolean paused,
				Deficiencies deficiencies) {
			this(maxFood, hurt, naturalRegen, peaceful, balanced, paused, deficiencies, 1.0F, 1.0F);
		}
	}

	/** Which nutrients are at or below the deficiency threshold. */
	public record Deficiencies(boolean protein, boolean fat, boolean carbs) {
		public static final Deficiencies NONE = new Deficiencies(false, false, false);

		public boolean any() {
			return protein || fat || carbs;
		}
	}

	/**
	 * Sprint endurance without carbs: ticks run so far and ticks left to rest. While resting the player is winded and
	 * cannot sprint. Server-only, not saved.
	 */
	public record Stamina(int sprintTicks, int restTicks) {
		public static final Stamina FRESH = new Stamina(0, 0);

		public boolean winded() {
			return restTicks > 0;
		}
	}

	/**
	 * New food state plus the health to restore and the starvation damage to deal this tick.
	 * {@code pointSpent} is true when exhaustion used up a point of saturation or food, which also drains nutrients.
	 */
	public record TickResult(FoodState state, float heal, float starvationDamage, boolean pointSpent) {
	}

	public static boolean needsFood(int food, int maxFood) {
		return food < maxFood;
	}

	/** Sprinting needs more than 30% of the food bar; with a full-size bar this is vanilla's "more than 6". */
	public static boolean canSprint(int food, int maxFood) {
		return food * 10 > maxFood * 3;
	}

	/** Number of shank icons for a food bar of the given size. */
	public static int shanks(int maxFood) {
		return (maxFood + 1) / 2;
	}

	public static TickResult tick(FoodState in, TickContext context, HungerRules rules) {
		int food = Math.min(in.food(), context.maxFood());
		float saturation = Math.min(in.saturation(), food);
		float exhaustion = context.paused() ? 0.0F : in.exhaustion() + rules.passiveExhaustion();
		int timer = in.tickTimer();
		float perPoint = rules.exhaustionPerPoint() * (context.balanced() ? rules.balancedHungerFactor() : 1.0F);
		if (context.deficiencies().fat()) {
			perPoint /= rules.deficiency().fatHungerFactor();
		}
		perPoint /= context.drainFactor();
		float regenFactor = 1.0F / context.regenFactor();
		if (context.balanced()) {
			regenFactor /= rules.balancedRegenFactor();
		}
		if (context.deficiencies().any()) {
			regenFactor *= rules.malnourishedRegenFactor();
		}
		int regenInterval = Math.max(1, Math.round(rules.regenIntervalTicks() * regenFactor));

		boolean pointSpent = false;
		if (exhaustion > perPoint) {
			exhaustion -= perPoint;
			// Peaceful never takes food, so nothing was spent and nutrients must not drain either.
			if (saturation > 0.0F) {
				saturation = Math.max(saturation - 1.0F, 0.0F);
				pointSpent = true;
			} else if (!context.peaceful()) {
				food = Math.max(food - 1, 0);
				pointSpent = true;
			}
		}

		float heal = 0.0F;
		float starvation = 0.0F;
		if (food <= 0) {
			if (++timer >= rules.starvationIntervalTicks()) {
				starvation = rules.starvationDamage();
				timer = 0;
			}
		} else if (context.naturalRegen() && context.hurt() && food >= rules.regenMinFood()) {
			if (++timer >= regenInterval) {
				heal = 1.0F;
				exhaustion += rules.regenExhaustion();
				timer = 0;
			}
		} else {
			timer = 0;
		}

		exhaustion = Math.min(exhaustion, MAX_EXHAUSTION);
		return new TickResult(new FoodState(food, saturation, exhaustion, timer), heal, starvation, pointSpent);
	}

	/** Adds a food's nutrients, each capped at {@link Nutrition#MAX}. */
	public static Nutrition eat(Nutrition current, float protein, float fat, float carbs) {
		return new Nutrition(
				clamp(current.protein() + protein),
				clamp(current.fat() + fat),
				clamp(current.carbs() + carbs)
		);
	}

	/** Lowers every nutrient by {@code amount}, not below zero. */
	public static Nutrition decay(Nutrition current, float amount) {
		return eat(current, -amount, -amount, -amount);
	}

	/** A balanced diet keeps all three nutrients at or above the threshold. */
	public static boolean balanced(Nutrition nutrition, int threshold) {
		return nutrition.protein() >= threshold && nutrition.fat() >= threshold && nutrition.carbs() >= threshold;
	}

	/** A nutrient at or below {@code threshold} is deficient. */
	public static Deficiencies deficiencies(Nutrition nutrition, int threshold) {
		return new Deficiencies(nutrition.protein() <= threshold, nutrition.fat() <= threshold, nutrition.carbs() <= threshold);
	}

	/**
	 * Advances sprint endurance by one tick. Without carbs, sprinting gives out after {@code carbsSprintTicks} of running
	 * and needs {@code carbsRestTicks} of rest; walking slowly recovers the run counter. With carbs nothing is tracked.
	 */
	public static Stamina stamina(Stamina in, boolean carbsDeficient, boolean sprinting, DeficiencyRules rules) {
		if (!carbsDeficient) {
			return Stamina.FRESH;
		}
		if (in.restTicks() > 0) {
			return new Stamina(0, in.restTicks() - 1);
		}
		if (sprinting) {
			int ran = in.sprintTicks() + 1;
			return ran >= rules.carbsSprintTicks() ? new Stamina(0, rules.carbsRestTicks()) : new Stamina(ran, 0);
		}
		return new Stamina(Math.max(in.sprintTicks() - 1, 0), 0);
	}

	private static float clamp(float value) {
		return Math.clamp(value, 0.0F, Nutrition.MAX);
	}
}
