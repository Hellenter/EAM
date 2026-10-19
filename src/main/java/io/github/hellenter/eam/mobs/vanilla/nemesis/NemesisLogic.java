package io.github.hellenter.eam.mobs.vanilla.nemesis;

/** Pure rules of the nemesis: how much stronger each kill makes it. */
public final class NemesisLogic {
	private NemesisLogic() {
	}

	/** The extra share of base health a nemesis with {@code kills} kills has; kills past {@code maxKills} add nothing. */
	public static double healthBonus(int kills, double perKill, int maxKills) {
		return perKill * Math.clamp(kills, 0, maxKills);
	}
}
