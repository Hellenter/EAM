package io.github.hellenter.eam.mobs.entity.fallenlord;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
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
 * @param movement           his pace, sight and short hops
 * @param backstab           the phase two dash behind the player
 * @param heavyStrike        the phase two overhead strike with the red blade
 * @param undead             how he deals with undead that come close
 * @param death              the experience burst when he dies
 * @param later              sections added after the codec's 16 fields ran out; they sit at the top level of the JSON too
 */
public record FallenLordSettings(int armor, double physicalResistance, double repeatPenalty, PhaseOne phaseOne, PhaseTwo phaseTwo,
		SwordDash swordDash, Regen regen, Burst burst, ShieldBreak shieldBreak, PlayerScaling players, Movement movement,
		Backstab backstab, HeavyStrike heavyStrike, Undead undead, Death death, Later later) {
	public static final FallenLordSettings DEFAULT = new FallenLordSettings(0, 0.25, 0.25, PhaseOne.DEFAULT, PhaseTwo.DEFAULT,
			SwordDash.DEFAULT, Regen.DEFAULT, Burst.DEFAULT, ShieldBreak.DEFAULT, PlayerScaling.DEFAULT, Movement.DEFAULT,
			Backstab.DEFAULT, HeavyStrike.DEFAULT, Undead.DEFAULT, Death.DEFAULT, Later.DEFAULT);

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
			PlayerScaling.CODEC.fieldOf("players").orElse(PlayerScaling.DEFAULT).forGetter(FallenLordSettings::players),
			Movement.CODEC.fieldOf("movement").orElse(Movement.DEFAULT).forGetter(FallenLordSettings::movement),
			Backstab.CODEC.fieldOf("backstab").orElse(Backstab.DEFAULT).forGetter(FallenLordSettings::backstab),
			HeavyStrike.CODEC.fieldOf("heavy_strike").orElse(HeavyStrike.DEFAULT).forGetter(FallenLordSettings::heavyStrike),
			Undead.CODEC.fieldOf("undead").orElse(Undead.DEFAULT).forGetter(FallenLordSettings::undead),
			Death.CODEC.fieldOf("death").orElse(Death.DEFAULT).forGetter(FallenLordSettings::death),
			Later.MAP_CODEC.forGetter(FallenLordSettings::later)
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
		public static final PhaseOne DEFAULT = new PhaseOne(80.0F, 6.0F, 1.0F, 20, 11.7F, 160, 1, 7.0F, 160, 80, 3.0F, 1.2F, 3, 60, 2.0F, 8);

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
	 * @param speed           movement speed multiplier while chasing, on top of {@link Movement#speed()}; phase two is
	 *                        aggressive (1.25 is a quarter faster)
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
		public static final PhaseTwo DEFAULT = new PhaseTwo(112.0F, 1.125, 9.0F, 20, 6.0F, 5.0F, 1.2F, 300, 10.0F, 140, 1.5F, 4.0, 1.0F, 400, 6);

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
	 * @param retreat          how far he leaps back before he drinks
	 * @param keepAway         while he drinks he backs away from a player closer than this
	 */
	public record Regen(double healthBelow, float healPerSecond, float maxHeal, int usesPerPhase, int cooldown, float counterDamage,
			float counterKnockback, double retreat, double keepAway) {
		public static final Regen DEFAULT = new Regen(0.5, 7.5F, 45.0F, 2, 800, 4.0F, 2.5F, 8.5, 9.0);

		public static final Codec<Regen> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(0.0, 1.0).fieldOf("health_below").orElse(DEFAULT.healthBelow).forGetter(Regen::healthBelow),
				Codec.floatRange(0.0F, 100.0F).fieldOf("heal_per_second").orElse(DEFAULT.healPerSecond).forGetter(Regen::healPerSecond),
				Codec.floatRange(0.0F, 10000.0F).fieldOf("max_heal").orElse(DEFAULT.maxHeal).forGetter(Regen::maxHeal),
				Codec.intRange(0, 100).fieldOf("uses_per_phase").orElse(DEFAULT.usesPerPhase).forGetter(Regen::usesPerPhase),
				ticks().fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Regen::cooldown),
				damage().fieldOf("counter_damage").orElse(DEFAULT.counterDamage).forGetter(Regen::counterDamage),
				Codec.floatRange(0.0F, 5.0F).fieldOf("counter_knockback").orElse(DEFAULT.counterKnockback).forGetter(Regen::counterKnockback),
				Codec.doubleRange(0.0, 16.0).fieldOf("retreat").orElse(DEFAULT.retreat).forGetter(Regen::retreat),
				Codec.doubleRange(0.0, 32.0).fieldOf("keep_away").orElse(DEFAULT.keepAway).forGetter(Regen::keepAway)
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

	/**
	 * His pace and sight, and the short hops he makes in either phase: in at a player who stays away or has not been
	 * hit for a while, or back out to make room for a throw or a dash (always when his shield has just been knocked out).
	 *
	 * @param speed          base movement speed in both phases (a wither skeleton walks at 0.25)
	 * @param followRange    how far he sees and follows players, in blocks
	 * @param hopMin         shortest hop
	 * @param hopMax         longest hop
	 * @param hopCooldown    ticks between hops
	 * @param hopInFrom      he hops in at a player at least this far away
	 * @param hopInAfter     or at one he has not hit for this many ticks, if not already close
	 * @param hopBackChance  weight of a hop back when a player crowds him and a throw or dash is ready (an attack is 1)
	 */
	public record Movement(double speed, double followRange, double hopMin, double hopMax, int hopCooldown, double hopInFrom,
			int hopInAfter, double hopBackChance) {
		public static final Movement DEFAULT = new Movement(0.2875, 48.0, 2.0, 3.5, 50, 7.0, 100, 0.35);

		public static final Codec<Movement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(0.01, 2.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Movement::speed),
				Codec.doubleRange(1.0, 128.0).fieldOf("follow_range").orElse(DEFAULT.followRange).forGetter(Movement::followRange),
				Codec.doubleRange(0.0, 10.0).fieldOf("hop_min").orElse(DEFAULT.hopMin).forGetter(Movement::hopMin),
				Codec.doubleRange(0.0, 10.0).fieldOf("hop_max").orElse(DEFAULT.hopMax).forGetter(Movement::hopMax),
				ticks().fieldOf("hop_cooldown").orElse(DEFAULT.hopCooldown).forGetter(Movement::hopCooldown),
				Codec.doubleRange(0.0, 64.0).fieldOf("hop_in_from").orElse(DEFAULT.hopInFrom).forGetter(Movement::hopInFrom),
				ticks().fieldOf("hop_in_after").orElse(DEFAULT.hopInAfter).forGetter(Movement::hopInAfter),
				Codec.doubleRange(0.0, 100.0).fieldOf("hop_back_chance").orElse(DEFAULT.hopBackChance).forGetter(Movement::hopBackChance)
		).apply(instance, Movement::new));
	}

	/**
	 * Phase two: he darts round the player's side to their back, where a raised shield does not cover, and cuts.
	 * He goes for it more often while the player holds a shield up.
	 *
	 * @param damage        the cut in the back
	 * @param cooldown      ticks between backstabs
	 * @param blockingBonus weight multiplier while the player blocks
	 */
	public record Backstab(float damage, int cooldown, double blockingBonus) {
		public static final Backstab DEFAULT = new Backstab(9.0F, 200, 3.0);

		public static final Codec<Backstab> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				FallenLordSettings.damage().fieldOf("damage").orElse(DEFAULT.damage).forGetter(Backstab::damage),
				ticks().fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Backstab::cooldown),
				Codec.doubleRange(0.0, 100.0).fieldOf("blocking_bonus").orElse(DEFAULT.blockingBonus).forGetter(Backstab::blockingBonus)
		).apply(instance, Backstab::new));
	}

	/**
	 * Phase two: he raises the sword overhead, its edge glows red, and he brings it down with more reach than a plain
	 * hit. He turns after the player until the last tick before the cut, and red arcs roll out along the ground over
	 * the struck area just before it lands. Stepping back out of reach dodges it. A shield does not stop it: the player
	 * takes a share of the damage and the shield is knocked out. When it lands he goes straight into his combo.
	 *
	 * @param damageMultiplier damage as a multiple of the plain hit
	 * @param extraReach       reach beyond the plain hit
	 * @param blockedShare     share of the damage that goes through a shield
	 * @param shieldSeconds    how long that shield cannot be raised again
	 * @param cooldown         ticks between heavy strikes
	 * @param extraWidth       how much further round his sides it reaches than a plain hit, in blocks
	 * @param sweepTicks       ticks each ground wave takes to roll from him to the end of the reach; the last ends on the cut
	 * @param waves            ground waves before the cut
	 * @param waveGap          ticks between one wave setting off and the next
	 */
	public record HeavyStrike(float damageMultiplier, double extraReach, float blockedShare, float shieldSeconds, int cooldown,
			double extraWidth, int sweepTicks, int waves, int waveGap, int extraWindup) {
		public static final HeavyStrike DEFAULT = new HeavyStrike(2.6F, 1.5, 0.5F, 5.0F, 183, 0.33, 8, 3, 4, 3);

		public static final Codec<HeavyStrike> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 20.0F).fieldOf("damage_multiplier").orElse(DEFAULT.damageMultiplier).forGetter(HeavyStrike::damageMultiplier),
				Codec.doubleRange(0.0, 5.0).fieldOf("extra_reach").orElse(DEFAULT.extraReach).forGetter(HeavyStrike::extraReach),
				Codec.floatRange(0.0F, 1.0F).fieldOf("blocked_share").orElse(DEFAULT.blockedShare).forGetter(HeavyStrike::blockedShare),
				Codec.floatRange(0.0F, 600.0F).fieldOf("shield_seconds").orElse(DEFAULT.shieldSeconds).forGetter(HeavyStrike::shieldSeconds),
				ticks().fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(HeavyStrike::cooldown),
				Codec.doubleRange(-1.0, 2.0).fieldOf("extra_width").orElse(DEFAULT.extraWidth).forGetter(HeavyStrike::extraWidth),
				Codec.intRange(1, 19).fieldOf("sweep_ticks").orElse(DEFAULT.sweepTicks).forGetter(HeavyStrike::sweepTicks),
				Codec.intRange(0, 5).fieldOf("waves").orElse(DEFAULT.waves).forGetter(HeavyStrike::waves),
				Codec.intRange(1, 10).fieldOf("wave_gap").orElse(DEFAULT.waveGap).forGetter(HeavyStrike::waveGap),
				Codec.intRange(0, 40).fieldOf("extra_windup").orElse(DEFAULT.extraWindup).forGetter(HeavyStrike::extraWindup)
		).apply(instance, HeavyStrike::new));
	}

	/**
	 * Undead (entity type tag {@code minecraft:undead}) that come close are cut down, but a player always comes
	 * first: he turns on any player he sees.
	 *
	 * @param radius           he goes for undead this close
	 * @param damageMultiplier his damage to undead
	 * @param freeAbilities    attacks used on undead don't go on cooldown
	 */
	public record Undead(double radius, float damageMultiplier, boolean freeAbilities) {
		public static final Undead DEFAULT = new Undead(10.0, 5.0F, true);

		public static final Codec<Undead> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(0.0, 64.0).fieldOf("radius").orElse(DEFAULT.radius).forGetter(Undead::radius),
				Codec.floatRange(0.0F, 100.0F).fieldOf("damage_multiplier").orElse(DEFAULT.damageMultiplier).forGetter(Undead::damageMultiplier),
				Codec.BOOL.fieldOf("free_abilities").orElse(DEFAULT.freeAbilities).forGetter(Undead::freeAbilities)
		).apply(instance, Undead::new));
	}

	/**
	 * When he dies, experience bursts from his chest in two spheres: a share of the orbs lands close, the rest further
	 * out. The amount grows with the players around him (counted as for {@link PlayerScaling}).
	 *
	 * @param xp          experience for one player
	 * @param xpPerPlayer extra experience for each more player
	 * @param maxPlayers  players beyond this many add nothing
	 * @param innerShare  share of the orbs in the inner sphere
	 * @param innerRadius radius of the inner sphere, blocks
	 * @param outerRadius radius of the outer sphere, blocks
	 * @param orbXp       experience in one orb, about; more experience means more orbs, not bigger ones
	 */
	public record Death(int xp, int xpPerPlayer, int maxPlayers, double innerShare, double innerRadius, double outerRadius, int orbXp) {
		public static final Death DEFAULT = new Death(750, 200, 4, 0.3, 1.3, 3.9, 8);

		public static final Codec<Death> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, 100000).fieldOf("xp").orElse(DEFAULT.xp).forGetter(Death::xp),
				Codec.intRange(0, 100000).fieldOf("xp_per_player").orElse(DEFAULT.xpPerPlayer).forGetter(Death::xpPerPlayer),
				Codec.intRange(1, 100).fieldOf("max_players").orElse(DEFAULT.maxPlayers).forGetter(Death::maxPlayers),
				Codec.doubleRange(0.0, 1.0).fieldOf("inner_share").orElse(DEFAULT.innerShare).forGetter(Death::innerShare),
				Codec.doubleRange(0.0, 16.0).fieldOf("inner_radius").orElse(DEFAULT.innerRadius).forGetter(Death::innerRadius),
				Codec.doubleRange(0.0, 16.0).fieldOf("outer_radius").orElse(DEFAULT.outerRadius).forGetter(Death::outerRadius),
				Codec.intRange(1, 10000).fieldOf("orb_xp").orElse(DEFAULT.orbXp).forGetter(Death::orbXp)
		).apply(instance, Death::new));
	}

	/**
	 * Sections past the top-level codec's 16 fields. They are read from the top level of the JSON like the others.
	 *
	 * @param spearDash the phase one spear dash, stronger the further he has run
	 * @param breather  phase two: he backs off after hurting a player badly in a short time
	 * @param leapBack  phase two: after a few attacks in a row he leaps back
	 * @param stance    phase two: the fencing stance after either leap, luring the player in
	 * @param vampirism  how he heals from kills and while no player is in sight
	 * @param leapUp     phase two: he leaps up at a player standing too high to reach
	 * @param sprint     both phases: he sprints after a player he cannot catch
	 * @param dashBreak  both dashes: how deep into blocks one dash can smash its way
	 * @param armorPiercing share of all his damage to a player that ignores armor and Protection; the Resistance effect
	 *                      and shields still count
	 * @param dashDelay  phase one: the spear dash comes later at a player close by, and not right after his shield is knocked out
	 * @param parry      phase two: a player who blocks an attack from his stance can parry it with a quick hit
	 */
	public record Later(SpearDash spearDash, Breather breather, LeapBack leapBack, Stance stance, Vampirism vampirism, LeapUp leapUp,
			Sprint sprint, DashBreak dashBreak, double armorPiercing, DashDelay dashDelay, Parry parry) {
		public static final Later DEFAULT = new Later(SpearDash.DEFAULT, Breather.DEFAULT, LeapBack.DEFAULT, Stance.DEFAULT,
				Vampirism.DEFAULT, LeapUp.DEFAULT, Sprint.DEFAULT, DashBreak.DEFAULT, 0.12, DashDelay.DEFAULT, Parry.DEFAULT);

		public static final MapCodec<Later> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				SpearDash.CODEC.fieldOf("spear_dash").orElse(SpearDash.DEFAULT).forGetter(Later::spearDash),
				Breather.CODEC.fieldOf("breather").orElse(Breather.DEFAULT).forGetter(Later::breather),
				LeapBack.CODEC.fieldOf("leap_back").orElse(LeapBack.DEFAULT).forGetter(Later::leapBack),
				Stance.CODEC.fieldOf("stance").orElse(Stance.DEFAULT).forGetter(Later::stance),
				Vampirism.CODEC.fieldOf("vampirism").orElse(Vampirism.DEFAULT).forGetter(Later::vampirism),
				LeapUp.CODEC.fieldOf("leap_up").orElse(LeapUp.DEFAULT).forGetter(Later::leapUp),
				Sprint.CODEC.fieldOf("sprint").orElse(Sprint.DEFAULT).forGetter(Later::sprint),
				DashBreak.CODEC.fieldOf("dash_break").orElse(DashBreak.DEFAULT).forGetter(Later::dashBreak),
				Codec.doubleRange(0.0, 1.0).fieldOf("armor_piercing").orElse(DEFAULT.armorPiercing).forGetter(Later::armorPiercing),
				DashDelay.CODEC.fieldOf("dash_delay").orElse(DashDelay.DEFAULT).forGetter(Later::dashDelay),
				Parry.CODEC.fieldOf("parry").orElse(Parry.DEFAULT).forGetter(Later::parry)
		).apply(instance, Later::new));
	}

	/**
	 * Phase one spear dash timing.
	 *
	 * @param closeRange       a player this close, flat, when the red-eyed windup ends
	 * @param closeTicks       gets this many more ticks of windup
	 * @param afterShieldKnock ticks the dash stays on cooldown at least once an axe knocks his shield out
	 */
	public record DashDelay(double closeRange, int closeTicks, int afterShieldKnock) {
		public static final DashDelay DEFAULT = new DashDelay(2.0, 3, 60);

		public static final Codec<DashDelay> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(0.0, 16.0).fieldOf("close_range").orElse(DEFAULT.closeRange).forGetter(DashDelay::closeRange),
				Codec.intRange(0, 40).fieldOf("close_ticks").orElse(DEFAULT.closeTicks).forGetter(DashDelay::closeTicks),
				ticks().fieldOf("after_shield_knock").orElse(DEFAULT.afterShieldKnock).forGetter(DashDelay::afterShieldKnock)
		).apply(instance, DashDelay::new));
	}

	/**
	 * Phase two: when a player's shield blocks an attack he made from his stance, the player has a moment to hit him
	 * back. A hit in time breaks off that attack and the combo after it, and he shoves the player away without harm.
	 *
	 * @param window ticks after the block in which the player's hit counts as a parry
	 * @param push   blocks the parried lord shoves the player back
	 */
	public record Parry(int window, double push) {
		public static final Parry DEFAULT = new Parry(10, 3.0);

		public static final Codec<Parry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, 200).fieldOf("window").orElse(DEFAULT.window).forGetter(Parry::window),
				Codec.doubleRange(0.0, 16.0).fieldOf("push").orElse(DEFAULT.push).forGetter(Parry::push)
		).apply(instance, Parry::new));
	}

	/**
	 * Both dashes smash weak blocks in his way, but only so deep: a wall up to two blocks thick is broken through, a
	 * thicker one stops the dash.
	 *
	 * @param maxDepth blocks, counted along the run, one dash smashes through; the block that would take it past this
	 *                 stays and stops him
	 */
	public record DashBreak(double maxDepth) {
		public static final DashBreak DEFAULT = new DashBreak(2.5);

		public static final Codec<DashBreak> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(0.0, 64.0).fieldOf("max_depth").orElse(DEFAULT.maxDepth).forGetter(DashBreak::maxDepth)
		).apply(instance, DashBreak::new));
	}

	/**
	 * A player he cannot catch for a while gets chased at a sprint, until his first hit lands.
	 *
	 * @param after ticks out of reach of his weapon before he sprints
	 * @param speed extra movement speed while sprinting, a share of his base speed
	 */
	public record Sprint(int after, double speed) {
		public static final Sprint DEFAULT = new Sprint(100, 0.33);

		public static final Codec<Sprint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(1, 2400).fieldOf("after").orElse(DEFAULT.after).forGetter(Sprint::after),
				Codec.doubleRange(0.0, 4.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Sprint::speed)
		).apply(instance, Sprint::new));
	}

	/**
	 * Phase two: a player who climbs out of his reach, onto a pillar or a ledge, gets leapt at. He crouches, springs up
	 * beside them and cuts at the top of the leap, knocking them off.
	 *
	 * @param minHeight  the player's feet must be at least this much higher than his
	 * @param maxHeight  and at most this much; higher than that he cannot leap
	 * @param maxRange   he only leaps at a player this close, counted flat
	 * @param cooldown   ticks before he can leap up again
	 * @param damage     the cut at the top of the leap
	 * @param knockback  how hard the cut throws the player
	 */
	public record LeapUp(double minHeight, double maxHeight, double maxRange, int cooldown, float damage, float knockback) {
		public static final LeapUp DEFAULT = new LeapUp(2.0, 8.0, 6.0, 60, 8.0F, 1.0F);

		public static final Codec<LeapUp> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(1.0, 32.0).fieldOf("min_height").orElse(DEFAULT.minHeight).forGetter(LeapUp::minHeight),
				Codec.doubleRange(1.0, 32.0).fieldOf("max_height").orElse(DEFAULT.maxHeight).forGetter(LeapUp::maxHeight),
				Codec.doubleRange(0.0, 32.0).fieldOf("max_range").orElse(DEFAULT.maxRange).forGetter(LeapUp::maxRange),
				Codec.intRange(0, 2400).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(LeapUp::cooldown),
				FallenLordSettings.damage().fieldOf("damage").orElse(DEFAULT.damage).forGetter(LeapUp::damage),
				Codec.floatRange(0.0F, 10.0F).fieldOf("knockback").orElse(DEFAULT.knockback).forGetter(LeapUp::knockback)
		).apply(instance, LeapUp::new));
	}

	/**
	 * He feeds on the fight: killing a player heals him by a share of the current phase's health, and while he sees no
	 * player he slowly regenerates.
	 *
	 * @param killHeal      share of the phase's health a player's death gives back
	 * @param unseenRegen   health per second while no player is in his sight
	 */
	public record Vampirism(double killHeal, float unseenRegen) {
		public static final Vampirism DEFAULT = new Vampirism(0.5, 1.0F);

		public static final Codec<Vampirism> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(0.0, 1.0).fieldOf("kill_heal").orElse(DEFAULT.killHeal).forGetter(Vampirism::killHeal),
				Codec.floatRange(0.0F, 100.0F).fieldOf("unseen_regen").orElse(DEFAULT.unseenRegen).forGetter(Vampirism::unseenRegen)
		).apply(instance, Vampirism::new));
	}

	/**
	 * The spear dash grows along its path: a player caught right at the start gets a smaller reach and damage, one
	 * caught at the end of the run a bigger one. In between it changes evenly. Multipliers of the dash's reach and of
	 * {@link PhaseOne#dashDamage()}.
	 *
	 * @param startReach  reach at the start of the run
	 * @param endReach    reach at the end of the run
	 * @param startDamage damage at the start of the run
	 * @param endDamage   damage at the end of the run
	 */
	public record SpearDash(double startReach, double endReach, float startDamage, float endDamage) {
		public static final SpearDash DEFAULT = new SpearDash(0.697, 1.08, 0.85F, 1.3F);

		public static final Codec<SpearDash> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(0.0, 5.0).fieldOf("start_reach").orElse(DEFAULT.startReach).forGetter(SpearDash::startReach),
				Codec.doubleRange(0.0, 5.0).fieldOf("end_reach").orElse(DEFAULT.endReach).forGetter(SpearDash::endReach),
				Codec.floatRange(0.0F, 10.0F).fieldOf("start_damage").orElse(DEFAULT.startDamage).forGetter(SpearDash::startDamage),
				Codec.floatRange(0.0F, 10.0F).fieldOf("end_damage").orElse(DEFAULT.endDamage).forGetter(SpearDash::endDamage)
		).apply(instance, SpearDash::new));
	}

	/**
	 * Phase two: when his hits on one player add up quickly (enough hits, or enough of the player's health), he leaps
	 * back and takes his fencing stance, so the player is not cut down in one go.
	 *
	 * @param hitsMin  hits that count as many, at least
	 * @param hitsMax  and at most; picked anew after every breather
	 * @param health   or this much of the player's health lost to him (12 is six hearts)
	 * @param window   within this many ticks
	 * @param distance how far he leaps back
	 * @param cooldown ticks from one breather to the next
	 */
	public record Breather(int hitsMin, int hitsMax, float health, int window, double distance, int cooldown) {
		public static final Breather DEFAULT = new Breather(4, 5, 12.0F, 80, 6.0, 700);

		public static final Codec<Breather> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(1, 100).fieldOf("hits_min").orElse(DEFAULT.hitsMin).forGetter(Breather::hitsMin),
				Codec.intRange(1, 100).fieldOf("hits_max").orElse(DEFAULT.hitsMax).forGetter(Breather::hitsMax),
				Codec.floatRange(0.0F, 1000.0F).fieldOf("health").orElse(DEFAULT.health).forGetter(Breather::health),
				Codec.intRange(1, 12000).fieldOf("window").orElse(DEFAULT.window).forGetter(Breather::window),
				Codec.doubleRange(0.0, 16.0).fieldOf("distance").orElse(DEFAULT.distance).forGetter(Breather::distance),
				ticks().fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Breather::cooldown)
		).apply(instance, Breather::new));
	}

	/**
	 * Phase two: after each attack there is a chance he leaps back and takes his fencing stance. It starts after
	 * {@code after} attacks in a row and grows by {@code chanceStep} with every further one.
	 *
	 * @param after      attacks in a row before he may leap back
	 * @param chanceStep chance after that many attacks, and added for each one more
	 * @param distance   how far he leaps back
	 * @param maxRange   he only leaps back from a player this close
	 */
	public record LeapBack(int after, double chanceStep, double distance, double maxRange) {
		public static final LeapBack DEFAULT = new LeapBack(2, 0.25, 4.5, 6.0);

		public static final Codec<LeapBack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(1, 100).fieldOf("after").orElse(DEFAULT.after).forGetter(LeapBack::after),
				Codec.doubleRange(0.0, 1.0).fieldOf("chance_step").orElse(DEFAULT.chanceStep).forGetter(LeapBack::chanceStep),
				Codec.doubleRange(0.0, 16.0).fieldOf("distance").orElse(DEFAULT.distance).forGetter(LeapBack::distance),
				Codec.doubleRange(0.0, 64.0).fieldOf("max_range").orElse(DEFAULT.maxRange).forGetter(LeapBack::maxRange)
		).apply(instance, LeapBack::new));
	}

	/**
	 * Phase two: the fencing stance after a leap back. He waits, sword pointed at the player, other hand raised behind,
	 * luring them in; a player who comes at him gets a lunging stab, one who hits him a riposte. A passive player, one
	 * who heals (eats, drinks, or their health goes up), runs or just waits the stance out, gets a sword dash at once.
	 *
	 * @param duration  ticks he holds it at most
	 * @param runAway   the player counts as running once this much further away than when the stance began
	 * @param healed    or once their health is up by this much
	 * @param lungeFrom a player this close who has come nearer gets the lunge
	 * @param approach  coming nearer means this much closer than when the stance began
	 */
	public record Stance(int duration, double runAway, float healed, double lungeFrom, double approach) {
		public static final Stance DEFAULT = new Stance(50, 1.5, 1.0F, 6.5, 0.5);

		public static final Codec<Stance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(1, 400).fieldOf("duration").orElse(DEFAULT.duration).forGetter(Stance::duration),
				Codec.doubleRange(0.0, 64.0).fieldOf("run_away").orElse(DEFAULT.runAway).forGetter(Stance::runAway),
				Codec.floatRange(0.0F, 100.0F).fieldOf("healed").orElse(DEFAULT.healed).forGetter(Stance::healed),
				Codec.doubleRange(0.0, 16.0).fieldOf("lunge_from").orElse(DEFAULT.lungeFrom).forGetter(Stance::lungeFrom),
				Codec.doubleRange(0.0, 16.0).fieldOf("approach").orElse(DEFAULT.approach).forGetter(Stance::approach)
		).apply(instance, Stance::new));
	}
}
