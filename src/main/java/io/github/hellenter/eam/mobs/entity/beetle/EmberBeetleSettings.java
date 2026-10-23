package io.github.hellenter.eam.mobs.entity.beetle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for ember beetles, read from {@code data/eam/mobs/ember_beetle.json} so datapacks can override them.
 * A missing or invalid field falls back to its default.
 *
 * @param health         health of a beetle
 * @param armor          armor of its coal shell
 * @param speed          movement speed; a boar's is 0.25
 * @param damage         damage of a bite
 * @param burnSeconds    how long a bite sets the player on fire
 * @param noticeRange    a player closer than this many blocks is attacked; farther ones are left alone until they hit one
 * @param crushMultiplier damage multiplier of crushing weapons (axes, the mace, forge hammers: tag {@code eam:crushes_shells})
 * @param eatHeal        health a beetle gets back for each coal it eats
 * @param maxEaten       how many eaten coals it carries in its belly, dropped when it dies
 * @param deathFire      whether it sets the ground under it alight when it bursts
 * @param spawnChance    chance that a natural spawn goes ahead, so the caves do not fill up at once
 */
public record EmberBeetleSettings(float health, float armor, double speed, float damage, int burnSeconds, double noticeRange,
		float crushMultiplier, float eatHeal, int maxEaten, boolean deathFire, float spawnChance) {
	public static final EmberBeetleSettings DEFAULT = new EmberBeetleSettings(12.0F, 6.0F, 0.25, 3.0F, 3, 4.0, 1.5F, 4.0F, 3, true, 0.6F);

	public static final Codec<EmberBeetleSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.floatRange(1.0F, 1024.0F).fieldOf("health").orElse(DEFAULT.health).forGetter(EmberBeetleSettings::health),
			Codec.floatRange(0.0F, 30.0F).fieldOf("armor").orElse(DEFAULT.armor).forGetter(EmberBeetleSettings::armor),
			Codec.doubleRange(0.05, 1.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(EmberBeetleSettings::speed),
			Codec.floatRange(0.0F, 100.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(EmberBeetleSettings::damage),
			Codec.intRange(0, 60).fieldOf("burn_seconds").orElse(DEFAULT.burnSeconds).forGetter(EmberBeetleSettings::burnSeconds),
			Codec.doubleRange(0.0, 32.0).fieldOf("notice_range").orElse(DEFAULT.noticeRange).forGetter(EmberBeetleSettings::noticeRange),
			Codec.floatRange(0.0F, 10.0F).fieldOf("crush_multiplier").orElse(DEFAULT.crushMultiplier).forGetter(EmberBeetleSettings::crushMultiplier),
			Codec.floatRange(0.0F, 100.0F).fieldOf("eat_heal").orElse(DEFAULT.eatHeal).forGetter(EmberBeetleSettings::eatHeal),
			Codec.intRange(0, 64).fieldOf("max_eaten").orElse(DEFAULT.maxEaten).forGetter(EmberBeetleSettings::maxEaten),
			Codec.BOOL.fieldOf("death_fire").orElse(DEFAULT.deathFire).forGetter(EmberBeetleSettings::deathFire),
			Codec.floatRange(0.0F, 1.0F).fieldOf("spawn_chance").orElse(DEFAULT.spawnChance).forGetter(EmberBeetleSettings::spawnChance)
	).apply(instance, EmberBeetleSettings::new));
}
