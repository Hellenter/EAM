package io.github.hellenter.eam.mobs.vanilla.villager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for villagers' locked chests, read from {@code data/eam/villagers/chests.json} so datapacks can
 * override them.
 *
 * @param intervalTicks      how often, in ticks, a villager by his chest puts things in or takes them out
 * @param reach              how close, in blocks, he must be to his chest to use it
 * @param keepSlots          inventory slots he keeps filled; what fills more goes in his chest
 * @param foodFromChest      food items he takes out at a time when hungry
 * @param placeIntervalTicks how often, in ticks, an elder whose village has no store tries to place one by his job site
 */
public record ChestSettings(int intervalTicks, int reach, int keepSlots, int foodFromChest, int placeIntervalTicks) {
	public static final ChestSettings DEFAULT = new ChestSettings(200, 4, 6, 16, 1200);

	public static final Codec<ChestSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(20, 1_000_000).fieldOf("interval_ticks").orElse(DEFAULT.intervalTicks).forGetter(ChestSettings::intervalTicks),
			Codec.intRange(1, 16).fieldOf("reach").orElse(DEFAULT.reach).forGetter(ChestSettings::reach),
			Codec.intRange(0, 8).fieldOf("keep_slots").orElse(DEFAULT.keepSlots).forGetter(ChestSettings::keepSlots),
			Codec.intRange(1, 64).fieldOf("food_from_chest").orElse(DEFAULT.foodFromChest).forGetter(ChestSettings::foodFromChest),
			Codec.intRange(20, 1_000_000).fieldOf("place_interval_ticks").orElse(DEFAULT.placeIntervalTicks).forGetter(ChestSettings::placeIntervalTicks)
	).apply(instance, ChestSettings::new));
}
