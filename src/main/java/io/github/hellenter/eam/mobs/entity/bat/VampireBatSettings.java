package io.github.hellenter.eam.mobs.entity.bat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for vampire bats, read from {@code data/eam/mobs/vampire_bat.json} so datapacks can override them.
 * A missing or invalid field falls back to its default. Times are in ticks.
 *
 * @param bleedingTicks   how long one bite makes the player bleed
 * @param attackCooldown  ticks between two bites of the same bat
 * @param chaseSpeed      flight speed toward its prey (a wandering bat flies at 0.5)
 * @param checkInterval   how often each player in a bat biome may get a flock
 * @param flockChance     chance of a flock at each check
 * @param minFlock        smallest flock
 * @param maxFlock        largest flock
 * @param minDistance     a flock appears at least this far from the player, horizontally
 * @param maxDistance     and at most this far
 * @param maxNearby       no new flock while this many vampire bats are within 64 blocks of the player
 */
public record VampireBatSettings(int bleedingTicks, int attackCooldown, double chaseSpeed, int checkInterval, float flockChance,
		int minFlock, int maxFlock, double minDistance, double maxDistance, int maxNearby) {
	public static final VampireBatSettings DEFAULT = new VampireBatSettings(60, 20, 0.7, 200, 0.06F, 4, 6, 40.0, 50.0, 10);

	public static final Codec<VampireBatSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(0, 2400).fieldOf("bleeding_ticks").orElse(DEFAULT.bleedingTicks).forGetter(VampireBatSettings::bleedingTicks),
			Codec.intRange(1, 1200).fieldOf("attack_cooldown").orElse(DEFAULT.attackCooldown).forGetter(VampireBatSettings::attackCooldown),
			Codec.doubleRange(0.1, 2.0).fieldOf("chase_speed").orElse(DEFAULT.chaseSpeed).forGetter(VampireBatSettings::chaseSpeed),
			Codec.intRange(1, 72000).fieldOf("check_interval").orElse(DEFAULT.checkInterval).forGetter(VampireBatSettings::checkInterval),
			Codec.floatRange(0.0F, 1.0F).fieldOf("flock_chance").orElse(DEFAULT.flockChance).forGetter(VampireBatSettings::flockChance),
			Codec.intRange(1, 32).fieldOf("min_flock").orElse(DEFAULT.minFlock).forGetter(VampireBatSettings::minFlock),
			Codec.intRange(1, 32).fieldOf("max_flock").orElse(DEFAULT.maxFlock).forGetter(VampireBatSettings::maxFlock),
			Codec.doubleRange(8.0, 128.0).fieldOf("min_distance").orElse(DEFAULT.minDistance).forGetter(VampireBatSettings::minDistance),
			Codec.doubleRange(8.0, 128.0).fieldOf("max_distance").orElse(DEFAULT.maxDistance).forGetter(VampireBatSettings::maxDistance),
			Codec.intRange(0, 256).fieldOf("max_nearby").orElse(DEFAULT.maxNearby).forGetter(VampireBatSettings::maxNearby)
	).apply(instance, VampireBatSettings::new));
}
