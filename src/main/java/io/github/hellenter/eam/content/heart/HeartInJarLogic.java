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
	public static final int FLIGHT_TICKS = 6;

	private HeartInJarLogic() {
	}

	/** Share of the way from the hand to the chest after {@code age} ticks of flight: slow off the hand, fast at the end. */
	public static double flightProgress(int age) {
		double t = Math.clamp((double) age / FLIGHT_TICKS, 0.0, 1.0);
		return t * t;
	}
}
