package io.github.hellenter.eam.mobs.vanilla.villager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for the butcher, read from {@code data/eam/villagers/butcher.json} so datapacks can override them.
 *
 * @param intervalTicks how often, in ticks, a working butcher may slaughter an animal
 * @param radius        how far, in blocks, he looks for livestock
 * @param keepPerKind   grown animals of each kind he always leaves alive, so the herd can breed
 * @param foodMax       villager food points he holds at most; with more he waits until he has given some away
 * @param penCells      ground an animal can walk over beyond which it is not in a pen
 */
public record ButcherSettings(int intervalTicks, int radius, int keepPerKind, int foodMax, int penCells) {
	public static final ButcherSettings DEFAULT = new ButcherSettings(2400, 16, 3, 32, 1024);

	public static final Codec<ButcherSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(20, 1_000_000).fieldOf("interval_ticks").orElse(DEFAULT.intervalTicks).forGetter(ButcherSettings::intervalTicks),
			Codec.intRange(1, 64).fieldOf("radius").orElse(DEFAULT.radius).forGetter(ButcherSettings::radius),
			Codec.intRange(0, 1000).fieldOf("keep_per_kind").orElse(DEFAULT.keepPerKind).forGetter(ButcherSettings::keepPerKind),
			Codec.intRange(0, 10000).fieldOf("food_max").orElse(DEFAULT.foodMax).forGetter(ButcherSettings::foodMax),
			Codec.intRange(16, 100_000).fieldOf("pen_cells").orElse(DEFAULT.penCells).forGetter(ButcherSettings::penCells)
	).apply(instance, ButcherSettings::new));
}
