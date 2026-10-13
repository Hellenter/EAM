package io.github.hellenter.eam.mobs.entity.warg;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for warg packs, read from {@code data/eam/mobs/warg.json} so datapacks can override them. A missing
 * or invalid field falls back to its default. Times are in ticks, distances in blocks, angles in degrees.
 *
 * @param seenAngle      a warg this close to where the player looks counts as watched: it never strikes then
 * @param ringRadius     how far from the player the pack circles
 * @param leaderRing     the leader circles this much further out
 * @param ringSpin       how fast the ring turns around the player, degrees per second
 * @param strikeInterval ticks between one warg's strike and the next one's
 * @param strikeTimeout  a strike that has not landed in this many ticks is called off
 * @param abortDistance  a striker the player turns to face further away than this backs off
 * @param aggroRange     the leader picks a player this close for the pack's hunt
 * @param fleeTicks      how long the pack runs flat out after its leader dies; it keeps away from players after
 * @param packMin        the fewest wargs a leader brings
 * @param packMax        and the most
 * @param spawnChance    chance that a leader's natural spawn goes ahead
 * @param packSpacing    no new pack spawns this close to another leader
 */
public record WargSettings(double seenAngle, double ringRadius, double leaderRing, double ringSpin, int strikeInterval, int strikeTimeout,
		double abortDistance, double aggroRange, int fleeTicks, int packMin, int packMax, float spawnChance, double packSpacing) {
	public static final WargSettings DEFAULT = new WargSettings(60.0, 6.0, 2.5, 8.0, 40, 80, 3.5, 20.0, 400, 3, 5, 0.3F, 96.0);

	public static final Codec<WargSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.doubleRange(10.0, 180.0).fieldOf("seen_angle").orElse(DEFAULT.seenAngle).forGetter(WargSettings::seenAngle),
			Codec.doubleRange(2.0, 16.0).fieldOf("ring_radius").orElse(DEFAULT.ringRadius).forGetter(WargSettings::ringRadius),
			Codec.doubleRange(0.0, 16.0).fieldOf("leader_ring").orElse(DEFAULT.leaderRing).forGetter(WargSettings::leaderRing),
			Codec.doubleRange(0.0, 90.0).fieldOf("ring_spin").orElse(DEFAULT.ringSpin).forGetter(WargSettings::ringSpin),
			Codec.intRange(0, 1200).fieldOf("strike_interval").orElse(DEFAULT.strikeInterval).forGetter(WargSettings::strikeInterval),
			Codec.intRange(20, 1200).fieldOf("strike_timeout").orElse(DEFAULT.strikeTimeout).forGetter(WargSettings::strikeTimeout),
			Codec.doubleRange(0.0, 16.0).fieldOf("abort_distance").orElse(DEFAULT.abortDistance).forGetter(WargSettings::abortDistance),
			Codec.doubleRange(4.0, 64.0).fieldOf("aggro_range").orElse(DEFAULT.aggroRange).forGetter(WargSettings::aggroRange),
			Codec.intRange(0, 12000).fieldOf("flee_ticks").orElse(DEFAULT.fleeTicks).forGetter(WargSettings::fleeTicks),
			Codec.intRange(0, 12).fieldOf("pack_min").orElse(DEFAULT.packMin).forGetter(WargSettings::packMin),
			Codec.intRange(0, 12).fieldOf("pack_max").orElse(DEFAULT.packMax).forGetter(WargSettings::packMax),
			Codec.floatRange(0.0F, 1.0F).fieldOf("spawn_chance").orElse(DEFAULT.spawnChance).forGetter(WargSettings::spawnChance),
			Codec.doubleRange(0.0, 256.0).fieldOf("pack_spacing").orElse(DEFAULT.packSpacing).forGetter(WargSettings::packSpacing)
	).apply(instance, WargSettings::new));
}
