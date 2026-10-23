package io.github.hellenter.eam.mobs.entity.lost;

/** The straggler's call for help and his spawning, free of game state so they can be unit tested. */
public final class LostStragglerLogic {
	/** Out of a fight his count of it fades this much faster than it grew. */
	private static final int FADE = 2;

	private LostStragglerLogic() {
	}

	/** His count of fight ticks after one more tick, {@code fighting} a player or not. */
	public static int fightTicks(int ticks, boolean fighting) {
		return fighting ? ticks + 1 : Math.max(0, ticks - FADE);
	}

	/** Whether he calls the Lost now: once per life, after a long enough fight, and only while he has no squad. */
	public static boolean callDue(int fightTicks, int callAfter, boolean called, boolean inSquad) {
		return !called && !inSquad && fightTicks >= callAfter;
	}

	/** Whether a straggler may spawn naturally, with {@code tooClose} others within the spacing and {@code around} in the range. */
	public static boolean spawnAllowed(int tooClose, int around, int maxStragglers) {
		return tooClose == 0 && around < maxStragglers;
	}
}
