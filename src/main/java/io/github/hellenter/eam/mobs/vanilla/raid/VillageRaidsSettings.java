package io.github.hellenter.eam.mobs.vanilla.raid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * Village raid numbers, read from {@code data/eam/events/village_raids.json} so datapacks can override them. A missing
 * or invalid field falls back to its default.
 *
 * @param intervalMinDays a random raid comes at least this many days after the previous one (the first one: after the world's
 *                        first day)
 * @param intervalMaxDays ... and is due at most this many days after it; it then waits for a player in a village
 * @param checkTicks      how often a due raid looks for a player in a village
 * @param villagerRadius  a random raid only comes to a village with a villager this close to the player
 * @param helmRadius      a lit King's helm keeps raids away this far around
 * @param glowSeconds     raiders of a wave that lasts longer than this start to glow
 * @param guardsPerWave   pillager guards added to waves 1, 2, 3...; the last number counts for later waves
 * @param elitesPerWave   elite crossbowmen added to waves 1, 2, 3...; the last number counts for later waves
 */
public record VillageRaidsSettings(int intervalMinDays, int intervalMaxDays, int checkTicks, int villagerRadius, int helmRadius,
		int glowSeconds, List<Integer> guardsPerWave, List<Integer> elitesPerWave) {
	public static final VillageRaidsSettings DEFAULT = new VillageRaidsSettings(15, 25, 200, 48, 200, 120,
			List.of(1, 1, 2, 2, 3, 3, 3, 4), List.of(0, 1, 1, 2, 2, 3, 3, 3));

	private static final Codec<List<Integer>> COUNTS = Codec.intRange(0, 32).listOf(1, 64);

	public static final Codec<VillageRaidsSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(1, 100_000).fieldOf("interval_min_days").orElse(DEFAULT.intervalMinDays).forGetter(VillageRaidsSettings::intervalMinDays),
			Codec.intRange(1, 100_000).fieldOf("interval_max_days").orElse(DEFAULT.intervalMaxDays).forGetter(VillageRaidsSettings::intervalMaxDays),
			Codec.intRange(20, 24_000).fieldOf("check_ticks").orElse(DEFAULT.checkTicks).forGetter(VillageRaidsSettings::checkTicks),
			Codec.intRange(1, 128).fieldOf("villager_radius").orElse(DEFAULT.villagerRadius).forGetter(VillageRaidsSettings::villagerRadius),
			Codec.intRange(1, 512).fieldOf("helm_radius").orElse(DEFAULT.helmRadius).forGetter(VillageRaidsSettings::helmRadius),
			Codec.intRange(1, 3600).fieldOf("glow_seconds").orElse(DEFAULT.glowSeconds).forGetter(VillageRaidsSettings::glowSeconds),
			COUNTS.fieldOf("guards_per_wave").orElse(DEFAULT.guardsPerWave).forGetter(VillageRaidsSettings::guardsPerWave),
			COUNTS.fieldOf("elites_per_wave").orElse(DEFAULT.elitesPerWave).forGetter(VillageRaidsSettings::elitesPerWave)
	).apply(instance, VillageRaidsSettings::new));
}
