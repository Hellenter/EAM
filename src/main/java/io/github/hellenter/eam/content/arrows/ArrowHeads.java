package io.github.hellenter.eam.content.arrows;

/**
 * Damage bonus of each arrow head over the vanilla flint arrow. The silver arrow also gets
 * {@link io.github.hellenter.eam.content.silver.SilverGear#UNDEAD_DAMAGE_BONUS} against undead.
 */
public final class ArrowHeads {
	public static final ArrowHead COPPER = new ArrowHead(0.2F);
	public static final ArrowHead SILVER = new ArrowHead(0.2F);
	public static final ArrowHead IRON = new ArrowHead(0.4F);
	public static final ArrowHead MITHRIL = new ArrowHead(0.6F);

	/** Arrows a recipe makes, as many as the vanilla one. */
	public static final int RECIPE_COUNT = 4;

	private ArrowHeads() {
	}
}
