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

	/** What the tick needs to know about the player and the world. */
	public record TickContext(int maxFood, boolean hurt, boolean naturalRegen, boolean peaceful) {
	}

	/** New food state plus the health to restore and the starvation damage to deal this tick. */
	public record TickResult(FoodState state, float heal, float starvationDamage) {
	}

	/** Food bar size: grows with the XP level, as in MITE. */
	public static int maxFood(int xpLevel, HungerRules rules) {
		int steps = Math.max(xpLevel, 0) / rules.levelsPerStep();
		long size = rules.baseMaxFood() + (long) steps * rules.maxFoodStep();
		return (int) Math.min(size, rules.maxFoodCap());
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
		float exhaustion = in.exhaustion() + rules.passiveExhaustion();
		int timer = in.tickTimer();

		if (exhaustion > rules.exhaustionPerPoint()) {
			exhaustion -= rules.exhaustionPerPoint();
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
			if (++timer >= rules.regenIntervalTicks()) {
				heal = 1.0F;
				exhaustion += rules.regenExhaustion();
				timer = 0;
			}
		} else {
			timer = 0;
		}

		exhaustion = Math.min(exhaustion, MAX_EXHAUSTION);
		return new TickResult(new FoodState(food, saturation, exhaustion, timer), heal, starvation);
	}
}
