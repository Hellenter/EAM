package io.github.hellenter.eam.mobs.entity.guard;

import io.github.hellenter.eam.mobs.entity.king.KingAction;

import org.jspecify.annotations.Nullable;

import java.util.function.DoubleSupplier;

/** A pillager guard's choices, free of game state so they can be unit tested. */
public final class PillagerGuardLogic {
	/** His two moves on top of plain axe swings. */
	public enum Attack {
		ONSLAUGHT,
		THROW
	}

	private PillagerGuardLogic() {
	}

	/**
	 * The move to start, or {@code null} to keep to plain swings.
	 *
	 * @param distance          flat distance to the target
	 * @param onslaughtCooldown ticks left before the next onslaught
	 * @param throwCooldown     ticks left before the next throw
	 * @param hasAxe            he holds his axe (it is not out flying)
	 * @param rolls             random numbers in [0, 1)
	 */
	public static @Nullable Attack choose(double distance, int onslaughtCooldown, int throwCooldown, boolean hasAxe,
			PillagerGuardSettings settings, DoubleSupplier rolls) {
		if (!hasAxe) {
			return null;
		}
		PillagerGuardSettings.Onslaught onslaught = settings.onslaught();
		if (onslaughtCooldown <= 0 && distance >= onslaught.from() && distance <= onslaught.to() && rolls.getAsDouble() < onslaught.chance()) {
			return Attack.ONSLAUGHT;
		}
		PillagerGuardSettings.AxeThrow axe = settings.axeThrow();
		if (throwCooldown <= 0 && distance >= axe.minRange() && distance <= axe.maxRange() && rolls.getAsDouble() < axe.chance()) {
			return Attack.THROW;
		}
		return null;
	}

	/** Whether a hit taken on tick {@code t} of an action parries him: while he is in the air in the onslaught. */
	public static boolean parries(GuardAction action, int t) {
		return action == GuardAction.ONSLAUGHT && t >= KingAction.ONSLAUGHT_PARRY_START && t < KingAction.ONSLAUGHT_PARRY_END;
	}

	/** Whether a freshly made pillager becomes a guard instead, for a roll in [0, 1). */
	public static boolean becomesGuard(double roll, float share) {
		return roll < share;
	}
}
