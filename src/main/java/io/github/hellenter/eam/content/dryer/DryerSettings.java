package io.github.hellenter.eam.content.dryer;

import io.github.hellenter.eam.survival.thirst.Thirst;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for the food dryer, read from {@code data/eam/food_dryer/settings.json} so datapacks can override
 * them. How much longer dried food keeps is in the spoilage settings ({@code dried_multiplier}).
 *
 * @param dryTicks ticks food lies on the dryer before it is dried (12000 = half a Minecraft day)
 * @param water    thirst points eating one dried item costs (2 points = one droplet)
 */
public record DryerSettings(int dryTicks, int water) {
	public static final DryerSettings DEFAULT = new DryerSettings(12000, 1);

	public static final Codec<DryerSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(1, Integer.MAX_VALUE).fieldOf("dry_ticks").orElse(DEFAULT.dryTicks).forGetter(DryerSettings::dryTicks),
			Codec.intRange(0, Thirst.MAX).fieldOf("water").orElse(DEFAULT.water).forGetter(DryerSettings::water)
	).apply(instance, DryerSettings::new));
}
