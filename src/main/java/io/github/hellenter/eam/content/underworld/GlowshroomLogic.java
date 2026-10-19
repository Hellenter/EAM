package io.github.hellenter.eam.content.underworld;

/** Rules of glowshroom tips, apart from the world. */
public final class GlowshroomLogic {
	private GlowshroomLogic() {
	}

	/** Whether a tip ripens on this random tick: only a bare tip does, when {@code roll} (0..1) falls under the chance. */
	public static boolean ripens(boolean ripe, float roll, float ripenChance) {
		return !ripe && roll < ripenChance;
	}

	/** Light of a tip: the bright caps of a ripe one, or the faint glow of a bare one. */
	public static int light(boolean ripe) {
		return ripe ? 10 : 4;
	}
}
