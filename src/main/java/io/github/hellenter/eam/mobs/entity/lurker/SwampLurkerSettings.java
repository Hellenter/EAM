package io.github.hellenter.eam.mobs.entity.lurker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for the swamp lurker, read from {@code data/eam/mobs/swamp_lurker.json} so datapacks can override
 * them. A missing or invalid field falls back to its default. Times are in ticks, distances in blocks.
 *
 * @param emergeRange       a hidden lurker bursts out when a player comes this close
 * @param sneakEmergeRange  the same for a sneaking player
 * @param hideAfter         with no target for this long it burrows into the ground under it
 * @param rehideCooldown    after burrowing it stays hidden at least this long
 * @param reach             how far its swipes reach, measured flat from its centre to the target's
 * @param hitMultiplier     damage of each swipe, times its attack damage
 * @param comboCooldown     after a combo it keeps its distance this long before going in again
 * @param leap              its long jumps
 * @param spawnSpacing      a natural lurker never spawns this close to another one
 */
public record SwampLurkerSettings(double emergeRange, double sneakEmergeRange, int hideAfter, int rehideCooldown, double reach,
		float hitMultiplier, int comboCooldown, Leap leap, double spawnSpacing) {
	public static final SwampLurkerSettings DEFAULT = new SwampLurkerSettings(4.5, 2.5, 160, 60, 1.9,
			1.0F, 50, Leap.DEFAULT, 24.0);

	/**
	 * @param maxHeight       the highest it jumps, in blocks above its feet
	 * @param maxDistance     the furthest it jumps
	 * @param minDistance     it walks rather than leaps at a target closer than this
	 * @param cooldown        ticks between two leaps
	 * @param retreatDistance after a combo it leaps back to about this far from the target
	 */
	public record Leap(double maxHeight, double maxDistance, double minDistance, int cooldown, double retreatDistance) {
		public static final Leap DEFAULT = new Leap(5.0, 7.0, 3.5, 25, 6.5);

		public static final Codec<Leap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(0.5, 12.0).fieldOf("max_height").orElse(DEFAULT.maxHeight).forGetter(Leap::maxHeight),
				Codec.doubleRange(1.0, 16.0).fieldOf("max_distance").orElse(DEFAULT.maxDistance).forGetter(Leap::maxDistance),
				Codec.doubleRange(0.0, 16.0).fieldOf("min_distance").orElse(DEFAULT.minDistance).forGetter(Leap::minDistance),
				Codec.intRange(0, 2400).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Leap::cooldown),
				Codec.doubleRange(0.0, 16.0).fieldOf("retreat_distance").orElse(DEFAULT.retreatDistance).forGetter(Leap::retreatDistance)
		).apply(instance, Leap::new));
	}

	public static final Codec<SwampLurkerSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.doubleRange(0.0, 32.0).fieldOf("emerge_range").orElse(DEFAULT.emergeRange).forGetter(SwampLurkerSettings::emergeRange),
			Codec.doubleRange(0.0, 32.0).fieldOf("sneak_emerge_range").orElse(DEFAULT.sneakEmergeRange)
					.forGetter(SwampLurkerSettings::sneakEmergeRange),
			Codec.intRange(0, 24000).fieldOf("hide_after").orElse(DEFAULT.hideAfter).forGetter(SwampLurkerSettings::hideAfter),
			Codec.intRange(0, 24000).fieldOf("rehide_cooldown").orElse(DEFAULT.rehideCooldown).forGetter(SwampLurkerSettings::rehideCooldown),
			Codec.doubleRange(1.0, 6.0).fieldOf("reach").orElse(DEFAULT.reach).forGetter(SwampLurkerSettings::reach),
			Codec.floatRange(0.0F, 10.0F).fieldOf("hit_multiplier").orElse(DEFAULT.hitMultiplier).forGetter(SwampLurkerSettings::hitMultiplier),
			Codec.intRange(0, 2400).fieldOf("combo_cooldown").orElse(DEFAULT.comboCooldown).forGetter(SwampLurkerSettings::comboCooldown),
			Leap.CODEC.fieldOf("leap").orElse(DEFAULT.leap).forGetter(SwampLurkerSettings::leap),
			Codec.doubleRange(0.0, 128.0).fieldOf("spawn_spacing").orElse(DEFAULT.spawnSpacing).forGetter(SwampLurkerSettings::spawnSpacing)
	).apply(instance, SwampLurkerSettings::new));
}
