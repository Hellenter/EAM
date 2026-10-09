package io.github.hellenter.eam.mobs.entity.fallenlord;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for the Fallen Lord, read from {@code data/eam/mobs/fallen_lord.json} so datapacks can override
 * them. A missing or invalid field falls back to its default. Damage is in health points (2 is one heart), times in
 * ticks (20 a second), distances in blocks.
 *
 * @param armor          armor points; the worn armor is only for looks
 * @param repeatPenalty  weight multiplier for picking the same attack twice in a row, so the boss mixes his attacks
 * @param phaseOne       spear and shield
 * @param phaseTwo       sword
 * @param regen          the regeneration potion he drinks when hurt, in either phase
 */
public record FallenLordSettings(int armor, double repeatPenalty, PhaseOne phaseOne, PhaseTwo phaseTwo, Regen regen) {
	public static final FallenLordSettings DEFAULT = new FallenLordSettings(0, 0.25, PhaseOne.DEFAULT, PhaseTwo.DEFAULT, Regen.DEFAULT);

	public static final Codec<FallenLordSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(0, 30).fieldOf("armor").orElse(DEFAULT.armor).forGetter(FallenLordSettings::armor),
			Codec.doubleRange(0.0, 1.0).fieldOf("repeat_penalty").orElse(DEFAULT.repeatPenalty).forGetter(FallenLordSettings::repeatPenalty),
			PhaseOne.CODEC.fieldOf("phase_one").orElse(PhaseOne.DEFAULT).forGetter(FallenLordSettings::phaseOne),
			PhaseTwo.CODEC.fieldOf("phase_two").orElse(PhaseTwo.DEFAULT).forGetter(FallenLordSettings::phaseTwo),
			Regen.CODEC.fieldOf("regen").orElse(Regen.DEFAULT).forGetter(FallenLordSettings::regen)
	).apply(instance, FallenLordSettings::new));

	private static Codec<Float> damage() {
		return Codec.floatRange(0.0F, 1000.0F);
	}

	private static Codec<Integer> ticks() {
		return Codec.intRange(0, 12000);
	}

	/**
	 * @param health                   health of this phase
	 * @param pokeDamage               spear jab when a player comes close
	 * @param pokeKnockback            how far the jab pushes (1 is about two blocks)
	 * @param pokeCooldown             ticks between jabs
	 * @param dashDamage               unblockable dash after the red eye flash
	 * @param dashCooldown             ticks between dashes
	 * @param throwDamage              thrown spear
	 * @param throwCooldown            ticks between throws
	 * @param throwAfter               a player has to stay out of reach this long before the boss throws
	 * @param swipeDamage              half-circle swipe after a broken shield and a flurry of hits
	 * @param swipeKnockback           how far the swipe pushes
	 * @param swipeHits                hits on a broken shield that trigger the swipe
	 * @param swipeWindow              the hits have to land within this many ticks
	 * @param shieldDisableMultiplier  an axe breaks the boss's shield this many times longer than a player's
	 * @param attackPause              ticks he waits after one attack before the next
	 */
	public record PhaseOne(float health, float pokeDamage, float pokeKnockback, int pokeCooldown, float dashDamage, int dashCooldown,
			float throwDamage, int throwCooldown, int throwAfter, float swipeDamage, float swipeKnockback, int swipeHits,
			int swipeWindow, float shieldDisableMultiplier, int attackPause) {
		public static final PhaseOne DEFAULT = new PhaseOne(100.0F, 6.0F, 1.0F, 30, 9.0F, 160, 7.0F, 200, 100, 3.0F, 1.2F, 3, 60, 2.0F, 16);

		public static final Codec<PhaseOne> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(1.0F, 10000.0F).fieldOf("health").orElse(DEFAULT.health).forGetter(PhaseOne::health),
				damage().fieldOf("poke_damage").orElse(DEFAULT.pokeDamage).forGetter(PhaseOne::pokeDamage),
				Codec.floatRange(0.0F, 5.0F).fieldOf("poke_knockback").orElse(DEFAULT.pokeKnockback).forGetter(PhaseOne::pokeKnockback),
				ticks().fieldOf("poke_cooldown").orElse(DEFAULT.pokeCooldown).forGetter(PhaseOne::pokeCooldown),
				damage().fieldOf("dash_damage").orElse(DEFAULT.dashDamage).forGetter(PhaseOne::dashDamage),
				ticks().fieldOf("dash_cooldown").orElse(DEFAULT.dashCooldown).forGetter(PhaseOne::dashCooldown),
				damage().fieldOf("throw_damage").orElse(DEFAULT.throwDamage).forGetter(PhaseOne::throwDamage),
				ticks().fieldOf("throw_cooldown").orElse(DEFAULT.throwCooldown).forGetter(PhaseOne::throwCooldown),
				ticks().fieldOf("throw_after").orElse(DEFAULT.throwAfter).forGetter(PhaseOne::throwAfter),
				damage().fieldOf("swipe_damage").orElse(DEFAULT.swipeDamage).forGetter(PhaseOne::swipeDamage),
				Codec.floatRange(0.0F, 5.0F).fieldOf("swipe_knockback").orElse(DEFAULT.swipeKnockback).forGetter(PhaseOne::swipeKnockback),
				Codec.intRange(1, 20).fieldOf("swipe_hits").orElse(DEFAULT.swipeHits).forGetter(PhaseOne::swipeHits),
				ticks().fieldOf("swipe_window").orElse(DEFAULT.swipeWindow).forGetter(PhaseOne::swipeWindow),
				Codec.floatRange(0.0F, 20.0F).fieldOf("shield_disable_multiplier").orElse(DEFAULT.shieldDisableMultiplier)
						.forGetter(PhaseOne::shieldDisableMultiplier),
				ticks().fieldOf("attack_pause").orElse(DEFAULT.attackPause).forGetter(PhaseOne::attackPause)
		).apply(instance, PhaseOne::new));
	}

	/**
	 * @param health          health of this phase
	 * @param speed           movement speed multiplier while chasing; phase two is aggressive (1.25 is a quarter faster)
	 * @param slashDamage     plain sword hit, used when nothing else fits
	 * @param slashCooldown   ticks between plain hits
	 * @param comboHitDamage  each of the three quick slashes of the combo
	 * @param comboKickDamage the kick that ends the combo
	 * @param comboKnockback  how far the kick pushes
	 * @param comboCooldown   ticks between combos
	 * @param lungeDamage     stab of the lunge
	 * @param lungeCooldown   ticks between lunges
	 * @param whirlMultiplier whirl damage as a multiple of the plain hit
	 * @param whirlRadius     whirl reach around the boss
	 * @param whirlKnockback  how far the whirl pushes everyone
	 * @param whirlCooldown   ticks between whirls
	 * @param attackPause     ticks he waits after one attack before the next
	 */
	public record PhaseTwo(float health, double speed, float slashDamage, int slashCooldown, float comboHitDamage, float comboKickDamage,
			float comboKnockback, int comboCooldown, float lungeDamage, int lungeCooldown, float whirlMultiplier, double whirlRadius,
			float whirlKnockback, int whirlCooldown, int attackPause) {
		public static final PhaseTwo DEFAULT = new PhaseTwo(140.0F, 1.25, 9.0F, 20, 6.0F, 5.0F, 1.2F, 300, 10.0F, 140, 1.5F, 4.0, 1.0F, 400, 6);

		public static final Codec<PhaseTwo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(1.0F, 10000.0F).fieldOf("health").orElse(DEFAULT.health).forGetter(PhaseTwo::health),
				Codec.doubleRange(0.1, 4.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(PhaseTwo::speed),
				damage().fieldOf("slash_damage").orElse(DEFAULT.slashDamage).forGetter(PhaseTwo::slashDamage),
				ticks().fieldOf("slash_cooldown").orElse(DEFAULT.slashCooldown).forGetter(PhaseTwo::slashCooldown),
				damage().fieldOf("combo_hit_damage").orElse(DEFAULT.comboHitDamage).forGetter(PhaseTwo::comboHitDamage),
				damage().fieldOf("combo_kick_damage").orElse(DEFAULT.comboKickDamage).forGetter(PhaseTwo::comboKickDamage),
				Codec.floatRange(0.0F, 5.0F).fieldOf("combo_knockback").orElse(DEFAULT.comboKnockback).forGetter(PhaseTwo::comboKnockback),
				ticks().fieldOf("combo_cooldown").orElse(DEFAULT.comboCooldown).forGetter(PhaseTwo::comboCooldown),
				damage().fieldOf("lunge_damage").orElse(DEFAULT.lungeDamage).forGetter(PhaseTwo::lungeDamage),
				ticks().fieldOf("lunge_cooldown").orElse(DEFAULT.lungeCooldown).forGetter(PhaseTwo::lungeCooldown),
				Codec.floatRange(0.0F, 10.0F).fieldOf("whirl_multiplier").orElse(DEFAULT.whirlMultiplier).forGetter(PhaseTwo::whirlMultiplier),
				Codec.doubleRange(1.0, 10.0).fieldOf("whirl_radius").orElse(DEFAULT.whirlRadius).forGetter(PhaseTwo::whirlRadius),
				Codec.floatRange(0.0F, 5.0F).fieldOf("whirl_knockback").orElse(DEFAULT.whirlKnockback).forGetter(PhaseTwo::whirlKnockback),
				ticks().fieldOf("whirl_cooldown").orElse(DEFAULT.whirlCooldown).forGetter(PhaseTwo::whirlCooldown),
				ticks().fieldOf("attack_pause").orElse(DEFAULT.attackPause).forGetter(PhaseTwo::attackPause)
		).apply(instance, PhaseTwo::new));
	}

	/**
	 * When he is hurt enough he leaps back from the player and drinks a regeneration potion, healing while he drinks.
	 * A player's hit stops the drink, and he answers it with a quick half-circle bash: the shield in phase one, the
	 * sword in phase two.
	 *
	 * @param healthBelow      drinks only at or below this share of the phase's health
	 * @param healPerSecond    health restored per second while he drinks
	 * @param maxHeal          health one drink restores at most
	 * @param usesPerPhase     drinks per phase
	 * @param cooldown         ticks from one drink to the next
	 * @param counterDamage    the bash that answers a hit during the drink
	 * @param counterKnockback how far the bash pushes (2.5 is about seven blocks)
	 */
	public record Regen(double healthBelow, float healPerSecond, float maxHeal, int usesPerPhase, int cooldown, float counterDamage,
			float counterKnockback) {
		public static final Regen DEFAULT = new Regen(0.5, 3.0F, 40.0F, 2, 800, 4.0F, 2.5F);

		public static final Codec<Regen> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(0.0, 1.0).fieldOf("health_below").orElse(DEFAULT.healthBelow).forGetter(Regen::healthBelow),
				Codec.floatRange(0.0F, 100.0F).fieldOf("heal_per_second").orElse(DEFAULT.healPerSecond).forGetter(Regen::healPerSecond),
				Codec.floatRange(0.0F, 10000.0F).fieldOf("max_heal").orElse(DEFAULT.maxHeal).forGetter(Regen::maxHeal),
				Codec.intRange(0, 100).fieldOf("uses_per_phase").orElse(DEFAULT.usesPerPhase).forGetter(Regen::usesPerPhase),
				ticks().fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Regen::cooldown),
				damage().fieldOf("counter_damage").orElse(DEFAULT.counterDamage).forGetter(Regen::counterDamage),
				Codec.floatRange(0.0F, 5.0F).fieldOf("counter_knockback").orElse(DEFAULT.counterKnockback).forGetter(Regen::counterKnockback)
		).apply(instance, Regen::new));
	}
}
