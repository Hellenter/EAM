package io.github.hellenter.eam.mobs.entity.crossbowman;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for the elite crossbowman, read from {@code data/eam/mobs/elite_crossbowman.json} so datapacks can
 * override them. A missing or invalid field falls back to its default.
 *
 * @param stats         health, armor and the like
 * @param replaceChance share of pillagers in patrols, outposts and raids that spawn as elite crossbowmen instead
 * @param minRange      closer than this he backs away from his target
 * @param maxRange      further than this, or out of sight, he walks closer
 * @param pauseMin      fewest ticks between one shot and loading the next
 * @param pauseMax      most ticks between one shot and loading the next
 * @param aimTicks      with the crossbow loaded he stops and holds his aim this long before he shoots
 * @param normal        the plain shot
 * @param burst         three quick bolts
 * @param charged       the red bolt
 * @param net           the net shot
 */
public record EliteCrossbowmanSettings(Stats stats, float replaceChance, double minRange, double maxRange, int pauseMin, int pauseMax,
		int aimTicks, Normal normal, Burst burst, Charged charged, Net net) {
	public static final EliteCrossbowmanSettings DEFAULT = new EliteCrossbowmanSettings(Stats.DEFAULT, 0.15F, 5.0, 24.0, 15, 35, 6,
			Normal.DEFAULT, Burst.DEFAULT, Charged.DEFAULT, Net.DEFAULT);

	/**
	 * @param health      max health
	 * @param armor       armor points of his cuirass and helmet
	 * @param speed       movement speed attribute
	 * @param followRange how far he notices targets
	 * @param xp          experience he drops
	 */
	public record Stats(double health, double armor, double speed, double followRange, int xp) {
		public static final Stats DEFAULT = new Stats(24.0, 6.0, 0.35, 64.0, 10);

		public static final Codec<Stats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(1.0, 1024.0).fieldOf("health").orElse(DEFAULT.health).forGetter(Stats::health),
				Codec.doubleRange(0.0, 30.0).fieldOf("armor").orElse(DEFAULT.armor).forGetter(Stats::armor),
				Codec.doubleRange(0.0, 1.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Stats::speed),
				Codec.doubleRange(1.0, 128.0).fieldOf("follow_range").orElse(DEFAULT.followRange).forGetter(Stats::followRange),
				Codec.intRange(0, 1000).fieldOf("xp").orElse(DEFAULT.xp).forGetter(Stats::xp)
		).apply(instance, Stats::new));
	}

	/**
	 * @param weight     how often he picks it among the attacks that are ready
	 * @param loadTicks  ticks he spends loading the crossbow
	 * @param damage     damage of a bolt that hits at full speed
	 * @param speed      speed of the bolt, blocks per tick (a player's full crossbow shot is 3.15)
	 * @param inaccuracy spread of the shot, like a mob's bow (vanilla pillagers: 6 on hard, 14 on easy)
	 */
	public record Normal(int weight, int loadTicks, float damage, float speed, float inaccuracy) {
		public static final Normal DEFAULT = new Normal(6, 22, 5.0F, 1.6F, 2.0F);

		public static final Codec<Normal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, 100).fieldOf("weight").orElse(DEFAULT.weight).forGetter(Normal::weight),
				Codec.intRange(1, 200).fieldOf("load_ticks").orElse(DEFAULT.loadTicks).forGetter(Normal::loadTicks),
				Codec.floatRange(0.0F, 100.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Normal::damage),
				Codec.floatRange(0.1F, 5.0F).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Normal::speed),
				Codec.floatRange(0.0F, 30.0F).fieldOf("inaccuracy").orElse(DEFAULT.inaccuracy).forGetter(Normal::inaccuracy)
		).apply(instance, Normal::new));
	}

	/**
	 * @param weight           how often he picks it among the attacks that are ready
	 * @param cooldown         ticks before he can pick it again
	 * @param loadTicks        ticks he spends loading the three bolts
	 * @param shots            bolts in a burst
	 * @param interval         ticks between bolts
	 * @param damageMultiplier each bolt's damage against a plain shot's
	 * @param inaccuracy       spread of each bolt
	 */
	public record Burst(int weight, int cooldown, int loadTicks, int shots, int interval, float damageMultiplier, float inaccuracy) {
		public static final Burst DEFAULT = new Burst(3, 120, 34, 3, 5, 0.85F, 5.0F);

		public static final Codec<Burst> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, 100).fieldOf("weight").orElse(DEFAULT.weight).forGetter(Burst::weight),
				Codec.intRange(0, 2400).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Burst::cooldown),
				Codec.intRange(1, 200).fieldOf("load_ticks").orElse(DEFAULT.loadTicks).forGetter(Burst::loadTicks),
				Codec.intRange(1, 10).fieldOf("shots").orElse(DEFAULT.shots).forGetter(Burst::shots),
				Codec.intRange(1, 40).fieldOf("interval").orElse(DEFAULT.interval).forGetter(Burst::interval),
				Codec.floatRange(0.0F, 10.0F).fieldOf("damage_multiplier").orElse(DEFAULT.damageMultiplier).forGetter(Burst::damageMultiplier),
				Codec.floatRange(0.0F, 30.0F).fieldOf("inaccuracy").orElse(DEFAULT.inaccuracy).forGetter(Burst::inaccuracy)
		).apply(instance, Burst::new));
	}

	/**
	 * @param weight           how often he picks it among the attacks that are ready
	 * @param cooldown         ticks before he can pick it again
	 * @param loadTicks        ticks he spends loading the crossbow
	 * @param windupTicks      ticks the crossbow glows red before the shot: the time to get out of the way
	 * @param damageMultiplier the red bolt's damage against a plain shot's
	 * @param speedMultiplier  the red bolt's speed against a plain shot's
	 * @param shieldMultiplier share of the damage that goes through a raised shield
	 */
	public record Charged(int weight, int cooldown, int loadTicks, int windupTicks, float damageMultiplier, float speedMultiplier,
			float shieldMultiplier) {
		public static final Charged DEFAULT = new Charged(2, 200, 22, 30, 2.0F, 2.0F, 0.5F);

		public static final Codec<Charged> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, 100).fieldOf("weight").orElse(DEFAULT.weight).forGetter(Charged::weight),
				Codec.intRange(0, 2400).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Charged::cooldown),
				Codec.intRange(1, 200).fieldOf("load_ticks").orElse(DEFAULT.loadTicks).forGetter(Charged::loadTicks),
				Codec.intRange(0, 200).fieldOf("windup_ticks").orElse(DEFAULT.windupTicks).forGetter(Charged::windupTicks),
				Codec.floatRange(0.0F, 10.0F).fieldOf("damage_multiplier").orElse(DEFAULT.damageMultiplier).forGetter(Charged::damageMultiplier),
				Codec.floatRange(0.1F, 4.0F).fieldOf("speed_multiplier").orElse(DEFAULT.speedMultiplier).forGetter(Charged::speedMultiplier),
				Codec.floatRange(0.0F, 1.0F).fieldOf("shield_multiplier").orElse(DEFAULT.shieldMultiplier).forGetter(Charged::shieldMultiplier)
		).apply(instance, Charged::new));
	}

	/**
	 * @param weight    how often he picks it among the attacks that are ready (only at players)
	 * @param cooldown  ticks before he can pick it again
	 * @param loadTicks ticks he spends loading the net
	 * @param speed     speed of the net, blocks per tick
	 * @param maxRange  he shoots a net only at a target this close
	 * @param seconds   the net he leaves under the player falls apart after this long if nobody cuts it
	 */
	public record Net(int weight, int cooldown, int loadTicks, float speed, double maxRange, int seconds) {
		public static final Net DEFAULT = new Net(1, 400, 26, 1.4F, 12.0, 10);

		public static final Codec<Net> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, 100).fieldOf("weight").orElse(DEFAULT.weight).forGetter(Net::weight),
				Codec.intRange(0, 2400).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Net::cooldown),
				Codec.intRange(1, 200).fieldOf("load_ticks").orElse(DEFAULT.loadTicks).forGetter(Net::loadTicks),
				Codec.floatRange(0.1F, 5.0F).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Net::speed),
				Codec.doubleRange(1.0, 48.0).fieldOf("max_range").orElse(DEFAULT.maxRange).forGetter(Net::maxRange),
				Codec.intRange(1, 600).fieldOf("seconds").orElse(DEFAULT.seconds).forGetter(Net::seconds)
		).apply(instance, Net::new));
	}

	public static final Codec<EliteCrossbowmanSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Stats.CODEC.fieldOf("stats").orElse(DEFAULT.stats).forGetter(EliteCrossbowmanSettings::stats),
			Codec.floatRange(0.0F, 1.0F).fieldOf("replace_chance").orElse(DEFAULT.replaceChance).forGetter(EliteCrossbowmanSettings::replaceChance),
			Codec.doubleRange(0.0, 32.0).fieldOf("min_range").orElse(DEFAULT.minRange).forGetter(EliteCrossbowmanSettings::minRange),
			Codec.doubleRange(1.0, 48.0).fieldOf("max_range").orElse(DEFAULT.maxRange).forGetter(EliteCrossbowmanSettings::maxRange),
			Codec.intRange(0, 400).fieldOf("pause_min").orElse(DEFAULT.pauseMin).forGetter(EliteCrossbowmanSettings::pauseMin),
			Codec.intRange(0, 400).fieldOf("pause_max").orElse(DEFAULT.pauseMax).forGetter(EliteCrossbowmanSettings::pauseMax),
			Codec.intRange(0, 60).fieldOf("aim_ticks").orElse(DEFAULT.aimTicks).forGetter(EliteCrossbowmanSettings::aimTicks),
			Normal.CODEC.fieldOf("normal").orElse(DEFAULT.normal).forGetter(EliteCrossbowmanSettings::normal),
			Burst.CODEC.fieldOf("burst").orElse(DEFAULT.burst).forGetter(EliteCrossbowmanSettings::burst),
			Charged.CODEC.fieldOf("charged").orElse(DEFAULT.charged).forGetter(EliteCrossbowmanSettings::charged),
			Net.CODEC.fieldOf("net").orElse(DEFAULT.net).forGetter(EliteCrossbowmanSettings::net)
	).apply(instance, EliteCrossbowmanSettings::new));
}
