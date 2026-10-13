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
 * @param targetMemoryTicks ticks a mob keeps chasing a target it cannot see (vanilla: 60); long enough to notice
 *                         the target walled itself in and start digging
 * @param senseRadius      mobs smell a player through walls within this many blocks (0 turns it off)
 * @param stoneTicksPerHardness ticks a stone breaker needs per point of hardness of stone (stone has 1.5: 80 makes it
 *                         6 seconds)
 */
public record BlockBreakingSettings(
		int ticksPerHardness,
		int replanTicks,
		int maxNodes,
		int searchRadius,
		int decayDelayTicks,
		float decayPerTick,
		int targetMemoryTicks,
		int senseRadius,
		int stoneTicksPerHardness
) {
	public static final BlockBreakingSettings DEFAULT = new BlockBreakingSettings(600, 40, 600, 16, 200, 0.25F, 200, 5, 80);

	public static final Codec<BlockBreakingSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(1, Integer.MAX_VALUE).fieldOf("ticks_per_hardness").orElse(DEFAULT.ticksPerHardness).forGetter(BlockBreakingSettings::ticksPerHardness),
			Codec.intRange(1, Integer.MAX_VALUE).fieldOf("replan_ticks").orElse(DEFAULT.replanTicks).forGetter(BlockBreakingSettings::replanTicks),
			Codec.intRange(1, 100_000).fieldOf("max_nodes").orElse(DEFAULT.maxNodes).forGetter(BlockBreakingSettings::maxNodes),
			Codec.intRange(1, 64).fieldOf("search_radius").orElse(DEFAULT.searchRadius).forGetter(BlockBreakingSettings::searchRadius),
			Codec.intRange(0, Integer.MAX_VALUE).fieldOf("decay_delay_ticks").orElse(DEFAULT.decayDelayTicks).forGetter(BlockBreakingSettings::decayDelayTicks),
			Codec.floatRange(0.0F, Float.MAX_VALUE).fieldOf("decay_per_tick").orElse(DEFAULT.decayPerTick).forGetter(BlockBreakingSettings::decayPerTick),
			Codec.intRange(0, Integer.MAX_VALUE).fieldOf("target_memory_ticks").orElse(DEFAULT.targetMemoryTicks).forGetter(BlockBreakingSettings::targetMemoryTicks),
			Codec.intRange(0, 32).fieldOf("sense_radius").orElse(DEFAULT.senseRadius).forGetter(BlockBreakingSettings::senseRadius),
			Codec.intRange(1, Integer.MAX_VALUE).fieldOf("stone_ticks_per_hardness").orElse(DEFAULT.stoneTicksPerHardness)
					.forGetter(BlockBreakingSettings::stoneTicksPerHardness)
	).apply(instance, BlockBreakingSettings::new));
}
