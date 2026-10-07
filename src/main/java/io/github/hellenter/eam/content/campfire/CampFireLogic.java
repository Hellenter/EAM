package io.github.hellenter.eam.content.campfire;

/** Pure camp fire rules: lighting, fuel, cooking and charcoal. */
public final class CampFireLogic {
	/** What happens to one food slot on a tick of a lit fire. */
	public enum FoodStep {
		WAIT, COOK, BURN
	}

	private CampFireLogic() {
	}

	public static boolean canAddStick(int sticks) {
		return sticks < CampFireBlock.MAX_STICKS;
	}

	public static boolean canLight(int sticks, CampFireSettings settings) {
		return sticks >= settings.sticksToLight();
	}

	/** True when a lit fire whose fuel timer just reached {@code fuelTicks} burns one stick. */
	public static boolean burnsStick(int fuelTicks, CampFireSettings settings) {
		return fuelTicks >= settings.ticksPerStick();
	}

	/** Raw food cooks after {@code cookTicks}; cooked food left for {@code burnTicks} more burns away. */
	public static FoodStep foodStep(boolean cooked, int progress, CampFireSettings settings) {
		if (!cooked) {
			return progress >= settings.cookTicks() ? FoodStep.COOK : FoodStep.WAIT;
		}
		return progress >= settings.burnTicks() ? FoodStep.BURN : FoodStep.WAIT;
	}

	/** Charcoal left by a fire that has burnt {@code burntSticks} sticks in total. */
	public static int charcoal(int burntSticks, CampFireSettings settings) {
		return Math.min(burntSticks / settings.sticksPerCharcoal(), settings.maxCharcoal());
	}

	/** How burnt the sticks look after the fire has burnt {@code litTicks}: 0 fresh, 1 scorched, 2 charred. */
	public static int charStage(int litTicks, CampFireSettings settings) {
		if (litTicks >= settings.charTicks()) {
			return CampFireBlock.CHARRED;
		}
		return litTicks >= settings.scorchTicks() ? CampFireBlock.SCORCHED : CampFireBlock.FRESH;
	}

	/** Size of the coal heap, 0 to {@link CampFireBlock#MAX_COALS}: the first burnt stick leaves coals, then one size per 3 sticks. */
	public static int coalStage(int burntSticks) {
		return Math.min(CampFireBlock.MAX_COALS, (burntSticks + 2) / 3);
	}

	/** Sticks a broken fire gives back: unburnt sticks only when it is not burning. */
	public static int sticksDropped(int sticks, boolean lit) {
		return lit ? 0 : sticks;
	}
}
