package io.github.hellenter.eam.mobs.entity.duergar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for the duergar renegades, read from {@code data/eam/mobs/duergar_renegade.json} so datapacks can
 * override them. A missing or invalid field falls back to its default. Where squads spawn is the biome tag
 * {@code eam:has_duergar_renegades}; how fast they dig through stone is {@code stone_ticks_per_hardness} in
 * {@code block_breaking.json}.
 *
 * @param health           a follower's health
 * @param leaderHealth     the leader's health
 * @param speed            movement speed attribute (a zombie's is 0.23)
 * @param attackDamage     bare-handed damage; his weapon adds its own
 * @param extraArmor       armor on top of what his pieces give
 * @param armorToughness   armor toughness
 * @param minFollowers     fewest followers a leader brings
 * @param maxFollowers     most followers a leader brings
 * @param spawnSpacing     no squad spawns within this many blocks of another renegade
 * @param baldChance       chance he is bald
 * @param weaponDropChance chance his weapon drops
 * @param attacks          his special attacks: the charged blow, the bomb and the thrown squad mate
 */
public record DuergarRenegadeSettings(double health, double leaderHealth, double speed, double attackDamage, double extraArmor,
		double armorToughness, int minFollowers, int maxFollowers, double spawnSpacing, double baldChance, float weaponDropChance,
		DuergarRenegadeAttacks attacks) {
	public static final DuergarRenegadeSettings DEFAULT = new DuergarRenegadeSettings(19.5, 27.0, 0.253, 4.0, 4.0, 4.0, 3, 4, 64.0, 0.5,
			0.05F, DuergarRenegadeAttacks.DEFAULT);

	public static final Codec<DuergarRenegadeSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.doubleRange(1.0, 1024.0).fieldOf("health").orElse(DEFAULT.health).forGetter(DuergarRenegadeSettings::health),
			Codec.doubleRange(1.0, 1024.0).fieldOf("leader_health").orElse(DEFAULT.leaderHealth).forGetter(DuergarRenegadeSettings::leaderHealth),
			Codec.doubleRange(0.0, 1.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(DuergarRenegadeSettings::speed),
			Codec.doubleRange(0.0, 100.0).fieldOf("attack_damage").orElse(DEFAULT.attackDamage).forGetter(DuergarRenegadeSettings::attackDamage),
			Codec.doubleRange(0.0, 30.0).fieldOf("extra_armor").orElse(DEFAULT.extraArmor).forGetter(DuergarRenegadeSettings::extraArmor),
			Codec.doubleRange(0.0, 20.0).fieldOf("armor_toughness").orElse(DEFAULT.armorToughness).forGetter(DuergarRenegadeSettings::armorToughness),
			Codec.intRange(0, 10).fieldOf("min_followers").orElse(DEFAULT.minFollowers).forGetter(DuergarRenegadeSettings::minFollowers),
			Codec.intRange(0, 10).fieldOf("max_followers").orElse(DEFAULT.maxFollowers).forGetter(DuergarRenegadeSettings::maxFollowers),
			Codec.doubleRange(0.0, 256.0).fieldOf("spawn_spacing").orElse(DEFAULT.spawnSpacing).forGetter(DuergarRenegadeSettings::spawnSpacing),
			Codec.doubleRange(0.0, 1.0).fieldOf("bald_chance").orElse(DEFAULT.baldChance).forGetter(DuergarRenegadeSettings::baldChance),
			Codec.floatRange(0.0F, 1.0F).fieldOf("weapon_drop_chance").orElse(DEFAULT.weaponDropChance)
					.forGetter(DuergarRenegadeSettings::weaponDropChance),
			DuergarRenegadeAttacks.CODEC.fieldOf("attacks").orElse(DEFAULT.attacks).forGetter(DuergarRenegadeSettings::attacks)
	).apply(instance, DuergarRenegadeSettings::new));
}
