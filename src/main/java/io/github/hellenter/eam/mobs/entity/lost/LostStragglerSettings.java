package io.github.hellenter.eam.mobs.entity.lost;

import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightSettings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The straggler's numbers, the {@code straggler} section of {@code data/eam/mobs/lost.json}.
 *
 * @param health         his health
 * @param attackDamage   his own damage, before his sword's
 * @param dropChance     chance for each piece of his gear to drop
 * @param chestChance    chance he still wears his torn breastplate
 * @param bootsChance    chance he still wears his boots
 * @param fight          his sword fighting
 * @param callAfter      ticks of fighting a player before he strikes the ground and calls the Lost
 * @param fightRange     the fight counts only with his target player this close
 * @param squadRange     a squad of the Lost this close hears his call and comes (no further than they follow a target);
 *                       failing one, a new squad comes
 * @param minDistance    a new squad comes this far from the player at least, out of the player's sight
 * @param maxDistance    and this far at most
 * @param maxNearby      with this many of the Lost (cavalry counted) around already, no new squad comes
 * @param joinRange      out of a fight he joins a squad whose leader is this close
 * @param spawnSpacing   no straggler spawns naturally this close to another
 * @param spawnRange     nor where {@code maxStragglers} of them are this close already
 * @param maxStragglers  how many stragglers may roam within {@code spawnRange}
 */
public record LostStragglerSettings(double health, double attackDamage, float dropChance, float chestChance, float bootsChance,
		SkeletonKnightSettings fight, int callAfter, double fightRange, double squadRange, double minDistance, double maxDistance,
		int maxNearby, double joinRange, double spawnSpacing, double spawnRange, int maxStragglers) {
	/** A follower's fighting, slower and with fewer combos. */
	public static final SkeletonKnightSettings FIGHT_DEFAULT = new SkeletonKnightSettings(28, 2.6, 0.0, 1.0F,
			0.1, 160, 0.6F, 0.0, 0.0,
			400, 1.5F, 0.5, 0.5F, 3.0F,
			0.5, new SkeletonKnightSettings.Dash(300, 3, 0.8));
	public static final LostStragglerSettings DEFAULT = new LostStragglerSettings(14.0, 0.0, 0.03F, 0.75F, 0.5F, FIGHT_DEFAULT,
			200, 16.0, 32.0, 16.0, 26.0, 12, 32.0, 24.0, 64.0, 2);

	public static final Codec<LostStragglerSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.doubleRange(1.0, 1024.0).fieldOf("health").orElse(DEFAULT.health).forGetter(LostStragglerSettings::health),
			Codec.doubleRange(0.0, 64.0).fieldOf("attack_damage").orElse(DEFAULT.attackDamage).forGetter(LostStragglerSettings::attackDamage),
			Codec.floatRange(0.0F, 1.0F).fieldOf("drop_chance").orElse(DEFAULT.dropChance).forGetter(LostStragglerSettings::dropChance),
			Codec.floatRange(0.0F, 1.0F).fieldOf("chest_chance").orElse(DEFAULT.chestChance).forGetter(LostStragglerSettings::chestChance),
			Codec.floatRange(0.0F, 1.0F).fieldOf("boots_chance").orElse(DEFAULT.bootsChance).forGetter(LostStragglerSettings::bootsChance),
			SkeletonKnightSettings.CODEC.fieldOf("fight").orElse(DEFAULT.fight).forGetter(LostStragglerSettings::fight),
			Codec.intRange(0, 24000).fieldOf("call_after").orElse(DEFAULT.callAfter).forGetter(LostStragglerSettings::callAfter),
			Codec.doubleRange(1.0, 64.0).fieldOf("fight_range").orElse(DEFAULT.fightRange).forGetter(LostStragglerSettings::fightRange),
			Codec.doubleRange(0.0, 128.0).fieldOf("squad_range").orElse(DEFAULT.squadRange).forGetter(LostStragglerSettings::squadRange),
			Codec.doubleRange(0.0, 64.0).fieldOf("min_distance").orElse(DEFAULT.minDistance).forGetter(LostStragglerSettings::minDistance),
			Codec.doubleRange(1.0, 64.0).fieldOf("max_distance").orElse(DEFAULT.maxDistance).forGetter(LostStragglerSettings::maxDistance),
			Codec.intRange(0, 256).fieldOf("max_nearby").orElse(DEFAULT.maxNearby).forGetter(LostStragglerSettings::maxNearby),
			Codec.doubleRange(0.0, 128.0).fieldOf("join_range").orElse(DEFAULT.joinRange).forGetter(LostStragglerSettings::joinRange),
			Codec.doubleRange(0.0, 256.0).fieldOf("spawn_spacing").orElse(DEFAULT.spawnSpacing).forGetter(LostStragglerSettings::spawnSpacing),
			Codec.doubleRange(0.0, 256.0).fieldOf("spawn_range").orElse(DEFAULT.spawnRange).forGetter(LostStragglerSettings::spawnRange),
			Codec.intRange(0, 64).fieldOf("max_stragglers").orElse(DEFAULT.maxStragglers).forGetter(LostStragglerSettings::maxStragglers)
	).apply(instance, LostStragglerSettings::new));
}
