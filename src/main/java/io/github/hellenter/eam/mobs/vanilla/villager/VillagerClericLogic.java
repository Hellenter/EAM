package io.github.hellenter.eam.mobs.vanilla.villager;

/** Pure cleric rules, unit tested. */
public final class VillagerClericLogic {
	private VillagerClericLogic() {
	}

	/**
	 * Whether a guard drinks a potion now: one that heals when he is down to half his health, any other while he fights
	 * and does not already have its effect.
	 */
	public static boolean drinks(boolean heals, float health, float maxHealth, boolean fighting, boolean hasEffect) {
		if (heals) return health <= maxHealth / 2.0F;
		return fighting && !hasEffect;
	}
}
