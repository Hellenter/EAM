package io.github.hellenter.eam.content.talisman;

/**
 * Pure rules of the black talisman and the nether portal. The activation runs {@link #ACTIVATION_TICKS} ticks, as long
 * as the totem of undying's screen animation it borrows: the talisman flies out to the frame, lights it on
 * {@link #IGNITE_TICK}, and flies back to the hand by the end.
 */
public final class TalismanLogic {
	/** The client's item activation animation is this long, so the world effects match it. */
	public static final int ACTIVATION_TICKS = 40;
	/** The portal lights halfway through. */
	public static final int IGNITE_TICK = 20;
	/** Durability one portal costs. */
	public static final int DURABILITY_COST = 1;

	private TalismanLogic() {
	}

	/**
	 * Whether fire may still light a portal in a dimension. Vanilla lights portals in the overworld and the nether; in
	 * the overworld only the talisman does now. From the nether side fire keeps working, so nobody is stranded there.
	 */
	public static boolean fireLightsPortal(boolean overworld, boolean restricted) {
		return !(overworld && restricted);
	}

	/** Where the talisman's trail is between the player (0) and the portal (1) at a tick of the activation. */
	public static double trailProgress(int age) {
		if (age <= IGNITE_TICK) {
			double t = Math.clamp(age / (double) IGNITE_TICK, 0.0, 1.0);
			// Eases out: it leaps from the hand and slows as it reaches the frame.
			return 1.0 - (1.0 - t) * (1.0 - t);
		}
		double t = Math.clamp((age - IGNITE_TICK) / (double) (ACTIVATION_TICKS - IGNITE_TICK), 0.0, 1.0);
		// And back, speeding up into the hand.
		return 1.0 - t * t;
	}

	/** How strongly the frame glows: rising to the ignition, then fading out. */
	public static double frameGlow(int age) {
		if (age <= IGNITE_TICK) {
			return Math.clamp(age / (double) IGNITE_TICK, 0.0, 1.0);
		}
		return Math.clamp(1.0 - (age - IGNITE_TICK) / (double) (ACTIVATION_TICKS - IGNITE_TICK), 0.0, 1.0);
	}
}
