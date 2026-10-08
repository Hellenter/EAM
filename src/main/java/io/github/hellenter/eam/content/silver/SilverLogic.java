package io.github.hellenter.eam.content.silver;

/** Silver's bonuses against undead. Pure logic: {@link SilverModule} feeds it the item components and the hit. */
public final class SilverLogic {
	private SilverLogic() {
	}

	/**
	 * Damage of a melee hit after the weapon's undead bonus.
	 *
	 * @param baseDamage    the attack damage before enchantments
	 * @param damage        the damage with enchantments, as vanilla computed it
	 * @param bonus         the weapon's {@code eam:undead_damage_bonus}, a share of {@code baseDamage}
	 * @param targetUndead  the target is undead
	 */
	public static float attackDamage(float baseDamage, float damage, float bonus, boolean targetUndead) {
		if (!targetUndead || bonus <= 0.0F || baseDamage <= 0.0F) {
			return damage;
		}
		return damage + baseDamage * bonus;
	}

	/**
	 * Damage sent back to an undead attacker.
	 *
	 * @param hit        the damage of the attacker's hit before armor
	 * @param perPiece   each worn armor piece's {@code eam:undead_thorns} share, 0 for pieces without it
	 */
	public static float reflectedDamage(float hit, float... perPiece) {
		if (hit <= 0.0F) {
			return 0.0F;
		}
		float share = 0.0F;
		for (float piece : perPiece) {
			share += Math.max(0.0F, piece);
		}
		return hit * Math.min(1.0F, share);
	}
}
