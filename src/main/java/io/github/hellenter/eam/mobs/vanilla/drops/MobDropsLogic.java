package io.github.hellenter.eam.mobs.vanilla.drops;

/** Pure rules for {@link MobDropsModule}. */
public final class MobDropsLogic {
	/** The sun sets an undead mob on fire for 8 seconds, refreshed every tick it stands in daylight. */
	public static final long SUN_FIRE_TICKS = 8 * 20;

	private MobDropsLogic() {
	}

	/**
	 * True if a mob that died from burning caught that fire from the sun: it was last set alight by daylight no longer ago
	 * than one sun fire lasts. {@code lastSunIgnition} is the game time of that ignition, or a negative value if never.
	 */
	public static boolean burnedBySun(boolean diedBurning, long lastSunIgnition, long now) {
		return diedBurning && lastSunIgnition >= 0 && now - lastSunIgnition <= SUN_FIRE_TICKS;
	}
}
