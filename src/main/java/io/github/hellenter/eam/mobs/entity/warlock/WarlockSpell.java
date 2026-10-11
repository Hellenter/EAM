package io.github.hellenter.eam.mobs.entity.warlock;

/** What the undead warlock is casting; synced so clients play its gestures. */
public enum WarlockSpell {
	NONE(0, 0),
	/** Right hand thrust at the target, red missiles leave it one after another from {@link #release()} on. */
	MISSILES(16, 0),
	/** Both palms pushed forward: a gust throws everything in front of him back. */
	WIND(10, 6),
	/** Both arms raised: the Lost around him get Speed I or Strength I. */
	BUFF(24, 6),
	/** Arms spread wide, head thrown back: a flock of vampire bats comes. */
	BATS(30, 8);

	private final int release;
	private final int after;

	WarlockSpell(int release, int after) {
		this.release = release;
		this.after = after;
	}

	/** Tick of the cast on which the spell goes off. */
	public int release() {
		return release;
	}

	/** Whole length of the cast in ticks; missiles take longer the more there are. */
	public int length(int missiles) {
		return this == MISSILES ? UndeadWarlockLogic.missileTick(missiles - 1) + 4 : release + after;
	}

	public static WarlockSpell byId(int id) {
		WarlockSpell[] values = values();
		return id >= 0 && id < values.length ? values[id] : NONE;
	}
}
