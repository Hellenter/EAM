package io.github.hellenter.eam.mobs.entity.leshy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for the leshy, read from {@code data/eam/mobs/leshy.json} so datapacks can override them. A missing or
 * invalid field falls back to its default. Times are in ticks, distances in blocks.
 */
public record LeshySettings(Spawn spawn, Haunt haunt, Combat combat, Offering offering) {
	public static final LeshySettings DEFAULT = new LeshySettings(Spawn.DEFAULT, Haunt.DEFAULT, Combat.DEFAULT, Offering.DEFAULT);

	public static final Codec<LeshySettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Spawn.CODEC.fieldOf("spawn").orElse(DEFAULT.spawn).forGetter(LeshySettings::spawn),
			Haunt.CODEC.fieldOf("haunt").orElse(DEFAULT.haunt).forGetter(LeshySettings::haunt),
			Combat.CODEC.fieldOf("combat").orElse(DEFAULT.combat).forGetter(LeshySettings::combat),
			Offering.CODEC.fieldOf("offering").orElse(DEFAULT.offering).forGetter(LeshySettings::offering)
	).apply(instance, LeshySettings::new));

	/**
	 * Where and how often it turns up.
	 *
	 * @param minDay        natural leshies spawn only from this day of the world on (day 1 is the first)
	 * @param regionSize    the world is cut into square regions this wide; each one is a leshy's wood or not, for good
	 * @param regionChance  share of the regions that are a leshy's wood; it never spawns anywhere else
	 * @param spawnChance   a natural spawn that passed every other rule still happens only this often
	 * @param spacing       no leshy spawns this close to another one
	 * @param treeRadius    it spawns only with a log this close, among the trees
	 */
	public record Spawn(int minDay, int regionSize, float regionChance, float spawnChance, double spacing, int treeRadius) {
		public static final Spawn DEFAULT = new Spawn(20, 512, 0.25F, 0.04F, 256.0, 4);

		public static final Codec<Spawn> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(1, 100000).fieldOf("min_day").orElse(DEFAULT.minDay).forGetter(Spawn::minDay),
				Codec.intRange(16, 65536).fieldOf("region_size").orElse(DEFAULT.regionSize).forGetter(Spawn::regionSize),
				Codec.floatRange(0.0F, 1.0F).fieldOf("region_chance").orElse(DEFAULT.regionChance).forGetter(Spawn::regionChance),
				Codec.floatRange(0.0F, 1.0F).fieldOf("spawn_chance").orElse(DEFAULT.spawnChance).forGetter(Spawn::spawnChance),
				Codec.doubleRange(0.0, 2048.0).fieldOf("spacing").orElse(DEFAULT.spacing).forGetter(Spawn::spacing),
				Codec.intRange(1, 16).fieldOf("tree_radius").orElse(DEFAULT.treeRadius).forGetter(Spawn::treeRadius)
		).apply(instance, Spawn::new));
	}

	/**
	 * Before it fights: it keeps among the trees out of reach and leads the player astray.
	 *
	 * @param keepMin            the closest it comes to its player while it haunts them
	 * @param keepMax            the farthest it lets them get before it follows
	 * @param noticeDistance     a player who comes this close has found it, and it attacks
	 * @param haunt              after this long haunting a player it attacks anyway
	 * @param bewilderRadius     players this close are led astray: compasses and maps spin
	 * @param bewilderDuration   the effect lasts this long after they leave the radius
	 * @param footstepInterval   about this often a player it haunts hears steps from the wrong side
	 * @param overgrowthInterval about this often a bush grows on the path behind them
	 * @param giveUpDistance     a player this far away is let go
	 */
	public record Haunt(double keepMin, double keepMax, double noticeDistance, int haunt, double bewilderRadius, int bewilderDuration,
			int footstepInterval, int overgrowthInterval, double giveUpDistance) {
		public static final Haunt DEFAULT = new Haunt(14.0, 26.0, 6.0, 2400, 40.0, 200, 160, 50, 64.0);

		public static final Codec<Haunt> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(2.0, 64.0).fieldOf("keep_min").orElse(DEFAULT.keepMin).forGetter(Haunt::keepMin),
				Codec.doubleRange(4.0, 96.0).fieldOf("keep_max").orElse(DEFAULT.keepMax).forGetter(Haunt::keepMax),
				Codec.doubleRange(0.0, 32.0).fieldOf("notice_distance").orElse(DEFAULT.noticeDistance).forGetter(Haunt::noticeDistance),
				Codec.intRange(0, 72000).fieldOf("haunt_time").orElse(DEFAULT.haunt).forGetter(Haunt::haunt),
				Codec.doubleRange(0.0, 128.0).fieldOf("bewilder_radius").orElse(DEFAULT.bewilderRadius).forGetter(Haunt::bewilderRadius),
				Codec.intRange(20, 12000).fieldOf("bewilder_duration").orElse(DEFAULT.bewilderDuration).forGetter(Haunt::bewilderDuration),
				Codec.intRange(10, 6000).fieldOf("footstep_interval").orElse(DEFAULT.footstepInterval).forGetter(Haunt::footstepInterval),
				Codec.intRange(5, 6000).fieldOf("overgrowth_interval").orElse(DEFAULT.overgrowthInterval).forGetter(Haunt::overgrowthInterval),
				Codec.doubleRange(16.0, 256.0).fieldOf("give_up_distance").orElse(DEFAULT.giveUpDistance).forGetter(Haunt::giveUpDistance)
		).apply(instance, Haunt::new));
	}

	/**
	 * The fight.
	 *
	 * @param health           its health
	 * @param armor            its armor points
	 * @param damage           a blow of its claws
	 * @param reach            how far its claws reach, from its centre to the target's
	 * @param hitInterval      ticks between two blows
	 * @param treeRadius       a log this close to it (sideways, from its feet to its head) makes it unhurt by any blow
	 * @param treeRegen        next to a tree it heals one point this often
	 * @param fireDamage       damage it takes while it burns, times the usual; burning, the trees do not shield it
	 * @param rootCooldown     ticks between two root attacks
	 * @param rootWarning      ticks the ground churns before a root bursts out of it
	 * @param rootDamage       damage of one root
	 * @param rootHold         a player struck by a root is held this long (slowness)
	 * @param rootMinRange     it calls the roots on a target at least this far away
	 * @param rootMaxRange     and at most this far
	 * @param wolves           the wolves it calls
	 * @param teleport         its flight as a flock of crows
	 */
	public record Combat(double health, double armor, double damage, double reach, int hitInterval, int treeRadius, int treeRegen,
			float fireDamage, int rootCooldown, int rootWarning, float rootDamage, int rootHold, double rootMinRange, double rootMaxRange,
			Wolves wolves, Teleport teleport) {
		public static final Combat DEFAULT = new Combat(60.0, 4.0, 8.0, 3.2, 30, 2, 40, 2.0F, 160, 20, 6.0F, 40, 4.0, 20.0,
				Wolves.DEFAULT, Teleport.DEFAULT);

		public static final Codec<Combat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(1.0, 1024.0).fieldOf("health").orElse(DEFAULT.health).forGetter(Combat::health),
				Codec.doubleRange(0.0, 30.0).fieldOf("armor").orElse(DEFAULT.armor).forGetter(Combat::armor),
				Codec.doubleRange(0.0, 100.0).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Combat::damage),
				Codec.doubleRange(1.0, 8.0).fieldOf("reach").orElse(DEFAULT.reach).forGetter(Combat::reach),
				Codec.intRange(5, 200).fieldOf("hit_interval").orElse(DEFAULT.hitInterval).forGetter(Combat::hitInterval),
				Codec.intRange(0, 6).fieldOf("tree_radius").orElse(DEFAULT.treeRadius).forGetter(Combat::treeRadius),
				Codec.intRange(0, 2400).fieldOf("tree_regen").orElse(DEFAULT.treeRegen).forGetter(Combat::treeRegen),
				Codec.floatRange(1.0F, 10.0F).fieldOf("fire_damage").orElse(DEFAULT.fireDamage).forGetter(Combat::fireDamage),
				Codec.intRange(20, 2400).fieldOf("root_cooldown").orElse(DEFAULT.rootCooldown).forGetter(Combat::rootCooldown),
				Codec.intRange(5, 100).fieldOf("root_warning").orElse(DEFAULT.rootWarning).forGetter(Combat::rootWarning),
				Codec.floatRange(0.0F, 100.0F).fieldOf("root_damage").orElse(DEFAULT.rootDamage).forGetter(Combat::rootDamage),
				Codec.intRange(0, 400).fieldOf("root_hold").orElse(DEFAULT.rootHold).forGetter(Combat::rootHold),
				Codec.doubleRange(0.0, 32.0).fieldOf("root_min_range").orElse(DEFAULT.rootMinRange).forGetter(Combat::rootMinRange),
				Codec.doubleRange(2.0, 32.0).fieldOf("root_max_range").orElse(DEFAULT.rootMaxRange).forGetter(Combat::rootMaxRange),
				Wolves.CODEC.fieldOf("wolves").orElse(DEFAULT.wolves).forGetter(Combat::wolves),
				Teleport.CODEC.fieldOf("crow_flight").orElse(DEFAULT.teleport).forGetter(Combat::teleport)
		).apply(instance, Combat::new));
	}

	/**
	 * The wolves it calls.
	 *
	 * @param cooldown ticks between two calls
	 * @param count    how many come
	 * @param lifetime they melt back into the forest after this long, or when it dies
	 */
	public record Wolves(int cooldown, int count, int lifetime) {
		public static final Wolves DEFAULT = new Wolves(1200, 2, 1200);

		public static final Codec<Wolves> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(20, 72000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Wolves::cooldown),
				Codec.intRange(0, 8).fieldOf("count").orElse(DEFAULT.count).forGetter(Wolves::count),
				Codec.intRange(20, 72000).fieldOf("lifetime").orElse(DEFAULT.lifetime).forGetter(Wolves::lifetime)
		).apply(instance, Wolves::new));
	}

	/**
	 * Its flight as a flock of crows, from one tree to another.
	 *
	 * @param cooldown ticks between two flights
	 * @param distance it flies to a target this far away
	 * @param health   it also flies off, once a fight, when out in the open its health drops below this share
	 * @param duration ticks it is gone as a flock
	 */
	public record Teleport(int cooldown, double distance, double health, int duration) {
		public static final Teleport DEFAULT = new Teleport(600, 18.0, 0.5, 30);

		public static final Codec<Teleport> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(20, 72000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Teleport::cooldown),
				Codec.doubleRange(4.0, 64.0).fieldOf("distance").orElse(DEFAULT.distance).forGetter(Teleport::distance),
				Codec.doubleRange(0.0, 1.0).fieldOf("health").orElse(DEFAULT.health).forGetter(Teleport::health),
				Codec.intRange(1, 200).fieldOf("duration").orElse(DEFAULT.duration).forGetter(Teleport::duration)
		).apply(instance, Teleport::new));
	}

	/**
	 * Buying it off: an offering (item tag {@code eam:leshy_offerings}) laid on a stump, the top of a log.
	 *
	 * @param count     items of the offering it wants in one stack
	 * @param search    it finds an offering this close to it
	 * @param peaceDays the player who laid it is left alone by every leshy for this many days
	 */
	public record Offering(int count, double search, int peaceDays) {
		public static final Offering DEFAULT = new Offering(3, 24.0, 3);

		public static final Codec<Offering> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(1, 64).fieldOf("count").orElse(DEFAULT.count).forGetter(Offering::count),
				Codec.doubleRange(2.0, 64.0).fieldOf("search").orElse(DEFAULT.search).forGetter(Offering::search),
				Codec.intRange(0, 1000).fieldOf("peace_days").orElse(DEFAULT.peaceDays).forGetter(Offering::peaceDays)
		).apply(instance, Offering::new));
	}
}
