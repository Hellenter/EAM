package io.github.hellenter.eam.mobs.entity.boss;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A second parry against the same boss soon after a successful one is harder: a shorter window and a smaller damage
 * bonus. Read from {@code data/eam/mobs/boss_parry.json}; shared by every boss that can be parried.
 *
 * @param memory    ticks after a successful parry in which the next parry against that boss is penalized
 * @param windowCut ticks taken off the parry window
 * @param bonusCut  taken off the parrying hit's damage multiplier (1.5 becomes 1.25 at 0.25)
 */
public record ParryPenalty(int memory, int windowCut, float bonusCut) {
	public static final ParryPenalty DEFAULT = new ParryPenalty(160, 1, 0.25F);

	public static final Codec<ParryPenalty> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(0, 24000).fieldOf("memory").orElse(DEFAULT.memory).forGetter(ParryPenalty::memory),
			Codec.intRange(0, 200).fieldOf("window_cut").orElse(DEFAULT.windowCut).forGetter(ParryPenalty::windowCut),
			Codec.floatRange(0.0F, 10.0F).fieldOf("bonus_cut").orElse(DEFAULT.bonusCut).forGetter(ParryPenalty::bonusCut)
	).apply(instance, ParryPenalty::new));

	/** The parry window in ticks, shortened when penalized. */
	public int window(int base, boolean penalized) {
		return penalized ? Math.max(1, base - windowCut) : base;
	}

	/** The parrying hit's damage multiplier, its bonus cut when penalized; never below a plain hit. */
	public float multiplier(float base, boolean penalized) {
		return penalized ? Math.max(Math.min(base, 1.0F), base - bonusCut) : base;
	}
}
