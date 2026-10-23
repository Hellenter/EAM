package io.github.hellenter.eam.mobs.entity.shambler;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for the mushroom shambler and its smoldering kin, read from {@code data/eam/mobs/mushroom_shambler.json}
 * so datapacks can override them. A missing or invalid field falls back to its default. Times are in ticks, distances
 * in blocks.
 *
 * @param spawnChance      a natural spawn that passed every other rule still happens only this often
 * @param maxSpawnLight    no natural spawns where block light is above this
 * @param wakeDistance     a disguised shambler stands up when a player comes this close
 * @param disguiseDelay    after this long with nobody to fight and no player near, it settles down as a mushroom again
 * @param sporeRadius      how far the spores (or the sparks of a smoldering one) reach from it when it is hit
 * @param nauseaTicks      how long the spores make a creature sick
 * @param sparkFireTicks   how long the sparks, and the blows, of a smoldering one set a creature on fire
 * @param burnTicks        a plain shambler that catches fire burns at least this long
 * @param burningDamage    damage a plain shambler takes while it burns, times the usual
 * @param snowballDamage   what a snowball does to a smoldering one
 */
public record ShamblerSettings(float spawnChance, int maxSpawnLight, double wakeDistance, int disguiseDelay, double sporeRadius,
		int nauseaTicks, int sparkFireTicks, int burnTicks, float burningDamage, float snowballDamage) {
	public static final ShamblerSettings DEFAULT = new ShamblerSettings(0.6F, 7, 5.0, 300, 2.5, 80, 40, 200, 2.0F, 3.0F);

	public static final Codec<ShamblerSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.floatRange(0.0F, 1.0F).fieldOf("spawn_chance").orElse(DEFAULT.spawnChance).forGetter(ShamblerSettings::spawnChance),
			Codec.intRange(0, 15).fieldOf("max_spawn_light").orElse(DEFAULT.maxSpawnLight).forGetter(ShamblerSettings::maxSpawnLight),
			Codec.doubleRange(1.0, 32.0).fieldOf("wake_distance").orElse(DEFAULT.wakeDistance).forGetter(ShamblerSettings::wakeDistance),
			Codec.intRange(20, 72000).fieldOf("disguise_delay").orElse(DEFAULT.disguiseDelay).forGetter(ShamblerSettings::disguiseDelay),
			Codec.doubleRange(0.5, 8.0).fieldOf("spore_radius").orElse(DEFAULT.sporeRadius).forGetter(ShamblerSettings::sporeRadius),
			Codec.intRange(0, 1200).fieldOf("nausea_ticks").orElse(DEFAULT.nauseaTicks).forGetter(ShamblerSettings::nauseaTicks),
			Codec.intRange(0, 1200).fieldOf("spark_fire_ticks").orElse(DEFAULT.sparkFireTicks).forGetter(ShamblerSettings::sparkFireTicks),
			Codec.intRange(0, 2400).fieldOf("burn_ticks").orElse(DEFAULT.burnTicks).forGetter(ShamblerSettings::burnTicks),
			Codec.floatRange(1.0F, 10.0F).fieldOf("burning_damage").orElse(DEFAULT.burningDamage).forGetter(ShamblerSettings::burningDamage),
			Codec.floatRange(0.0F, 100.0F).fieldOf("snowball_damage").orElse(DEFAULT.snowballDamage).forGetter(ShamblerSettings::snowballDamage)
	).apply(instance, ShamblerSettings::new));
}
