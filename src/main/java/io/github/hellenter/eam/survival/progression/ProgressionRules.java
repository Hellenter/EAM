package io.github.hellenter.eam.survival.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * How the player grows with XP level, loaded from {@code data/<namespace>/survival/progression.json} so a datapack
 * can override it; missing keys fall back to {@link #DEFAULT}. One capacity curve sizes the health, food and water
 * bars, as in MITE.
 *
 * @param baseCapacity      bar size (in half icons) at XP level 0
 * @param capacityStep      how much every bar grows per step
 * @param levelsPerStep     XP levels per growth step
 * @param maxCapacity       largest bar size
 * @param gracePeriodTicks  after the first spawn and after every death, hunger and thirst do not drain for this long
 */
public record ProgressionRules(int baseCapacity, int capacityStep, int levelsPerStep, int maxCapacity, int gracePeriodTicks) {
	/** Vanilla size of the health, food and water bars. */
	public static final int VANILLA_CAPACITY = 20;

	public static final ProgressionRules DEFAULT = new ProgressionRules(6, 2, 5, VANILLA_CAPACITY, 2400);

	public static final Codec<ProgressionRules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(1, VANILLA_CAPACITY).optionalFieldOf("base_capacity", DEFAULT.baseCapacity).forGetter(ProgressionRules::baseCapacity),
			Codec.intRange(0, VANILLA_CAPACITY).optionalFieldOf("capacity_step", DEFAULT.capacityStep).forGetter(ProgressionRules::capacityStep),
			Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("levels_per_step", DEFAULT.levelsPerStep).forGetter(ProgressionRules::levelsPerStep),
			Codec.intRange(1, VANILLA_CAPACITY).optionalFieldOf("max_capacity", DEFAULT.maxCapacity).forGetter(ProgressionRules::maxCapacity),
			Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("grace_period_ticks", DEFAULT.gracePeriodTicks).forGetter(ProgressionRules::gracePeriodTicks)
	).apply(instance, ProgressionRules::new));
}
