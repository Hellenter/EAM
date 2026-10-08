package io.github.hellenter.eam.content.slingshot;

/** Slingshot numbers: draw power and pebble damage over distance. Pure logic. */
public final class SlingshotLogic {
	/** Full draw takes one second. */
	public static final int FULL_DRAW_TICKS = 20;
	/** Below this power the band slips and nothing is shot, as with a bow. */
	public static final float MIN_POWER = 0.1F;
	/** Pebble speed at full draw, blocks per tick (a thrown pebble flies at 1.5, an arrow from a full bow at 3.0). */
	public static final float FULL_SPEED = 2.5F;
	/** Damage of a fully drawn shot point-blank: two hearts. */
	public static final float POINT_BLANK_DAMAGE = 4.0F;
	/** Damage of a fully drawn shot at {@link #FALLOFF_DISTANCE} and beyond: half a heart. */
	public static final float FAR_DAMAGE = 1.0F;
	/** Distance in blocks over which the damage falls linearly from point-blank to far. */
	public static final double FALLOFF_DISTANCE = 24.0;

	private SlingshotLogic() {
	}

	/** Draw power from 0 to 1 after holding for the given ticks, on the bow's curve stretched to {@link #FULL_DRAW_TICKS}. */
	public static float powerForTime(int ticksHeld) {
		float draw = ticksHeld / (float) FULL_DRAW_TICKS;
		return Math.min(1.0F, (draw * draw + draw * 2.0F) / 3.0F);
	}

	/** Damage of a pebble shot with the given power that hits after flying the given distance. */
	public static float damage(float power, double distance) {
		double t = Math.clamp(distance / FALLOFF_DISTANCE, 0.0, 1.0);
		float full = (float) (POINT_BLANK_DAMAGE + (FAR_DAMAGE - POINT_BLANK_DAMAGE) * t);
		return full * Math.clamp(power, 0.0F, 1.0F);
	}
}
