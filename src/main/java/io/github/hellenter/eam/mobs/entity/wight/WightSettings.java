package io.github.hellenter.eam.mobs.entity.wight;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for the wight, read from {@code data/eam/mobs/wight.json} so datapacks can override them. A missing
 * or invalid field falls back to its default. Times are in ticks, distances in blocks.
 *
 * @param minDay            natural wights spawn only from this day of the world on (day 1 is the first)
 * @param spawnChance       a natural spawn that passed every other rule still happens only this often
 * @param stalkDistance     how far from its player it keeps while it stalks
 * @param raidChance        chance, checked once a second, that it raids a busy player it can see
 * @param raidCooldown      after a raid it only stalks for this long
 * @param raidHits          blows in one raid before it runs
 * @param huntHealth        below this share of their max health a player gets hunted down
 * @param huntHits          blows in one pass of a hunt before it runs and circles round behind them
 * @param huntCooldown      between two passes of a hunt
 * @param reach             how far its claws reach, from its centre to the player's
 * @param hitInterval       ticks between two blows
 * @param recloakDelay      ticks after its last blow before it turns invisible again
 * @param burningDamage     damage it takes while on fire, times the usual
 * @param groanInterval     ticks between its groans while it burns
 * @param giveUpDistance    a player this far away is no longer stalked
 */
public record WightSettings(int minDay, float spawnChance, double stalkDistance, float raidChance, int raidCooldown, int raidHits,
		double huntHealth, int huntHits, int huntCooldown, double reach, int hitInterval, int recloakDelay, float burningDamage,
		int groanInterval, double giveUpDistance) {
	public static final WightSettings DEFAULT = new WightSettings(20, 0.25F, 14.0, 0.35F, 2400, 2,
			1.0 / 3.0, 2, 50, 2.2, 12, 30, 2.0F, 30, 64.0);

	public static final Codec<WightSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(1, 100000).fieldOf("min_day").orElse(DEFAULT.minDay).forGetter(WightSettings::minDay),
			Codec.floatRange(0.0F, 1.0F).fieldOf("spawn_chance").orElse(DEFAULT.spawnChance).forGetter(WightSettings::spawnChance),
			Codec.doubleRange(4.0, 48.0).fieldOf("stalk_distance").orElse(DEFAULT.stalkDistance).forGetter(WightSettings::stalkDistance),
			Codec.floatRange(0.0F, 1.0F).fieldOf("raid_chance").orElse(DEFAULT.raidChance).forGetter(WightSettings::raidChance),
			Codec.intRange(0, 72000).fieldOf("raid_cooldown").orElse(DEFAULT.raidCooldown).forGetter(WightSettings::raidCooldown),
			Codec.intRange(1, 10).fieldOf("raid_hits").orElse(DEFAULT.raidHits).forGetter(WightSettings::raidHits),
			Codec.doubleRange(0.0, 1.0).fieldOf("hunt_health").orElse(DEFAULT.huntHealth).forGetter(WightSettings::huntHealth),
			Codec.intRange(1, 10).fieldOf("hunt_hits").orElse(DEFAULT.huntHits).forGetter(WightSettings::huntHits),
			Codec.intRange(0, 2400).fieldOf("hunt_cooldown").orElse(DEFAULT.huntCooldown).forGetter(WightSettings::huntCooldown),
			Codec.doubleRange(1.0, 6.0).fieldOf("reach").orElse(DEFAULT.reach).forGetter(WightSettings::reach),
			Codec.intRange(1, 200).fieldOf("hit_interval").orElse(DEFAULT.hitInterval).forGetter(WightSettings::hitInterval),
			Codec.intRange(0, 2400).fieldOf("recloak_delay").orElse(DEFAULT.recloakDelay).forGetter(WightSettings::recloakDelay),
			Codec.floatRange(1.0F, 10.0F).fieldOf("burning_damage").orElse(DEFAULT.burningDamage).forGetter(WightSettings::burningDamage),
			Codec.intRange(5, 400).fieldOf("groan_interval").orElse(DEFAULT.groanInterval).forGetter(WightSettings::groanInterval),
			Codec.doubleRange(16.0, 256.0).fieldOf("give_up_distance").orElse(DEFAULT.giveUpDistance).forGetter(WightSettings::giveUpDistance)
	).apply(instance, WightSettings::new));
}
