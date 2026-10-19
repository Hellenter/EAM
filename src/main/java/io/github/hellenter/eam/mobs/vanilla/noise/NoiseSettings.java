package io.github.hellenter.eam.mobs.vanilla.noise;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for mobs hearing noisy players, read from {@code data/eam/mobs/noise.json} so datapacks can override
 * them. Every radius is in blocks; 0 makes that action silent. A missing or invalid field falls back to its default.
 *
 * @param digBase         breaking or starting to break a block is heard this far, plus {@link #digPerHardness}
 * @param digPerHardness  extra blocks per point of the block's hardness (dirt 0.5, stone 1.5)
 * @param digMax          digging is never heard further than this
 * @param sprint          sprinting, also while swimming
 * @param fight           hitting a mob or being hit by one
 * @param door            opening or closing a block in {@code #eam:noisy_when_used}: wooden doors, trapdoors, gates
 * @param sneakMultiplier a sneaking player's digging, fighting and doors are this much quieter
 * @param holdTicks       how long a mob can still hear the player after the noise
 * @param forgetTicks     a mob that heard the player gives up after this many ticks without seeing or hearing him
 */
public record NoiseSettings(
		float digBase,
		float digPerHardness,
		float digMax,
		float sprint,
		float fight,
		float door,
		float sneakMultiplier,
		int holdTicks,
		int forgetTicks
) {
	public static final NoiseSettings DEFAULT = new NoiseSettings(6.0F, 3.0F, 16.0F, 10.0F, 12.0F, 8.0F, 0.5F, 60, 200);

	private static final Codec<Float> RADIUS = Codec.floatRange(0.0F, 32.0F);

	public static final Codec<NoiseSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			RADIUS.fieldOf("dig_base").orElse(DEFAULT.digBase).forGetter(NoiseSettings::digBase),
			RADIUS.fieldOf("dig_per_hardness").orElse(DEFAULT.digPerHardness).forGetter(NoiseSettings::digPerHardness),
			RADIUS.fieldOf("dig_max").orElse(DEFAULT.digMax).forGetter(NoiseSettings::digMax),
			RADIUS.fieldOf("sprint").orElse(DEFAULT.sprint).forGetter(NoiseSettings::sprint),
			RADIUS.fieldOf("fight").orElse(DEFAULT.fight).forGetter(NoiseSettings::fight),
			RADIUS.fieldOf("door").orElse(DEFAULT.door).forGetter(NoiseSettings::door),
			Codec.floatRange(0.0F, 1.0F).fieldOf("sneak_multiplier").orElse(DEFAULT.sneakMultiplier).forGetter(NoiseSettings::sneakMultiplier),
			Codec.intRange(1, 1200).fieldOf("hold_ticks").orElse(DEFAULT.holdTicks).forGetter(NoiseSettings::holdTicks),
			Codec.intRange(1, 6000).fieldOf("forget_ticks").orElse(DEFAULT.forgetTicks).forGetter(NoiseSettings::forgetTicks)
	).apply(instance, NoiseSettings::new));
}
