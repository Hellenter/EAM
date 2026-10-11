package io.github.hellenter.eam.mobs.entity.warlock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for the undead warlock, read from {@code data/eam/mobs/undead_warlock.json} so datapacks can override
 * them. A missing or invalid field falls back to its default. Times are in ticks.
 *
 * @param health      his health
 * @param squadChance chance that a squad of the Lost brings a warlock along
 * @param minRange    closer than this he backs away from the player
 * @param maxRange    further than this, or out of sight, he walks closer
 * @param spellGap    ticks he rests after any spell before the next
 * @param missiles    red homing missiles
 * @param wind        a gust that throws a player who came too close back
 * @param buff        Speed I or Strength I for the Lost around him
 * @param bats        a flock of vampire bats called to the fight
 */
public record UndeadWarlockSettings(double health, float squadChance, double minRange, double maxRange, int spellGap, Missiles missiles,
		Wind wind, Buff buff, Bats bats) {
	public static final UndeadWarlockSettings DEFAULT = new UndeadWarlockSettings(30.0, 0.1F, 7.0, 14.0, 15, Missiles.DEFAULT,
			Wind.DEFAULT, Buff.DEFAULT, Bats.DEFAULT);

	/**
	 * @param count    missiles in one cast, released one after another
	 * @param damage   damage of each, through armor and shields like all magic
	 * @param speed    blocks per tick
	 * @param turn     how sharply it turns toward its target each tick, 0 (never) to 1 (at once)
	 * @param lifetime ticks before it fizzles out
	 * @param range    he casts only at a target this close
	 * @param cooldown ticks between two casts
	 */
	public record Missiles(int count, float damage, double speed, double turn, int lifetime, double range, int cooldown) {
		public static final Missiles DEFAULT = new Missiles(3, 1.0F, 0.55, 0.12, 100, 20.0, 70);

		public static final Codec<Missiles> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(1, 10).fieldOf("count").orElse(DEFAULT.count).forGetter(Missiles::count),
				Codec.floatRange(0.0F, 100.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Missiles::damage),
				Codec.doubleRange(0.05, 3.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Missiles::speed),
				Codec.doubleRange(0.0, 1.0).fieldOf("turn").orElse(DEFAULT.turn).forGetter(Missiles::turn),
				Codec.intRange(1, 1200).fieldOf("lifetime").orElse(DEFAULT.lifetime).forGetter(Missiles::lifetime),
				Codec.doubleRange(1.0, 64.0).fieldOf("range").orElse(DEFAULT.range).forGetter(Missiles::range),
				Codec.intRange(0, 2400).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Missiles::cooldown)
		).apply(instance, Missiles::new));
	}

	/**
	 * @param trigger  he casts it when a player is this close
	 * @param reach    the gust carries this far in front of him
	 * @param push     horizontal speed it gives, blocks per tick
	 * @param lift     upward speed it gives
	 * @param damage   damage it deals
	 * @param cooldown ticks between two gusts
	 */
	public record Wind(double trigger, double reach, double push, double lift, float damage, int cooldown) {
		public static final Wind DEFAULT = new Wind(5.0, 7.0, 1.1, 0.4, 1.0F, 140);

		public static final Codec<Wind> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(0.0, 32.0).fieldOf("trigger").orElse(DEFAULT.trigger).forGetter(Wind::trigger),
				Codec.doubleRange(0.0, 32.0).fieldOf("reach").orElse(DEFAULT.reach).forGetter(Wind::reach),
				Codec.doubleRange(0.0, 5.0).fieldOf("push").orElse(DEFAULT.push).forGetter(Wind::push),
				Codec.doubleRange(0.0, 3.0).fieldOf("lift").orElse(DEFAULT.lift).forGetter(Wind::lift),
				Codec.floatRange(0.0F, 100.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Wind::damage),
				Codec.intRange(0, 2400).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Wind::cooldown)
		).apply(instance, Wind::new));
	}

	/**
	 * @param radius   the Lost within this many blocks get it
	 * @param duration ticks it lasts
	 * @param cooldown ticks between two casts
	 */
	public record Buff(double radius, int duration, int cooldown) {
		public static final Buff DEFAULT = new Buff(16.0, 600, 400);

		public static final Codec<Buff> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(1.0, 64.0).fieldOf("radius").orElse(DEFAULT.radius).forGetter(Buff::radius),
				Codec.intRange(1, 12000).fieldOf("duration").orElse(DEFAULT.duration).forGetter(Buff::duration),
				Codec.intRange(0, 12000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Buff::cooldown)
		).apply(instance, Buff::new));
	}

	/**
	 * @param min        fewest bats he calls
	 * @param max        most bats
	 * @param chance     chance he calls them whenever he could
	 * @param firstDelay ticks into a fight before he may call them the first time
	 * @param cooldown   ticks between two flocks
	 * @param maxNearby  he does not call more while this many vampire bats are within 32 blocks
	 */
	public record Bats(int min, int max, float chance, int firstDelay, int cooldown, int maxNearby) {
		public static final Bats DEFAULT = new Bats(3, 4, 0.4F, 300, 900, 8);

		public static final Codec<Bats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(1, 16).fieldOf("min").orElse(DEFAULT.min).forGetter(Bats::min),
				Codec.intRange(1, 16).fieldOf("max").orElse(DEFAULT.max).forGetter(Bats::max),
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Bats::chance),
				Codec.intRange(0, 12000).fieldOf("first_delay").orElse(DEFAULT.firstDelay).forGetter(Bats::firstDelay),
				Codec.intRange(0, 12000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Bats::cooldown),
				Codec.intRange(0, 256).fieldOf("max_nearby").orElse(DEFAULT.maxNearby).forGetter(Bats::maxNearby)
		).apply(instance, Bats::new));
	}

	public static final Codec<UndeadWarlockSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.doubleRange(1.0, 1024.0).fieldOf("health").orElse(DEFAULT.health).forGetter(UndeadWarlockSettings::health),
			Codec.floatRange(0.0F, 1.0F).fieldOf("squad_chance").orElse(DEFAULT.squadChance).forGetter(UndeadWarlockSettings::squadChance),
			Codec.doubleRange(0.0, 32.0).fieldOf("min_range").orElse(DEFAULT.minRange).forGetter(UndeadWarlockSettings::minRange),
			Codec.doubleRange(1.0, 48.0).fieldOf("max_range").orElse(DEFAULT.maxRange).forGetter(UndeadWarlockSettings::maxRange),
			Codec.intRange(0, 400).fieldOf("spell_gap").orElse(DEFAULT.spellGap).forGetter(UndeadWarlockSettings::spellGap),
			Missiles.CODEC.fieldOf("missiles").orElse(DEFAULT.missiles).forGetter(UndeadWarlockSettings::missiles),
			Wind.CODEC.fieldOf("wind").orElse(DEFAULT.wind).forGetter(UndeadWarlockSettings::wind),
			Buff.CODEC.fieldOf("buff").orElse(DEFAULT.buff).forGetter(UndeadWarlockSettings::buff),
			Bats.CODEC.fieldOf("bats").orElse(DEFAULT.bats).forGetter(UndeadWarlockSettings::bats)
	).apply(instance, UndeadWarlockSettings::new));
}
