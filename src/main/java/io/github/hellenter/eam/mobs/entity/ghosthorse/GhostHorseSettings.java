package io.github.hellenter.eam.mobs.entity.ghosthorse;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for the ghost horse, read from {@code data/eam/mobs/ghost_horse.json} so datapacks can override them.
 * A missing or invalid field falls back to its default.
 *
 * @param health          its own health
 * @param overflowHealth  how high its master's blows can heal it past its own health
 * @param regen           health it heals each second up to its own
 * @param overflowDecay   health above its own that it loses each second
 * @param lifesteal       share of its master's damage that heals it
 * @param riderTaken      damage its rider takes, as a share of the full blow
 * @param riderDealt      damage its rider deals, as a share of the full blow
 * @param speedBonus      speed above the fastest bred horse (0.2 is a fifth faster)
 * @param jumpBonus       jump height above the best jumping bred horse
 * @param frostRadius     radius of the water it freezes under its hooves, like Frost Walker
 * @param leashDistance   blocks from its master past which it goes back into the horn
 * @param followDistance  blocks from its master past which it trots after him when nobody rides it
 */
public record GhostHorseSettings(float health, float overflowHealth, float regen, float overflowDecay, float lifesteal, float riderTaken,
		float riderDealt, double speedBonus, double jumpBonus, int frostRadius, double leashDistance, double followDistance) {
	public static final GhostHorseSettings DEFAULT = new GhostHorseSettings(30.0F, 50.0F, 1.0F, 0.25F, 0.25F, 0.8F, 1.15F, 0.2, 0.2, 3, 64.0,
			8.0);

	public static final Codec<GhostHorseSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.floatRange(1.0F, 1024.0F).fieldOf("health").orElse(DEFAULT.health).forGetter(GhostHorseSettings::health),
			Codec.floatRange(1.0F, 1024.0F).fieldOf("overflow_health").orElse(DEFAULT.overflowHealth).forGetter(GhostHorseSettings::overflowHealth),
			Codec.floatRange(0.0F, 100.0F).fieldOf("regen_per_second").orElse(DEFAULT.regen).forGetter(GhostHorseSettings::regen),
			Codec.floatRange(0.0F, 100.0F).fieldOf("overflow_decay_per_second").orElse(DEFAULT.overflowDecay).forGetter(GhostHorseSettings::overflowDecay),
			Codec.floatRange(0.0F, 10.0F).fieldOf("lifesteal").orElse(DEFAULT.lifesteal).forGetter(GhostHorseSettings::lifesteal),
			Codec.floatRange(0.0F, 10.0F).fieldOf("rider_damage_taken").orElse(DEFAULT.riderTaken).forGetter(GhostHorseSettings::riderTaken),
			Codec.floatRange(0.0F, 10.0F).fieldOf("rider_damage_dealt").orElse(DEFAULT.riderDealt).forGetter(GhostHorseSettings::riderDealt),
			Codec.doubleRange(-0.9, 5.0).fieldOf("speed_bonus").orElse(DEFAULT.speedBonus).forGetter(GhostHorseSettings::speedBonus),
			Codec.doubleRange(-0.9, 5.0).fieldOf("jump_bonus").orElse(DEFAULT.jumpBonus).forGetter(GhostHorseSettings::jumpBonus),
			Codec.intRange(0, 16).fieldOf("frost_radius").orElse(DEFAULT.frostRadius).forGetter(GhostHorseSettings::frostRadius),
			Codec.doubleRange(8.0, 512.0).fieldOf("leash_distance").orElse(DEFAULT.leashDistance).forGetter(GhostHorseSettings::leashDistance),
			Codec.doubleRange(2.0, 64.0).fieldOf("follow_distance").orElse(DEFAULT.followDistance).forGetter(GhostHorseSettings::followDistance)
	).apply(instance, GhostHorseSettings::new));
}
