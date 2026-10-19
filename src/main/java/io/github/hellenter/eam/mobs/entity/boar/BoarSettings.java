package io.github.hellenter.eam.mobs.entity.boar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for underground boars, read from {@code data/eam/mobs/boar.json} so datapacks can override them. A
 * missing or invalid field falls back to its default.
 *
 * @param damage      damage of a tusk hit (2 is one heart, the same as a cow's kick)
 * @param knockback   how hard a hit throws the player back
 * @param chaseSpeed  speed multiplier while a boar goes for its attacker
 * @param spawnChance chance that a natural spawn goes ahead, so the caves do not fill up at once
 */
public record BoarSettings(float damage, float knockback, double chaseSpeed, float spawnChance) {
	public static final BoarSettings DEFAULT = new BoarSettings(2.0F, 0.5F, 1.4, 0.5F);

	public static final Codec<BoarSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.floatRange(0.0F, 100.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(BoarSettings::damage),
			Codec.floatRange(0.0F, 5.0F).fieldOf("knockback").orElse(DEFAULT.knockback).forGetter(BoarSettings::knockback),
			Codec.doubleRange(0.1, 4.0).fieldOf("chase_speed").orElse(DEFAULT.chaseSpeed).forGetter(BoarSettings::chaseSpeed),
			Codec.floatRange(0.0F, 1.0F).fieldOf("spawn_chance").orElse(DEFAULT.spawnChance).forGetter(BoarSettings::spawnChance)
	).apply(instance, BoarSettings::new));
}
