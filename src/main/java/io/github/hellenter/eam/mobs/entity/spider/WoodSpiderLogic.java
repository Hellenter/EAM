package io.github.hellenter.eam.mobs.entity.spider;

/** Pure rules of the small spiders, kept apart from the entity so they can be unit tested. */
public final class WoodSpiderLogic {
	/** Above this light level (0..1, as {@code getLightLevelDependentMagicValue}) it counts as bright. */
	static final float BRIGHT = 0.65F;

	private WoodSpiderLogic() {
	}

	/** How far it looks for a player: its follow range, cut to {@code brightRange} of it in bright light. */
	public static double followDistance(double followRange, float light, double brightRange) {
		return light > BRIGHT ? followRange * brightRange : followRange;
	}

	/**
	 * Poison from one bite: none on peaceful, half on easy, the configured time on normal, one and a half times on hard.
	 *
	 * @param difficulty 0 peaceful, 1 easy, 2 normal, 3 hard
	 */
	public static int poisonTicks(int normalTicks, int difficulty) {
		return switch (difficulty) {
			case 1 -> normalTicks / 2;
			case 2 -> normalTicks;
			case 3 -> normalTicks * 3 / 2;
			default -> 0;
		};
	}

	/**
	 * Whether a natural spawn may go ahead on light: in the dark always, and on leaves in daylight now and then, since
	 * that is where it lives.
	 */
	public static boolean lightAllows(boolean darkEnough, boolean onLeaves, float roll, float daySpawnChance) {
		return darkEnough || onLeaves && roll < daySpawnChance;
	}
}
