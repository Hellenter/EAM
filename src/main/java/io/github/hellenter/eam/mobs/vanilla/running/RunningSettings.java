package io.github.hellenter.eam.mobs.vanilla.running;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for hostile mobs running after a target, read from {@code data/eam/mobs/running.json} so datapacks
 * can override them. A missing or invalid field falls back to its default.
 *
 * @param chasePace how fast a chasing mob moves as a fraction of a walking player's pace. Below 1 a walking player
 *                  still gets away slowly; vanilla zombies move at 0.53.
 */
public record RunningSettings(double chasePace) {
	public static final RunningSettings DEFAULT = new RunningSettings(0.85);

	public static final Codec<RunningSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.doubleRange(0.0, 2.0).fieldOf("chase_pace").orElse(DEFAULT.chasePace).forGetter(RunningSettings::chasePace)
	).apply(instance, RunningSettings::new));
}
