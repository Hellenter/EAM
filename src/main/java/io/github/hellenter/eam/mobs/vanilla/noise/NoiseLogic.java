package io.github.hellenter.eam.mobs.vanilla.noise;

/**
 * How loud a player is. Every noisy action makes the player heard within some radius for a few seconds; a louder action
 * replaces a quieter one, a quieter one never cuts a louder one short.
 */
public final class NoiseLogic {
	private NoiseLogic() {
	}

	/** A player heard within {@code radius} blocks until game time {@code until}. */
	public record Noise(float radius, long until) {
	}

	/** Radius the player is heard within at {@code now}: 0 once the noise has died down. */
	public static float radius(Noise noise, long now) {
		return noise == null || now >= noise.until() ? 0.0F : noise.radius();
	}

	/** The noise after an action heard within {@code radius} blocks for {@code holdTicks}. */
	public static Noise add(Noise current, float radius, long now, int holdTicks) {
		if (radius <= 0.0F || radius < radius(current, now)) {
			return current;
		}
		return new Noise(radius, now + holdTicks);
	}

	/** A sneaking player makes the same action quieter. */
	public static float sneak(float radius, boolean sneaking, float sneakMultiplier) {
		return sneaking ? radius * sneakMultiplier : radius;
	}

	/** Breaking harder blocks is louder: picking at stone carries further than digging dirt. */
	public static float dig(NoiseSettings settings, float hardness) {
		return Math.min(settings.digMax(), settings.digBase() + settings.digPerHardness() * Math.max(0.0F, hardness));
	}

	/** Whether a mob {@code distanceSqr} away from the player hears noise of {@code radius}. */
	public static boolean hears(double distanceSqr, float radius) {
		return radius > 0.0F && distanceSqr <= (double) radius * radius;
	}
}
