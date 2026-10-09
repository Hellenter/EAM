package io.github.hellenter.eam.content.arrows;

import io.github.hellenter.eam.content.silver.SilverLogic;

/** Arrow damage by head. Pure logic: {@link ArrowHeadModule} feeds it the arrow's item components and the target. */
public final class ArrowHeadLogic {
	private ArrowHeadLogic() {
	}

	/**
	 * Base damage of an arrow hit before speed, Power and crits, which vanilla applies on top.
	 *
	 * @param baseDamage    the arrow's vanilla base damage
	 * @param headBonus     the head's {@code eam:arrow_head} bonus, a share of {@code baseDamage}
	 * @param undeadBonus   the arrow's {@code eam:undead_damage_bonus}, 0 if it has none
	 * @param targetUndead  the target is undead
	 */
	public static double baseDamage(double baseDamage, float headBonus, float undeadBonus, boolean targetUndead) {
		if (baseDamage <= 0.0) {
			return baseDamage;
		}
		double damage = baseDamage * (1.0 + Math.max(0.0F, headBonus));
		return SilverLogic.attackDamage((float) baseDamage, (float) damage, undeadBonus, targetUndead);
	}
}
