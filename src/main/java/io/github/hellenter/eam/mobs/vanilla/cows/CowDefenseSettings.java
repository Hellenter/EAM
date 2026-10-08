package io.github.hellenter.eam.mobs.vanilla.cows;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for cows fighting back, read from {@code data/eam/mobs/cow_defense.json} so datapacks can override
 * them. A missing or invalid field falls back to its default.
 *
 * @param damage              damage per hit (2 is one heart)
 * @param attackCooldownTicks ticks between two hits
 * @param giveUpTicks         a charge that has not reached the attacker by then is dropped
 * @param retreatTicks        how long the cow backs off after a hit
 * @param chaseSpeed          speed multiplier while charging (vanilla panic is 2.0)
 * @param retreatSpeed        speed multiplier while backing off
 * @param knockback           how hard a hit pushes the attacker
 */
public record CowDefenseSettings(
		float damage,
		int attackCooldownTicks,
		int giveUpTicks,
		int retreatTicks,
		double chaseSpeed,
		double retreatSpeed,
		float knockback
) {
	public static final CowDefenseSettings DEFAULT = new CowDefenseSettings(2.0F, 30, 100, 60, 1.8, 1.8, 0.5F);

	public static final Codec<CowDefenseSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.floatRange(0.0F, 100.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(CowDefenseSettings::damage),
			Codec.intRange(1, 1200).fieldOf("attack_cooldown_ticks").orElse(DEFAULT.attackCooldownTicks).forGetter(CowDefenseSettings::attackCooldownTicks),
			Codec.intRange(1, 1200).fieldOf("give_up_ticks").orElse(DEFAULT.giveUpTicks).forGetter(CowDefenseSettings::giveUpTicks),
			Codec.intRange(1, 1200).fieldOf("retreat_ticks").orElse(DEFAULT.retreatTicks).forGetter(CowDefenseSettings::retreatTicks),
			Codec.doubleRange(0.1, 4.0).fieldOf("chase_speed").orElse(DEFAULT.chaseSpeed).forGetter(CowDefenseSettings::chaseSpeed),
			Codec.doubleRange(0.1, 4.0).fieldOf("retreat_speed").orElse(DEFAULT.retreatSpeed).forGetter(CowDefenseSettings::retreatSpeed),
			Codec.floatRange(0.0F, 5.0F).fieldOf("knockback").orElse(DEFAULT.knockback).forGetter(CowDefenseSettings::knockback)
	).apply(instance, CowDefenseSettings::new));
}
