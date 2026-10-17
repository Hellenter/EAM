package io.github.hellenter.eam.survival.sleep;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The «Rested» buff a player gets after sleeping through the night in a safe room. The {@code rested} object of
 * {@code data/eam/sleep/safe_room.json}.
 *
 * @param durationTicks how long the buff lasts
 * @param miningBonus   mining gets this fraction faster
 * @param drainFactor   food and water drain at this fraction of the normal rate
 * @param regenFactor   natural regeneration runs this many times faster
 */
public record RestedSettings(int durationTicks, float miningBonus, float drainFactor, float regenFactor) {
	public static final RestedSettings DEFAULT = new RestedSettings(12_000, 0.1F, 0.9F, 1.1F);

	public static final Codec<RestedSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("duration_ticks", DEFAULT.durationTicks).forGetter(RestedSettings::durationTicks),
			Codec.floatRange(0.0F, 10.0F).optionalFieldOf("mining_bonus", DEFAULT.miningBonus).forGetter(RestedSettings::miningBonus),
			Codec.floatRange(0.01F, 10.0F).optionalFieldOf("drain_factor", DEFAULT.drainFactor).forGetter(RestedSettings::drainFactor),
			Codec.floatRange(0.01F, 10.0F).optionalFieldOf("regen_factor", DEFAULT.regenFactor).forGetter(RestedSettings::regenFactor)
	).apply(instance, RestedSettings::new));
}
