package io.github.hellenter.eam.mobs.entity.duergar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * Balance numbers for the zombie duergar, read from {@code data/eam/mobs/zombie_duergar.json} so datapacks can override
 * them. A missing or invalid field falls back to its default. Where and how many spawn is the mine structure's
 * {@code spawn_overrides}.
 *
 * @param health           health
 * @param speed            movement speed attribute (a zombie's is 0.23)
 * @param attackDamage     bare-handed damage; a tool in his hand adds its own
 * @param pieceWeights     weights of wearing 0, 1, 2, 3 and 4 armor pieces
 * @param beltChance       chance of a belt
 * @param baldChance       chance he has no hair left (the beard always stays)
 * @param weaponWeights    weights of a pickaxe, an axe, a shovel and bare hands
 * @param weaponDropChance chance his tool drops
 */
public record ZombieDuergarSettings(double health, double speed, double attackDamage, List<Double> pieceWeights, double beltChance,
		double baldChance, List<Double> weaponWeights, float weaponDropChance) {
	public static final ZombieDuergarSettings DEFAULT = new ZombieDuergarSettings(16.0, 0.21, 3.0, List.of(1.0, 3.0, 3.0, 2.0, 1.0), 0.5,
			0.4, List.of(40.0, 20.0, 15.0, 25.0), 0.04F);

	public static final Codec<ZombieDuergarSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.doubleRange(1.0, 1024.0).fieldOf("health").orElse(DEFAULT.health).forGetter(ZombieDuergarSettings::health),
			Codec.doubleRange(0.0, 1.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(ZombieDuergarSettings::speed),
			Codec.doubleRange(0.0, 100.0).fieldOf("attack_damage").orElse(DEFAULT.attackDamage).forGetter(ZombieDuergarSettings::attackDamage),
			Codec.DOUBLE.listOf().fieldOf("piece_weights").orElse(DEFAULT.pieceWeights).forGetter(ZombieDuergarSettings::pieceWeights),
			Codec.doubleRange(0.0, 1.0).fieldOf("belt_chance").orElse(DEFAULT.beltChance).forGetter(ZombieDuergarSettings::beltChance),
			Codec.doubleRange(0.0, 1.0).fieldOf("bald_chance").orElse(DEFAULT.baldChance).forGetter(ZombieDuergarSettings::baldChance),
			Codec.DOUBLE.listOf().fieldOf("weapon_weights").orElse(DEFAULT.weaponWeights).forGetter(ZombieDuergarSettings::weaponWeights),
			Codec.floatRange(0.0F, 1.0F).fieldOf("weapon_drop_chance").orElse(DEFAULT.weaponDropChance)
					.forGetter(ZombieDuergarSettings::weaponDropChance)
	).apply(instance, ZombieDuergarSettings::new));

	double[] pieceWeightArray() {
		return pieceWeights.stream().mapToDouble(Double::doubleValue).limit(5).toArray();
	}

	double[] weaponWeightArray() {
		return weaponWeights.stream().mapToDouble(Double::doubleValue).limit(4).toArray();
	}
}
