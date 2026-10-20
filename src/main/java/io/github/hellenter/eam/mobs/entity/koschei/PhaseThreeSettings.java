package io.github.hellenter.eam.mobs.entity.koschei;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Koschei's third phase, undead and in a fury: his ice shell shatters, his staff breaks and he comes on with two daggers,
 * slow, but faster the longer he can't reach his target; hurt, he does not back off but strikes faster. Read with the rest
 * of {@link KoscheiSettings} from {@code data/eam/mobs/koschei.json}, under {@code phase_three}.
 *
 * @param health          his health in this phase
 * @param speedMultiplier his speed is this many times his first-phase speed
 * @param attackPause     least ticks between two of his attacks while he is calm
 * @param chase           how he speeds up when he can't reach the target
 * @param rage            how hits on him make him strike faster
 * @param daggers         his attacks with the daggers
 * @param thralls         the draugr he raises
 */
public record PhaseThreeSettings(float health, double speedMultiplier, int attackPause, Chase chase, Rage rage, Daggers daggers, Thralls thralls) {
	public static final PhaseThreeSettings DEFAULT = new PhaseThreeSettings(200.0F, 0.7, 14, Chase.DEFAULT, Rage.DEFAULT, Daggers.DEFAULT,
			Thralls.DEFAULT);

	public static final Codec<PhaseThreeSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.floatRange(1.0F, 2048.0F).fieldOf("health").orElse(DEFAULT.health).forGetter(PhaseThreeSettings::health),
			Codec.doubleRange(0.1, 10.0).fieldOf("speed_multiplier").orElse(DEFAULT.speedMultiplier).forGetter(PhaseThreeSettings::speedMultiplier),
			Codec.intRange(0, 200).fieldOf("attack_pause").orElse(DEFAULT.attackPause).forGetter(PhaseThreeSettings::attackPause),
			Chase.CODEC.fieldOf("chase").orElse(DEFAULT.chase).forGetter(PhaseThreeSettings::chase),
			Rage.CODEC.fieldOf("rage").orElse(DEFAULT.rage).forGetter(PhaseThreeSettings::rage),
			Daggers.CODEC.fieldOf("daggers").orElse(DEFAULT.daggers).forGetter(PhaseThreeSettings::daggers),
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
	 * Placeholder attacks until his own are worked out: a quick stab, a flurry of three cuts, and a dash with both blades
	 * at one a few blocks off. Fixed timings; a raised shield blocks them as any blow.
	 *
	 * @param reach          how far the blades reach
	 * @param stabDamage     the stab
	 * @param flurryDamage   each of the three cuts of the flurry
	 * @param flurryChance   chance he cuts the flurry rather than stabs, when it is ready
	 * @param flurryCooldown ticks before the next flurry
	 * @param dashDamage     the dash
	 * @param dashMin        least distance to the target for the dash
	 * @param dashMax        most distance to the target for the dash
	 * @param dashCooldown   ticks before the next dash
	 */
	public record Daggers(double reach, float stabDamage, float flurryDamage, float flurryChance, int flurryCooldown, float dashDamage,
			double dashMin, double dashMax, int dashCooldown) {
		public static final Daggers DEFAULT = new Daggers(2.6, 6.0F, 3.5F, 0.35F, 40, 9.0F, 4.0, 8.0, 100);

		public static final Codec<Daggers> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(0.5, 8.0).fieldOf("reach").orElse(DEFAULT.reach).forGetter(Daggers::reach),
				Codec.floatRange(0.0F, 1000.0F).fieldOf("stab_damage").orElse(DEFAULT.stabDamage).forGetter(Daggers::stabDamage),
				Codec.floatRange(0.0F, 1000.0F).fieldOf("flurry_damage").orElse(DEFAULT.flurryDamage).forGetter(Daggers::flurryDamage),
				Codec.floatRange(0.0F, 1.0F).fieldOf("flurry_chance").orElse(DEFAULT.flurryChance).forGetter(Daggers::flurryChance),
				Codec.intRange(0, 24000).fieldOf("flurry_cooldown").orElse(DEFAULT.flurryCooldown).forGetter(Daggers::flurryCooldown),
				Codec.floatRange(0.0F, 1000.0F).fieldOf("dash_damage").orElse(DEFAULT.dashDamage).forGetter(Daggers::dashDamage),
				Codec.doubleRange(0.0, 32.0).fieldOf("dash_min").orElse(DEFAULT.dashMin).forGetter(Daggers::dashMin),
				Codec.doubleRange(0.0, 32.0).fieldOf("dash_max").orElse(DEFAULT.dashMax).forGetter(Daggers::dashMax),
				Codec.intRange(0, 24000).fieldOf("dash_cooldown").orElse(DEFAULT.dashCooldown).forGetter(Daggers::dashCooldown)
		).apply(instance, Daggers::new));
	}

	/**
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
