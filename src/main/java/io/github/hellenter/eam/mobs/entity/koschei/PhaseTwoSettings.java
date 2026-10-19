package io.github.hellenter.eam.mobs.entity.koschei;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Koschei's second phase, the lord of the undead: he takes up a staff, keeps his distance and raises draugr. Read with the
 * rest of {@link KoscheiSettings} from {@code data/eam/mobs/koschei.json}, under {@code phase_two}.
 *
 * @param health          his health in this phase
 * @param speedMultiplier his speed is this many times his first-phase speed
 * @param attackPause     least ticks between two of his attacks
 * @param kite            how he keeps his distance
 * @param teleport        he vanishes and stands somewhere else, away from the target
 * @param bolt            the blue bolt from the staff, his base attack
 * @param strike          a blow with the staff at one who comes close
 * @param souls           three green souls thrown out over the arena, each raising a draugr where it lands
 * @param trap            he lets the target come right up to him, vanishes and raises draugr round it
 * @param thralls         the draugr he raises
 * @param ball            a ball of lightning that hunts the target and bursts when struck down; rare
 * @param storm           a wide icy blast from his hands that pushes slowly back and burns those without a shield
 * @param iceRavens       three ravens of ice that fly at the target; one of them knocks a shield out
 * @param iceWall         a wall of ice in front of a target that runs at him; rare
 * @param wiles           the turtle, the ice copies and the drain of strength
 */
public record PhaseTwoSettings(float health, double speedMultiplier, int attackPause, Kite kite, Teleport teleport, Bolt bolt, Strike strike,
		Souls souls, Trap trap, Thralls thralls, Ball ball, Storm storm, IceRavens iceRavens, IceWall iceWall, Wiles wiles) {
	public static final PhaseTwoSettings DEFAULT = new PhaseTwoSettings(120.0F, 1.15, 14, Kite.DEFAULT, Teleport.DEFAULT, Bolt.DEFAULT,
			Strike.DEFAULT, Souls.DEFAULT, Trap.DEFAULT, Thralls.DEFAULT, Ball.DEFAULT, Storm.DEFAULT, IceRavens.DEFAULT, IceWall.DEFAULT,
			Wiles.DEFAULT);

	public static final Codec<PhaseTwoSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.floatRange(1.0F, 2048.0F).fieldOf("health").orElse(DEFAULT.health).forGetter(PhaseTwoSettings::health),
			Codec.doubleRange(0.1, 10.0).fieldOf("speed_multiplier").orElse(DEFAULT.speedMultiplier).forGetter(PhaseTwoSettings::speedMultiplier),
			Codec.intRange(0, 200).fieldOf("attack_pause").orElse(DEFAULT.attackPause).forGetter(PhaseTwoSettings::attackPause),
			Kite.CODEC.fieldOf("kite").orElse(DEFAULT.kite).forGetter(PhaseTwoSettings::kite),
			Teleport.CODEC.fieldOf("teleport").orElse(DEFAULT.teleport).forGetter(PhaseTwoSettings::teleport),
			Bolt.CODEC.fieldOf("bolt").orElse(DEFAULT.bolt).forGetter(PhaseTwoSettings::bolt),
			Strike.CODEC.fieldOf("strike").orElse(DEFAULT.strike).forGetter(PhaseTwoSettings::strike),
			Souls.CODEC.fieldOf("souls").orElse(DEFAULT.souls).forGetter(PhaseTwoSettings::souls),
			Trap.CODEC.fieldOf("trap").orElse(DEFAULT.trap).forGetter(PhaseTwoSettings::trap),
			Thralls.CODEC.fieldOf("thralls").orElse(DEFAULT.thralls).forGetter(PhaseTwoSettings::thralls),
			Ball.CODEC.fieldOf("ball_lightning").orElse(DEFAULT.ball).forGetter(PhaseTwoSettings::ball),
			Storm.CODEC.fieldOf("snow_storm").orElse(DEFAULT.storm).forGetter(PhaseTwoSettings::storm),
			IceRavens.CODEC.fieldOf("ice_ravens").orElse(DEFAULT.iceRavens).forGetter(PhaseTwoSettings::iceRavens),
			IceWall.CODEC.fieldOf("ice_wall").orElse(DEFAULT.iceWall).forGetter(PhaseTwoSettings::iceWall),
			Wiles.MAP_CODEC.forGetter(PhaseTwoSettings::wiles)
	).apply(instance, PhaseTwoSettings::new));

	public Turtle turtle() {
		return wiles.turtle();
	}

	public IceCopies iceCopies() {
		return wiles.iceCopies();
	}

	public Drain drain() {
		return wiles.drain();
	}

	/**
	 * The last of his attacks, read from {@code turtle}, {@code ice_copies} and {@code drain} beside the others; kept together
	 * only because a codec takes so many fields at most.
	 */
	public record Wiles(Turtle turtle, IceCopies iceCopies, Drain drain) {
		public static final Wiles DEFAULT = new Wiles(Turtle.DEFAULT, IceCopies.DEFAULT, Drain.DEFAULT);

		public static final MapCodec<Wiles> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Turtle.CODEC.fieldOf("turtle").orElse(DEFAULT.turtle).forGetter(Wiles::turtle),
				IceCopies.CODEC.fieldOf("ice_copies").orElse(DEFAULT.iceCopies).forGetter(Wiles::iceCopies),
				Drain.CODEC.fieldOf("drain").orElse(DEFAULT.drain).forGetter(Wiles::drain)
		).apply(instance, Wiles::new));
	}

	/**
	 * Draugr with shields rise in a row before him, lock their shields like the Lost's shield wall, and he hides behind
	 * them.
	 *
	 * @param chance        chance each time he is about to attack that he raises them
	 * @param cooldown      ticks before he raises them again
	 * @param count         shield bearers raised, fewer when that would pass the cap of draugr
	 * @param minRoom       he raises them only with room under the cap for at least this many
	 * @param duration      ticks the row stands and he keeps behind it; it breaks up sooner when all its bearers fall
	 * @param spacing       flat distance between neighbours in the row, centre to centre
	 * @param frontDistance the row stands this far in front of him, toward the target, or halfway to it when closer
	 * @param reach         a bearer swings at the target this close, lowering his shield for the blow
	 */
	public record Turtle(float chance, int cooldown, int count, int minRoom, int duration, double spacing, double frontDistance,
			double reach) {
		public static final Turtle DEFAULT = new Turtle(0.1F, 600, 3, 2, 300, 1.0, 2.5, 2.6);

		public static final Codec<Turtle> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Turtle::chance),
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Turtle::cooldown),
				Codec.intRange(1, 8).fieldOf("count").orElse(DEFAULT.count).forGetter(Turtle::count),
				Codec.intRange(1, 8).fieldOf("min_room").orElse(DEFAULT.minRoom).forGetter(Turtle::minRoom),
				Codec.intRange(1, 24000).fieldOf("duration").orElse(DEFAULT.duration).forGetter(Turtle::duration),
				Codec.doubleRange(0.5, 3.0).fieldOf("spacing").orElse(DEFAULT.spacing).forGetter(Turtle::spacing),
				Codec.doubleRange(0.5, 16.0).fieldOf("front_distance").orElse(DEFAULT.frontDistance).forGetter(Turtle::frontDistance),
				Codec.doubleRange(0.5, 8.0).fieldOf("reach").orElse(DEFAULT.reach).forGetter(Turtle::reach)
		).apply(instance, Turtle::new));
	}

	/**
	 * Copies of him in ice stand up at his sides; they keep their distance and shoot the bolt only, and shatter after a
	 * while.
	 *
	 * @param chance       chance each time he is about to attack that he makes them, while none of his are about
	 * @param cooldown     ticks before he makes them again, from when the last of them is gone
	 * @param count        copies made at once
	 * @param health       a copy's health
	 * @param lifetime     ticks before a copy shatters on its own
	 * @param boltCooldown ticks between two bolts of a copy
	 * @param distance     they stand up this far to his sides
	 */
	public record IceCopies(float chance, int cooldown, int count, float health, int lifetime, int boltCooldown, double distance) {
		public static final IceCopies DEFAULT = new IceCopies(0.1F, 600, 2, 8.0F, 300, 45, 3.0);

		public static final Codec<IceCopies> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").orElse(DEFAULT.chance).forGetter(IceCopies::chance),
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(IceCopies::cooldown),
				Codec.intRange(1, 8).fieldOf("count").orElse(DEFAULT.count).forGetter(IceCopies::count),
				Codec.floatRange(1.0F, 1024.0F).fieldOf("health").orElse(DEFAULT.health).forGetter(IceCopies::health),
				Codec.intRange(1, 24000).fieldOf("lifetime").orElse(DEFAULT.lifetime).forGetter(IceCopies::lifetime),
				Codec.intRange(1, 24000).fieldOf("bolt_cooldown").orElse(DEFAULT.boltCooldown).forGetter(IceCopies::boltCooldown),
				Codec.doubleRange(1.0, 16.0).fieldOf("distance").orElse(DEFAULT.distance).forGetter(IceCopies::distance)
		).apply(instance, IceCopies::new));
	}

	/**
	 * He rises into the air and draws the strength out of one of his draugr to heal himself; nothing hurts him meanwhile.
	 * It stops when the draugr falls.
	 *
	 * @param below           only below this share of his health
	 * @param uses            at most this many times in the phase
	 * @param chance          chance each time he is about to attack that he does it, while he may
	 * @param cooldown        ticks before he does it again
	 * @param range           he drains a draugr at most this far from him, in sight
	 * @param damagePerSecond the draugr loses this much each second
	 * @param healStart       he heals this much in the first second
	 * @param healStep        and this much less each second after
	 * @param healMin         but never less than this a second
	 * @param height          he hovers this high over the ground meanwhile
	 */
	public record Drain(float below, int uses, float chance, int cooldown, double range, float damagePerSecond, float healStart, float healStep,
			float healMin, double height) {
		public static final Drain DEFAULT = new Drain(0.7F, 2, 0.3F, 800, 16.0, 2.0F, 10.0F, 1.0F, 7.0F, 3.0);

		public static final Codec<Drain> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1.0F).fieldOf("below").orElse(DEFAULT.below).forGetter(Drain::below),
				Codec.intRange(0, 64).fieldOf("uses").orElse(DEFAULT.uses).forGetter(Drain::uses),
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Drain::chance),
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Drain::cooldown),
				Codec.doubleRange(1.0, 64.0).fieldOf("range").orElse(DEFAULT.range).forGetter(Drain::range),
				Codec.floatRange(0.0F, 1000.0F).fieldOf("damage_per_second").orElse(DEFAULT.damagePerSecond).forGetter(Drain::damagePerSecond),
				Codec.floatRange(0.0F, 1000.0F).fieldOf("heal_start").orElse(DEFAULT.healStart).forGetter(Drain::healStart),
				Codec.floatRange(0.0F, 1000.0F).fieldOf("heal_step").orElse(DEFAULT.healStep).forGetter(Drain::healStep),
				Codec.floatRange(0.0F, 1000.0F).fieldOf("heal_min").orElse(DEFAULT.healMin).forGetter(Drain::healMin),
				Codec.doubleRange(0.0, 8.0).fieldOf("height").orElse(DEFAULT.height).forGetter(Drain::height)
		).apply(instance, Drain::new));
	}

	/**
	 * As Baba Yaga on foot: he runs from a target closer than {@code fleeDistance}, steps back from one closer than
	 * {@code minDistance}, walks up to one further than {@code maxDistance} or out of sight, and stands still between.
	 *
	 * @param fleeSpeed speed multiplier while he runs
	 * @param hopChance chance each tick a target inside {@code minDistance} makes him hop aside or back
	 */
	public record Kite(double minDistance, double maxDistance, double fleeDistance, double fleeSpeed, float hopChance) {
		public static final Kite DEFAULT = new Kite(6.0, 13.0, 3.5, 1.3, 0.08F);

		public static final Codec<Kite> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(0.0, 64.0).fieldOf("min_distance").orElse(DEFAULT.minDistance).forGetter(Kite::minDistance),
				Codec.doubleRange(0.0, 64.0).fieldOf("max_distance").orElse(DEFAULT.maxDistance).forGetter(Kite::maxDistance),
				Codec.doubleRange(0.0, 64.0).fieldOf("flee_distance").orElse(DEFAULT.fleeDistance).forGetter(Kite::fleeDistance),
				Codec.doubleRange(0.1, 4.0).fieldOf("flee_speed").orElse(DEFAULT.fleeSpeed).forGetter(Kite::fleeSpeed),
				Codec.floatRange(0.0F, 1.0F).fieldOf("hop_chance").orElse(DEFAULT.hopChance).forGetter(Kite::hopChance)
		).apply(instance, Kite::new));
	}

	/**
	 * @param cooldown ticks between two teleports
	 * @param chance   chance each tick he teleports while it is ready
	 * @param distance how far from the target he stands after it
	 */
	public record Teleport(int cooldown, float chance, double distance) {
		public static final Teleport DEFAULT = new Teleport(140, 0.03F, 9.0);

		public static final Codec<Teleport> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Teleport::cooldown),
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Teleport::chance),
				Codec.doubleRange(2.0, 32.0).fieldOf("distance").orElse(DEFAULT.distance).forGetter(Teleport::distance)
		).apply(instance, Teleport::new));
	}

	/**
	 * @param damage   pure damage, through armor; a shield turned toward it stops it
	 * @param speed    blocks per tick
	 * @param cooldown ticks before he shoots again
	 * @param maxRange he shoots only at a target at most this far away
	 */
	public record Bolt(float damage, double speed, int cooldown, double maxRange) {
		public static final Bolt DEFAULT = new Bolt(4.0F, 0.45, 30, 24.0);

		public static final Codec<Bolt> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1000.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Bolt::damage),
				Codec.doubleRange(0.05, 4.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Bolt::speed),
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Bolt::cooldown),
				Codec.doubleRange(0.0, 64.0).fieldOf("max_range").orElse(DEFAULT.maxRange).forGetter(Bolt::maxRange)
		).apply(instance, Bolt::new));
	}

	/**
	 * @param damage   damage of the blow
	 * @param push     it throws the target this many blocks back
	 * @param reach    he swings only at a target this close
	 * @param cooldown ticks before he swings again
	 */
	public record Strike(float damage, double push, double reach, int cooldown) {
		public static final Strike DEFAULT = new Strike(7.0F, 1.5, 2.8, 30);

		public static final Codec<Strike> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1000.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Strike::damage),
				Codec.doubleRange(0.0, 32.0).fieldOf("push").orElse(DEFAULT.push).forGetter(Strike::push),
				Codec.doubleRange(0.5, 8.0).fieldOf("reach").orElse(DEFAULT.reach).forGetter(Strike::reach),
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Strike::cooldown)
		).apply(instance, Strike::new));
	}

	/**
	 * @param count    souls thrown at once, each raising one draugr; fewer when that would pass the cap
	 * @param chance   chance each time he is about to attack that he throws them, while there is room under the cap
	 * @param cooldown ticks before he throws them again
	 * @param spread   they land this far from him at most
	 */
	public record Souls(int count, float chance, int cooldown, double spread) {
		public static final Souls DEFAULT = new Souls(3, 0.2F, 260, 9.0);

		public static final Codec<Souls> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, 16).fieldOf("count").orElse(DEFAULT.count).forGetter(Souls::count),
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Souls::chance),
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Souls::cooldown),
				Codec.doubleRange(2.0, 32.0).fieldOf("spread").orElse(DEFAULT.spread).forGetter(Souls::spread)
		).apply(instance, Souls::new));
	}

	/**
	 * @param chance       chance each time he is about to attack that he lays the trap: stops backing off and waits
	 * @param cooldown     ticks before he lays it again
	 * @param waitTicks    he waits this long at most for the target to come close
	 * @param triggerRange the target this close springs it: he lets it stand by him (and hit him) through
	 *                     {@link KoscheiAction#TRAP}, then vanishes
	 * @param count        draugr raised round the target, fewer when that would pass the cap
	 * @param radius       they rise this far from the target at most
	 */
	public record Trap(float chance, int cooldown, int waitTicks, double triggerRange, int count, double radius) {
		public static final Trap DEFAULT = new Trap(0.12F, 420, 120, 2.2, 3, 3.5);

		public static final Codec<Trap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Trap::chance),
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Trap::cooldown),
				Codec.intRange(1, 24000).fieldOf("wait_ticks").orElse(DEFAULT.waitTicks).forGetter(Trap::waitTicks),
				Codec.doubleRange(0.5, 16.0).fieldOf("trigger_range").orElse(DEFAULT.triggerRange).forGetter(Trap::triggerRange),
				Codec.intRange(0, 16).fieldOf("count").orElse(DEFAULT.count).forGetter(Trap::count),
				Codec.doubleRange(1.0, 16.0).fieldOf("radius").orElse(DEFAULT.radius).forGetter(Trap::radius)
		).apply(instance, Trap::new));
	}

	/**
	 * @param cap              he keeps at most this many draugr up at once
	 * @param speedMultiplier  his draugr are this many times as fast as a draugr of the ice caves
	 * @param healthMultiplier and have this share of a draugr's health
	 */
	public record Thralls(int cap, double speedMultiplier, double healthMultiplier) {
		public static final Thralls DEFAULT = new Thralls(5, 1.4, 0.5);

		public static final Codec<Thralls> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, 64).fieldOf("cap").orElse(DEFAULT.cap).forGetter(Thralls::cap),
				Codec.doubleRange(0.1, 10.0).fieldOf("speed_multiplier").orElse(DEFAULT.speedMultiplier).forGetter(Thralls::speedMultiplier),
				Codec.doubleRange(0.05, 10.0).fieldOf("health_multiplier").orElse(DEFAULT.healthMultiplier).forGetter(Thralls::healthMultiplier)
		).apply(instance, Thralls::new));
	}

	/**
	 * @param chance           chance each time he is about to attack that he calls it, while none of his is about
	 * @param cooldown         ticks before he calls another
	 * @param unstableAfter    ticks after which it flickers and a hit or an arrow bursts it; before that nothing touches it
	 * @param lifetime         ticks before it fades out on its own
	 * @param speed            blocks per tick it follows the target at
	 * @param radius           its field reaches this far
	 * @param damagePerSecond  pure damage of the field to one inside it
	 * @param burstDamage      pure damage of its burst
	 * @param burstRadius      the burst reaches this far
	 * @param dartChance       chance each tick it darts off aside before it takes up the chase again
	 * @param dartDistance     how far it darts
	 */
	public record Ball(float chance, int cooldown, int unstableAfter, int lifetime, double speed, double radius, float damagePerSecond,
			float burstDamage, double burstRadius, float dartChance, double dartDistance) {
		public static final Ball DEFAULT = new Ball(0.04F, 600, 140, 400, 0.13, 2.0, 1.0F, 5.0F, 2.5, 0.02F, 2.5);

		public static final Codec<Ball> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Ball::chance),
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Ball::cooldown),
				Codec.intRange(0, 24000).fieldOf("unstable_after").orElse(DEFAULT.unstableAfter).forGetter(Ball::unstableAfter),
				Codec.intRange(1, 24000).fieldOf("lifetime").orElse(DEFAULT.lifetime).forGetter(Ball::lifetime),
				Codec.doubleRange(0.0, 2.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Ball::speed),
				Codec.doubleRange(0.0, 16.0).fieldOf("radius").orElse(DEFAULT.radius).forGetter(Ball::radius),
				Codec.floatRange(0.0F, 1000.0F).fieldOf("damage_per_second").orElse(DEFAULT.damagePerSecond).forGetter(Ball::damagePerSecond),
				Codec.floatRange(0.0F, 1000.0F).fieldOf("burst_damage").orElse(DEFAULT.burstDamage).forGetter(Ball::burstDamage),
				Codec.doubleRange(0.0, 16.0).fieldOf("burst_radius").orElse(DEFAULT.burstRadius).forGetter(Ball::burstRadius),
				Codec.floatRange(0.0F, 1.0F).fieldOf("dart_chance").orElse(DEFAULT.dartChance).forGetter(Ball::dartChance),
				Codec.doubleRange(0.0, 16.0).fieldOf("dart_distance").orElse(DEFAULT.dartDistance).forGetter(Ball::dartDistance)
		).apply(instance, Ball::new));
	}

	/**
	 * @param chance          chance each time he is about to attack that he looses it at a target in its reach
	 * @param cooldown        ticks before he looses it again
	 * @param range           it reaches this far in front of him
	 * @param angle           and this many degrees to either side
	 * @param pushPerSecond   it pushes those in it back this many blocks a second
	 * @param damagePerSecond pure damage to one in it who does not hold a shield up to it
	 */
	public record Storm(float chance, int cooldown, double range, float angle, double pushPerSecond, float damagePerSecond) {
		public static final Storm DEFAULT = new Storm(0.15F, 240, 9.0, 35.0F, 0.33, 1.0F);

		public static final Codec<Storm> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Storm::chance),
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Storm::cooldown),
				Codec.doubleRange(1.0, 32.0).fieldOf("range").orElse(DEFAULT.range).forGetter(Storm::range),
				Codec.floatRange(1.0F, 180.0F).fieldOf("angle").orElse(DEFAULT.angle).forGetter(Storm::angle),
				Codec.doubleRange(0.0, 10.0).fieldOf("push_per_second").orElse(DEFAULT.pushPerSecond).forGetter(Storm::pushPerSecond),
				Codec.floatRange(0.0F, 1000.0F).fieldOf("damage_per_second").orElse(DEFAULT.damagePerSecond).forGetter(Storm::damagePerSecond)
		).apply(instance, Storm::new));
	}

	/**
	 * @param chance          chance each time he is about to attack that he looses them
	 * @param cooldown        ticks before he looses them again
	 * @param count           ravens loosed at once; the middle one knocks a shield out
	 * @param health          health of each; an arrow or a blow brings it down
	 * @param damage          damage of each that strikes
	 * @param slownessSeconds each that strikes adds a level of slowness for this long
	 * @param speed           blocks per tick they fly
	 * @param turn            how sharply they turn after the target, 0 to 1
	 * @param lifetime        ticks before one that has not struck shatters
	 * @param shieldSeconds   the shield knocked out is down this long
	 * @param maxRange        he looses them only at a target at most this far away
	 */
	public record IceRavens(float chance, int cooldown, int count, float health, float damage, float slownessSeconds, double speed, double turn,
			int lifetime, float shieldSeconds, double maxRange) {
		public static final IceRavens DEFAULT = new IceRavens(0.12F, 300, 3, 2.0F, 2.0F, 4.0F, 0.55, 0.12, 160, 5.0F, 20.0);

		public static final Codec<IceRavens> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").orElse(DEFAULT.chance).forGetter(IceRavens::chance),
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(IceRavens::cooldown),
				Codec.intRange(1, 16).fieldOf("count").orElse(DEFAULT.count).forGetter(IceRavens::count),
				Codec.floatRange(0.5F, 1000.0F).fieldOf("health").orElse(DEFAULT.health).forGetter(IceRavens::health),
				Codec.floatRange(0.0F, 1000.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(IceRavens::damage),
				Codec.floatRange(0.0F, 600.0F).fieldOf("slowness_seconds").orElse(DEFAULT.slownessSeconds).forGetter(IceRavens::slownessSeconds),
				Codec.doubleRange(0.05, 4.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(IceRavens::speed),
				Codec.doubleRange(0.0, 1.0).fieldOf("turn").orElse(DEFAULT.turn).forGetter(IceRavens::turn),
				Codec.intRange(1, 24000).fieldOf("lifetime").orElse(DEFAULT.lifetime).forGetter(IceRavens::lifetime),
				Codec.floatRange(0.0F, 600.0F).fieldOf("shield_seconds").orElse(DEFAULT.shieldSeconds).forGetter(IceRavens::shieldSeconds),
				Codec.doubleRange(0.0, 64.0).fieldOf("max_range").orElse(DEFAULT.maxRange).forGetter(IceRavens::maxRange)
		).apply(instance, IceRavens::new));
	}

	/**
	 * @param chance       chance each time he is about to attack that he raises it before a target running at him
	 * @param cooldown     ticks before he raises another
	 * @param rushSpeed    the target counts as running at him when it closes in at least this many blocks a tick
	 * @param minDistance  only at a target at least this far
	 * @param maxDistance  and at most this far
	 * @param ahead        it stands this many blocks in front of the target
	 * @param size         blocks wide and high
	 * @param lifetime     ticks before it melts
	 */
	public record IceWall(float chance, int cooldown, double rushSpeed, double minDistance, double maxDistance, double ahead, int size,
			int lifetime) {
		public static final IceWall DEFAULT = new IceWall(0.25F, 400, 0.15, 4.0, 10.0, 2.0, 3, 200);

		public static final Codec<IceWall> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").orElse(DEFAULT.chance).forGetter(IceWall::chance),
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(IceWall::cooldown),
				Codec.doubleRange(0.0, 2.0).fieldOf("rush_speed").orElse(DEFAULT.rushSpeed).forGetter(IceWall::rushSpeed),
				Codec.doubleRange(0.0, 32.0).fieldOf("min_distance").orElse(DEFAULT.minDistance).forGetter(IceWall::minDistance),
				Codec.doubleRange(0.0, 32.0).fieldOf("max_distance").orElse(DEFAULT.maxDistance).forGetter(IceWall::maxDistance),
				Codec.doubleRange(0.5, 16.0).fieldOf("ahead").orElse(DEFAULT.ahead).forGetter(IceWall::ahead),
				Codec.intRange(1, 7).fieldOf("size").orElse(DEFAULT.size).forGetter(IceWall::size),
				Codec.intRange(1, 24000).fieldOf("lifetime").orElse(DEFAULT.lifetime).forGetter(IceWall::lifetime)
		).apply(instance, IceWall::new));
	}
}
