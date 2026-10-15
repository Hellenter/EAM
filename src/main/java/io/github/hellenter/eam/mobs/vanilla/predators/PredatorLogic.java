package io.github.hellenter.eam.mobs.vanilla.predators;

/** Pure rules for night raids on livestock. */
public final class PredatorLogic {
	private PredatorLogic() {
	}

	/** Whether a predator goes for an animal: only at night, and only where the block light is low enough. */
	public static boolean mayHunt(boolean dark, int blockLight, PredatorSettings.Group group) {
		return dark && blockLight <= group.maxBlockLight();
	}

	/** How far the predator looks for prey: its group's range, never beyond what the mob can follow. */
	public static double searchRange(double followRange, PredatorSettings.Group group) {
		return Math.min(followRange, group.range());
	}
}
