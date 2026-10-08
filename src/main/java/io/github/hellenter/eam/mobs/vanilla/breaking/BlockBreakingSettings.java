package io.github.hellenter.eam.mobs.vanilla.breaking;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for mobs breaking blocks, read from {@code data/eam/mobs/block_breaking.json} so datapacks can
 * override them. A missing or invalid field falls back to its default.
 *
 * @param ticksPerHardness ticks one mob needs per point of block hardness (dirt has 0.5: 600 makes it 15 seconds)
 * @param replanTicks      how often a mob re-plans its route through the walls
 * @param maxNodes         most cells one route search expands; caps the cost of a plan
 * @param searchRadius     the route search stays within this many blocks of the mob
 * @param decayDelayTicks  ticks a damaged block must be left alone before its damage starts to heal
 * @param decayPerTick     break ticks a damaged block heals per tick once nobody works on it
 */
public record BlockBreakingSettings(
		int ticksPerHardness,
		int replanTicks,
		int maxNodes,
		int searchRadius,
		int decayDelayTicks,
		float decayPerTick
) {
	public static final BlockBreakingSettings DEFAULT = new BlockBreakingSettings(600, 40, 1200, 16, 200, 0.25F);

	public static final Codec<BlockBreakingSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(1, Integer.MAX_VALUE).fieldOf("ticks_per_hardness").orElse(DEFAULT.ticksPerHardness).forGetter(BlockBreakingSettings::ticksPerHardness),
			Codec.intRange(1, Integer.MAX_VALUE).fieldOf("replan_ticks").orElse(DEFAULT.replanTicks).forGetter(BlockBreakingSettings::replanTicks),
			Codec.intRange(1, 100_000).fieldOf("max_nodes").orElse(DEFAULT.maxNodes).forGetter(BlockBreakingSettings::maxNodes),
			Codec.intRange(1, 64).fieldOf("search_radius").orElse(DEFAULT.searchRadius).forGetter(BlockBreakingSettings::searchRadius),
			Codec.intRange(0, Integer.MAX_VALUE).fieldOf("decay_delay_ticks").orElse(DEFAULT.decayDelayTicks).forGetter(BlockBreakingSettings::decayDelayTicks),
			Codec.floatRange(0.0F, Float.MAX_VALUE).fieldOf("decay_per_tick").orElse(DEFAULT.decayPerTick).forGetter(BlockBreakingSettings::decayPerTick)
	).apply(instance, BlockBreakingSettings::new));
}
