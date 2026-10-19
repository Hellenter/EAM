package io.github.hellenter.eam.mobs.vanilla.villager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for villager guards, read from {@code data/eam/villagers/guards.json} so datapacks can override them.
 * What makes a guard (a weapon from {@code eam:villager_weapons} and any armor) is in {@link VillagerGuardModule}.
 *
 * @param meleeSpeed     walk speed while closing in for a blow, as a multiple of the villager's speed
 * @param bowRange       how far, in blocks, a guard with a bow shoots from; farther he walks closer
 * @param bowDrawTicks   ticks he draws the bow before each shot
 * @param bowRestTicks   ticks between shots
 * @param arrowPower     an arrow's base damage, as a skeleton's power (vanilla skeletons: 2 on normal)
 * @param armRadius      how near, in blocks, a monster in sight makes a villager who is no guard put on his gear
 * @param calmTicks      ticks without danger before he puts it away again
 * @param guardSearchRadius how far, in blocks, an armed villager looks for a guard to run to
 * @param guardReach     how close, in blocks, he runs to the guard before he fights
 */
public record GuardSettings(double meleeSpeed, int bowRange, int bowDrawTicks, int bowRestTicks, float arrowPower, int armRadius,
		int calmTicks, int guardSearchRadius, double guardReach) {
	public static final GuardSettings DEFAULT = new GuardSettings(0.8, 15, 20, 20, 2.0F, 12, 600, 32, 4.0);

	public static final Codec<GuardSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.doubleRange(0.1, 4.0).fieldOf("melee_speed").orElse(DEFAULT.meleeSpeed).forGetter(GuardSettings::meleeSpeed),
			Codec.intRange(2, 64).fieldOf("bow_range").orElse(DEFAULT.bowRange).forGetter(GuardSettings::bowRange),
			Codec.intRange(1, 200).fieldOf("bow_draw_ticks").orElse(DEFAULT.bowDrawTicks).forGetter(GuardSettings::bowDrawTicks),
			Codec.intRange(0, 200).fieldOf("bow_rest_ticks").orElse(DEFAULT.bowRestTicks).forGetter(GuardSettings::bowRestTicks),
			Codec.floatRange(0.0F, 100.0F).fieldOf("arrow_power").orElse(DEFAULT.arrowPower).forGetter(GuardSettings::arrowPower),
			Codec.intRange(1, 64).fieldOf("arm_radius").orElse(DEFAULT.armRadius).forGetter(GuardSettings::armRadius),
			Codec.intRange(20, 72000).fieldOf("calm_ticks").orElse(DEFAULT.calmTicks).forGetter(GuardSettings::calmTicks),
			Codec.intRange(1, 128).fieldOf("guard_search_radius").orElse(DEFAULT.guardSearchRadius).forGetter(GuardSettings::guardSearchRadius),
			Codec.doubleRange(1.0, 32.0).fieldOf("guard_reach").orElse(DEFAULT.guardReach).forGetter(GuardSettings::guardReach)
	).apply(instance, GuardSettings::new));
}
