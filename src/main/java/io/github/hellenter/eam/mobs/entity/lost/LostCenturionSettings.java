package io.github.hellenter.eam.mobs.entity.lost;

import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightSettings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The Lost centurion's numbers, the {@code centurion} section of {@code data/eam/mobs/lost.json}.
 *
 * @param chance     chance that a squad of the Lost has a centurion with it
 * @param health     his health
 * @param extraArmor armor points on top of his ancient metal plate
 * @param spearReach how far his long spear reaches, flat from his centre to the target's
 * @param dropChance chance for each piece of his gear to drop, battered
 * @param fight      his spear and shield fighting
 * @param horn       when his horn calls up more of the Lost, and which
 * @param defend     how the Lost rush to him when he is hit
 */
public record LostCenturionSettings(double chance, double health, double extraArmor, double spearReach, float dropChance,
		SkeletonKnightSettings fight, Horn horn, Defend defend) {
	public static final SkeletonKnightSettings FIGHT_DEFAULT = new SkeletonKnightSettings(16, 2.7, 14.0, 0.8F,
			0.0, 400, 0.6F, 0.0, 0.0,
			400, 1.5F, 0.5, 0.5F, 3.0F,
			0.5, new SkeletonKnightSettings.Dash(240, 4, 0.85));
	public static final LostCenturionSettings DEFAULT = new LostCenturionSettings(0.25, 45.0, 4.0, 3.9, 0.08F, FIGHT_DEFAULT,
			Horn.DEFAULT, Defend.DEFAULT);

	/**
	 * The horn. The first call comes once he has had a player in sight for {@code firstDelay} ticks; the second
	 * {@code secondCooldown} ticks after it, every later one {@code laterCooldown} ticks after the one before. The
	 * first call always brings a pair of heavy cavalrymen, the second brings them with {@code secondCavalryChance} and
	 * otherwise a squad, every later one with {@code laterCavalryChance}.
	 *
	 * @param firstDelay          ticks with a player in sight before the first call
	 * @param secondCooldown      ticks from the first call to the second
	 * @param laterCooldown       ticks between later calls
	 * @param secondCavalryChance chance the second call brings cavalry rather than a squad
	 * @param laterCavalryChance  the same for every later call
	 * @param range               he calls only with his target player this close
	 * @param minDistance         the reinforcements come this far from the player at least, out of the player's sight
	 * @param maxDistance         and this far at most
	 * @param maxNearby           with this many of the Lost (cavalry counted) around already, the horn calls no one
	 */
	public record Horn(int firstDelay, int secondCooldown, int laterCooldown, double secondCavalryChance, double laterCavalryChance,
			double range, double minDistance, double maxDistance, int maxNearby) {
		public static final Horn DEFAULT = new Horn(30, 600, 800, 0.5, 0.3, 40.0, 15.0, 25.0, 24);

		public static final Codec<Horn> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, 2400).fieldOf("first_delay").orElse(DEFAULT.firstDelay).forGetter(Horn::firstDelay),
				Codec.intRange(1, 24000).fieldOf("second_cooldown").orElse(DEFAULT.secondCooldown).forGetter(Horn::secondCooldown),
				Codec.intRange(1, 24000).fieldOf("later_cooldown").orElse(DEFAULT.laterCooldown).forGetter(Horn::laterCooldown),
				Codec.doubleRange(0.0, 1.0).fieldOf("second_cavalry_chance").orElse(DEFAULT.secondCavalryChance)
						.forGetter(Horn::secondCavalryChance),
				Codec.doubleRange(0.0, 1.0).fieldOf("later_cavalry_chance").orElse(DEFAULT.laterCavalryChance)
						.forGetter(Horn::laterCavalryChance),
				Codec.doubleRange(1.0, 128.0).fieldOf("range").orElse(DEFAULT.range).forGetter(Horn::range),
				Codec.doubleRange(0.0, 64.0).fieldOf("min_distance").orElse(DEFAULT.minDistance).forGetter(Horn::minDistance),
				Codec.doubleRange(1.0, 64.0).fieldOf("max_distance").orElse(DEFAULT.maxDistance).forGetter(Horn::maxDistance),
				Codec.intRange(0, 256).fieldOf("max_nearby").orElse(DEFAULT.maxNearby).forGetter(Horn::maxNearby)
		).apply(instance, Horn::new));
	}

	/**
	 * When he is hit, the Lost already fighting his attacker leave their places and go for it.
	 *
	 * @param range how far from him they hear it
	 * @param ticks how long they keep at the attacker before going back to their places
	 * @param speed how much faster than their walk they hurry to him
	 */
	public record Defend(double range, int ticks, double speed) {
		public static final Defend DEFAULT = new Defend(32.0, 200, 1.25);

		public static final Codec<Defend> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(0.0, 128.0).fieldOf("range").orElse(DEFAULT.range).forGetter(Defend::range),
				Codec.intRange(0, 2400).fieldOf("ticks").orElse(DEFAULT.ticks).forGetter(Defend::ticks),
				Codec.doubleRange(0.5, 3.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Defend::speed)
		).apply(instance, Defend::new));
	}

	public static final Codec<LostCenturionSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.doubleRange(0.0, 1.0).fieldOf("chance").orElse(DEFAULT.chance).forGetter(LostCenturionSettings::chance),
			Codec.doubleRange(1.0, 1024.0).fieldOf("health").orElse(DEFAULT.health).forGetter(LostCenturionSettings::health),
			Codec.doubleRange(0.0, 30.0).fieldOf("extra_armor").orElse(DEFAULT.extraArmor).forGetter(LostCenturionSettings::extraArmor),
			Codec.doubleRange(1.0, 6.0).fieldOf("spear_reach").orElse(DEFAULT.spearReach).forGetter(LostCenturionSettings::spearReach),
			Codec.floatRange(0.0F, 1.0F).fieldOf("drop_chance").orElse(DEFAULT.dropChance).forGetter(LostCenturionSettings::dropChance),
			SkeletonKnightSettings.CODEC.fieldOf("fight").orElse(DEFAULT.fight).forGetter(LostCenturionSettings::fight),
			Horn.CODEC.fieldOf("horn").orElse(DEFAULT.horn).forGetter(LostCenturionSettings::horn),
			Defend.CODEC.fieldOf("defend").orElse(DEFAULT.defend).forGetter(LostCenturionSettings::defend)
	).apply(instance, LostCenturionSettings::new));
}
