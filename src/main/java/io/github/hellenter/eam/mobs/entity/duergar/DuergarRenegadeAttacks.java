package io.github.hellenter.eam.mobs.entity.duergar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The renegades' special attacks, the {@code attacks} object of {@code duergar_renegade.json}. Once a second, in a fight,
 * a renegade whose attack is off cooldown rolls its chance; each attack has a clear wind-up the player can read. A
 * missing or invalid field falls back to its default.
 *
 * @param charge    the charged blow
 * @param bomb      the thrown bomb
 * @param allyThrow one renegade throwing a squad mate at the player
 */
public record DuergarRenegadeAttacks(Charge charge, Bomb bomb, AllyThrow allyThrow) {
	public static final DuergarRenegadeAttacks DEFAULT = new DuergarRenegadeAttacks(Charge.DEFAULT, Bomb.DEFAULT, AllyThrow.DEFAULT);

	public static final Codec<DuergarRenegadeAttacks> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Charge.CODEC.fieldOf("charge").orElse(Charge.DEFAULT).forGetter(DuergarRenegadeAttacks::charge),
			Bomb.CODEC.fieldOf("bomb").orElse(Bomb.DEFAULT).forGetter(DuergarRenegadeAttacks::bomb),
			AllyThrow.CODEC.fieldOf("ally_throw").orElse(AllyThrow.DEFAULT).forGetter(DuergarRenegadeAttacks::allyThrow)
	).apply(instance, DuergarRenegadeAttacks::new));

	/**
	 * He raises his weapon over his head, then brings it down: a hit the player can step back from. Blocked by a shield,
	 * it puts the shield on cooldown, like an axe.
	 *
	 * @param chance           chance per second, while the player is in reach
	 * @param cooldownTicks    ticks before he can do it again
	 * @param damageMultiplier times his usual hit
	 * @param reach            how far the blow lands, in blocks from him
	 * @param shieldSeconds    how long a shield that blocks it stays down
	 */
	public record Charge(double chance, int cooldownTicks, float damageMultiplier, double reach, float shieldSeconds) {
		public static final Charge DEFAULT = new Charge(0.3, 120, 1.5F, 2.6, 5.0F);

		public static final Codec<Charge> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(0.0, 1.0).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Charge::chance),
				Codec.intRange(0, 72000).fieldOf("cooldown_ticks").orElse(DEFAULT.cooldownTicks).forGetter(Charge::cooldownTicks),
				Codec.floatRange(0.0F, 20.0F).fieldOf("damage_multiplier").orElse(DEFAULT.damageMultiplier).forGetter(Charge::damageMultiplier),
				Codec.doubleRange(0.5, 8.0).fieldOf("reach").orElse(DEFAULT.reach).forGetter(Charge::reach),
				Codec.floatRange(0.0F, 60.0F).fieldOf("shield_seconds").orElse(DEFAULT.shieldSeconds).forGetter(Charge::shieldSeconds)
		).apply(instance, Charge::new));
	}

	/**
	 * He takes out a bomb and a flint and steel, lights the fuse and lobs the bomb at the player. It lies hissing on the
	 * floor until the fuse burns down, then bursts: full damage at its centre, less the farther away, none past the radius.
	 * It breaks no blocks and does not hurt renegades.
	 *
	 * @param chance              chance per second
	 * @param cooldownTicks       ticks before he can throw another
	 * @param squadCooldownTicks  ticks his squad mates wait after his throw, so a squad does not throw them all at once
	 * @param minDistance         nearest the player may be
	 * @param maxDistance         farthest the player may be
	 * @param damage              damage at the centre
	 * @param radius              blast radius in blocks
	 * @param fuseTicks           ticks from the throw to the burst
	 */
	public record Bomb(double chance, int cooldownTicks, int squadCooldownTicks, double minDistance, double maxDistance, float damage,
			double radius, int fuseTicks) {
		public static final Bomb DEFAULT = new Bomb(0.08, 400, 300, 4.0, 14.0, 12.0F, 4.5, 40);

		public static final Codec<Bomb> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(0.0, 1.0).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Bomb::chance),
				Codec.intRange(0, 72000).fieldOf("cooldown_ticks").orElse(DEFAULT.cooldownTicks).forGetter(Bomb::cooldownTicks),
				Codec.intRange(0, 72000).fieldOf("squad_cooldown_ticks").orElse(DEFAULT.squadCooldownTicks).forGetter(Bomb::squadCooldownTicks),
				Codec.doubleRange(0.0, 64.0).fieldOf("min_distance").orElse(DEFAULT.minDistance).forGetter(Bomb::minDistance),
				Codec.doubleRange(0.0, 64.0).fieldOf("max_distance").orElse(DEFAULT.maxDistance).forGetter(Bomb::maxDistance),
				Codec.floatRange(0.0F, 200.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Bomb::damage),
				Codec.doubleRange(0.5, 16.0).fieldOf("radius").orElse(DEFAULT.radius).forGetter(Bomb::radius),
				Codec.intRange(1, 600).fieldOf("fuse_ticks").orElse(DEFAULT.fuseTicks).forGetter(Bomb::fuseTicks)
		).apply(instance, Bomb::new));
	}

	/**
	 * With a squad mate beside him, he grabs him, lifts him over his head and hurls him at the player. The thrown one hits
	 * whoever he flies into and lands on his feet, unhurt by the fall. Rarer than the bomb.
	 *
	 * @param chance             chance per second
	 * @param cooldownTicks      ticks before he can throw again
	 * @param squadCooldownTicks ticks his squad mates wait after his throw
	 * @param minDistance        nearest the player may be
	 * @param maxDistance        farthest the player may be
	 * @param grabRange          how near the mate must stand
	 * @param hitMultiplier      the thrown one's hit, times his usual one
	 * @param knockback          extra knockback of that hit
	 */
	public record AllyThrow(double chance, int cooldownTicks, int squadCooldownTicks, double minDistance, double maxDistance,
			double grabRange, float hitMultiplier, float knockback) {
		public static final AllyThrow DEFAULT = new AllyThrow(0.04, 900, 500, 5.0, 14.0, 3.0, 1.0F, 1.0F);

		public static final Codec<AllyThrow> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(0.0, 1.0).fieldOf("chance").orElse(DEFAULT.chance).forGetter(AllyThrow::chance),
				Codec.intRange(0, 72000).fieldOf("cooldown_ticks").orElse(DEFAULT.cooldownTicks).forGetter(AllyThrow::cooldownTicks),
				Codec.intRange(0, 72000).fieldOf("squad_cooldown_ticks").orElse(DEFAULT.squadCooldownTicks)
						.forGetter(AllyThrow::squadCooldownTicks),
				Codec.doubleRange(0.0, 64.0).fieldOf("min_distance").orElse(DEFAULT.minDistance).forGetter(AllyThrow::minDistance),
				Codec.doubleRange(0.0, 64.0).fieldOf("max_distance").orElse(DEFAULT.maxDistance).forGetter(AllyThrow::maxDistance),
				Codec.doubleRange(0.5, 8.0).fieldOf("grab_range").orElse(DEFAULT.grabRange).forGetter(AllyThrow::grabRange),
				Codec.floatRange(0.0F, 20.0F).fieldOf("hit_multiplier").orElse(DEFAULT.hitMultiplier).forGetter(AllyThrow::hitMultiplier),
				Codec.floatRange(0.0F, 5.0F).fieldOf("knockback").orElse(DEFAULT.knockback).forGetter(AllyThrow::knockback)
		).apply(instance, AllyThrow::new));
	}
}
