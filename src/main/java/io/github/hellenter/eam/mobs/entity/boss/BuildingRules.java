package io.github.hellenter.eam.mobs.entity.boss;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Which player-placed blocks the bosses may break. Read from {@code data/eam/mobs/boss_building.json}; shared by every
 * boss that takes towers and walls apart.
 *
 * @param radius    a block placed within this many blocks of a fighting boss is remembered
 * @param keepTicks it is remembered this long after it was placed
 * @param topple    when a boss breaks one, at most this many remembered blocks stacked on it fall down with it
 */
public record BuildingRules(double radius, int keepTicks, int topple) {
	public static final BuildingRules DEFAULT = new BuildingRules(32.0, 6000, 24);

	public static final Codec<BuildingRules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.doubleRange(1.0, 128.0).fieldOf("radius").orElse(DEFAULT.radius).forGetter(BuildingRules::radius),
			Codec.intRange(1, 1_728_000).fieldOf("keep_ticks").orElse(DEFAULT.keepTicks).forGetter(BuildingRules::keepTicks),
			Codec.intRange(0, 256).fieldOf("topple").orElse(DEFAULT.topple).forGetter(BuildingRules::topple)
	).apply(instance, BuildingRules::new));

	/** Whether a block remembered until {@code until} still counts at {@code now}. */
	public static boolean remembered(long until, long now) {
		return until > now;
	}
}
