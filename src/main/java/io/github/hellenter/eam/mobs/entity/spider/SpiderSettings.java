package io.github.hellenter.eam.mobs.entity.spider;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for one of the small spiders, read from {@code data/eam/mobs/wood_spider.json} and
 * {@code data/eam/mobs/black_widow.json} so datapacks can override them. A missing or invalid field falls back to its
 * default. Times are in ticks.
 *
 * @param poisonTicks      how long its bite poisons on normal difficulty (half on easy, one and a half times on hard)
 * @param brightRange      share of its follow range it keeps in bright light: it sees worse in the sun
 * @param spawnChance      a natural spawn that passed every other rule still happens only this often
 * @param daySpawnChance   chance that a natural spawn on leaves goes ahead where it is too light for other monsters
 * @param maxNearby        no natural spawn while this many of its kind are within 16 blocks
 * @param needsTrees       natural spawns only next to a tree: a log and leaves close by
 */
public record SpiderSettings(int poisonTicks, double brightRange, float spawnChance, float daySpawnChance, int maxNearby,
		boolean needsTrees) {
	public static final SpiderSettings WOOD_SPIDER = new SpiderSettings(40, 0.5, 1.0F, 0.03F, 3, true);
	public static final SpiderSettings BLACK_WIDOW = new SpiderSettings(100, 0.5, 0.5F, 0.03F, 2, false);

	public static Codec<SpiderSettings> codec(SpiderSettings defaults) {
		return RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, 2400).fieldOf("poison_ticks").orElse(defaults.poisonTicks).forGetter(SpiderSettings::poisonTicks),
				Codec.doubleRange(0.0, 1.0).fieldOf("bright_range").orElse(defaults.brightRange).forGetter(SpiderSettings::brightRange),
				Codec.floatRange(0.0F, 1.0F).fieldOf("spawn_chance").orElse(defaults.spawnChance).forGetter(SpiderSettings::spawnChance),
				Codec.floatRange(0.0F, 1.0F).fieldOf("day_spawn_chance").orElse(defaults.daySpawnChance).forGetter(SpiderSettings::daySpawnChance),
				Codec.intRange(1, 64).fieldOf("max_nearby").orElse(defaults.maxNearby).forGetter(SpiderSettings::maxNearby),
				Codec.BOOL.fieldOf("needs_trees").orElse(defaults.needsTrees).forGetter(SpiderSettings::needsTrees)
		).apply(instance, SpiderSettings::new));
	}
}
