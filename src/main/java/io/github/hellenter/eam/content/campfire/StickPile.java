package io.github.hellenter.eam.content.campfire;

/**
 * The sticks in a camp fire as two groups: old sticks that have burnt longer and blacken first, and sticks put in
 * since, which start fresh. Old sticks fill positions 1 to {@code old}, new ones the positions above. The fire burns
 * the oldest stick first; when the old group is gone the new one takes its place.
 *
 * @param sticks  all sticks, 0 to {@link CampFireBlock#MAX_STICKS}
 * @param old     sticks in the old group, at most {@code sticks}
 * @param oldChar how burnt the old sticks look
 * @param newChar how burnt the new sticks look
 */
public record StickPile(int sticks, int old, int oldChar, int newChar) {
	public static final StickPile EMPTY = new StickPile(0, 0, CampFireBlock.FRESH, CampFireBlock.FRESH);

	public int fresh() {
		return sticks - old;
	}

	/** True when a stick put in now starts a new group, so the age of the new group must start from zero. */
	public boolean startsNewGroup() {
		return oldChar != CampFireBlock.FRESH && (fresh() == 0 || newChar != CampFireBlock.FRESH);
	}

	/**
	 * One more stick, always fresh. While nothing has started to blacken it joins the others; otherwise it joins the
	 * fresh new group, or starts one, and new sticks that already darkened join the old group.
	 */
	public StickPile add() {
		if (oldChar == CampFireBlock.FRESH) {
			return new StickPile(sticks + 1, sticks + 1, CampFireBlock.FRESH, CampFireBlock.FRESH);
		}
		if (fresh() > 0 && newChar == CampFireBlock.FRESH) {
			return new StickPile(sticks + 1, old, oldChar, CampFireBlock.FRESH);
		}
		return new StickPile(sticks + 1, sticks, oldChar, CampFireBlock.FRESH);
	}

	/** New looks for both groups; groups that look the same merge into one. */
	public StickPile withChar(int oldStage, int newStage) {
		if (fresh() > 0 && newStage == oldStage) {
			return new StickPile(sticks, sticks, oldStage, CampFireBlock.FRESH);
		}
		return new StickPile(sticks, old, oldStage, fresh() > 0 ? newStage : CampFireBlock.FRESH);
	}

	/** The oldest stick burns away; see {@link #burnPromotes()}. */
	public StickPile burn() {
		if (sticks <= 1) {
			return EMPTY;
		}
		if (old > 1) {
			return new StickPile(sticks - 1, old - 1, oldChar, newChar);
		}
		// The last old stick is gone: the new sticks are now the oldest.
		return new StickPile(sticks - 1, sticks - 1, newChar, CampFireBlock.FRESH);
	}

	/** True when burning one stick from this pile hands the old role to the new group. */
	public boolean burnPromotes() {
		return sticks > 1 && old <= 1;
	}
}
