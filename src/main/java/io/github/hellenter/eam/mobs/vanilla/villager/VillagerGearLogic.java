package io.github.hellenter.eam.mobs.vanilla.villager;

/** Pure rules of a villager's hands, unit tested. */
public final class VillagerGearLogic {
	private VillagerGearLogic() {
	}

	/**
	 * Whether the villager's main hand should be set to his own work item (or emptied, when he has none): only when it
	 * holds nothing a player gave him and does not already hold the right work item.
	 *
	 * @param empty         the hand is empty
	 * @param ownWorkItem   the hand holds a work item, his own or that of a profession he had before
	 * @param rightWorkItem the hand holds exactly the work item of his current profession
	 * @param hasWorkItem   his current profession has a work item
	 */
	public static boolean resetsHand(boolean empty, boolean ownWorkItem, boolean rightWorkItem, boolean hasWorkItem) {
		if (empty) return hasWorkItem;
		return ownWorkItem && !rightWorkItem;
	}
}
