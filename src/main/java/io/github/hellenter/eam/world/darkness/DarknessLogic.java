package io.github.hellenter.eam.world.darkness;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure rules for darker nights and caves and for fires the rain puts out. The night numbers end up in the
 * {@code eam:darkness} timeline, so datapacks can override them there.
 */
public final class DarknessLogic {
	/** Ticks in a day, and the day ticks where vanilla's own sky light fades out at dusk and back in at dawn. */
	public static final int DAY_TICKS = 24000;
	public static final int DAWN_END = 730;
	public static final int DUSK_START = 11270;
	public static final int NIGHT_START = 13140;
	public static final int NIGHT_END = 22860;

	/**
	 * How much of vanilla's night sky light is left, per moon phase in {@code minecraft:moon} order: full moon,
	 * waning gibbous, third quarter, waning crescent, new moon, waxing crescent, first quarter, waxing gibbous.
	 */
	public static final float[] NIGHT_SKY_FACTOR = {0.8F, 0.6F, 0.45F, 0.3F, 0.15F, 0.3F, 0.45F, 0.6F};

	/** Ambient light left in the overworld, so a cave without torches is close to black. */
	public static final float OVERWORLD_AMBIENT = 0.25F;

	/** Highest brightness (gamma) setting that still counts: 0.5 is vanilla's default, "Bright" acts as default. */
	public static final float MAX_BRIGHTNESS = 0.5F;

	private DarknessLogic() {
	}

	public record Keyframe(int ticks, float value) {
	}

	/**
	 * Keyframes over a whole moon cycle for a sky light multiplier: 1 by day, {@code factors[day]} through the night,
	 * fading in step with vanilla's dusk and dawn.
	 */
	public static List<Keyframe> nightKeyframes(float[] factors) {
		List<Keyframe> keyframes = new ArrayList<>();
		for (int day = 0; day < factors.length; day++) {
			int start = day * DAY_TICKS;
			keyframes.add(new Keyframe(start + DAWN_END, 1.0F));
			keyframes.add(new Keyframe(start + DUSK_START, 1.0F));
			keyframes.add(new Keyframe(start + NIGHT_START, factors[day]));
			keyframes.add(new Keyframe(start + NIGHT_END, factors[day]));
		}
		return keyframes;
	}

	/** The brightness setting the lightmap uses: the player's own, but no higher than {@code cap}. */
	public static float capBrightness(float option, float cap) {
		return Math.min(option, cap);
	}

	/** True when a fire out in the rain goes out on this random tick; {@code roll} is uniform in [0, 1). */
	public static boolean douses(boolean rainedOn, float chance, float roll) {
		return rainedOn && roll < chance;
	}
}
