package io.github.hellenter.eam.mobs.entity.upyr;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for the upyr and the wards against bloodsuckers, read from {@code data/eam/mobs/upyr.json} so
 * datapacks can override them. A missing or invalid field falls back to its default. Times are in ticks, distances in
 * blocks.
 *
 * @param weaknessTicks      how long a bite leaves its victim weakened
 * @param weaknessLevel      level of that weakness, 0 being Weakness I
 * @param healFactor         the upyr heals the health its bite took, times this
 * @param garlicDamageFactor upyrs and vampire bats deal this share of their damage to a victim who ate garlic
 * @param blackBloodDamage   damage an upyr or vampire bat takes each time it hurts a victim with black blood in them
 * @param checkInterval      ticks between two spawn checks around each player
 * @param spawnChance        chance at each check, at night by an abandoned village, that an upyr crawls out
 * @param maxNearby          no new upyr while this many are within 48 blocks of the player
 * @param minDistance        a new upyr appears at least this far from the player
 * @param maxDistance        and at most this far
 */
public record UpyrSettings(int weaknessTicks, int weaknessLevel, float healFactor, float garlicDamageFactor, float blackBloodDamage,
		int checkInterval, float spawnChance, int maxNearby, double minDistance, double maxDistance) {
	public static final UpyrSettings DEFAULT = new UpyrSettings(200, 0, 1.0F, 0.5F, 2.0F, 200, 0.2F, 2, 20.0, 36.0);

	public static final Codec<UpyrSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(0, 72000).fieldOf("weakness_ticks").orElse(DEFAULT.weaknessTicks).forGetter(UpyrSettings::weaknessTicks),
			Codec.intRange(0, 4).fieldOf("weakness_level").orElse(DEFAULT.weaknessLevel).forGetter(UpyrSettings::weaknessLevel),
			Codec.floatRange(0.0F, 10.0F).fieldOf("heal_factor").orElse(DEFAULT.healFactor).forGetter(UpyrSettings::healFactor),
			Codec.floatRange(0.0F, 1.0F).fieldOf("garlic_damage_factor").orElse(DEFAULT.garlicDamageFactor)
					.forGetter(UpyrSettings::garlicDamageFactor),
			Codec.floatRange(0.0F, 100.0F).fieldOf("black_blood_damage").orElse(DEFAULT.blackBloodDamage)
					.forGetter(UpyrSettings::blackBloodDamage),
			Codec.intRange(20, 24000).fieldOf("check_interval").orElse(DEFAULT.checkInterval).forGetter(UpyrSettings::checkInterval),
			Codec.floatRange(0.0F, 1.0F).fieldOf("spawn_chance").orElse(DEFAULT.spawnChance).forGetter(UpyrSettings::spawnChance),
			Codec.intRange(0, 64).fieldOf("max_nearby").orElse(DEFAULT.maxNearby).forGetter(UpyrSettings::maxNearby),
			Codec.doubleRange(8.0, 96.0).fieldOf("min_distance").orElse(DEFAULT.minDistance).forGetter(UpyrSettings::minDistance),
			Codec.doubleRange(8.0, 96.0).fieldOf("max_distance").orElse(DEFAULT.maxDistance).forGetter(UpyrSettings::maxDistance)
	).apply(instance, UpyrSettings::new));
}
