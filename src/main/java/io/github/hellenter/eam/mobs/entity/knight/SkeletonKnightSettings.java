package io.github.hellenter.eam.mobs.entity.knight;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for the skeleton knight, read from {@code data/eam/mobs/skeleton_knight.json} so datapacks can
 * override them. A missing or invalid field falls back to its default. Times are in ticks, distances in blocks; damage
 * multipliers apply to the knight's own sword hit (attack damage attribute plus the sword and its enchantments).
 *
 * @param attackPause            ticks between two attacks
 * @param reach                  how far his sword reaches, measured flat from his centre to the target's
 * @param shieldRange            he raises the shield when his target is this close and he is not attacking
 * @param shieldDisableMultiplier an axe that would disable a player's shield for N seconds takes his down for N times this
 * @param comboChance            chance to pick the three-cut combo when it is ready
 * @param comboCooldown          ticks before the next combo
 * @param comboHitMultiplier     damage of each combo cut
 * @param heavyChance            chance to pick the heavy strike when it is ready
 * @param heavyChanceVsShield    the same when the target holds up a shield
 * @param heavyCooldown          ticks before the next heavy strike
 * @param heavyDamageMultiplier  damage of the heavy strike
 * @param heavyExtraReach        the heavy strike reaches this much further
 * @param heavyBlockedShare      share of the heavy strike that goes through a shield
 * @param heavyShieldSeconds     a shield hit by the heavy strike is knocked out for this long
 * @param backstep               speed of his hop back after a combo
 * @param dash                   his short dash at a target stepping out of reach just before a hit
 */
public record SkeletonKnightSettings(int attackPause, double reach, double shieldRange, float shieldDisableMultiplier,
		double comboChance, int comboCooldown, float comboHitMultiplier, double heavyChance, double heavyChanceVsShield,
		int heavyCooldown, float heavyDamageMultiplier, double heavyExtraReach, float heavyBlockedShare, float heavyShieldSeconds,
		double backstep, Dash dash) {
	public static final SkeletonKnightSettings DEFAULT = new SkeletonKnightSettings(14, 2.7, 10.0, 1.0F,
			0.4, 90, 0.6F, 0.25, 0.7,
			140, 1.8F, 0.5, 0.5F, 5.0F,
			0.55, Dash.DEFAULT);

	/**
	 * A few fast steps at the target, started {@code ticks} before a hit when the target would be out of reach by then.
	 *
	 * @param cooldown ticks before the next dash
	 * @param ticks    how many ticks the dash lasts; it is started this long before the hit
	 * @param speed    blocks per tick, so the dash covers at most {@code ticks * speed}
	 */
	public record Dash(int cooldown, int ticks, double speed) {
		public static final Dash DEFAULT = new Dash(200, 4, 0.9);

		public static final Codec<Dash> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, 2400).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Dash::cooldown),
				Codec.intRange(1, 10).fieldOf("ticks").orElse(DEFAULT.ticks).forGetter(Dash::ticks),
				Codec.doubleRange(0.0, 2.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Dash::speed)
		).apply(instance, Dash::new));

		/** The furthest the dash carries him. */
		public double range() {
			return ticks * speed;
		}
	}

	public static final Codec<SkeletonKnightSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(0, 200).fieldOf("attack_pause").orElse(DEFAULT.attackPause).forGetter(SkeletonKnightSettings::attackPause),
			Codec.doubleRange(1.0, 6.0).fieldOf("reach").orElse(DEFAULT.reach).forGetter(SkeletonKnightSettings::reach),
			Codec.doubleRange(0.0, 32.0).fieldOf("shield_range").orElse(DEFAULT.shieldRange).forGetter(SkeletonKnightSettings::shieldRange),
			Codec.floatRange(0.0F, 10.0F).fieldOf("shield_disable_multiplier").orElse(DEFAULT.shieldDisableMultiplier)
					.forGetter(SkeletonKnightSettings::shieldDisableMultiplier),
			Codec.doubleRange(0.0, 1.0).fieldOf("combo_chance").orElse(DEFAULT.comboChance).forGetter(SkeletonKnightSettings::comboChance),
			Codec.intRange(0, 2400).fieldOf("combo_cooldown").orElse(DEFAULT.comboCooldown).forGetter(SkeletonKnightSettings::comboCooldown),
			Codec.floatRange(0.0F, 10.0F).fieldOf("combo_hit_multiplier").orElse(DEFAULT.comboHitMultiplier)
					.forGetter(SkeletonKnightSettings::comboHitMultiplier),
			Codec.doubleRange(0.0, 1.0).fieldOf("heavy_chance").orElse(DEFAULT.heavyChance).forGetter(SkeletonKnightSettings::heavyChance),
			Codec.doubleRange(0.0, 1.0).fieldOf("heavy_chance_vs_shield").orElse(DEFAULT.heavyChanceVsShield)
					.forGetter(SkeletonKnightSettings::heavyChanceVsShield),
			Codec.intRange(0, 2400).fieldOf("heavy_cooldown").orElse(DEFAULT.heavyCooldown).forGetter(SkeletonKnightSettings::heavyCooldown),
			Codec.floatRange(0.0F, 10.0F).fieldOf("heavy_damage_multiplier").orElse(DEFAULT.heavyDamageMultiplier)
					.forGetter(SkeletonKnightSettings::heavyDamageMultiplier),
			Codec.doubleRange(0.0, 4.0).fieldOf("heavy_extra_reach").orElse(DEFAULT.heavyExtraReach)
					.forGetter(SkeletonKnightSettings::heavyExtraReach),
			Codec.floatRange(0.0F, 1.0F).fieldOf("heavy_blocked_share").orElse(DEFAULT.heavyBlockedShare)
					.forGetter(SkeletonKnightSettings::heavyBlockedShare),
			Codec.floatRange(0.0F, 60.0F).fieldOf("heavy_shield_seconds").orElse(DEFAULT.heavyShieldSeconds)
					.forGetter(SkeletonKnightSettings::heavyShieldSeconds),
			Codec.doubleRange(0.0, 2.0).fieldOf("backstep").orElse(DEFAULT.backstep).forGetter(SkeletonKnightSettings::backstep),
			Dash.CODEC.fieldOf("dash").orElse(DEFAULT.dash).forGetter(SkeletonKnightSettings::dash)
	).apply(instance, SkeletonKnightSettings::new));
}
