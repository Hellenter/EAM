package io.github.hellenter.eam.mobs.entity.draugr;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * Balance numbers for the draugr, read from {@code data/eam/mobs/draugr.json} so datapacks can override them. A missing
 * or invalid field falls back to its default. How many lie in a pack and how many of them wait is in the worldgen
 * feature {@code eam:underworld_draugr_pack}.
 *
 * @param health          health
 * @param speed           movement speed attribute (a zombie's is 0.23)
 * @param fireMultiplier  fire and lava hurt him this many times as much
 * @param waitRadius      a player this close to a waiting draugr makes him rise and call the others
 * @param waves           the waves of waking that call sends out, each so long after it and reaching so far
 * @param wakeStagger     woken by a wave, each draugr rises after a random delay up to this many ticks
 * @param riseTicks       how long climbing out of the floor takes
 * @param pieceWeights    weights of wearing 1, 2, 3 and 4 armor pieces
 * @param ownPieceChance  chance a piece comes from the draugr's own faction rather than another
 * @param beltChance      chance of a belt on top of the armor
 * @param weaponWeights   weights of an axe, a sword and a spear
 * @param weaponDropChance chance his weapon drops
 * @param climbHeight     he climbs only toward a target at most this many blocks above him, and never higher than it
 * @param climbReach      and only toward a target at most this many blocks across
 * @param shieldDisableMultiplier an axe that would disable a player's shield for N seconds knocks his down for N times
 *                        this
 */
public record DraugrSettings(double health, double speed, float fireMultiplier, double waitRadius, List<Wave> waves, int wakeStagger,
		int riseTicks, List<Double> pieceWeights, double ownPieceChance, double beltChance, List<Double> weaponWeights,
		float weaponDropChance, double climbHeight, double climbReach, float shieldDisableMultiplier) {
	/**
	 * @param delay  ticks after the waiting draugr rose
	 * @param radius sleepers this close to where he lay wake
	 */
	public record Wave(int delay, double radius) {
		public static final Codec<Wave> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, 2400).fieldOf("delay_ticks").forGetter(Wave::delay),
				Codec.doubleRange(0.0, 128.0).fieldOf("radius").forGetter(Wave::radius)
		).apply(instance, Wave::new));
	}

	public static final DraugrSettings DEFAULT = new DraugrSettings(22.0, 0.255, 2.0F, 10.0,
			List.of(new Wave(0, 15.0), new Wave(100, 30.0), new Wave(200, 50.0)), 20, 30, List.of(2.0, 3.0, 3.0, 2.0), 0.75, 0.4,
			List.of(35.0, 35.0, 30.0), 0.05F, 24.0, 12.0, 1.0F);

	public static final Codec<DraugrSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.doubleRange(1.0, 1024.0).fieldOf("health").orElse(DEFAULT.health).forGetter(DraugrSettings::health),
			Codec.doubleRange(0.0, 1.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(DraugrSettings::speed),
			Codec.floatRange(0.0F, 16.0F).fieldOf("fire_multiplier").orElse(DEFAULT.fireMultiplier).forGetter(DraugrSettings::fireMultiplier),
			Codec.doubleRange(0.0, 64.0).fieldOf("wait_radius").orElse(DEFAULT.waitRadius).forGetter(DraugrSettings::waitRadius),
			Wave.CODEC.listOf().fieldOf("waves").orElse(DEFAULT.waves).forGetter(DraugrSettings::waves),
			Codec.intRange(0, 200).fieldOf("wake_stagger").orElse(DEFAULT.wakeStagger).forGetter(DraugrSettings::wakeStagger),
			Codec.intRange(1, 200).fieldOf("rise_ticks").orElse(DEFAULT.riseTicks).forGetter(DraugrSettings::riseTicks),
			Codec.DOUBLE.listOf().fieldOf("piece_weights").orElse(DEFAULT.pieceWeights).forGetter(DraugrSettings::pieceWeights),
			Codec.doubleRange(0.0, 1.0).fieldOf("own_piece_chance").orElse(DEFAULT.ownPieceChance).forGetter(DraugrSettings::ownPieceChance),
			Codec.doubleRange(0.0, 1.0).fieldOf("belt_chance").orElse(DEFAULT.beltChance).forGetter(DraugrSettings::beltChance),
			Codec.DOUBLE.listOf().fieldOf("weapon_weights").orElse(DEFAULT.weaponWeights).forGetter(DraugrSettings::weaponWeights),
			Codec.floatRange(0.0F, 1.0F).fieldOf("weapon_drop_chance").orElse(DEFAULT.weaponDropChance)
					.forGetter(DraugrSettings::weaponDropChance),
			Codec.doubleRange(0.0, 64.0).fieldOf("climb_height").orElse(DEFAULT.climbHeight).forGetter(DraugrSettings::climbHeight),
			Codec.doubleRange(0.0, 64.0).fieldOf("climb_reach").orElse(DEFAULT.climbReach).forGetter(DraugrSettings::climbReach),
			Codec.floatRange(0.0F, 10.0F).fieldOf("shield_disable_multiplier").orElse(DEFAULT.shieldDisableMultiplier)
					.forGetter(DraugrSettings::shieldDisableMultiplier)
	).apply(instance, DraugrSettings::new));

	double[] pieceWeightArray() {
		return pieceWeights.stream().mapToDouble(Double::doubleValue).limit(4).toArray();
	}

	double[] weaponWeightArray() {
		return weaponWeights.stream().mapToDouble(Double::doubleValue).limit(3).toArray();
	}
}
