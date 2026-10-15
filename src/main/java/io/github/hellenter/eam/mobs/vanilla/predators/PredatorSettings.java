package io.github.hellenter.eam.mobs.vanilla.predators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for night raids on livestock, read from {@code data/eam/mobs/predators.json} so datapacks can
 * override them. A missing or invalid field falls back to its default. Distances are in blocks, times in ticks.
 *
 * @param hunters       wolves and wargs: they go for any livestock they can reach
 * @param raiders       zombies: they go only for livestock in an open, dark pen
 * @param recheckTicks  a hunter on its way checks this often that it can still reach its prey (a gate shut behind it)
 */
public record PredatorSettings(Group hunters, Group raiders, int recheckTicks) {
	public static final PredatorSettings DEFAULT = new PredatorSettings(new Group(24.0, 15), new Group(16.0, 7), 40);

	public static final Codec<PredatorSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Group.CODEC.fieldOf("hunters").orElse(DEFAULT.hunters).forGetter(PredatorSettings::hunters),
			Group.CODEC.fieldOf("raiders").orElse(DEFAULT.raiders).forGetter(PredatorSettings::raiders),
			Codec.intRange(1, 1200).fieldOf("recheck_ticks").orElse(DEFAULT.recheckTicks).forGetter(PredatorSettings::recheckTicks)
	).apply(instance, PredatorSettings::new));

	/**
	 * One kind of predator.
	 *
	 * @param range         how far it spots livestock
	 * @param maxBlockLight livestock standing in block light above this is left alone; 15 means light never helps
	 */
	public record Group(double range, int maxBlockLight) {
		public static final Codec<Group> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(1.0, 64.0).fieldOf("range").orElse(16.0).forGetter(Group::range),
				Codec.intRange(0, 15).fieldOf("max_block_light").orElse(15).forGetter(Group::maxBlockLight)
		).apply(instance, Group::new));
	}
}
