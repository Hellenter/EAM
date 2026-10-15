package io.github.hellenter.eam.survival.thirst;

/**
 * Pure flask rules. Capacity, sip size and filter durability are item properties ({@link Type}), so they are
 * constants; the contamination chance and poisoning length are data ({@link FlaskSettings}).
 */
public final class FlaskLogic {
	/**
	 * What a kind of flask can do.
	 *
	 * @param capacity   half droplets the flask holds
	 * @param sip        half droplets one drink restores
	 * @param filterUses half droplets a fresh filter cleans before it is spent
	 */
	public record Type(int capacity, int sip, int filterUses) {
	}

	/** Leather flask with or without a charcoal filter. */
	public static final Type LEATHER = new Type(12, 2, 64);
	/** Glass flask with a charcoal-silver filter: holds and pours half as much again, the filter lasts as many refills. */
	public static final Type GLASS = new Type(18, 3, 96);
	/** The most any flask holds; bounds the saved water. */
	public static final int MAX_CAPACITY = GLASS.capacity();
	/** Water bar colour of a full flask (light blue). */
	public static final int BAR_COLOR_FULL = 0x33B0FF;
	/** Water bar colour of a nearly empty flask (dark blue). */
	public static final int BAR_COLOR_LOW = 0x0820A8;

	private FlaskLogic() {
	}

	/**
	 * Fills the flask to the top. Fresh water is dirty with the configured chance, and dirty water already in the
	 * flask stays dirty.
	 *
	 * @param roll a uniform random number in [0, 1)
	 */
	public static FlaskWater fill(FlaskWater current, Type type, FlaskSettings settings, float roll) {
		return new FlaskWater(type.capacity(), current.contaminated() || roll < settings.contaminationChance());
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
	public static Drink drink(FlaskWater current, Type type, int filter) {
		int water = Math.min(type.sip(), current.amount());
		boolean filtered = filter > 0;
		int filterAfter = filter < 0 ? -1 : Math.max(filter - water, 0);
		FlaskWater remaining = new FlaskWater(current.amount() - water, current.contaminated());
		return new Drink(water, remaining, filterAfter, current.contaminated() && !filtered && water > 0);
	}

	/** Water bar length (0..13) for the item slot. */
	public static int barWidth(int amount, int capacity) {
		return Math.clamp(Math.round(13.0F * amount / capacity), 0, 13);
	}

	/** Water bar colour: light blue when full, darkening towards dark blue as the flask empties. */
	public static int barColor(int amount, int capacity) {
		float t = Math.clamp((float) (amount - 1) / (capacity - 1), 0.0F, 1.0F);
		int color = 0;
		for (int shift = 0; shift <= 16; shift += 8) {
			int low = BAR_COLOR_LOW >> shift & 0xFF;
			int full = BAR_COLOR_FULL >> shift & 0xFF;
			color |= Math.round(low + (full - low) * t) << shift;
		}
		return color;
	}

	/**
	 * Whether a drinking sound plays on this use tick: every 4 ticks once the first ~22% of the drink has passed, like
	 * vanilla consumables.
	 */
	public static boolean isGulpTick(int useTicks, int ticksRemaining) {
		return useTicks - ticksRemaining > (int) (useTicks * 0.21875F) && ticksRemaining % 4 == 0;
	}
}
