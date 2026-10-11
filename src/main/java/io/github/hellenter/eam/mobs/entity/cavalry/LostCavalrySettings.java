package io.github.hellenter.eam.mobs.entity.cavalry;

import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightSettings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for the heavy Lost cavalrymen and their horses, read from {@code data/eam/mobs/lost_cavalry.json} so
 * datapacks can override them. A missing or invalid field falls back to its default. Times are in ticks, distances in
 * blocks.
 *
 * @param axe   fighting on foot with axe and shield: the skeleton knight's numbers (his slash and heavy strike; his
 *              three-cut combo is not used)
 * @param moves the axe moves of their own: the double cut and the sweep
 * @param body  health, the horse's barding, dropped gear
 * @param speed how fast they ride, walk and run
 * @param lance the couched lance charge
 * @param throwing throwing the lance from the saddle
 */
public record LostCavalrySettings(SkeletonKnightSettings axe, Moves moves, Body body, Speed speed, Lance lance, Throwing throwing) {
	public static final SkeletonKnightSettings AXE_DEFAULT = new SkeletonKnightSettings(14, 2.7, 12.0, 1.0F,
			0.0, 200, 0.75F, 0.2, 0.35,
			160, 1.5F, 0.5, 1.0F / 3.0F, 4.0F,
			0.5, new SkeletonKnightSettings.Dash(220, 4, 0.85));
	public static final LostCavalrySettings DEFAULT = new LostCavalrySettings(AXE_DEFAULT, Moves.DEFAULT, Body.DEFAULT, Speed.DEFAULT,
			Lance.DEFAULT, Throwing.DEFAULT);

	/**
	 * @param doubleChance     chance of the double cut against a target with no shield ready
	 * @param doubleCooldown   ticks before the next double cut
	 * @param doubleMultiplier damage of each of its two cuts, of a plain axe cut
	 * @param sweepChance      chance of the sweep against a target behind a ready shield
	 * @param sweepCooldown    ticks before the next sweep
	 * @param sweepMultiplier  damage of the sweep, of a plain axe cut; it goes round a shield and leaves it up
	 * @param sweepRadius      the sweep hits everyone this close around him
	 */
	public record Moves(double doubleChance, int doubleCooldown, float doubleMultiplier, double sweepChance, int sweepCooldown,
			float sweepMultiplier, double sweepRadius) {
		public static final Moves DEFAULT = new Moves(0.65, 50, 0.75F, 0.3, 140, 1.15F, 3.3);

		public static final Codec<Moves> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(0.0, 1.0).fieldOf("double_chance").orElse(DEFAULT.doubleChance).forGetter(Moves::doubleChance),
				Codec.intRange(0, 2400).fieldOf("double_cooldown").orElse(DEFAULT.doubleCooldown).forGetter(Moves::doubleCooldown),
				Codec.floatRange(0.0F, 10.0F).fieldOf("double_multiplier").orElse(DEFAULT.doubleMultiplier).forGetter(Moves::doubleMultiplier),
				Codec.doubleRange(0.0, 1.0).fieldOf("sweep_chance").orElse(DEFAULT.sweepChance).forGetter(Moves::sweepChance),
				Codec.intRange(0, 2400).fieldOf("sweep_cooldown").orElse(DEFAULT.sweepCooldown).forGetter(Moves::sweepCooldown),
				Codec.floatRange(0.0F, 10.0F).fieldOf("sweep_multiplier").orElse(DEFAULT.sweepMultiplier).forGetter(Moves::sweepMultiplier),
				Codec.doubleRange(1.0, 8.0).fieldOf("sweep_radius").orElse(DEFAULT.sweepRadius).forGetter(Moves::sweepRadius)
		).apply(instance, Moves::new));
	}

	/**
	 * @param riderHealth        the cavalryman's health
	 * @param horseHealth        his horse's health
	 * @param horseArmor         share of weapon and projectile damage the horse's barding takes off
	 * @param gearDropChance     chance for each piece of the cavalryman's gear to drop, battered
	 */
	public record Body(double riderHealth, double horseHealth, float horseArmor, float gearDropChance) {
		public static final Body DEFAULT = new Body(35.0, 20.0, 0.55F, 0.1F);

		public static final Codec<Body> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(1.0, 1024.0).fieldOf("rider_health").orElse(DEFAULT.riderHealth).forGetter(Body::riderHealth),
				Codec.doubleRange(1.0, 1024.0).fieldOf("horse_health").orElse(DEFAULT.horseHealth).forGetter(Body::horseHealth),
				Codec.floatRange(0.0F, 0.95F).fieldOf("horse_armor").orElse(DEFAULT.horseArmor).forGetter(Body::horseArmor),
				Codec.floatRange(0.0F, 1.0F).fieldOf("gear_drop_chance").orElse(DEFAULT.gearDropChance).forGetter(Body::gearDropChance)
		).apply(instance, Body::new));
	}

	/**
	 * Speeds are ground speeds, as multiples of a walking player's or of a skeleton's.
	 *
	 * @param foot           on foot, of a skeleton's walk
	 * @param sprint         how many times faster he runs when he cannot catch the player
	 * @param sprintTicks    how long he runs
	 * @param sprintAfter    he starts running after chasing out of reach this long
	 * @param sprintCooldown ticks after a run before the next one
	 * @param gallop         the horse at full gallop (a charge), of a walking player's speed
	 * @param canter         the horse riding round the player between charges, of a walking player's speed
	 */
	public record Speed(double foot, double sprint, int sprintTicks, int sprintAfter, int sprintCooldown, double gallop, double canter) {
		public static final Speed DEFAULT = new Speed(0.75, 2.0, 60, 60, 160, 2.0, 1.3);

		public static final Codec<Speed> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(0.05, 4.0).fieldOf("foot").orElse(DEFAULT.foot).forGetter(Speed::foot),
				Codec.doubleRange(1.0, 4.0).fieldOf("sprint").orElse(DEFAULT.sprint).forGetter(Speed::sprint),
				Codec.intRange(0, 400).fieldOf("sprint_ticks").orElse(DEFAULT.sprintTicks).forGetter(Speed::sprintTicks),
				Codec.intRange(0, 1200).fieldOf("sprint_after").orElse(DEFAULT.sprintAfter).forGetter(Speed::sprintAfter),
				Codec.intRange(0, 2400).fieldOf("sprint_cooldown").orElse(DEFAULT.sprintCooldown).forGetter(Speed::sprintCooldown),
				Codec.doubleRange(0.1, 6.0).fieldOf("gallop").orElse(DEFAULT.gallop).forGetter(Speed::gallop),
				Codec.doubleRange(0.1, 6.0).fieldOf("canter").orElse(DEFAULT.canter).forGetter(Speed::canter)
		).apply(instance, Speed::new));
	}

	/**
	 * @param damage          damage of a lance strike at the gallop
	 * @param reach           how far the lance reaches, flat from the rider's centre to the target's
	 * @param blockedShare    share of the strike that goes through a shield
	 * @param shieldSeconds   a shield hit by the lance is stunned (unusable) this long
	 * @param cooldown        ticks between two charges of one rider
	 * @param minDistance     he starts a charge only from at least this far
	 * @param maxDistance     and from no further than this
	 * @param maxTicks        a charge that has not struck by then is given up
	 * @param keepMin         between charges he rides round the player at least this far away
	 * @param keepMax         and at most this far
	 * @param followUpDelay   when one of a pair charges, the other follows this many ticks later
	 * @param coverMultiplier with his partner unhorsed, his cooldown and riding distance shrink to this share
	 */
	public record Lance(float damage, double reach, float blockedShare, float shieldSeconds, int cooldown, double minDistance,
			double maxDistance, int maxTicks, double keepMin, double keepMax, int followUpDelay, double coverMultiplier) {
		public static final Lance DEFAULT = new Lance(8.0F, 3.4, 0.25F, 4.0F, 150, 6.0, 26.0, 90, 9.0, 14.0, 16, 0.5);

		public static final Codec<Lance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 100.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Lance::damage),
				Codec.doubleRange(1.0, 8.0).fieldOf("reach").orElse(DEFAULT.reach).forGetter(Lance::reach),
				Codec.floatRange(0.0F, 1.0F).fieldOf("blocked_share").orElse(DEFAULT.blockedShare).forGetter(Lance::blockedShare),
				Codec.floatRange(0.0F, 60.0F).fieldOf("shield_seconds").orElse(DEFAULT.shieldSeconds).forGetter(Lance::shieldSeconds),
				Codec.intRange(0, 2400).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Lance::cooldown),
				Codec.doubleRange(0.0, 64.0).fieldOf("min_distance").orElse(DEFAULT.minDistance).forGetter(Lance::minDistance),
				Codec.doubleRange(1.0, 64.0).fieldOf("max_distance").orElse(DEFAULT.maxDistance).forGetter(Lance::maxDistance),
				Codec.intRange(10, 400).fieldOf("max_ticks").orElse(DEFAULT.maxTicks).forGetter(Lance::maxTicks),
				Codec.doubleRange(2.0, 48.0).fieldOf("keep_min").orElse(DEFAULT.keepMin).forGetter(Lance::keepMin),
				Codec.doubleRange(2.0, 48.0).fieldOf("keep_max").orElse(DEFAULT.keepMax).forGetter(Lance::keepMax),
				Codec.intRange(0, 200).fieldOf("follow_up_delay").orElse(DEFAULT.followUpDelay).forGetter(Lance::followUpDelay),
				Codec.doubleRange(0.05, 1.0).fieldOf("cover_multiplier").orElse(DEFAULT.coverMultiplier).forGetter(Lance::coverMultiplier)
		).apply(instance, Lance::new));
	}

	/**
	 * @param chance   chance, checked once a second, to throw while the charge is cooling down
	 * @param cooldown ticks between throws
	 * @param damage   damage of a thrown lance
	 * @param minRange he throws only at a target at least this far
	 * @param maxRange and no further than this
	 * @param spares   lances he can throw; each throw he takes the next one from the saddle
	 */
	public record Throwing(double chance, int cooldown, float damage, double minRange, double maxRange, int spares) {
		public static final Throwing DEFAULT = new Throwing(0.2, 240, 7.0F, 6.0, 18.0, 2);

		public static final Codec<Throwing> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(0.0, 1.0).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Throwing::chance),
				Codec.intRange(0, 2400).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Throwing::cooldown),
				Codec.floatRange(0.0F, 100.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Throwing::damage),
				Codec.doubleRange(0.0, 64.0).fieldOf("min_range").orElse(DEFAULT.minRange).forGetter(Throwing::minRange),
				Codec.doubleRange(1.0, 64.0).fieldOf("max_range").orElse(DEFAULT.maxRange).forGetter(Throwing::maxRange),
				Codec.intRange(0, 16).fieldOf("spares").orElse(DEFAULT.spares).forGetter(Throwing::spares)
		).apply(instance, Throwing::new));
	}

	public static final Codec<LostCavalrySettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			SkeletonKnightSettings.CODEC.fieldOf("axe").orElse(DEFAULT.axe).forGetter(LostCavalrySettings::axe),
			Moves.CODEC.fieldOf("moves").orElse(DEFAULT.moves).forGetter(LostCavalrySettings::moves),
			Body.CODEC.fieldOf("body").orElse(DEFAULT.body).forGetter(LostCavalrySettings::body),
			Speed.CODEC.fieldOf("speed").orElse(DEFAULT.speed).forGetter(LostCavalrySettings::speed),
			Lance.CODEC.fieldOf("lance").orElse(DEFAULT.lance).forGetter(LostCavalrySettings::lance),
			Throwing.CODEC.fieldOf("throwing").orElse(DEFAULT.throwing).forGetter(LostCavalrySettings::throwing)
	).apply(instance, LostCavalrySettings::new));
}
