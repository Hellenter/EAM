package io.github.hellenter.eam.survival.sleep;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Limits of the safe room check, read from {@code data/eam/sleep/safe_room.json} so datapacks can override them. A
 * missing or invalid field falls back to its default.
 *
 * @param maxVolume     most open cells a room may have; bigger rooms count as not closed. Caps the cost of a check.
 * @param maxRadius     the room may reach this many blocks from the bed along any axis
 * @param doorBelowSize a room narrower than this along X or Z needs a door (3: anything smaller than 3x3)
 * @param minLight      the brightest block light in the room must reach this (a torch gives 14 next to it)
 */
public record SafeRoomSettings(int maxVolume, int maxRadius, int doorBelowSize, int minLight) {
	public static final SafeRoomSettings DEFAULT = new SafeRoomSettings(1024, 16, 3, 10);

	public static final Codec<SafeRoomSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(1, 32_768).fieldOf("max_volume").orElse(DEFAULT.maxVolume).forGetter(SafeRoomSettings::maxVolume),
			Codec.intRange(1, 64).fieldOf("max_radius").orElse(DEFAULT.maxRadius).forGetter(SafeRoomSettings::maxRadius),
			Codec.intRange(0, 64).fieldOf("door_below_size").orElse(DEFAULT.doorBelowSize).forGetter(SafeRoomSettings::doorBelowSize),
			Codec.intRange(0, 15).fieldOf("min_light").orElse(DEFAULT.minLight).forGetter(SafeRoomSettings::minLight)
	).apply(instance, SafeRoomSettings::new));
}
