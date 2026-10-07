package io.github.hellenter.eam.survival.thirst;

/**
 * Pure flask rules. Capacity, sip size and filter durability are item properties, so they are constants;
 * the contamination chance and poisoning length are data ({@link FlaskSettings}).
 */
public final class FlaskLogic {
	/** Half droplets a flask holds. */
	public static final int CAPACITY = 12;
	/** Half droplets one drink restores. */
	public static final int SIP = 2;
	/** Half droplets a charcoal filter cleans before it is spent. */
	public static final int FILTER_USES = 64;

	private FlaskLogic() {
	}

	/**
	 * Fills the flask to the top. Fresh water is dirty with the configured chance, and dirty water already in the
	 * flask stays dirty.
	 *
	 * @param roll a uniform random number in [0, 1)
	 */
	public static FlaskWater fill(FlaskWater current, FlaskSettings settings, float roll) {
		return new FlaskWater(CAPACITY, current.contaminated() || roll < settings.contaminationChance());
	}

	/**
	 * @param water     half droplets restored to the player
	 * @param remaining what is left in the flask
	 * @param filter    filter uses left after this drink, or -1 for a flask without a filter
	 * @param poisons   the drink poisons the player (before immunity is considered)
	 */
	public record Drink(int water, FlaskWater remaining, int filter, boolean poisons) {
	}

	/**
	 * One drink. Every half droplet drunk through a working filter costs one filter use; a spent filter stops
	 * cleaning but the flask still works.
	 *
	 * @param filter filter uses left, or -1 for a flask without a filter
	 */
	public static Drink drink(FlaskWater current, int filter) {
		int water = Math.min(SIP, current.amount());
		boolean filtered = filter > 0;
		int filterAfter = filter < 0 ? -1 : Math.max(filter - water, 0);
		FlaskWater remaining = new FlaskWater(current.amount() - water, current.contaminated());
		return new Drink(water, remaining, filterAfter, current.contaminated() && !filtered && water > 0);
	}
}
