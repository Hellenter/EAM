package io.github.hellenter.eam.mobs.vanilla.cows;

/**
 * What a cow that was hit by a player does each tick: charge the attacker, hit once, back off, and give up when the
 * charge takes too long. Being hit again starts a new charge.
 */
public final class CowDefenseLogic {
	public enum Phase {
		CHARGE,
		RETREAT,
		DONE
	}

	/**
	 * @param phase    what the cow is doing
	 * @param ticks    ticks spent in this phase
	 * @param cooldown ticks until the cow may hit again
	 */
	public record State(Phase phase, int ticks, int cooldown) {
		public static final State START = new State(Phase.CHARGE, 0, 0);
	}

	/** The next state, and whether the cow hits its attacker this tick. */
	public record Step(State state, boolean attack) {
	}

	private CowDefenseLogic() {
	}

	/**
	 * @param inReach   the attacker is within the cow's melee reach
	 * @param hurtAgain the attacker hit the cow again since the last tick
	 */
	public static Step tick(State state, boolean inReach, boolean hurtAgain, CowDefenseSettings settings) {
		int cooldown = Math.max(0, state.cooldown() - 1);
		if (state.phase() == Phase.DONE) {
			return new Step(state, false);
		}
		if (hurtAgain && state.phase() == Phase.RETREAT) {
			return new Step(new State(Phase.CHARGE, 0, cooldown), false);
		}
		int ticks = state.ticks() + 1;
		return switch (state.phase()) {
			case CHARGE -> {
				if (inReach && cooldown == 0) {
					yield new Step(new State(Phase.RETREAT, 0, settings.attackCooldownTicks()), true);
				}
				if (ticks >= settings.giveUpTicks()) {
					yield new Step(new State(Phase.DONE, 0, cooldown), false);
				}
				yield new Step(new State(Phase.CHARGE, ticks, cooldown), false);
			}
			case RETREAT -> ticks >= settings.retreatTicks()
					? new Step(new State(Phase.DONE, 0, cooldown), false)
					: new Step(new State(Phase.RETREAT, ticks, cooldown), false);
			case DONE -> new Step(state, false);
		};
	}
}
