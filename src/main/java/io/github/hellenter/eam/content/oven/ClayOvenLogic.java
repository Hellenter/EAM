package io.github.hellenter.eam.content.oven;

/** The clay oven's arithmetic on top of the vanilla furnace's. */
public final class ClayOvenLogic {
	private ClayOvenLogic() {
	}

	/** The furnace speed multiplier for a fuel with {@code fuelSpeed}: cooking takes {@code cookTimeMultiplier} times longer. */
	public static float speedMultiplier(float fuelSpeed, float cookTimeMultiplier) {
		return fuelSpeed / cookTimeMultiplier;
	}

	/** Ticks a fuel that burns {@code burnTicks} in a furnace lasts in the oven; never below 1 tick for a real fuel. */
	public static int burnDuration(int burnTicks, float fuelTimeMultiplier) {
		return burnTicks <= 0 ? burnTicks : Math.max(1, Math.round(burnTicks * fuelTimeMultiplier));
	}
}
