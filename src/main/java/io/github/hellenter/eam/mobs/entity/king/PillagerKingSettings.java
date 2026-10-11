package io.github.hellenter.eam.mobs.entity.king;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for the Pillager King, read from {@code data/eam/mobs/pillager_king.json} so datapacks can override
 * them. A missing or invalid field falls back to its default. Times are in ticks, distances in blocks, damage in health
 * points (2 per heart) before the player's armor; the timings of the attacks themselves are in {@link KingAction}.
 *
 * @param stats       health, armor, speed and the rest of his attributes
 * @param run         he runs after a player he could not attack for a while
 * @param totem       his totem of undying
 * @param attackPause ticks between two attacks
 * @param reach       how far his axes reach, measured flat from his centre to the target's
 * @param slash       one cut with the right axe
 * @param combo       four cuts, the first of them parryable
 * @param onslaught   a rush and a leap with both axes, which a shield only softens
 * @param tornado     a long spin of weak cuts all around
 * @param axeThrow    the left axe thrown, mostly to knock a raised shield out
 * @param boomerang   the left axe thrown round the target to strike it in the back
 * @param charge      a run at the target after a red warning: three cuts and a kick
 * @param arrowParry  he knocks arrows from the front aside with an axe
 * @param dash        a few fast steps at the target
 * @param hop         a hop back from a player who crowds him
 * @param mobDamageMultiplier his blows and thrown axes deal this many times their damage to anything but a player
 */
public record PillagerKingSettings(Stats stats, Run run, Totem totem, int attackPause, double reach, Slash slash, Combo combo,
		Onslaught onslaught, Tornado tornado, AxeThrow axeThrow, Boomerang boomerang, Charge charge, ArrowParry arrowParry, Dash dash,
		Hop hop, float mobDamageMultiplier) {
	public static final PillagerKingSettings DEFAULT = new PillagerKingSettings(Stats.DEFAULT, Run.DEFAULT, Totem.DEFAULT, 12, 3.33,
			Slash.DEFAULT, Combo.DEFAULT, Onslaught.DEFAULT, Tornado.DEFAULT, AxeThrow.DEFAULT, Boomerang.DEFAULT, Charge.DEFAULT,
			ArrowParry.DEFAULT, Dash.DEFAULT, Hop.DEFAULT, 5.0F);

	/**
	 * @param health        his health
	 * @param armor         armor points (full iron armor is 15)
	 * @param speed         movement speed attribute; 0.3324 walks 15% slower than a sprinting player
	 * @param followRange   how far he notices and follows players
	 * @param xp            experience dropped on death
	 * @param regen         health per second he regains while he has no target
	 * @param armorPiercing share of all his damage that ignores the player's armor and Protection
	 */
	public record Stats(float health, int armor, double speed, double followRange, int xp, float regen, double armorPiercing) {
		public static final Stats DEFAULT = new Stats(100.0F, 10, 0.3324, 32.0, 200, 1.0F, 0.1);

		public static final Codec<Stats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(1.0F, 2048.0F).fieldOf("health").orElse(DEFAULT.health).forGetter(Stats::health),
				Codec.intRange(0, 30).fieldOf("armor").orElse(DEFAULT.armor).forGetter(Stats::armor),
				Codec.doubleRange(0.01, 2.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Stats::speed),
				Codec.doubleRange(1.0, 128.0).fieldOf("follow_range").orElse(DEFAULT.followRange).forGetter(Stats::followRange),
				Codec.intRange(0, 100000).fieldOf("xp").orElse(DEFAULT.xp).forGetter(Stats::xp),
				Codec.floatRange(0.0F, 100.0F).fieldOf("regen").orElse(DEFAULT.regen).forGetter(Stats::regen),
				Codec.doubleRange(0.0, 1.0).fieldOf("armor_piercing").orElse(DEFAULT.armorPiercing).forGetter(Stats::armorPiercing)
		).apply(instance, Stats::new));
	}

	/**
	 * @param after ticks without a chance to attack (target out of reach) before he starts running
	 * @param bonus how much faster he moves while running, a share of his walking speed
	 */
	public record Run(int after, double bonus) {
		public static final Run DEFAULT = new Run(110, 0.33);

		public static final Codec<Run> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(1, 2400).fieldOf("after").orElse(DEFAULT.after).forGetter(Run::after),
				Codec.doubleRange(0.0, 4.0).fieldOf("bonus").orElse(DEFAULT.bonus).forGetter(Run::bonus)
		).apply(instance, Run::new));
	}

	/** @param heal health he is left with when the totem saves him from death */
	public record Totem(float heal) {
		public static final Totem DEFAULT = new Totem(35.0F);

		public static final Codec<Totem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 2048.0F).fieldOf("heal").orElse(DEFAULT.heal).forGetter(Totem::heal)
		).apply(instance, Totem::new));
	}

	/** @param damage damage of the cut */
	public record Slash(float damage) {
		public static final Slash DEFAULT = new Slash(7.0F);

		public static final Codec<Slash> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 100.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Slash::damage)
		).apply(instance, Slash::new));
	}

	/**
	 * @param damage           damage of each of the four cuts
	 * @param chance           chance to pick it when it is ready
	 * @param cooldown         ticks before the next combo
	 * @param parryMultiplier  a parrying hit does this many times its damage
	 */
	public record Combo(float damage, double chance, int cooldown, float parryMultiplier) {
		public static final Combo DEFAULT = new Combo(5.5F, 0.4, 100, 1.5F);

		public static final Codec<Combo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 100.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Combo::damage),
				Codec.doubleRange(0.0, 1.0).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Combo::chance),
				Codec.intRange(0, 2400).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Combo::cooldown),
				Codec.floatRange(0.0F, 10.0F).fieldOf("parry_multiplier").orElse(DEFAULT.parryMultiplier).forGetter(Combo::parryMultiplier)
		).apply(instance, Combo::new));
	}

	/**
	 * @param damage          damage of the blow of both axes at the end of the leap
	 * @param shieldReduction a shield raised against him takes off only this share of the blow
	 * @param rushSpeed       blocks per tick he rushes in before the leap
	 * @param chance          chance to pick it when it is ready
	 * @param chanceVsShield  the same when the target holds up a shield
	 * @param cooldown        ticks before the next onslaught
	 */
	public record Onslaught(float damage, float shieldReduction, double rushSpeed, double chance,
			double chanceVsShield, int cooldown) {
		public static final Onslaught DEFAULT = new Onslaught(25.0F, 0.25F, 0.7, 0.25, 0.5, 220);

		public static final Codec<Onslaught> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 200.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Onslaught::damage),
				Codec.floatRange(0.0F, 1.0F).fieldOf("shield_reduction").orElse(DEFAULT.shieldReduction).forGetter(Onslaught::shieldReduction),
				Codec.doubleRange(0.0, 2.0).fieldOf("rush_speed").orElse(DEFAULT.rushSpeed).forGetter(Onslaught::rushSpeed),
				Codec.doubleRange(0.0, 1.0).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Onslaught::chance),
				Codec.doubleRange(0.0, 1.0).fieldOf("chance_vs_shield").orElse(DEFAULT.chanceVsShield).forGetter(Onslaught::chanceVsShield),
				Codec.intRange(0, 2400).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Onslaught::cooldown)
		).apply(instance, Onslaught::new));
	}

	/**
	 * @param damage   damage of each of the six cuts; a shield blocks them, but the second knocks it out
	 * @param radius   everyone this close (flat) is cut
	 * @param chance   chance to pick it when it is ready
	 * @param cooldown ticks before the next tornado
	 * @param drift    blocks per tick he drifts at the target while he spins
	 * @param shieldSeconds a shield the second cut hits is knocked out for this long
	 */
	public record Tornado(float damage, double radius, double chance, int cooldown, double drift, float shieldSeconds) {
		public static final Tornado DEFAULT = new Tornado(5.0F, 3.53, 0.2, 240, 0.1, 5.0F);

		public static final Codec<Tornado> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 100.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Tornado::damage),
				Codec.doubleRange(0.5, 8.0).fieldOf("radius").orElse(DEFAULT.radius).forGetter(Tornado::radius),
				Codec.doubleRange(0.0, 1.0).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Tornado::chance),
				Codec.intRange(0, 2400).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Tornado::cooldown),
				Codec.doubleRange(0.0, 1.0).fieldOf("drift").orElse(DEFAULT.drift).forGetter(Tornado::drift),
				Codec.floatRange(0.0F, 60.0F).fieldOf("shield_seconds").orElse(DEFAULT.shieldSeconds).forGetter(Tornado::shieldSeconds)
		).apply(instance, Tornado::new));
	}

	/**
	 * @param damage          damage of the axe
	 * @param minRange        a player who does not block must be at least this far for a throw
	 * @param maxRange        and at most this far
	 * @param after           ticks such a player must have stayed out of his reach
	 * @param chanceVsShield  chance to throw at a player blocking within his reach
	 * @param cooldown        ticks before the next throw
	 * @param shieldSeconds   a shield the axe hits is knocked out for this long
	 * @param speed           blocks per tick the axe flies
	 */
	public record AxeThrow(float damage, double minRange, double maxRange, int after, double chanceVsShield, int cooldown,
			float shieldSeconds, double speed) {
		public static final AxeThrow DEFAULT = new AxeThrow(5.0F, 4.0, 14.0, 40, 0.6, 140, 5.0F, 1.5);

		public static final Codec<AxeThrow> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 100.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(AxeThrow::damage),
				Codec.doubleRange(0.0, 64.0).fieldOf("min_range").orElse(DEFAULT.minRange).forGetter(AxeThrow::minRange),
				Codec.doubleRange(0.0, 64.0).fieldOf("max_range").orElse(DEFAULT.maxRange).forGetter(AxeThrow::maxRange),
				Codec.intRange(0, 2400).fieldOf("after").orElse(DEFAULT.after).forGetter(AxeThrow::after),
				Codec.doubleRange(0.0, 1.0).fieldOf("chance_vs_shield").orElse(DEFAULT.chanceVsShield).forGetter(AxeThrow::chanceVsShield),
				Codec.intRange(0, 2400).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(AxeThrow::cooldown),
				Codec.floatRange(0.0F, 60.0F).fieldOf("shield_seconds").orElse(DEFAULT.shieldSeconds).forGetter(AxeThrow::shieldSeconds),
				Codec.doubleRange(0.1, 5.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(AxeThrow::speed)
		).apply(instance, AxeThrow::new));
	}

	/**
	 * @param damage   damage of the axe when it strikes the back
	 * @param minRange the target must be at least this far
	 * @param maxRange and at most this far
	 * @param chance   chance to pick it when it is ready and the target is in range
	 * @param cooldown ticks before the next boomerang
	 * @param speed    blocks per tick the axe flies
	 */
	public record Boomerang(float damage, double minRange, double maxRange, double chance, int cooldown, double speed) {
		public static final Boomerang DEFAULT = new Boomerang(6.0F, 4.0, 16.0, 0.4, 200, 1.0);

		public static final Codec<Boomerang> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 100.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Boomerang::damage),
				Codec.doubleRange(0.0, 64.0).fieldOf("min_range").orElse(DEFAULT.minRange).forGetter(Boomerang::minRange),
				Codec.doubleRange(0.0, 64.0).fieldOf("max_range").orElse(DEFAULT.maxRange).forGetter(Boomerang::maxRange),
				Codec.doubleRange(0.0, 1.0).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Boomerang::chance),
				Codec.intRange(0, 2400).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Boomerang::cooldown),
				Codec.doubleRange(0.1, 5.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Boomerang::speed)
		).apply(instance, Boomerang::new));
	}

	/**
	 * @param damage        damage of each of the three cuts
	 * @param kickDamage    damage of the kick that ends it
	 * @param kickPush      how hard the kick pushes the target back
	 * @param slowAfterHit  share of his speed he loses after the first cut, to the end of the attack
	 * @param shieldSeconds a shield the first cut hits is knocked out for this long
	 * @param speed         blocks per tick he runs
	 * @param from          he charges at a target at least this far
	 * @param to            and at most this far
	 * @param chance        chance to pick it when it is ready and the target is in range
	 * @param cooldown      ticks before the next charge
	 */
	public record Charge(float damage, float kickDamage, double kickPush, double slowAfterHit, double speed, double from, double to,
			double chance, int cooldown, float shieldSeconds) {
		public static final Charge DEFAULT = new Charge(6.0F, 2.0F, 1.2, 0.33, 0.95, 4.0, 12.0, 0.35, 200, 5.0F);

		public static final Codec<Charge> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 100.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Charge::damage),
				Codec.floatRange(0.0F, 100.0F).fieldOf("kick_damage").orElse(DEFAULT.kickDamage).forGetter(Charge::kickDamage),
				Codec.doubleRange(0.0, 5.0).fieldOf("kick_push").orElse(DEFAULT.kickPush).forGetter(Charge::kickPush),
				Codec.doubleRange(0.0, 0.95).fieldOf("slow_after_hit").orElse(DEFAULT.slowAfterHit).forGetter(Charge::slowAfterHit),
				Codec.doubleRange(0.0, 3.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Charge::speed),
				Codec.doubleRange(0.0, 64.0).fieldOf("from").orElse(DEFAULT.from).forGetter(Charge::from),
				Codec.doubleRange(0.0, 64.0).fieldOf("to").orElse(DEFAULT.to).forGetter(Charge::to),
				Codec.doubleRange(0.0, 1.0).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Charge::chance),
				Codec.intRange(0, 2400).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Charge::cooldown),
				Codec.floatRange(0.0F, 60.0F).fieldOf("shield_seconds").orElse(DEFAULT.shieldSeconds).forGetter(Charge::shieldSeconds)
		).apply(instance, Charge::new));
	}

	/** @param chance chance to knock an arrow from the front aside while he is not in the middle of an attack */
	public record ArrowParry(double chance) {
		public static final ArrowParry DEFAULT = new ArrowParry(1.0);

		public static final Codec<ArrowParry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(0.0, 1.0).fieldOf("chance").orElse(DEFAULT.chance).forGetter(ArrowParry::chance)
		).apply(instance, ArrowParry::new));
	}

	/**
	 * Fast steps at the target: when it is a few blocks off and he is ready to attack, or when a hit is about to miss.
	 *
	 * @param cooldown ticks before the next dash
	 * @param ticks    how long the dash lasts
	 * @param speed    blocks per tick
	 * @param from     he dashes in at a target at least this far
	 * @param to       and at most this far
	 */
	public record Dash(int cooldown, int ticks, double speed, double from, double to) {
		public static final Dash DEFAULT = new Dash(70, 4, 0.9, 4.5, 8.0);

		public static final Codec<Dash> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, 2400).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Dash::cooldown),
				Codec.intRange(1, 10).fieldOf("ticks").orElse(DEFAULT.ticks).forGetter(Dash::ticks),
				Codec.doubleRange(0.0, 2.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Dash::speed),
				Codec.doubleRange(0.0, 32.0).fieldOf("from").orElse(DEFAULT.from).forGetter(Dash::from),
				Codec.doubleRange(0.0, 32.0).fieldOf("to").orElse(DEFAULT.to).forGetter(Dash::to)
		).apply(instance, Dash::new));

		/** The furthest the dash carries him. */
		public double range() {
			return ticks * speed;
		}
	}

	/**
	 * @param cooldown ticks before the next hop
	 * @param chance   chance per attack pause to hop back from a target within his reach, when the hop is ready
	 * @param speed    how hard he pushes off
	 */
	public record Hop(int cooldown, double chance, double speed) {
		public static final Hop DEFAULT = new Hop(60, 0.15, 0.6);

		public static final Codec<Hop> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, 2400).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Hop::cooldown),
				Codec.doubleRange(0.0, 1.0).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Hop::chance),
				Codec.doubleRange(0.0, 2.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Hop::speed)
		).apply(instance, Hop::new));
	}

	public static final Codec<PillagerKingSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Stats.CODEC.fieldOf("stats").orElse(DEFAULT.stats).forGetter(PillagerKingSettings::stats),
			Run.CODEC.fieldOf("run").orElse(DEFAULT.run).forGetter(PillagerKingSettings::run),
			Totem.CODEC.fieldOf("totem").orElse(DEFAULT.totem).forGetter(PillagerKingSettings::totem),
			Codec.intRange(0, 200).fieldOf("attack_pause").orElse(DEFAULT.attackPause).forGetter(PillagerKingSettings::attackPause),
			Codec.doubleRange(1.0, 6.0).fieldOf("reach").orElse(DEFAULT.reach).forGetter(PillagerKingSettings::reach),
			Slash.CODEC.fieldOf("slash").orElse(DEFAULT.slash).forGetter(PillagerKingSettings::slash),
			Combo.CODEC.fieldOf("combo").orElse(DEFAULT.combo).forGetter(PillagerKingSettings::combo),
			Onslaught.CODEC.fieldOf("onslaught").orElse(DEFAULT.onslaught).forGetter(PillagerKingSettings::onslaught),
			Tornado.CODEC.fieldOf("tornado").orElse(DEFAULT.tornado).forGetter(PillagerKingSettings::tornado),
			AxeThrow.CODEC.fieldOf("throw").orElse(DEFAULT.axeThrow).forGetter(PillagerKingSettings::axeThrow),
			Boomerang.CODEC.fieldOf("boomerang").orElse(DEFAULT.boomerang).forGetter(PillagerKingSettings::boomerang),
			Charge.CODEC.fieldOf("charge").orElse(DEFAULT.charge).forGetter(PillagerKingSettings::charge),
			ArrowParry.CODEC.fieldOf("arrow_parry").orElse(DEFAULT.arrowParry).forGetter(PillagerKingSettings::arrowParry),
			Dash.CODEC.fieldOf("dash").orElse(DEFAULT.dash).forGetter(PillagerKingSettings::dash),
			Hop.CODEC.fieldOf("hop").orElse(DEFAULT.hop).forGetter(PillagerKingSettings::hop),
			Codec.floatRange(0.0F, 100.0F).fieldOf("mob_damage_multiplier").orElse(DEFAULT.mobDamageMultiplier)
					.forGetter(PillagerKingSettings::mobDamageMultiplier)
	).apply(instance, PillagerKingSettings::new));
}
