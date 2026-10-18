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

	/** What a thing in a villager's inventory is to him, for how much of it he keeps. */
	public enum Kind {
		/** Food, and what villagers pick up on their own: he keeps it all; a full inventory goes to his chest. */
		FOOD,
		/** What his trade uses, such as a cleric's ingredients: he keeps it all. */
		GOODS,
		POTION,
		ARROW,
		/** Anything else: gear, tools of another trade. */
		OTHER
	}

	/**
	 * How many of a kind of thing a villager keeps; the rest he hands to the elder. A guard keeps {@code potionMax}
	 * potions and, with a bow, {@code arrowMax} arrows; a cleric gives every potion he brews to the elder; anyone else
	 * keeps one potion.
	 */
	public static int keeps(Kind kind, boolean guard, boolean cleric, boolean bow, int potionMax, int arrowMax) {
		return switch (kind) {
			case FOOD, GOODS -> Integer.MAX_VALUE;
			case POTION -> cleric ? 0 : guard ? potionMax : 1;
			case ARROW -> guard && bow ? arrowMax : 0;
			case OTHER -> 0;
		};
	}
}
