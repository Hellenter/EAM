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
 * @param whirl       a full turn with both axes out, through shields
 * @param tornado     a long spin of weak cuts all around
 * @param axeThrow    the left axe thrown, mostly to knock a raised shield out
 * @param dash        a few fast steps at the target
 * @param hop         a hop back from a player who crowds him
 */
public record PillagerKingSettings(Stats stats, Run run, Totem totem, int attackPause, double reach, Slash slash, Combo combo,
		Whirl whirl, Tornado tornado, AxeThrow axeThrow, Dash dash, Hop hop) {
	public static final PillagerKingSettings DEFAULT = new PillagerKingSettings(Stats.DEFAULT, Run.DEFAULT, Totem.DEFAULT, 12, 3.0,
			Slash.DEFAULT, Combo.DEFAULT, Whirl.DEFAULT, Tornado.DEFAULT, AxeThrow.DEFAULT, Dash.DEFAULT, Hop.DEFAULT);

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
		public static final Stats DEFAULT = new Stats(75.0F, 10, 0.3324, 32.0, 200, 1.0F, 0.1);

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
		public static final Totem DEFAULT = new Totem(25.0F);

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
	 * @param damage          damage of each of the two cuts
	 * @param radius          everyone this close (flat) is cut, whichever side they stand on
	 * @param chance          chance to pick it when it is ready
	 * @param chanceVsShield  the same when the target holds up a shield
	 * @param cooldown        ticks before the next whirl
	 */
	public record Whirl(float damage, double radius, double chance, double chanceVsShield, int cooldown) {
		public static final Whirl DEFAULT = new Whirl(6.0F, 3.6, 0.25, 0.5, 160);

		public static final Codec<Whirl> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 100.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Whirl::damage),
				Codec.doubleRange(0.5, 8.0).fieldOf("radius").orElse(DEFAULT.radius).forGetter(Whirl::radius),
				Codec.doubleRange(0.0, 1.0).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Whirl::chance),
				Codec.doubleRange(0.0, 1.0).fieldOf("chance_vs_shield").orElse(DEFAULT.chanceVsShield).forGetter(Whirl::chanceVsShield),
				Codec.intRange(0, 2400).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Whirl::cooldown)
		).apply(instance, Whirl::new));
	}

	/**
	 * @param damage   damage of each of the six cuts; a shield blocks them and stays up
	 * @param radius   everyone this close (flat) is cut
	 * @param chance   chance to pick it when it is ready
	 * @param cooldown ticks before the next tornado
	 * @param drift    blocks per tick he drifts at the target while he spins
	 */
	public record Tornado(float damage, double radius, double chance, int cooldown, double drift) {
		public static final Tornado DEFAULT = new Tornado(5.0F, 3.2, 0.2, 240, 0.1);

		public static final Codec<Tornado> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 100.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Tornado::damage),
				Codec.doubleRange(0.5, 8.0).fieldOf("radius").orElse(DEFAULT.radius).forGetter(Tornado::radius),
				Codec.doubleRange(0.0, 1.0).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Tornado::chance),
				Codec.intRange(0, 2400).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Tornado::cooldown),
				Codec.doubleRange(0.0, 1.0).fieldOf("drift").orElse(DEFAULT.drift).forGetter(Tornado::drift)
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
			Whirl.CODEC.fieldOf("whirl").orElse(DEFAULT.whirl).forGetter(PillagerKingSettings::whirl),
			Tornado.CODEC.fieldOf("tornado").orElse(DEFAULT.tornado).forGetter(PillagerKingSettings::tornado),
			AxeThrow.CODEC.fieldOf("throw").orElse(DEFAULT.axeThrow).forGetter(PillagerKingSettings::axeThrow),
			Dash.CODEC.fieldOf("dash").orElse(DEFAULT.dash).forGetter(PillagerKingSettings::dash),
			Hop.CODEC.fieldOf("hop").orElse(DEFAULT.hop).forGetter(PillagerKingSettings::hop)
	).apply(instance, PillagerKingSettings::new));
}
