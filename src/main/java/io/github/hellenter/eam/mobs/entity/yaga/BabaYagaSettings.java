package io.github.hellenter.eam.mobs.entity.yaga;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for Baba Yaga, read from {@code data/eam/mobs/baba_yaga.json} so datapacks can override them. A missing
 * or invalid field falls back to its default. Times are in ticks (20 a second), distances in blocks, damage and healing
 * in health points (2 per heart), effect levels count from 0 (0 is level I).
 *
 * @param stats    health of both phases, armor, speed and the rest
 * @param flight   phase one: how she circles over the player in her mortar
 * @param swoop    phase one: the rare dive with her staff, the only moment she can be hurt (by a parry)
 * @param summon   the groups of swamp creatures she raises around the player
 * @param potion   the poison flasks she throws
 * @param spell    the spell she casts on one of her groups: absorption, speed or strength
 * @param glyphs   the glowing glyphs she lays on the ground under the player
 * @param phaseTwo phase two, on foot: how she keeps her distance and raises one group in front of her
 * @param escape   phase two: after a couple of hits she blinds the player, runs and comes back behind them
 * @param acid     phase two: a fast acid bolt that goes through a shield and leaves a burning puddle
 * @param threads  phase two: a purple bolt that ties the player to the ground with threads
 * @param beam     phase two: a fire beam a shield blocks
 * @param claws    phase two: she blinks behind the player and rakes them with her claws
 * @param eat      phase two: she eats one of her creatures to heal
 */
public record BabaYagaSettings(Stats stats, Flight flight, Swoop swoop, Summon summon, Potion potion, Spell spell, Glyphs glyphs,
		PhaseTwo phaseTwo, Escape escape, Acid acid, Threads threads, Beam beam, Claws claws, Eat eat) {
	public static final BabaYagaSettings DEFAULT = new BabaYagaSettings(Stats.DEFAULT, Flight.DEFAULT, Swoop.DEFAULT, Summon.DEFAULT,
			Potion.DEFAULT, Spell.DEFAULT, Glyphs.DEFAULT, PhaseTwo.DEFAULT, Escape.DEFAULT, Acid.DEFAULT, Threads.DEFAULT, Beam.DEFAULT,
			Claws.DEFAULT, Eat.DEFAULT);

	public static final Codec<BabaYagaSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Stats.CODEC.fieldOf("stats").orElse(Stats.DEFAULT).forGetter(BabaYagaSettings::stats),
			Flight.CODEC.fieldOf("flight").orElse(Flight.DEFAULT).forGetter(BabaYagaSettings::flight),
			Swoop.CODEC.fieldOf("swoop").orElse(Swoop.DEFAULT).forGetter(BabaYagaSettings::swoop),
			Summon.CODEC.fieldOf("summon").orElse(Summon.DEFAULT).forGetter(BabaYagaSettings::summon),
			Potion.CODEC.fieldOf("potion").orElse(Potion.DEFAULT).forGetter(BabaYagaSettings::potion),
			Spell.CODEC.fieldOf("spell").orElse(Spell.DEFAULT).forGetter(BabaYagaSettings::spell),
			Glyphs.CODEC.fieldOf("glyphs").orElse(Glyphs.DEFAULT).forGetter(BabaYagaSettings::glyphs),
			PhaseTwo.CODEC.fieldOf("phase_two").orElse(PhaseTwo.DEFAULT).forGetter(BabaYagaSettings::phaseTwo),
			Escape.CODEC.fieldOf("escape").orElse(Escape.DEFAULT).forGetter(BabaYagaSettings::escape),
			Acid.CODEC.fieldOf("acid").orElse(Acid.DEFAULT).forGetter(BabaYagaSettings::acid),
			Threads.CODEC.fieldOf("threads").orElse(Threads.DEFAULT).forGetter(BabaYagaSettings::threads),
			Beam.CODEC.fieldOf("beam").orElse(Beam.DEFAULT).forGetter(BabaYagaSettings::beam),
			Claws.CODEC.fieldOf("claws").orElse(Claws.DEFAULT).forGetter(BabaYagaSettings::claws),
			Eat.CODEC.fieldOf("eat").orElse(Eat.DEFAULT).forGetter(BabaYagaSettings::eat)
	).apply(instance, BabaYagaSettings::new));

	/**
	 * @param phaseOneHealth health in her mortar; only parries take it away
	 * @param phaseTwoHealth health on foot, after the mortar breaks
	 * @param armor          armor points in phase two
	 * @param speed          movement speed attribute on foot
	 * @param followRange    how far she notices and follows players
	 * @param xp             experience dropped on death
	 */
	public record Stats(float phaseOneHealth, float phaseTwoHealth, double armor, double speed, double followRange, int xp) {
		public static final Stats DEFAULT = new Stats(50.0F, 100.0F, 4.0, 0.42, 64.0, 300);
		public static final Codec<Stats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(1.0F, 2048.0F).fieldOf("phase_one_health").orElse(DEFAULT.phaseOneHealth).forGetter(Stats::phaseOneHealth),
				Codec.floatRange(1.0F, 2048.0F).fieldOf("phase_two_health").orElse(DEFAULT.phaseTwoHealth).forGetter(Stats::phaseTwoHealth),
				Codec.doubleRange(0.0, 30.0).fieldOf("armor").orElse(DEFAULT.armor).forGetter(Stats::armor),
				Codec.doubleRange(0.05, 1.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Stats::speed),
				Codec.doubleRange(16.0, 256.0).fieldOf("follow_range").orElse(DEFAULT.followRange).forGetter(Stats::followRange),
				Codec.intRange(0, 100000).fieldOf("xp").orElse(DEFAULT.xp).forGetter(Stats::xp)
		).apply(instance, Stats::new));
	}

	/**
	 * @param altitude    how high above the player she flies
	 * @param orbitRadius how far from the player she circles
	 * @param orbitTurn   degrees of the circle she flies per tick
	 * @param speed       her top flying speed, blocks per tick
	 */
	public record Flight(double altitude, double orbitRadius, float orbitTurn, double speed) {
		public static final Flight DEFAULT = new Flight(7.0, 14.0, 0.6F, 0.35);
		public static final Codec<Flight> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(2.0, 30.0).fieldOf("altitude").orElse(DEFAULT.altitude).forGetter(Flight::altitude),
				Codec.doubleRange(3.0, 48.0).fieldOf("orbit_radius").orElse(DEFAULT.orbitRadius).forGetter(Flight::orbitRadius),
				Codec.floatRange(0.0F, 10.0F).fieldOf("orbit_turn").orElse(DEFAULT.orbitTurn).forGetter(Flight::orbitTurn),
				Codec.doubleRange(0.05, 2.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Flight::speed)
		).apply(instance, Flight::new));
	}

	/**
	 * @param delay        ticks after the player killed her last creature before she dives
	 * @param strikeTick   tick of the dive on which her staff strikes; the dive is timed to reach the player on it
	 * @param parryWindow  ticks before the strike in which a player's hit parries her; she glows meanwhile
	 * @param damage       her staff's blow
	 * @param parryDamage  a parry takes exactly this much health, whatever the weapon
	 * @param reach        how far her staff reaches, from her centre to the player's
	 * @param speed        top speed of the dive, blocks per tick
	 * @param retreatTicks after the dive she flies back up for this long before she raises the next wave
	 * @param strikeDrop   her staff reaches this far below her feet, and the dive aims her this much lower
	 */
	public record Swoop(int delay, int strikeTick, int parryWindow, float damage, float parryDamage, double reach,
			double speed, int retreatTicks, double strikeDrop) {
		public static final Swoop DEFAULT = new Swoop(40, 30, 7, 5.0F, 10.0F, 3.0, 1.0, 40, 1.0);
		public static final Codec<Swoop> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, 1200).fieldOf("delay").orElse(DEFAULT.delay).forGetter(Swoop::delay),
				Codec.intRange(10, 200).fieldOf("strike_tick").orElse(DEFAULT.strikeTick).forGetter(Swoop::strikeTick),
				Codec.intRange(1, 60).fieldOf("parry_window").orElse(DEFAULT.parryWindow).forGetter(Swoop::parryWindow),
				Codec.floatRange(0.0F, 100.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Swoop::damage),
				Codec.floatRange(0.0F, 1000.0F).fieldOf("parry_damage").orElse(DEFAULT.parryDamage).forGetter(Swoop::parryDamage),
				Codec.doubleRange(1.0, 8.0).fieldOf("reach").orElse(DEFAULT.reach).forGetter(Swoop::reach),
				Codec.doubleRange(0.1, 4.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Swoop::speed),
				Codec.intRange(0, 400).fieldOf("retreat_ticks").orElse(DEFAULT.retreatTicks).forGetter(Swoop::retreatTicks),
				Codec.doubleRange(0.0, 3.0).fieldOf("strike_drop").orElse(DEFAULT.strikeDrop).forGetter(Swoop::strikeDrop)
		).apply(instance, Swoop::new));
	}

	/**
	 * @param groups        groups in one phase-one wave
	 * @param halfDelay     each group of a wave rises in two halves: the first at once, the second this many ticks later
	 * @param lurkerGroup   a phase-one group of swamp lurkers: how many lurkers and how many drowned soldiers go with them
	 * @param wightGroup    a phase-one group of wights: how many wights and how many drowned soldiers go with them
	 * @param maxWights     wights in one phase-two group, at most
	 * @param minRadius     groups appear at least this far from the player
	 * @param maxRadius     and at most this far
	 * @param firstDelay    ticks after she notices a player before she raises her first wave
	 * @param lurkerWeight  phase two: how often a creature is a swamp lurker, against the other two weights
	 * @param wightWeight   phase two: how often it is a wight
	 * @param soldierWeight phase two: how often it is a drowned soldier
	 * @param followRange   her creatures follow the player from this far
	 * @param glowAfter     a phase-one group still alive this long after it rose starts to glow
	 * @param fadeAfter     and this long after it rose it starts to die
	 * @param fadeTicks     dying takes this long: each creature loses its full health over it, then falls
	 * @param healthMultiplier every creature she raises has this share of its usual health
	 */
	public record Summon(int groups, int halfDelay, Mix lurkerGroup, Mix wightGroup, int maxWights, double minRadius, double maxRadius,
			int firstDelay, int lurkerWeight, int wightWeight, int soldierWeight, double followRange,
			int glowAfter, int fadeAfter, int fadeTicks, double healthMultiplier) {
		public static final Summon DEFAULT = new Summon(2, 400, new Mix(1, 2, 1, 3), new Mix(1, 1, 2, 3), 3, 16.0, 35.0, 40, 4, 3, 4, 64.0, 1800, 3000, 200,
				0.7);
		public static final Codec<Summon> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(1, 8).fieldOf("groups").orElse(DEFAULT.groups).forGetter(Summon::groups),
				Codec.intRange(0, 2400).fieldOf("half_delay").orElse(DEFAULT.halfDelay).forGetter(Summon::halfDelay),
				Mix.CODEC.fieldOf("lurker_group").orElse(DEFAULT.lurkerGroup).forGetter(Summon::lurkerGroup),
				Mix.CODEC.fieldOf("wight_group").orElse(DEFAULT.wightGroup).forGetter(Summon::wightGroup),
				Codec.intRange(0, 16).fieldOf("max_wights").orElse(DEFAULT.maxWights).forGetter(Summon::maxWights),
				Codec.doubleRange(2.0, 64.0).fieldOf("min_radius").orElse(DEFAULT.minRadius).forGetter(Summon::minRadius),
				Codec.doubleRange(2.0, 64.0).fieldOf("max_radius").orElse(DEFAULT.maxRadius).forGetter(Summon::maxRadius),
				Codec.intRange(0, 1200).fieldOf("first_delay").orElse(DEFAULT.firstDelay).forGetter(Summon::firstDelay),
				Codec.intRange(0, 100).fieldOf("lurker_weight").orElse(DEFAULT.lurkerWeight).forGetter(Summon::lurkerWeight),
				Codec.intRange(0, 100).fieldOf("wight_weight").orElse(DEFAULT.wightWeight).forGetter(Summon::wightWeight),
				Codec.intRange(0, 100).fieldOf("soldier_weight").orElse(DEFAULT.soldierWeight).forGetter(Summon::soldierWeight),
				Codec.doubleRange(16.0, 128.0).fieldOf("follow_range").orElse(DEFAULT.followRange).forGetter(Summon::followRange),
				Codec.intRange(0, 72000).fieldOf("glow_after").orElse(DEFAULT.glowAfter).forGetter(Summon::glowAfter),
				Codec.intRange(0, 72000).fieldOf("fade_after").orElse(DEFAULT.fadeAfter).forGetter(Summon::fadeAfter),
				Codec.intRange(20, 2400).fieldOf("fade_ticks").orElse(DEFAULT.fadeTicks).forGetter(Summon::fadeTicks),
				Codec.doubleRange(0.05, 4.0).fieldOf("health_multiplier").orElse(DEFAULT.healthMultiplier).forGetter(Summon::healthMultiplier)
		).apply(instance, Summon::new));

		/**
		 * One kind of phase-one group: its own creatures (lurkers or wights) and the drowned soldiers that rise with them.
		 *
		 * @param min         its own creatures, at least
		 * @param max         at most
		 * @param soldiersMin drowned soldiers with them, at least
		 * @param soldiersMax at most
		 */
		public record Mix(int min, int max, int soldiersMin, int soldiersMax) {
			public static final Codec<Mix> CODEC = RecordCodecBuilder.create(instance -> instance.group(
					Codec.intRange(0, 16).fieldOf("min").forGetter(Mix::min),
					Codec.intRange(0, 16).fieldOf("max").forGetter(Mix::max),
					Codec.intRange(0, 16).fieldOf("soldiers_min").forGetter(Mix::soldiersMin),
					Codec.intRange(0, 16).fieldOf("soldiers_max").forGetter(Mix::soldiersMax)
			).apply(instance, Mix::new));
		}
	}

	/**
	 * @param cooldown      ticks between two flasks
	 * @param range         she throws at a player this close
	 * @param radius        the splash reaches this far (a vanilla splash potion: 4)
	 * @param poisonTicks   poison I
	 * @param nauseaTicks   nausea
	 * @param nauseaLevel   nausea's level
	 * @param weaknessTicks weakness (none by default)
	 * @param weaknessLevel weakness's level
	 * @param flightTicks   the flask is in the air at least this long, so a player can step out from under it
	 */
	public record Potion(int cooldown, double range, double radius, int poisonTicks, int nauseaTicks, int nauseaLevel, int weaknessTicks,
			int weaknessLevel, int flightTicks) {
		public static final Potion DEFAULT = new Potion(300, 28.0, 4.0, 200, 50, 0, 0, 0, 24);
		public static final Codec<Potion> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(10, 12000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Potion::cooldown),
				Codec.doubleRange(2.0, 64.0).fieldOf("range").orElse(DEFAULT.range).forGetter(Potion::range),
				Codec.doubleRange(1.0, 16.0).fieldOf("radius").orElse(DEFAULT.radius).forGetter(Potion::radius),
				Codec.intRange(0, 12000).fieldOf("poison_ticks").orElse(DEFAULT.poisonTicks).forGetter(Potion::poisonTicks),
				Codec.intRange(0, 12000).fieldOf("nausea_ticks").orElse(DEFAULT.nauseaTicks).forGetter(Potion::nauseaTicks),
				Codec.intRange(0, 9).fieldOf("nausea_level").orElse(DEFAULT.nauseaLevel).forGetter(Potion::nauseaLevel),
				Codec.intRange(0, 12000).fieldOf("weakness_ticks").orElse(DEFAULT.weaknessTicks).forGetter(Potion::weaknessTicks),
				Codec.intRange(0, 9).fieldOf("weakness_level").orElse(DEFAULT.weaknessLevel).forGetter(Potion::weaknessLevel),
				Codec.intRange(5, 100).fieldOf("flight_ticks").orElse(DEFAULT.flightTicks).forGetter(Potion::flightTicks)
		).apply(instance, Potion::new));
	}

	/**
	 * @param cooldown        ticks between two spells
	 * @param duration        how long a spell's effect lasts on the creatures
	 * @param farDistance     a group this far from the player or further gets speed
	 * @param nearDistance    a group this close gets strength (or absorption, if the player hurt it)
	 * @param hurtMemory      a group the player hurt within this many ticks gets absorption
	 * @param flightTicks     ticks the spell's sparks fly from her staff to the group
	 * @param absorptionLevel absorption's level (1 is II)
	 * @param speedLevel      speed's level (1 is II)
	 * @param strengthLevel   strength's level (0 is I)
	 */
	public record Spell(int cooldown, int duration, double farDistance, double nearDistance, int hurtMemory, int flightTicks,
			int absorptionLevel, int speedLevel, int strengthLevel) {
		public static final Spell DEFAULT = new Spell(120, 400, 10.0, 5.0, 100, 20, 1, 1, 0);
		public static final Codec<Spell> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(10, 12000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Spell::cooldown),
				Codec.intRange(20, 12000).fieldOf("duration").orElse(DEFAULT.duration).forGetter(Spell::duration),
				Codec.doubleRange(1.0, 64.0).fieldOf("far_distance").orElse(DEFAULT.farDistance).forGetter(Spell::farDistance),
				Codec.doubleRange(0.0, 64.0).fieldOf("near_distance").orElse(DEFAULT.nearDistance).forGetter(Spell::nearDistance),
				Codec.intRange(0, 2400).fieldOf("hurt_memory").orElse(DEFAULT.hurtMemory).forGetter(Spell::hurtMemory),
				Codec.intRange(4, 100).fieldOf("flight_ticks").orElse(DEFAULT.flightTicks).forGetter(Spell::flightTicks),
				Codec.intRange(0, 9).fieldOf("absorption_level").orElse(DEFAULT.absorptionLevel).forGetter(Spell::absorptionLevel),
				Codec.intRange(0, 9).fieldOf("speed_level").orElse(DEFAULT.speedLevel).forGetter(Spell::speedLevel),
				Codec.intRange(0, 9).fieldOf("strength_level").orElse(DEFAULT.strengthLevel).forGetter(Spell::strengthLevel)
		).apply(instance, Spell::new));
	}

	/**
	 * @param cooldown      ticks between two glyphs
	 * @param firstDelay    ticks after she raises a wave before her first glyph
	 * @param radius        reach of the blinding and draining glyphs
	 * @param damageRadius  reach of the glyph of harm
	 * @param duration      how long the blinding and draining glyphs stay
	 * @param damageDelay   the glyph of harm strikes this long after it appears
	 * @param damage        what the glyph of harm deals to a player inside, through armor
	 * @param mobHeal       what it heals each creature inside
	 * @param drainHeal     health a draining glyph gives each creature inside every second, if it cannot take regeneration
	 * @param effectTicks   an effect a glyph gives lasts this long, renewed while the target stays inside (and taken away
	 *                      the moment it steps out)
	 * @param inactiveTicks a glyph lies grey and does nothing this long after it appears
	 */
	public record Glyphs(int cooldown, int firstDelay, double radius, double damageRadius, int duration, int damageDelay, float damage,
			float mobHeal, float drainHeal, int effectTicks, int inactiveTicks) {
		public static final Glyphs DEFAULT = new Glyphs(260, 160, 8.0, 6.0, 240, 100, 8.0F, 16.0F, 1.0F, 30, 40);
		public static final Codec<Glyphs> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(20, 12000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Glyphs::cooldown),
				Codec.intRange(0, 12000).fieldOf("first_delay").orElse(DEFAULT.firstDelay).forGetter(Glyphs::firstDelay),
				Codec.doubleRange(1.0, 24.0).fieldOf("radius").orElse(DEFAULT.radius).forGetter(Glyphs::radius),
				Codec.doubleRange(1.0, 24.0).fieldOf("damage_radius").orElse(DEFAULT.damageRadius).forGetter(Glyphs::damageRadius),
				Codec.intRange(20, 12000).fieldOf("duration").orElse(DEFAULT.duration).forGetter(Glyphs::duration),
				Codec.intRange(0, 1200).fieldOf("damage_delay").orElse(DEFAULT.damageDelay).forGetter(Glyphs::damageDelay),
				Codec.floatRange(0.0F, 1000.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Glyphs::damage),
				Codec.floatRange(0.0F, 1000.0F).fieldOf("mob_heal").orElse(DEFAULT.mobHeal).forGetter(Glyphs::mobHeal),
				Codec.floatRange(0.0F, 100.0F).fieldOf("drain_heal").orElse(DEFAULT.drainHeal).forGetter(Glyphs::drainHeal),
				Codec.intRange(2, 600).fieldOf("effect_ticks").orElse(DEFAULT.effectTicks).forGetter(Glyphs::effectTicks),
				Codec.intRange(0, 1200).fieldOf("inactive_ticks").orElse(DEFAULT.inactiveTicks).forGetter(Glyphs::inactiveTicks)
		).apply(instance, Glyphs::new));
	}

	/**
	 * @param minDistance   she keeps at least this far from the player
	 * @param maxDistance   and comes closer when further than this
	 * @param fleeDistance  a player this close makes her run
	 * @param attackPause   ticks between two of her attacks
	 * @param summonCap     she raises nothing while this many of her creatures are alive, and a group never takes her past it
	 * @param capLockout    once her creatures reach the cap she raises none for this many ticks, even with fewer left
	 * @param lockoutResetBelow fewer creatures than this alive end that lockout at once
	 * @param maxLurkers    she raises no lurker while this many of hers are alive
	 * @param maxWights     nor a wight while this many are
	 * @param summonCooldown ticks between two groups
	 * @param minSize       creatures in her group, at least
	 * @param maxSize       and at most
	 * @param fleeSpeed     her speed while she runs from a player, times her walk
	 */
	public record PhaseTwo(double minDistance, double maxDistance, double fleeDistance, int attackPause, int summonCap,
			int capLockout, int lockoutResetBelow, int maxLurkers, int maxWights, int summonCooldown, int minSize, int maxSize, double fleeSpeed) {
		public static final PhaseTwo DEFAULT = new PhaseTwo(10.0, 18.0, 7.0, 30, 4, 300, 2, 1, 1, 300, 3, 5, 1.4);
		public static final Codec<PhaseTwo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(2.0, 48.0).fieldOf("min_distance").orElse(DEFAULT.minDistance).forGetter(PhaseTwo::minDistance),
				Codec.doubleRange(2.0, 64.0).fieldOf("max_distance").orElse(DEFAULT.maxDistance).forGetter(PhaseTwo::maxDistance),
				Codec.doubleRange(1.0, 32.0).fieldOf("flee_distance").orElse(DEFAULT.fleeDistance).forGetter(PhaseTwo::fleeDistance),
				Codec.intRange(0, 1200).fieldOf("attack_pause").orElse(DEFAULT.attackPause).forGetter(PhaseTwo::attackPause),
				Codec.intRange(0, 32).fieldOf("summon_cap").orElse(DEFAULT.summonCap).forGetter(PhaseTwo::summonCap),
				Codec.intRange(0, 12000).fieldOf("cap_lockout").orElse(DEFAULT.capLockout).forGetter(PhaseTwo::capLockout),
				Codec.intRange(0, 32).fieldOf("lockout_reset_below").orElse(DEFAULT.lockoutResetBelow).forGetter(PhaseTwo::lockoutResetBelow),
				Codec.intRange(0, 32).fieldOf("max_lurkers").orElse(DEFAULT.maxLurkers).forGetter(PhaseTwo::maxLurkers),
				Codec.intRange(0, 32).fieldOf("max_wights").orElse(DEFAULT.maxWights).forGetter(PhaseTwo::maxWights),
				Codec.intRange(20, 12000).fieldOf("summon_cooldown").orElse(DEFAULT.summonCooldown).forGetter(PhaseTwo::summonCooldown),
				Codec.intRange(1, 16).fieldOf("min_size").orElse(DEFAULT.minSize).forGetter(PhaseTwo::minSize),
				Codec.intRange(1, 16).fieldOf("max_size").orElse(DEFAULT.maxSize).forGetter(PhaseTwo::maxSize),
				Codec.doubleRange(0.5, 5.0).fieldOf("flee_speed").orElse(DEFAULT.fleeSpeed).forGetter(PhaseTwo::fleeSpeed)
		).apply(instance, PhaseTwo::new));
	}

	/**
	 * @param hits        this many hits from a player...
	 * @param window      ...within this many ticks make her escape
	 * @param blindTicks  blindness she casts on the player
	 * @param runTicks    she runs off for this long before she blinks
	 * @param runSpeed    her running speed, times her walk
	 * @param distance    she blinks to this far behind the player
	 * @param minSpawn    creatures she raises next to the player when she comes back, at least
	 * @param maxSpawn    and at most
	 * @param cooldown    ticks after an escape before the next one
	 */
	public record Escape(int hits, int window, int blindTicks, int runTicks, double runSpeed, double distance, int minSpawn, int maxSpawn,
			int cooldown) {
		public static final Escape DEFAULT = new Escape(2, 100, 30, 12, 5.0, 20.0, 2, 3, 200);
		public static final Codec<Escape> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(1, 20).fieldOf("hits").orElse(DEFAULT.hits).forGetter(Escape::hits),
				Codec.intRange(1, 1200).fieldOf("window").orElse(DEFAULT.window).forGetter(Escape::window),
				Codec.intRange(0, 1200).fieldOf("blind_ticks").orElse(DEFAULT.blindTicks).forGetter(Escape::blindTicks),
				Codec.intRange(1, 200).fieldOf("run_ticks").orElse(DEFAULT.runTicks).forGetter(Escape::runTicks),
				Codec.doubleRange(1.0, 10.0).fieldOf("run_speed").orElse(DEFAULT.runSpeed).forGetter(Escape::runSpeed),
				Codec.doubleRange(3.0, 64.0).fieldOf("distance").orElse(DEFAULT.distance).forGetter(Escape::distance),
				Codec.intRange(0, 16).fieldOf("min_spawn").orElse(DEFAULT.minSpawn).forGetter(Escape::minSpawn),
				Codec.intRange(0, 16).fieldOf("max_spawn").orElse(DEFAULT.maxSpawn).forGetter(Escape::maxSpawn),
				Codec.intRange(0, 12000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Escape::cooldown)
		).apply(instance, Escape::new));
	}

	/**
	 * @param cooldown     ticks between two bolts
	 * @param range        she shoots at a player this close
	 * @param damage       the bolt's hit, through armor and shield
	 * @param speed        blocks per tick: fast, but it flies straight at where the player was
	 * @param poolTicks    the puddle under the player lasts this long
	 * @param poolDamage   it burns for this much each second, through armor
	 * @param poolRadius   its size
	 */
	public record Acid(int cooldown, double range, float damage, double speed, int poolTicks, float poolDamage, double poolRadius) {
		public static final Acid DEFAULT = new Acid(110, 28.0, 5.0F, 1.6, 60, 3.0F, 1.6);
		public static final Codec<Acid> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(10, 12000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Acid::cooldown),
				Codec.doubleRange(2.0, 64.0).fieldOf("range").orElse(DEFAULT.range).forGetter(Acid::range),
				Codec.floatRange(0.0F, 100.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Acid::damage),
				Codec.doubleRange(0.2, 5.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Acid::speed),
				Codec.intRange(1, 1200).fieldOf("pool_ticks").orElse(DEFAULT.poolTicks).forGetter(Acid::poolTicks),
				Codec.floatRange(0.0F, 100.0F).fieldOf("pool_damage").orElse(DEFAULT.poolDamage).forGetter(Acid::poolDamage),
				Codec.doubleRange(0.5, 8.0).fieldOf("pool_radius").orElse(DEFAULT.poolRadius).forGetter(Acid::poolRadius)
		).apply(instance, Acid::new));
	}

	/**
	 * @param cooldown  ticks between two bolts
	 * @param range     she shoots at a player this close
	 * @param speed     blocks per tick
	 * @param count     threads that tie the player
	 * @param health    each thread's health
	 * @param maxTicks  the threads wither by themselves after this long
	 */
	public record Threads(int cooldown, double range, double speed, int count, float health, int maxTicks) {
		public static final Threads DEFAULT = new Threads(360, 24.0, 0.9, 3, 1.0F, 300);
		public static final Codec<Threads> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(10, 12000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Threads::cooldown),
				Codec.doubleRange(2.0, 64.0).fieldOf("range").orElse(DEFAULT.range).forGetter(Threads::range),
				Codec.doubleRange(0.2, 5.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Threads::speed),
				Codec.intRange(1, 8).fieldOf("count").orElse(DEFAULT.count).forGetter(Threads::count),
				Codec.floatRange(1.0F, 100.0F).fieldOf("health").orElse(DEFAULT.health).forGetter(Threads::health),
				Codec.intRange(20, 12000).fieldOf("max_ticks").orElse(DEFAULT.maxTicks).forGetter(Threads::maxTicks)
		).apply(instance, Threads::new));
	}

	/**
	 * @param cooldown   ticks between two beams
	 * @param range      the beam reaches this far
	 * @param windup     ticks she aims before the beam lights
	 * @param ticks      how long the beam burns
	 * @param damage     what it deals over its whole length, through armor; a shield held towards her blocks it
	 * @param fireTicks  it sets the player alight for this long
	 * @param turn       degrees per tick she can turn the beam after a dodging player
	 */
	public record Beam(int cooldown, double range, int windup, int ticks, float damage, int fireTicks, float turn) {
		public static final Beam DEFAULT = new Beam(260, 22.0, 16, 27, 4.5F, 40, 3.0F);
		public static final Codec<Beam> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(10, 12000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Beam::cooldown),
				Codec.doubleRange(2.0, 64.0).fieldOf("range").orElse(DEFAULT.range).forGetter(Beam::range),
				Codec.intRange(0, 100).fieldOf("windup").orElse(DEFAULT.windup).forGetter(Beam::windup),
				Codec.intRange(10, 400).fieldOf("ticks").orElse(DEFAULT.ticks).forGetter(Beam::ticks),
				Codec.floatRange(0.0F, 100.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Beam::damage),
				Codec.intRange(0, 1200).fieldOf("fire_ticks").orElse(DEFAULT.fireTicks).forGetter(Beam::fireTicks),
				Codec.floatRange(0.0F, 45.0F).fieldOf("turn").orElse(DEFAULT.turn).forGetter(Beam::turn)
		).apply(instance, Beam::new));
	}

	/**
	 * @param cooldown       ticks between two claw attacks
	 * @param blindTicks     blindness she casts first
	 * @param glowTicks      she glows (vanilla glowing) this long from the blink, so she can be found behind the player
	 * @param distance       she blinks to this far behind the player
	 * @param angle          at most this many degrees off straight behind them
	 * @param lunge          ticks from the blink to her first rake; the parry window ends with them
	 * @param parryWindow    ticks before the first rake in which a hit parries her; she glows meanwhile
	 * @param hits           rakes
	 * @param gap            ticks between two rakes
	 * @param damage         each rake
	 * @param shieldSeconds  the first rake knocks a raised shield out for this long
	 * @param parryMultiplier a parrying hit deals this many times its damage
	 */
	public record Claws(int cooldown, int blindTicks, int glowTicks, double distance, float angle, int lunge, int parryWindow, int hits, int gap,
			float damage, float shieldSeconds, float parryMultiplier) {
		public static final Claws DEFAULT = new Claws(280, 30, 60, 10.0, 35.0F, 24, 14, 5, 4, 6.0F, 5.0F, 1.5F);
		public static final Codec<Claws> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(10, 12000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Claws::cooldown),
				Codec.intRange(0, 1200).fieldOf("blind_ticks").orElse(DEFAULT.blindTicks).forGetter(Claws::blindTicks),
				Codec.intRange(0, 1200).fieldOf("glow_ticks").orElse(DEFAULT.glowTicks).forGetter(Claws::glowTicks),
				Codec.doubleRange(2.0, 24.0).fieldOf("distance").orElse(DEFAULT.distance).forGetter(Claws::distance),
				Codec.floatRange(0.0F, 180.0F).fieldOf("angle").orElse(DEFAULT.angle).forGetter(Claws::angle),
				Codec.intRange(4, 60).fieldOf("lunge").orElse(DEFAULT.lunge).forGetter(Claws::lunge),
				Codec.intRange(1, 60).fieldOf("parry_window").orElse(DEFAULT.parryWindow).forGetter(Claws::parryWindow),
				Codec.intRange(1, 20).fieldOf("hits").orElse(DEFAULT.hits).forGetter(Claws::hits),
				Codec.intRange(1, 40).fieldOf("gap").orElse(DEFAULT.gap).forGetter(Claws::gap),
				Codec.floatRange(0.0F, 100.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Claws::damage),
				Codec.floatRange(0.0F, 60.0F).fieldOf("shield_seconds").orElse(DEFAULT.shieldSeconds).forGetter(Claws::shieldSeconds),
				Codec.floatRange(1.0F, 10.0F).fieldOf("parry_multiplier").orElse(DEFAULT.parryMultiplier).forGetter(Claws::parryMultiplier)
		).apply(instance, Claws::new));
	}

	/**
	 * @param below    she eats only below this much health
	 * @param heal     health one creature gives her back
	 * @param cooldown at most once in this many ticks
	 * @param range    a creature this close to her
	 * @param ticks    how long the meal takes
	 */
	public record Eat(float below, float heal, int cooldown, double range, int ticks) {
		public static final Eat DEFAULT = new Eat(60.0F, 25.0F, 800, 20.0, 30);
		public static final Codec<Eat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 2048.0F).fieldOf("below").orElse(DEFAULT.below).forGetter(Eat::below),
				Codec.floatRange(0.0F, 2048.0F).fieldOf("heal").orElse(DEFAULT.heal).forGetter(Eat::heal),
				Codec.intRange(20, 72000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Eat::cooldown),
				Codec.doubleRange(2.0, 64.0).fieldOf("range").orElse(DEFAULT.range).forGetter(Eat::range),
				Codec.intRange(5, 200).fieldOf("ticks").orElse(DEFAULT.ticks).forGetter(Eat::ticks)
		).apply(instance, Eat::new));
	}
}
