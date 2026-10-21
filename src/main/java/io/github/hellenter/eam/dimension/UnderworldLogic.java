package io.github.hellenter.eam.dimension;

/**
 * Pure rules of the underworld: its layout in height and where a nether portal leads.
 * <p>
 * The underworld is 256 blocks tall (y -48..207): vanilla bedrock at the bottom (y -48..-44) and at the top (y 203..207),
 * and in between a band of deep bedrock ({@link #BAND_BOTTOM}..{@link #BAND_TOP}) that splits it into two levels. The
 * upper level (about 156 blocks) holds the biomes; the lower level (about 90 blocks with its bedrock) is the hot depths,
 * where nether portals lead to the nether. Only a special item breaks the deep bedrock.
 */
public final class UnderworldLogic {
	public static final int MIN_Y = -48;
	public static final int HEIGHT = 256;
	/** One above the highest block. */
	public static final int TOP = MIN_Y + HEIGHT;
	/** The deep bedrock band: always solid from {@link #BAND_CORE_BOTTOM} to {@link #BAND_CORE_TOP}, ragged a few blocks around. */
	public static final int BAND_BOTTOM = 42;
	public static final int BAND_CORE_BOTTOM = 45;
	public static final int BAND_CORE_TOP = 48;
	public static final int BAND_TOP = 51;
	/** Below this a spot belongs to the lower level, at and above it to the upper one. */
	public static final int LEVEL_SPLIT = 46;
	/** Where a portal may be built when one is made for a traveller: its lowest block, in each level. */
	public static final int LOWER_PORTAL_MIN_Y = MIN_Y + 6;
	public static final int LOWER_PORTAL_MAX_Y = 30;
	public static final int UPPER_PORTAL_MIN_Y = 54;
	public static final int UPPER_PORTAL_MAX_Y = TOP - 16;
	/**
	 * How far out an exit portal looks for a safe spot, ring by ring: the next ring is searched only when the previous one
	 * had none. The first is vanilla's radius.
	 */
	public static final int[] PORTAL_SEARCH_RADII = {16, 32, 48};

	private UnderworldLogic() {
	}

	/** Where a traveller stands or arrives. */
	public enum Place {
		OVERWORLD,
		UNDERWORLD_UPPER,
		UNDERWORLD_LOWER,
		NETHER,
		/** Any other dimension: portals there keep vanilla rules. */
		OTHER
	}

	public static boolean isLowerLevel(int y) {
		return y < LEVEL_SPLIT;
	}

	public static Place underworldLevel(int y) {
		return isLowerLevel(y) ? Place.UNDERWORLD_LOWER : Place.UNDERWORLD_UPPER;
	}

	/**
	 * Where a nether portal at {@code from} leads, or {@link Place#OTHER} to keep vanilla's rule. The overworld and the
	 * underworld's upper level lead to each other; the lower level and the nether lead to each other. So the nether is
	 * reached only through the underworld, as in MITE.
	 */
	public static Place portalTarget(Place from) {
		return switch (from) {
			case OVERWORLD -> Place.UNDERWORLD_UPPER;
			case UNDERWORLD_UPPER -> Place.OVERWORLD;
			case UNDERWORLD_LOWER -> Place.NETHER;
			case NETHER -> Place.UNDERWORLD_LOWER;
			case OTHER -> Place.OTHER;
		};
	}

	/** The arrival height in the target level: the traveller's own height, kept inside the level. */
	public static int arrivalY(Place target, int y) {
		return switch (target) {
			case UNDERWORLD_UPPER -> Math.clamp(y, UPPER_PORTAL_MIN_Y, UPPER_PORTAL_MAX_Y);
			case UNDERWORLD_LOWER -> Math.clamp(y, LOWER_PORTAL_MIN_Y, LOWER_PORTAL_MAX_Y);
			default -> y;
		};
	}

	/** The lowest and highest y a portal's bottom frame block may take in an underworld level. */
	public static int portalMinY(Place level) {
		return level == Place.UNDERWORLD_LOWER ? LOWER_PORTAL_MIN_Y - 1 : UPPER_PORTAL_MIN_Y - 1;
	}

	public static int portalMaxY(Place level) {
		return level == Place.UNDERWORLD_LOWER ? LOWER_PORTAL_MAX_Y : UPPER_PORTAL_MAX_Y;
	}

	/** Whether an existing portal block at {@code y} belongs to the given underworld level. */
	public static boolean inLevel(Place level, int y) {
		return underworldLevel(y) == level;
	}
}
