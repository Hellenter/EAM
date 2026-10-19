package io.github.hellenter.eam.mobs.entity.ram;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for cave rams, read from {@code data/eam/mobs/cave_ram.json} so datapacks can override them. A
 * missing or invalid field falls back to its default.
 *
 * @param health      health of a grown ram
 * @param speed       movement speed; a boar's is 0.25
 * @param damage      damage of a butt with the horns
 * @param knockback   how hard a butt throws the player back; a boar's is 0.5
 * @param chaseSpeed  speed multiplier while a ram goes for its attacker
 * @param spawnChance chance that a natural spawn goes ahead, so the caves do not fill up at once
 */
public record CaveRamSettings(float health, double speed, float damage, float knockback, double chaseSpeed, float spawnChance) {
	public static final CaveRamSettings DEFAULT = new CaveRamSettings(12.0F, 0.27, 3.0F, 1.5F, 1.45, 0.5F);

	public static final Codec<CaveRamSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.floatRange(1.0F, 1024.0F).fieldOf("health").orElse(DEFAULT.health).forGetter(CaveRamSettings::health),
			Codec.doubleRange(0.05, 1.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(CaveRamSettings::speed),
			Codec.floatRange(0.0F, 100.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(CaveRamSettings::damage),
			Codec.floatRange(0.0F, 5.0F).fieldOf("knockback").orElse(DEFAULT.knockback).forGetter(CaveRamSettings::knockback),
			Codec.doubleRange(0.1, 4.0).fieldOf("chase_speed").orElse(DEFAULT.chaseSpeed).forGetter(CaveRamSettings::chaseSpeed),
			Codec.floatRange(0.0F, 1.0F).fieldOf("spawn_chance").orElse(DEFAULT.spawnChance).forGetter(CaveRamSettings::spawnChance)
	).apply(instance, CaveRamSettings::new));
}
