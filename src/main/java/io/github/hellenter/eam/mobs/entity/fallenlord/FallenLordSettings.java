package io.github.hellenter.eam.mobs.entity.fallenlord;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.ExtraCodecs;

import java.util.List;

/**
 * Balance numbers for the Fallen Lord, read from {@code data/eam/mobs/fallen_lord.json} so datapacks can override
 * them. A missing or invalid field falls back to its default. Damage is in health points (2 is one heart), times in
 * ticks (20 a second), distances in blocks.
 *
 * @param armor              armor points; the worn armor is only for looks
 * @param physicalResistance share of weapon and projectile damage (damage type tag {@code eam:physical}) he shrugs off
 * @param repeatPenalty      weight multiplier for picking the same attack twice in a row, so the boss mixes his attacks
 * @param phaseOne           spear and shield
 * @param phaseTwo           sword
 * @param swordDash          the blockable sword dash of phase two
 * @param regen              the regeneration potion he drinks when hurt, in either phase
 * @param burst              his answer to losing a lot of health quickly
 * @param shieldBreak        how he knocks a player's shield out
 * @param players            how much stronger he gets with more players around
 */
public record FallenLordSettings(int armor, double physicalResistance, double repeatPenalty, PhaseOne phaseOne, PhaseTwo phaseTwo,
		SwordDash swordDash, Regen regen, Burst burst, ShieldBreak shieldBreak, PlayerScaling players) {
	public static final FallenLordSettings DEFAULT = new FallenLordSettings(0, 0.25, 0.25, PhaseOne.DEFAULT, PhaseTwo.DEFAULT,
			SwordDash.DEFAULT, Regen.DEFAULT, Burst.DEFAULT, ShieldBreak.DEFAULT, PlayerScaling.DEFAULT);

	public static final Codec<FallenLordSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(0, 30).fieldOf("armor").orElse(DEFAULT.armor).forGetter(FallenLordSettings::armor),
			Codec.doubleRange(0.0, 1.0).fieldOf("physical_resistance").orElse(DEFAULT.physicalResistance)
					.forGetter(FallenLordSettings::physicalResistance),
			Codec.doubleRange(0.0, 1.0).fieldOf("repeat_penalty").orElse(DEFAULT.repeatPenalty).forGetter(FallenLordSettings::repeatPenalty),
			PhaseOne.CODEC.fieldOf("phase_one").orElse(PhaseOne.DEFAULT).forGetter(FallenLordSettings::phaseOne),
			PhaseTwo.CODEC.fieldOf("phase_two").orElse(PhaseTwo.DEFAULT).forGetter(FallenLordSettings::phaseTwo),
			SwordDash.CODEC.fieldOf("sword_dash").orElse(SwordDash.DEFAULT).forGetter(FallenLordSettings::swordDash),
			Regen.CODEC.fieldOf("regen").orElse(Regen.DEFAULT).forGetter(FallenLordSettings::regen),
			Burst.CODEC.fieldOf("burst").orElse(Burst.DEFAULT).forGetter(FallenLordSettings::burst),
			ShieldBreak.CODEC.fieldOf("shield_break").orElse(ShieldBreak.DEFAULT).forGetter(FallenLordSettings::shieldBreak),
			PlayerScaling.CODEC.fieldOf("players").orElse(PlayerScaling.DEFAULT).forGetter(FallenLordSettings::players)
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
			int dashChain, float throwDamage, int throwCooldown, int throwAfter, float swipeDamage, float swipeKnockback, int swipeHits,
			int swipeWindow, float shieldDisableMultiplier, int attackPause) {
		public static final PhaseOne DEFAULT = new PhaseOne(80.0F, 6.0F, 1.0F, 30, 11.7F, 160, 1, 7.0F, 160, 80, 3.0F, 1.2F, 3, 60, 2.0F, 16);

		public static final Codec<PhaseOne> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(1.0F, 10000.0F).fieldOf("health").orElse(DEFAULT.health).forGetter(PhaseOne::health),
				damage().fieldOf("poke_damage").orElse(DEFAULT.pokeDamage).forGetter(PhaseOne::pokeDamage),
				Codec.floatRange(0.0F, 5.0F).fieldOf("poke_knockback").orElse(DEFAULT.pokeKnockback).forGetter(PhaseOne::pokeKnockback),
				ticks().fieldOf("poke_cooldown").orElse(DEFAULT.pokeCooldown).forGetter(PhaseOne::pokeCooldown),
				damage().fieldOf("dash_damage").orElse(DEFAULT.dashDamage).forGetter(PhaseOne::dashDamage),
				ticks().fieldOf("dash_cooldown").orElse(DEFAULT.dashCooldown).forGetter(PhaseOne::dashCooldown),
				Codec.intRange(0, 10).fieldOf("dash_chain").orElse(DEFAULT.dashChain).forGetter(PhaseOne::dashChain),
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
		public static final PhaseTwo DEFAULT = new PhaseTwo(112.0F, 1.25, 9.0F, 20, 6.0F, 5.0F, 1.2F, 300, 10.0F, 140, 1.5F, 4.0, 1.0F, 400, 6);

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
	 * Phase two: a short wind-up, then he rushes at the player and stops right in front of them, pushing them back a
	 * block. A shield blocks it but is knocked out at once. When it connects, hit or blocked, he goes straight into
	 * his combo, whatever its cooldown.
	 *
	 * @param damage        the hit at the end of the rush
	 * @param cooldown      ticks between sword dashes
	 * @param shieldSeconds how long a shield that blocked it cannot be raised again
	 */
	public record SwordDash(float damage, int cooldown, float shieldSeconds) {
		public static final SwordDash DEFAULT = new SwordDash(7.0F, 240, 5.0F);

		public static final Codec<SwordDash> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				FallenLordSettings.damage().fieldOf("damage").orElse(DEFAULT.damage).forGetter(SwordDash::damage),
				ticks().fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(SwordDash::cooldown),
				Codec.floatRange(0.0F, 600.0F).fieldOf("shield_seconds").orElse(DEFAULT.shieldSeconds).forGetter(SwordDash::shieldSeconds)
		).apply(instance, SwordDash::new));
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
		public static final Regen DEFAULT = new Regen(0.5, 7.5F, 45.0F, 2, 800, 4.0F, 2.5F);

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

	/**
	 * In either phase, when he loses a big share of his health quickly he leaps back from the player and all his
	 * cooldowns are reset (his own shield comes back up too). Sometimes he drinks the regeneration potion after the
	 * leap; that counts as one of the phase's drinks.
	 *
	 * @param healthShare share of the phase's health he has to lose
	 * @param window      within this many ticks
	 * @param drinkChance chance he drinks after the leap, if drinks are left this phase
	 */
	public record Burst(double healthShare, int window, double drinkChance) {
		public static final Burst DEFAULT = new Burst(0.3, 100, 0.5);

		public static final Codec<Burst> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(0.01, 1.0).fieldOf("health_share").orElse(DEFAULT.healthShare).forGetter(Burst::healthShare),
				Codec.intRange(1, 12000).fieldOf("window").orElse(DEFAULT.window).forGetter(Burst::window),
				Codec.doubleRange(0.0, 1.0).fieldOf("drink_chance").orElse(DEFAULT.drinkChance).forGetter(Burst::drinkChance)
		).apply(instance, Burst::new));
	}

	/**
	 * A player's shield blocks a few of his hits, then he knocks it out the way an axe does. Only melee hits count; the
	 * dash goes through shields anyway.
	 *
	 * @param hitsMin blocked hits before the shield is knocked out, at least
	 * @param hitsMax and at most; picked anew between the two for every count
	 * @param seconds how long the shield cannot be raised again (an axe does 5)
	 * @param window  a blocked hit further than this many ticks after the previous one starts the count again
	 */
	public record ShieldBreak(int hitsMin, int hitsMax, float seconds, int window) {
		public static final ShieldBreak DEFAULT = new ShieldBreak(2, 3, 5.0F, 200);

		public static final Codec<ShieldBreak> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(1, 100).fieldOf("hits_min").orElse(DEFAULT.hitsMin).forGetter(ShieldBreak::hitsMin),
				Codec.intRange(1, 100).fieldOf("hits_max").orElse(DEFAULT.hitsMax).forGetter(ShieldBreak::hitsMax),
				Codec.floatRange(0.0F, 600.0F).fieldOf("seconds").orElse(DEFAULT.seconds).forGetter(ShieldBreak::seconds),
				ticks().fieldOf("window").orElse(DEFAULT.window).forGetter(ShieldBreak::window)
		).apply(instance, ShieldBreak::new));
	}

	/**
	 * He is tuned for the number of players fighting him: {@code levels} holds the bonuses for one player, two,
	 * three and so on; the last level also covers more players. A bonus of 0.15 is 15% more, -0.15 is 15% less.
	 * Health keeps its share when the count changes.
	 *
	 * @param radius players in survival or adventure within this many blocks count
	 * @param levels bonuses by player count, starting at one player
	 */
	public record PlayerScaling(double radius, List<Level> levels) {
		public static final PlayerScaling DEFAULT = new PlayerScaling(32.0, List.of(
				new Level(0.0, 0.0, 0.0),
				new Level(0.15, 0.15, 0.0),
				new Level(0.30, 0.35, 0.0),
				new Level(0.45, 0.60, 0.10)));

		public static final Codec<PlayerScaling> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(1.0, 256.0).fieldOf("radius").orElse(DEFAULT.radius).forGetter(PlayerScaling::radius),
				ExtraCodecs.nonEmptyList(Level.CODEC.listOf()).fieldOf("levels").orElse(DEFAULT.levels).forGetter(PlayerScaling::levels)
		).apply(instance, PlayerScaling::new));

		/**
		 * @param damage bonus to all his damage
		 * @param health bonus to the health of each phase
		 * @param speed  bonus to his movement speed
		 */
		public record Level(double damage, double health, double speed) {
			public static final Codec<Level> CODEC = RecordCodecBuilder.create(instance -> instance.group(
					Codec.doubleRange(-0.99, 100.0).fieldOf("damage").orElse(0.0).forGetter(Level::damage),
					Codec.doubleRange(-0.99, 100.0).fieldOf("health").orElse(0.0).forGetter(Level::health),
					Codec.doubleRange(-0.99, 100.0).fieldOf("speed").orElse(0.0).forGetter(Level::speed)
			).apply(instance, Level::new));
		}
	}
}
