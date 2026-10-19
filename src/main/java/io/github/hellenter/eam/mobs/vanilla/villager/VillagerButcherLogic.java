package io.github.hellenter.eam.mobs.vanilla.villager;

/** Pure butcher rules, unit tested. */
public final class VillagerButcherLogic {
	private VillagerButcherLogic() {
	}

	/** Whether a herd of {@code grown} animals is big enough to slaughter one and still leave {@code keep}. */
	public static boolean slaughters(int grown, int keep) {
		return grown > keep;
	}
}
