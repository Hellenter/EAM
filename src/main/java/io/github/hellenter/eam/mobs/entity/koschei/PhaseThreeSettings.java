package io.github.hellenter.eam.mobs.entity.koschei;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Koschei's third phase, undead and in a fury: his ice shell shatters, his staff breaks and he comes on with two daggers,
 * slow, but faster the longer he can't reach his target; hurt, he does not back off but strikes faster. He darts left,
 * right, in and back, round to the target's back, and vanishes to stand behind or before it. Read with the rest of
 * {@link KoscheiSettings} from {@code data/eam/mobs/koschei.json}, under {@code phase_three}.
 *
 * @param health          his health in this phase
 * @param speedMultiplier his speed is this many times his first-phase speed
 * @param attackPause     least ticks between two of his attacks while he is calm
 * @param chase           how he speeds up when he can't reach the target
 * @param rage            how hits on him make him strike faster
 * @param moves           his darts, his dash round to the target's back and his vanishing
 * @param combo           three quick cuts, his plain attack
 * @param leap            the red blow from a leap
 * @param dash            the dash with both blades out ahead
 * @param chains          the daggers thrown on chains
 * @param orange          the orange blow
 * @param feint           the feint: behind the target, a leap at it and behind it again
 * @param thralls         the draugr he raises
 */
public record PhaseThreeSettings(float health, double speedMultiplier, int attackPause, Chase chase, Rage rage, Moves moves, Combo combo,
		Leap leap, Dash dash, Chains chains, Orange orange, Feint feint, Thralls thralls) {
	public static final PhaseThreeSettings DEFAULT = new PhaseThreeSettings(200.0F, 0.7, 14, Chase.DEFAULT, Rage.DEFAULT, Moves.DEFAULT,
			Combo.DEFAULT, Leap.DEFAULT, Dash.DEFAULT, Chains.DEFAULT, Orange.DEFAULT, Feint.DEFAULT, Thralls.DEFAULT);

	public static final Codec<PhaseThreeSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.floatRange(1.0F, 2048.0F).fieldOf("health").orElse(DEFAULT.health).forGetter(PhaseThreeSettings::health),
			Codec.doubleRange(0.1, 10.0).fieldOf("speed_multiplier").orElse(DEFAULT.speedMultiplier).forGetter(PhaseThreeSettings::speedMultiplier),
			Codec.intRange(0, 200).fieldOf("attack_pause").orElse(DEFAULT.attackPause).forGetter(PhaseThreeSettings::attackPause),
			Chase.CODEC.fieldOf("chase").orElse(DEFAULT.chase).forGetter(PhaseThreeSettings::chase),
			Rage.CODEC.fieldOf("rage").orElse(DEFAULT.rage).forGetter(PhaseThreeSettings::rage),
			Moves.CODEC.fieldOf("moves").orElse(DEFAULT.moves).forGetter(PhaseThreeSettings::moves),
			Combo.CODEC.fieldOf("combo").orElse(DEFAULT.combo).forGetter(PhaseThreeSettings::combo),
			Leap.CODEC.fieldOf("leap").orElse(DEFAULT.leap).forGetter(PhaseThreeSettings::leap),
			Dash.CODEC.fieldOf("dash").orElse(DEFAULT.dash).forGetter(PhaseThreeSettings::dash),
			Chains.CODEC.fieldOf("chains").orElse(DEFAULT.chains).forGetter(PhaseThreeSettings::chains),
			Orange.CODEC.fieldOf("orange").orElse(DEFAULT.orange).forGetter(PhaseThreeSettings::orange),
			Feint.CODEC.fieldOf("feint").orElse(DEFAULT.feint).forGetter(PhaseThreeSettings::feint),
			Thralls.CODEC.fieldOf("thralls").orElse(DEFAULT.thralls).forGetter(PhaseThreeSettings::thralls)
	).apply(instance, PhaseThreeSettings::new));

	/**
	 * @param after         ticks out of reach of the target before he starts to speed up
	 * @param ramp          ticks more until he is at his fastest
	 * @param maxMultiplier his fastest, this many times his speed in this phase
	 */
	public record Chase(int after, int ramp, double maxMultiplier) {
		public static final Chase DEFAULT = new Chase(60, 100, 1.7);

		public static final Codec<Chase> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, 24000).fieldOf("after").orElse(DEFAULT.after).forGetter(Chase::after),
				Codec.intRange(1, 24000).fieldOf("ramp").orElse(DEFAULT.ramp).forGetter(Chase::ramp),
				Codec.doubleRange(1.0, 10.0).fieldOf("max_multiplier").orElse(DEFAULT.maxMultiplier).forGetter(Chase::maxMultiplier)
		).apply(instance, Chase::new));
	}

	/**
	 * @param maxStacks  each hit on him adds one stack of fury, up to this many
	 * @param decay      ticks without a hit on him before one stack goes
	 * @param pauseCut   each stack cuts the pause between his attacks by this share of it
	 * @param minPause   the pause is never shorter than this many ticks
	 * @param speedBonus each stack adds this share to his speed
	 */
	public record Rage(int maxStacks, int decay, float pauseCut, int minPause, double speedBonus) {
		public static final Rage DEFAULT = new Rage(5, 40, 0.18F, 2, 0.04);

		public static final Codec<Rage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, 100).fieldOf("max_stacks").orElse(DEFAULT.maxStacks).forGetter(Rage::maxStacks),
				Codec.intRange(1, 24000).fieldOf("decay").orElse(DEFAULT.decay).forGetter(Rage::decay),
				Codec.floatRange(0.0F, 1.0F).fieldOf("pause_cut").orElse(DEFAULT.pauseCut).forGetter(Rage::pauseCut),
				Codec.intRange(0, 200).fieldOf("min_pause").orElse(DEFAULT.minPause).forGetter(Rage::minPause),
				Codec.doubleRange(0.0, 1.0).fieldOf("speed_bonus").orElse(DEFAULT.speedBonus).forGetter(Rage::speedBonus)
		).apply(instance, Rage::new));
	}

	/**
	 * His darts are short flat dashes to one side, back, or in at a target a few blocks off.
	 *
	 * @param dartChance       chance each tick he waits in reach, or closes in from afar, that he darts, when it is ready
	 * @param dartCooldown     ticks between two darts
	 * @param dartDistance     how far a dart takes him
	 * @param flankChance      chance each time he is about to attack that he dashes round to the target's back first
	 * @param flankCooldown    ticks between two such dashes
	 * @param flankRange       the target is at most this far for it
	 * @param teleportChance   chance each time he is about to attack that he vanishes to stand by the target first
	 * @param teleportCooldown ticks between two such vanishings
	 * @param frontShare       share of them that put him before the target rather than behind it
	 */
	public record Moves(float dartChance, int dartCooldown, double dartDistance, float flankChance, int flankCooldown, double flankRange,
			float teleportChance, int teleportCooldown, float frontShare) {
		public static final Moves DEFAULT = new Moves(0.08F, 14, 2.6, 0.15F, 60, 5.0, 0.06F, 140, 0.4F);

		public static final Codec<Moves> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1.0F).fieldOf("dart_chance").orElse(DEFAULT.dartChance).forGetter(Moves::dartChance),
				Codec.intRange(0, 24000).fieldOf("dart_cooldown").orElse(DEFAULT.dartCooldown).forGetter(Moves::dartCooldown),
				Codec.doubleRange(0.5, 8.0).fieldOf("dart_distance").orElse(DEFAULT.dartDistance).forGetter(Moves::dartDistance),
				Codec.floatRange(0.0F, 1.0F).fieldOf("flank_chance").orElse(DEFAULT.flankChance).forGetter(Moves::flankChance),
				Codec.intRange(0, 24000).fieldOf("flank_cooldown").orElse(DEFAULT.flankCooldown).forGetter(Moves::flankCooldown),
				Codec.doubleRange(0.0, 16.0).fieldOf("flank_range").orElse(DEFAULT.flankRange).forGetter(Moves::flankRange),
				Codec.floatRange(0.0F, 1.0F).fieldOf("teleport_chance").orElse(DEFAULT.teleportChance).forGetter(Moves::teleportChance),
				Codec.intRange(0, 24000).fieldOf("teleport_cooldown").orElse(DEFAULT.teleportCooldown).forGetter(Moves::teleportCooldown),
				Codec.floatRange(0.0F, 1.0F).fieldOf("front_share").orElse(DEFAULT.frontShare).forGetter(Moves::frontShare)
		).apply(instance, Moves::new));
	}

	/**
	 * Three quick cuts. A shield blocks each, but {@code shieldHits} blocked in a row knock it out.
	 *
	 * @param reach        how far the blades reach
	 * @param damage       each cut
	 * @param shieldHits   cuts blocked in a row that knock a shield out
	 * @param shieldSeconds how long it stays knocked out
	 * @param shieldMemory ticks after a blocked cut before the count starts over
	 */
	public record Combo(double reach, float damage, int shieldHits, float shieldSeconds, int shieldMemory) {
		public static final Combo DEFAULT = new Combo(2.6, 6.5F, 3, 3.0F, 60);

		public static final Codec<Combo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(0.5, 8.0).fieldOf("reach").orElse(DEFAULT.reach).forGetter(Combo::reach),
				Codec.floatRange(0.0F, 1000.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Combo::damage),
				Codec.intRange(1, 100).fieldOf("shield_hits").orElse(DEFAULT.shieldHits).forGetter(Combo::shieldHits),
				Codec.floatRange(0.0F, 60.0F).fieldOf("shield_seconds").orElse(DEFAULT.shieldSeconds).forGetter(Combo::shieldSeconds),
				Codec.intRange(1, 24000).fieldOf("shield_memory").orElse(DEFAULT.shieldMemory).forGetter(Combo::shieldMemory)
		).apply(instance, Combo::new));
	}

	/**
	 * The red blow: both blades glow red and he leaps at the target, striking down from over his head as he lands. A shield
	 * raised as he hangs at the top of the leap stops it; one raised earlier or later only takes {@code 1 - shieldShare} off.
	 *
	 * @param damage      the blow
	 * @param shieldShare share of it that goes through a shield not raised in time
	 * @param minRange    least distance to the target
	 * @param maxRange    most distance to the target
	 * @param chance      chance he leaps when it is ready
	 * @param cooldown    ticks before the next
	 * @param blockEarly  the shield may go up this many ticks before the top of the leap
	 * @param blockLate   or this many ticks after it
	 */
	public record Leap(float damage, float shieldShare, double minRange, double maxRange, float chance, int cooldown, int blockEarly, int blockLate) {
		public static final Leap DEFAULT = new Leap(11.0F, 0.6F, 3.0, 7.0, 0.35F, 120, 3, 2);

		public static final Codec<Leap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1000.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Leap::damage),
				Codec.floatRange(0.0F, 1.0F).fieldOf("shield_share").orElse(DEFAULT.shieldShare).forGetter(Leap::shieldShare),
				Codec.doubleRange(0.0, 32.0).fieldOf("min_range").orElse(DEFAULT.minRange).forGetter(Leap::minRange),
				Codec.doubleRange(0.0, 32.0).fieldOf("max_range").orElse(DEFAULT.maxRange).forGetter(Leap::maxRange),
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Leap::chance),
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Leap::cooldown),
				Codec.intRange(0, 20).fieldOf("block_early").orElse(DEFAULT.blockEarly).forGetter(Leap::blockEarly),
				Codec.intRange(0, 20).fieldOf("block_late").orElse(DEFAULT.blockLate).forGetter(Leap::blockLate)
		).apply(instance, Leap::new));
	}

	/**
	 * Both blades out ahead and a dash straight at where the target stood: stepping aside dodges it. Point-blank he drives
	 * them in; a shield takes it but is knocked out, and a quick hit back parries him. His cuts follow at once.
	 *
	 * @param damage        the blow
	 * @param minRange      least distance to the target
	 * @param maxRange      most distance to the target
	 * @param chance        chance he dashes when it is ready
	 * @param cooldown      ticks before the next
	 * @param shieldSeconds how long a shield that takes it stays knocked out
	 * @param parryWindow   ticks after that a hit on him parries him
	 */
	public record Dash(float damage, double minRange, double maxRange, float chance, int cooldown, float shieldSeconds, int parryWindow) {
		public static final Dash DEFAULT = new Dash(8.0F, 4.0, 9.0, 0.35F, 100, 4.0F, 11);

		public static final Codec<Dash> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1000.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Dash::damage),
				Codec.doubleRange(0.0, 32.0).fieldOf("min_range").orElse(DEFAULT.minRange).forGetter(Dash::minRange),
				Codec.doubleRange(0.0, 32.0).fieldOf("max_range").orElse(DEFAULT.maxRange).forGetter(Dash::maxRange),
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Dash::chance),
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Dash::cooldown),
				Codec.floatRange(0.0F, 60.0F).fieldOf("shield_seconds").orElse(DEFAULT.shieldSeconds).forGetter(Dash::shieldSeconds),
				Codec.intRange(0, 100).fieldOf("parry_window").orElse(DEFAULT.parryWindow).forGetter(Dash::parryWindow)
		).apply(instance, Dash::new));
	}

	/**
	 * Both daggers thrown on chains straight at where the target is: stepping aside dodges them. Caught, the target is
	 * dragged to him and his cuts follow.
	 *
	 * @param hookDamage hurt by the hooks as they bite; a shield takes this, but not the pull
	 * @param minRange   least distance to the target
	 * @param range      how far the chains reach
	 * @param speed      how fast the daggers fly, blocks a tick
	 * @param pullSpeed  how fast the target is dragged in, blocks a tick
	 * @param chance     chance he throws when it is ready
	 * @param cooldown   ticks before the next
	 */
	public record Chains(float hookDamage, double minRange, double range, double speed, double pullSpeed, float chance, int cooldown) {
		public static final Chains DEFAULT = new Chains(3.0F, 5.0, 14.0, 1.3, 0.9, 0.3F, 240);

		public static final Codec<Chains> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1000.0F).fieldOf("hook_damage").orElse(DEFAULT.hookDamage).forGetter(Chains::hookDamage),
				Codec.doubleRange(0.0, 32.0).fieldOf("min_range").orElse(DEFAULT.minRange).forGetter(Chains::minRange),
				Codec.doubleRange(1.0, 32.0).fieldOf("range").orElse(DEFAULT.range).forGetter(Chains::range),
				Codec.doubleRange(0.1, 5.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Chains::speed),
				Codec.doubleRange(0.1, 5.0).fieldOf("pull_speed").orElse(DEFAULT.pullSpeed).forGetter(Chains::pullSpeed),
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Chains::chance),
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Chains::cooldown)
		).apply(instance, Chains::new));
	}

	/**
	 * The orange blow: a blade flickering orange, one heavy cut. A raised shield takes it and is knocked out; anything else
	 * takes the cut and bleeds.
	 *
	 * @param damage        the cut
	 * @param bleedingTicks how long the wound bleeds
	 * @param shieldSeconds how long a shield that takes it stays knocked out
	 * @param chance        chance he strikes it rather than the cuts, in reach, when it is ready
	 * @param cooldown      ticks before the next
	 */
	public record Orange(float damage, int bleedingTicks, float shieldSeconds, float chance, int cooldown) {
		public static final Orange DEFAULT = new Orange(7.0F, 100, 5.0F, 0.25F, 160);

		public static final Codec<Orange> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1000.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Orange::damage),
				Codec.intRange(0, 24000).fieldOf("bleeding_ticks").orElse(DEFAULT.bleedingTicks).forGetter(Orange::bleedingTicks),
				Codec.floatRange(0.0F, 60.0F).fieldOf("shield_seconds").orElse(DEFAULT.shieldSeconds).forGetter(Orange::shieldSeconds),
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Orange::chance),
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Orange::cooldown)
		).apply(instance, Orange::new));
	}

	/**
	 * The feint: he stands behind the target, leaps at it, and at the top of the leap vanishes and stands behind it again,
	 * cutting a moment later. A shield blocks the cut as any blow, if the target has turned round to him in time.
	 *
	 * @param damage   the cut
	 * @param maxRange the target is at most this far
	 * @param chance   chance he feints when it is ready
	 * @param cooldown ticks before the next
	 */
	public record Feint(float damage, double maxRange, float chance, int cooldown) {
		public static final Feint DEFAULT = new Feint(9.0F, 12.0, 0.12F, 300);

		public static final Codec<Feint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1000.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Feint::damage),
				Codec.doubleRange(0.0, 64.0).fieldOf("max_range").orElse(DEFAULT.maxRange).forGetter(Feint::maxRange),
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Feint::chance),
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Feint::cooldown)
		).apply(instance, Feint::new));
	}

	/**
	 * Souls fly out of him to where his draugr climb out of the ground.
	 *
	 * @param count       draugr raised at once; fewer when that would pass the cap of the second phase
	 * @param chance      chance each time he is about to attack that he raises them, when it is ready
	 * @param cooldown    ticks before he raises them again
	 * @param minDistance they climb out at least this far from the target
	 * @param maxDistance and at most this far
	 */
	public record Thralls(int count, float chance, int cooldown, double minDistance, double maxDistance) {
		public static final Thralls DEFAULT = new Thralls(3, 0.15F, 600, 10.0, 14.0);

		public static final Codec<Thralls> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, 16).fieldOf("count").orElse(DEFAULT.count).forGetter(Thralls::count),
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Thralls::chance),
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Thralls::cooldown),
				Codec.doubleRange(0.0, 64.0).fieldOf("min_distance").orElse(DEFAULT.minDistance).forGetter(Thralls::minDistance),
				Codec.doubleRange(0.0, 64.0).fieldOf("max_distance").orElse(DEFAULT.maxDistance).forGetter(Thralls::maxDistance)
		).apply(instance, Thralls::new));
	}
}
