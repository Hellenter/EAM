package io.github.hellenter.eam.content.sieve;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for the sieve, read from {@code data/eam/sieve/settings.json} so datapacks can override them. What
 * a sift gives is the loot table {@code data/eam/loot_table/gameplay/sieve.json}.
 *
 * @param siftTicks   ticks one gravel takes to sift (100 = 5 seconds)
 * @param minSticks   fewest sticks a worn-out sieve falls apart into
 * @param maxSticks   most sticks a worn-out sieve falls apart into
 */
public record SieveSettings(int siftTicks, int minSticks, int maxSticks) {
	public static final SieveSettings DEFAULT = new SieveSettings(100, 1, 2);

	public static final Codec<SieveSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(1, Integer.MAX_VALUE).fieldOf("sift_ticks").orElse(DEFAULT.siftTicks).forGetter(SieveSettings::siftTicks),
			Codec.intRange(0, 64).fieldOf("min_sticks").orElse(DEFAULT.minSticks).forGetter(SieveSettings::minSticks),
			Codec.intRange(0, 64).fieldOf("max_sticks").orElse(DEFAULT.maxSticks).forGetter(SieveSettings::maxSticks)
	).apply(instance, SieveSettings::new));
}
