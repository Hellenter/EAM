package io.github.hellenter.eam.mobs.vanilla.nemesis;

/** Pure rules of the nemesis: which of the victim's things is worth the most and how much stronger each kill makes it. */
public final class NemesisLogic {
	private NemesisLogic() {
	}

	/**
	 * How much a piece of gear is worth to a monster: what it adds to hitting and to armor first, then its enchantments,
	 * then how long it lasts. Only the order matters.
	 */
	public static double score(double attackDamage, double armor, double toughness, int enchantmentLevels, int maxDamage) {
		return attackDamage * 4.0 + armor * 5.0 + toughness * 5.0 + enchantmentLevels * 3.0 + maxDamage / 100.0;
	}

	/** The extra share of base health a nemesis with {@code kills} kills has; kills past {@code maxKills} add nothing. */
	public static double healthBonus(int kills, double perKill, int maxKills) {
		return perKill * Math.clamp(kills, 0, maxKills);
	}
}
