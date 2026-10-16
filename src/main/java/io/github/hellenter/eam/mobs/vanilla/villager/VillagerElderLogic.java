package io.github.hellenter.eam.mobs.vanilla.villager;

/** Pure elder rules, unit tested. */
public final class VillagerElderLogic {
	private VillagerElderLogic() {
	}

	/**
	 * Which of the villagers who can take a piece of gear gets it: the one who already has the most, the first of them
	 * on a tie, so the elder arms one villager fully before the next. {@code -1} when there is none.
	 */
	public static int armsFirst(int[] gear) {
		int best = -1;
		for (int i = 0; i < gear.length; i++) {
			if (best < 0 || gear[i] > gear[best]) best = i;
		}
		return best;
	}
}
