package io.github.hellenter.eam.mobs.entity.lost;

/** What one of the Lost does in his squad. */
public enum LostRole {
	/** Sword and shield, the best gear; presses the player into melee. */
	LEADER,
	/** Sword and shield; stands between the player and the squad's archers. */
	SWORDSMAN,
	/** Spear and shield; like the swordsman, with a longer jab instead of cuts. */
	SPEARMAN,
	/** Bow; keeps away from the player and shoots. */
	ARCHER,
	/** The centurion ({@link LostCenturion}): long spear and shield, and a horn that calls up more of the Lost. */
	CENTURION,
	/** Spear and shield in a shield wall: with his mates he locks shields in a row in front of their commander. */
	WALL_SPEARMAN,
	/** The shield wall's commander ({@link LostWallCommander}): a crossbow, from behind the row. */
	WALL_COMMANDER,
	/** The longbowman ({@link LostLongbowman}): a longbow, from far behind; marks the player for the others. */
	LONGBOWMAN;

	public static LostRole byId(int id) {
		LostRole[] values = values();
		return id >= 0 && id < values.length ? values[id] : SWORDSMAN;
	}

	/** Whether he carries a shield and stands in the way. */
	public boolean shielded() {
		return !shooter();
	}

	/** Whether he shoots and leaves sword and spear fighting to others. */
	public boolean shooter() {
		return this == ARCHER || this == WALL_COMMANDER || this == LONGBOWMAN;
	}

	/** Whether he takes a spot between the player and the squad's archers rather than pressing in. */
	public boolean guards() {
		return this == SWORDSMAN || this == SPEARMAN;
	}

	/** Whether he fights with spear jabs. */
	public boolean spear() {
		return this == SPEARMAN || this == CENTURION || this == WALL_SPEARMAN;
	}
}
