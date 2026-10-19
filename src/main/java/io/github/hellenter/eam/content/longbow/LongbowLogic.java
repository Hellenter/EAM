package io.github.hellenter.eam.content.longbow;

/**
 * The longbow's numbers against the vanilla bow: twice the draw, twice the arrow speed and two and a half times the
 * damage. Vanilla multiplies an arrow's base damage by its speed when it hits, so the faster arrow already hurts twice
 * as much; the base damage makes up the rest. Pure logic.
 */
public final class LongbowLogic {
	/** A full draw takes two seconds, twice the bow's one. */
	public static final int FULL_DRAW_TICKS = 40;
	/** A fully drawn arrow flies this many times faster than one from a bow. */
	public static final float SPEED_MULTIPLIER = 2.0F;
	/** And hurts this many times more. */
	public static final float DAMAGE_MULTIPLIER = 2.5F;
	/** Base damage every arrow starts with; Power and metal heads add to it when it hits. */
	public static final double ARROW_BASE_DAMAGE = 2.0;
	/** Arrow speed of a full vanilla bow draw, blocks per tick. */
	private static final float BOW_SPEED = 3.0F;

	private LongbowLogic() {
	}

	/** Draw power from 0 to 1 after holding for the given ticks: the bow's curve stretched to {@link #FULL_DRAW_TICKS}. */
	public static float powerForTime(int ticksHeld) {
		float draw = ticksHeld / (float) FULL_DRAW_TICKS;
		return Math.min(1.0F, (draw * draw + draw * 2.0F) / 3.0F);
	}

	/** Arrow speed for a draw of this power. */
	public static float speed(float power) {
		return power * BOW_SPEED * SPEED_MULTIPLIER;
	}

	/**
	 * The base damage for an arrow that should hurt {@code damageMultiplier} times more than a plain one with base damage
	 * {@code base}, flying {@code speedMultiplier} times faster: the speed already multiplies the damage on hit.
	 */
	public static double baseDamage(double base, float damageMultiplier, float speedMultiplier) {
		return base * damageMultiplier / speedMultiplier;
	}
}
