package io.github.hellenter.eam.mobs.entity.koschei;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for Koschei, read from {@code data/eam/mobs/koschei.json} so datapacks can override them. A missing or
 * invalid field falls back to its default. Times are in ticks, distances in blocks, damage in health points (2 per
 * heart) before the player's armor; the timings of the attacks themselves are in {@link KoscheiAction}.
 *
 * @param stats               health, armor, speed and the rest of his attributes
 * @param phaseOne            the sword duel of his first phase
 * @param teleport            he vanishes and stands behind the target now and then
 * @param hop                 short hops aside or back, and a leap back when he loses health fast
 * @param mobDamageMultiplier his blows deal this many times their damage to anything but a player
 */
public record KoscheiSettings(Stats stats, PhaseOne phaseOne, Teleport teleport, Hop hop, float mobDamageMultiplier) {
	public static final KoscheiSettings DEFAULT = new KoscheiSettings(Stats.DEFAULT, PhaseOne.DEFAULT, Teleport.DEFAULT, Hop.DEFAULT, 4.0F);

	public static final Codec<KoscheiSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Stats.CODEC.fieldOf("stats").orElse(DEFAULT.stats).forGetter(KoscheiSettings::stats),
			PhaseOne.CODEC.fieldOf("phase_one").orElse(DEFAULT.phaseOne).forGetter(KoscheiSettings::phaseOne),
			Teleport.CODEC.fieldOf("teleport").orElse(DEFAULT.teleport).forGetter(KoscheiSettings::teleport),
			Hop.CODEC.fieldOf("hop").orElse(DEFAULT.hop).forGetter(KoscheiSettings::hop),
			Codec.floatRange(0.0F, 100.0F).fieldOf("mob_damage_multiplier").orElse(DEFAULT.mobDamageMultiplier)
					.forGetter(KoscheiSettings::mobDamageMultiplier)
	).apply(instance, KoscheiSettings::new));

	/**
	 * @param health         his health in the first phase
	 * @param armor          armor points
	 * @param speed          movement speed attribute
	 * @param followRange    how far he notices and follows players
	 * @param xp             experience dropped on death
	 * @param regen          health per second he regains while he has no target
	 * @param armorPiercing  share of his sword's damage that ignores the player's armor and Protection
	 * @param physicalResist share taken off physical damage to him (blows, arrows, falls, explosions); fire and magic go
	 *                       through in full
	 * @param attackSpeed    sword swings per second, like the attack speed of a player's sword (1.6); sets the pause
	 *                       between his attacks
	 */
	public record Stats(float health, int armor, double speed, double followRange, int xp, float regen, double armorPiercing,
			float physicalResist, double attackSpeed) {
		public static final Stats DEFAULT = new Stats(100.0F, 6, 0.384, 32.0, 300, 1.0F, 0.15, 0.2F, 1.75);

		public static final Codec<Stats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(1.0F, 2048.0F).fieldOf("health").orElse(DEFAULT.health).forGetter(Stats::health),
				Codec.intRange(0, 30).fieldOf("armor").orElse(DEFAULT.armor).forGetter(Stats::armor),
				Codec.doubleRange(0.01, 2.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Stats::speed),
				Codec.doubleRange(1.0, 128.0).fieldOf("follow_range").orElse(DEFAULT.followRange).forGetter(Stats::followRange),
				Codec.intRange(0, 100000).fieldOf("xp").orElse(DEFAULT.xp).forGetter(Stats::xp),
				Codec.floatRange(0.0F, 100.0F).fieldOf("regen").orElse(DEFAULT.regen).forGetter(Stats::regen),
				Codec.doubleRange(0.0, 1.0).fieldOf("armor_piercing").orElse(DEFAULT.armorPiercing).forGetter(Stats::armorPiercing),
				Codec.floatRange(0.0F, 1.0F).fieldOf("physical_resist").orElse(DEFAULT.physicalResist).forGetter(Stats::physicalResist),
				Codec.doubleRange(0.1, 20.0).fieldOf("attack_speed").orElse(DEFAULT.attackSpeed).forGetter(Stats::attackSpeed)
		).apply(instance, Stats::new));

		/** Ticks between two of his attacks: one swing at his attack speed. */
		public int attackPause() {
			return (int) Math.round(20.0 / attackSpeed);
		}
	}

	/**
	 * The first phase: a sword duel. Weights pick among the blows a target in reach can get; a blow on cooldown or
	 * just used is less likely or out.
	 *
	 * @param reach            how far his sword reaches, flat from his centre to the target's
	 * @param strike           one plain cut
	 * @param doubleStrike     two quick cuts
	 * @param orange           the orange blow only a shield stops, knocking it out
	 * @param red              the red blow a shield only halves
	 * @param overhead         the red blow from over the head, parried as it comes down
	 * @param lunge            the stab from four blocks off, parried just before it lands; a shield that takes it is knocked
	 *                         out and a quick hit back then parries him
	 * @param kick             the kick at close range, the way the Fallen Lord kicks: it knocks a raised shield out
	 * @param force            the blue burst of force from the right hand that throws the target back
	 * @param bolt             the lightning shot from the sword's point
	 * @param parry            a player's parry
	 * @param arrowParryChance chance he knocks an arrow from the front aside with his sword
	 */
	public record PhaseOne(double reach, Blow strike, Blow doubleStrike, Orange orange, Shielded red, Shielded overhead, Lunge lunge,
			Kick kick, Force force, Bolt bolt, Parry parry, float arrowParryChance) {
		public static final PhaseOne DEFAULT = new PhaseOne(3.2, new Blow(8.0F, 4.0F, 0), new Blow(6.0F, 2.0F, 50), Orange.DEFAULT,
				new Shielded(12.0F, 0.5F, 1.5F, 100, 0), new Shielded(12.0F, 0.5F, 1.5F, 120, 7), Lunge.DEFAULT, Kick.DEFAULT, Force.DEFAULT, Bolt.DEFAULT,
				Parry.DEFAULT, 1.0F);

		public static final Codec<PhaseOne> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(1.0, 8.0).fieldOf("reach").orElse(DEFAULT.reach).forGetter(PhaseOne::reach),
				Blow.CODEC.fieldOf("strike").orElse(DEFAULT.strike).forGetter(PhaseOne::strike),
				Blow.CODEC.fieldOf("double_strike").orElse(DEFAULT.doubleStrike).forGetter(PhaseOne::doubleStrike),
				Orange.CODEC.fieldOf("orange").orElse(DEFAULT.orange).forGetter(PhaseOne::orange),
				Shielded.CODEC.fieldOf("red").orElse(DEFAULT.red).forGetter(PhaseOne::red),
				Shielded.CODEC.fieldOf("overhead").orElse(DEFAULT.overhead).forGetter(PhaseOne::overhead),
				Lunge.CODEC.fieldOf("lunge").orElse(DEFAULT.lunge).forGetter(PhaseOne::lunge),
				Kick.CODEC.fieldOf("kick").orElse(DEFAULT.kick).forGetter(PhaseOne::kick),
				Force.CODEC.fieldOf("force").orElse(DEFAULT.force).forGetter(PhaseOne::force),
				Bolt.CODEC.fieldOf("bolt").orElse(DEFAULT.bolt).forGetter(PhaseOne::bolt),
				Parry.CODEC.fieldOf("parry").orElse(DEFAULT.parry).forGetter(PhaseOne::parry),
				Codec.floatRange(0.0F, 1.0F).fieldOf("arrow_parry_chance").orElse(DEFAULT.arrowParryChance).forGetter(PhaseOne::arrowParryChance)
		).apply(instance, PhaseOne::new));
	}

	/**
	 * @param damage   damage of each cut
	 * @param weight   how likely he picks it, against the other blows' weights
	 * @param cooldown ticks before he picks it again
	 */
	public record Blow(float damage, float weight, int cooldown) {
		public static final Codec<Blow> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1000.0F).fieldOf("damage").forGetter(Blow::damage),
				Codec.floatRange(0.0F, 100.0F).fieldOf("weight").forGetter(Blow::weight),
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(0).forGetter(Blow::cooldown)
		).apply(instance, Blow::new));
	}

	/**
	 * @param damage        damage when no shield takes it
	 * @param shieldSeconds how long a shield that takes it stays knocked out
	 * @param weight        how likely he picks it
	 * @param cooldown      ticks before he picks it again
	 */
	public record Orange(float damage, float shieldSeconds, float weight, int cooldown) {
		public static final Orange DEFAULT = new Orange(10.0F, 5.0F, 1.5F, 100);

		public static final Codec<Orange> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1000.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Orange::damage),
				Codec.floatRange(0.0F, 60.0F).fieldOf("shield_seconds").orElse(DEFAULT.shieldSeconds).forGetter(Orange::shieldSeconds),
				Codec.floatRange(0.0F, 100.0F).fieldOf("weight").orElse(DEFAULT.weight).forGetter(Orange::weight),
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Orange::cooldown)
		).apply(instance, Orange::new));
	}

	/**
	 * A blow a shield only softens.
	 *
	 * @param damage          damage of the blow
	 * @param shieldReduction share a raised shield takes off
	 * @param weight          how likely he picks it
	 * @param cooldown        ticks before he picks it again
	 * @param parryWindow     ticks before it lands in which a player's hit parries him; 0 for a blow that can't be parried
	 */
	public record Shielded(float damage, float shieldReduction, float weight, int cooldown, int parryWindow) {
		public static final Codec<Shielded> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1000.0F).fieldOf("damage").forGetter(Shielded::damage),
				Codec.floatRange(0.0F, 1.0F).fieldOf("shield_reduction").forGetter(Shielded::shieldReduction),
				Codec.floatRange(0.0F, 100.0F).fieldOf("weight").forGetter(Shielded::weight),
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(0).forGetter(Shielded::cooldown),
				Codec.intRange(0, 19).fieldOf("parry_window").orElse(0).forGetter(Shielded::parryWindow)
		).apply(instance, Shielded::new));
	}

	/**
	 * @param damage       damage of the stab
	 * @param minRange     closer than this he can't lunge
	 * @param maxRange     further than this he walks in first
	 * @param chance       chance he lunges at a target in range when the lunge is ready
	 * @param cooldown     ticks before he lunges again
	 * @param parryWindow  ticks before the stab in which a player's hit parries him
	 * @param maxFails     after this many times the target stood too close for a lunge he wanted, he makes room for one
	 * @param shieldSeconds     how long a shield that takes the stab stays knocked out
	 * @param shieldParryWindow ticks after the stab knocks a shield out in which the player's hit parries him
	 */
	public record Lunge(float damage, double minRange, double maxRange, float chance, int cooldown, int parryWindow, int maxFails,
			float shieldSeconds, int shieldParryWindow) {
		public static final Lunge DEFAULT = new Lunge(10.0F, 3.5, 5.0, 0.3F, 140, 7, 2, 5.0F, 11);

		public static final Codec<Lunge> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1000.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Lunge::damage),
				Codec.doubleRange(0.0, 16.0).fieldOf("min_range").orElse(DEFAULT.minRange).forGetter(Lunge::minRange),
				Codec.doubleRange(0.0, 16.0).fieldOf("max_range").orElse(DEFAULT.maxRange).forGetter(Lunge::maxRange),
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Lunge::chance),
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Lunge::cooldown),
				Codec.intRange(1, 11).fieldOf("parry_window").orElse(DEFAULT.parryWindow).forGetter(Lunge::parryWindow),
				Codec.intRange(0, 100).fieldOf("max_fails").orElse(DEFAULT.maxFails).forGetter(Lunge::maxFails),
				Codec.floatRange(0.0F, 60.0F).fieldOf("shield_seconds").orElse(DEFAULT.shieldSeconds).forGetter(Lunge::shieldSeconds),
				Codec.intRange(1, 100).fieldOf("shield_parry_window").orElse(DEFAULT.shieldParryWindow).forGetter(Lunge::shieldParryWindow)
		).apply(instance, Lunge::new));
	}

	/**
	 * The kick, as the Fallen Lord kicks: a step in and a boot to the target. It knocks a raised shield out first, so it
	 * always lands.
	 *
	 * @param damage        damage of the kick
	 * @param knockback     how hard it pushes the target back
	 * @param shieldSeconds how long a shield it kicks aside stays knocked out
	 * @param reach         he kicks a target this close, flat from his centre
	 * @param shieldTicks   a target that has held its shield up this many ticks this close gets kicked
	 * @param weight        how likely he kicks a close target with no shield up, against his blows' weights
	 * @param cooldown      ticks before he kicks again
	 */
	public record Kick(float damage, float knockback, float shieldSeconds, double reach, int shieldTicks, float weight, int cooldown) {
		public static final Kick DEFAULT = new Kick(5.0F, 0.8F, 5.0F, 2.2, 8, 1.5F, 80);

		public static final Codec<Kick> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1000.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Kick::damage),
				Codec.floatRange(0.0F, 16.0F).fieldOf("knockback").orElse(DEFAULT.knockback).forGetter(Kick::knockback),
				Codec.floatRange(0.0F, 60.0F).fieldOf("shield_seconds").orElse(DEFAULT.shieldSeconds).forGetter(Kick::shieldSeconds),
				Codec.doubleRange(0.5, 8.0).fieldOf("reach").orElse(DEFAULT.reach).forGetter(Kick::reach),
				Codec.intRange(0, 200).fieldOf("shield_ticks").orElse(DEFAULT.shieldTicks).forGetter(Kick::shieldTicks),
				Codec.floatRange(0.0F, 100.0F).fieldOf("weight").orElse(DEFAULT.weight).forGetter(Kick::weight),
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Kick::cooldown)
		).apply(instance, Kick::new));
	}

	/**
	 * @param push        blocks the burst throws a target back
	 * @param shieldPush  blocks it throws back a target behind a raised shield
	 * @param range       how far in front of him it reaches
	 * @param lift        upward speed it gives
	 * @param airTicks    ticks a thrown target is reckoned to stay in the air, for the push speed
	 * @param chance      chance he uses it on a target in reach when it is ready, just to make room
	 * @param cooldown    ticks before he uses it again
	 */
	public record Force(double push, double shieldPush, double range, double lift, int airTicks, float chance, int cooldown) {
		public static final Force DEFAULT = new Force(10.0, 6.0, 5.0, 0.45, 13, 0.1F, 160);

		public static final Codec<Force> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(0.0, 64.0).fieldOf("push").orElse(DEFAULT.push).forGetter(Force::push),
				Codec.doubleRange(0.0, 64.0).fieldOf("shield_push").orElse(DEFAULT.shieldPush).forGetter(Force::shieldPush),
				Codec.doubleRange(0.0, 32.0).fieldOf("range").orElse(DEFAULT.range).forGetter(Force::range),
				Codec.doubleRange(0.0, 4.0).fieldOf("lift").orElse(DEFAULT.lift).forGetter(Force::lift),
				Codec.intRange(1, 100).fieldOf("air_ticks").orElse(DEFAULT.airTicks).forGetter(Force::airTicks),
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Force::chance),
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Force::cooldown)
		).apply(instance, Force::new));
	}

	/**
	 * The lightning shot, from his rare pool.
	 *
	 * @param damagePerSecond pure damage per second while it holds on the target
	 * @param pulse           ticks between two pulses of damage
	 * @param minRange        closer than this he never shoots
	 * @param maxRange        further than this he doesn't shoot, and the bolt breaks off
	 * @param chance          chance each tick he shoots while it is ready and the target is in range and he is free
	 * @param cooldown        ticks before he shoots again
	 * @param shieldBack      after he knocks the target's shield out he starts the bolt no sooner than this many ticks
	 *                        before the shield is ready again
	 * @param hungerTicks     once the bolt has been ready this long without a cast (the shield kept knocked out, the
	 *                        target kept close) he stops knocking shields out and goes for the bolt
	 * @param hungerChance    the bolt's chance is multiplied by this then
	 * @param hungerMinRange  and he shoots from this close
	 */
	public record Bolt(float damagePerSecond, int pulse, double minRange, double maxRange, float chance, int cooldown, int shieldBack,
			int hungerTicks, float hungerChance, double hungerMinRange) {
		public static final Bolt DEFAULT = new Bolt(8.0F, 5, 6.0, 18.0, 0.03F, 400, 10, 300, 5.0F, 3.0);

		public static final Codec<Bolt> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1000.0F).fieldOf("damage_per_second").orElse(DEFAULT.damagePerSecond).forGetter(Bolt::damagePerSecond),
				Codec.intRange(1, 30).fieldOf("pulse").orElse(DEFAULT.pulse).forGetter(Bolt::pulse),
				Codec.doubleRange(0.0, 64.0).fieldOf("min_range").orElse(DEFAULT.minRange).forGetter(Bolt::minRange),
				Codec.doubleRange(0.0, 64.0).fieldOf("max_range").orElse(DEFAULT.maxRange).forGetter(Bolt::maxRange),
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Bolt::chance),
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Bolt::cooldown),
				Codec.intRange(0, 24000).fieldOf("shield_back").orElse(DEFAULT.shieldBack).forGetter(Bolt::shieldBack),
				Codec.intRange(0, 24000).fieldOf("hunger_ticks").orElse(DEFAULT.hungerTicks).forGetter(Bolt::hungerTicks),
				Codec.floatRange(1.0F, 100.0F).fieldOf("hunger_chance").orElse(DEFAULT.hungerChance).forGetter(Bolt::hungerChance),
				Codec.doubleRange(0.0, 64.0).fieldOf("hunger_min_range").orElse(DEFAULT.hungerMinRange).forGetter(Bolt::hungerMinRange)
		).apply(instance, Bolt::new));
	}

	/**
	 * @param multiplier  damage multiplier of the parrying hit
	 * @param windupHits  a player who hits him more than this many times while he raises the overhead blow (just clicking
	 *                    at him) loses its parry for that blow
	 */
	public record Parry(float multiplier, int windupHits) {
		public static final Parry DEFAULT = new Parry(1.5F, 2);

		public static final Codec<Parry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 10.0F).fieldOf("multiplier").orElse(DEFAULT.multiplier).forGetter(Parry::multiplier),
				Codec.intRange(0, 100).fieldOf("windup_hits").orElse(DEFAULT.windupHits).forGetter(Parry::windupHits)
		).apply(instance, Parry::new));
	}

	/**
	 * @param cooldown ticks between two teleports
	 * @param chance   chance each tick he teleports while it is ready and he is free to
	 * @param distance how far behind the target he appears
	 * @param maxAngle he appears up to this many degrees to either side of straight behind the target
	 */
	public record Teleport(int cooldown, float chance, double distance, float maxAngle) {
		public static final Teleport DEFAULT = new Teleport(240, 0.05F, 6.0, 70.0F);

		public static final Codec<Teleport> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Teleport::cooldown),
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Teleport::chance),
				Codec.doubleRange(1.0, 32.0).fieldOf("distance").orElse(DEFAULT.distance).forGetter(Teleport::distance),
				Codec.floatRange(0.0F, 180.0F).fieldOf("max_angle").orElse(DEFAULT.maxAngle).forGetter(Teleport::maxAngle)
		).apply(instance, Teleport::new));
	}

	/**
	 * @param cooldown    ticks between two hops
	 * @param chance      chance he hops aside or back while he waits in reach
	 * @param speed       speed of a hop
	 * @param burstShare  losing more than this share of his health within {@code burstWindow} makes him leap back
	 * @param burstWindow ticks the lost health is counted over
	 * @param burstSpeed  speed of that leap
	 * @param inChance    chance each tick he hops in at a target he is walking up to, as the Fallen Lord does
	 * @param inFrom      he hops in only at a target at least this far away
	 * @param inMin       shortest hop in, blocks
	 * @param inMax       longest hop in, blocks; it never lands closer than a couple of blocks to the target
	 * @param flankChance chance he dashes round to the target's side or back, then strikes, each time he is about to attack
	 *                    one in reach; a fifth of it each tick he walks up to one
	 * @param flankCooldown ticks between two such dashes
	 * @param flankRange  he dashes round only a target at most this far away
	 */
	public record Hop(int cooldown, float chance, double speed, float burstShare, int burstWindow, double burstSpeed, float inChance, double inFrom,
			double inMin, double inMax, float flankChance, int flankCooldown, double flankRange) {
		public static final Hop DEFAULT = new Hop(50, 0.15F, 0.6, 0.25F, 100, 1.1, 0.06F, 4.5, 2.0, 3.5, 0.3F, 70, 6.0);

		public static final Codec<Hop> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Hop::cooldown),
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Hop::chance),
				Codec.doubleRange(0.0, 4.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Hop::speed),
				Codec.floatRange(0.0F, 1.0F).fieldOf("burst_share").orElse(DEFAULT.burstShare).forGetter(Hop::burstShare),
				Codec.intRange(1, 24000).fieldOf("burst_window").orElse(DEFAULT.burstWindow).forGetter(Hop::burstWindow),
				Codec.doubleRange(0.0, 4.0).fieldOf("burst_speed").orElse(DEFAULT.burstSpeed).forGetter(Hop::burstSpeed),
				Codec.floatRange(0.0F, 1.0F).fieldOf("in_chance").orElse(DEFAULT.inChance).forGetter(Hop::inChance),
				Codec.doubleRange(0.0, 64.0).fieldOf("in_from").orElse(DEFAULT.inFrom).forGetter(Hop::inFrom),
				Codec.doubleRange(0.0, 16.0).fieldOf("in_min").orElse(DEFAULT.inMin).forGetter(Hop::inMin),
				Codec.doubleRange(0.0, 16.0).fieldOf("in_max").orElse(DEFAULT.inMax).forGetter(Hop::inMax),
				Codec.floatRange(0.0F, 1.0F).fieldOf("flank_chance").orElse(DEFAULT.flankChance).forGetter(Hop::flankChance),
				Codec.intRange(0, 24000).fieldOf("flank_cooldown").orElse(DEFAULT.flankCooldown).forGetter(Hop::flankCooldown),
				Codec.doubleRange(0.0, 32.0).fieldOf("flank_range").orElse(DEFAULT.flankRange).forGetter(Hop::flankRange)
		).apply(instance, Hop::new));
	}
}
