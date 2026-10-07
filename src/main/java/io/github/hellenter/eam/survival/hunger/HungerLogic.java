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
	 * What the tick needs to know about the player and the world. During the {@code grace} period after spawning,
	 * hunger does not drain: all exhaustion is dropped.
	 */
	public record TickContext(int maxFood, boolean hurt, boolean naturalRegen, boolean peaceful, boolean balanced, boolean grace) {
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
		float exhaustion = context.grace() ? 0.0F : in.exhaustion() + rules.passiveExhaustion();
		int timer = in.tickTimer();
		float perPoint = rules.exhaustionPerPoint() * (context.balanced() ? rules.balancedHungerFactor() : 1.0F);
		int regenInterval = context.balanced()
				? Math.max(1, Math.round(rules.regenIntervalTicks() / rules.balancedRegenFactor()))
				: rules.regenIntervalTicks();

		boolean pointSpent = false;
		if (exhaustion > perPoint) {
			exhaustion -= perPoint;
			pointSpent = true;
			if (saturation > 0.0F) {
				saturation = Math.max(saturation - 1.0F, 0.0F);
			} else if (!context.peaceful()) {
				food = Math.max(food - 1, 0);
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

	private static float clamp(float value) {
		return Math.clamp(value, 0.0F, Nutrition.MAX);
	}
}
