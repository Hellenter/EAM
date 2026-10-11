package io.github.hellenter.eam.content.heart;

/** Numbers of the heart in a jar, a golden apple used in an instant. Pure logic. */
public final class HeartInJarLogic {
	/** Health restored at once: two hearts. */
	public static final float HEAL = 4.0F;
	/** Regeneration II for 8 seconds. */
	public static final int REGENERATION_TICKS = 160;
	public static final int REGENERATION_AMPLIFIER = 1;
	/** Saturation II for 4 seconds. */
	public static final int SATURATION_TICKS = 80;
	public static final int SATURATION_AMPLIFIER = 1;
	/** Two seconds before the next jar. */
	public static final int COOLDOWN_TICKS = 40;
	/** How long the freed heart flies from the hand into the chest. */
	public static final int FLIGHT_TICKS = 16;
	/** Size of the flying heart, in blocks. */
	public static final float HEART_SCALE = 0.2F;

	/*
	 * The flight path in the player's view, in blocks from the eyes: x to the side of the jar hand, y up, z forward.
	 * The heart leaves the jar, rises, swings across the view to the other side and drops into the chest.
	 */
	public static final double[] HAND = {0.32, -0.30, 0.65};
	public static final double[] RISE = {0.42, 0.45, 0.85};
	public static final double[] ACROSS = {-0.42, 0.45, 0.85};

	private HeartInJarLogic() {
	}

	/** Share of the way from the hand to the chest after {@code age} ticks of flight: slow off the hand, fast at the end. */
	public static double flightProgress(int age) {
		double t = Math.clamp((double) age / FLIGHT_TICKS, 0.0, 1.0);
		return 0.6 * t + 0.4 * t * t;
	}

	/** The point at {@code t} on the cubic Bezier curve through {@code a} and {@code d}, pulled by {@code b} and {@code c}. */
	public static double[] bezier(double t, double[] a, double[] b, double[] c, double[] d) {
		double u = 1.0 - t;
		double wa = u * u * u;
		double wb = 3.0 * u * u * t;
		double wc = 3.0 * u * t * t;
		double wd = t * t * t;
		double[] point = new double[3];
		for (int i = 0; i < 3; i++) {
			point[i] = wa * a[i] + wb * b[i] + wc * c[i] + wd * d[i];
		}
		return point;
	}
}
