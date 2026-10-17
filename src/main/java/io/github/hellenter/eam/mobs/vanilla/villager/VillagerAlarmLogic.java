package io.github.hellenter.eam.mobs.vanilla.villager;

/** Pure village alarm rules, unit tested. */
public final class VillagerAlarmLogic {
	private VillagerAlarmLogic() {
	}

	/** Whether a hurt villager runs to ring the bell: a grown one hurt by a monster, unless he is a guard and fights. */
	public static boolean raisesAlarm(boolean adult, boolean guard, boolean hurtByMonster) {
		return adult && !guard && hurtByMonster;
	}

	/** Whether the villager running to the bell gives up: he has run for longer than {@code timeoutTicks}. */
	public static boolean givesUp(long now, long startedAt, int timeoutTicks) {
		return now - startedAt > timeoutTicks;
	}

	/** Whether a guard or cleric answering the alarm still walks to the bell, or already stands close enough. */
	public static boolean walksToBell(double distanceSqr, double gatherDistance) {
		return distanceSqr > gatherDistance * gatherDistance;
	}

	/**
	 * How much a guard needs the cleric: nothing (0) when he is unhurt and carries all the potions he may hold, more the
	 * more health he has lost, plus one for a missing potion.
	 */
	public static float need(float health, float maxHealth, int potions, int potionMax) {
		float need = Math.max(0.0F, maxHealth - health);
		if (potions < potionMax) need += 1.0F;
		return need;
	}
}
