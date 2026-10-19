package io.github.hellenter.eam.mobs.entity.guard;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for the pillager guard, read from {@code data/eam/mobs/pillager_guard.json} so datapacks can override
 * them. A missing or invalid field falls back to its default. Times are in ticks, distances in blocks, damage in health
 * points (2 per heart) before the player's armor.
 *
 * @param stats         health, armor, speed and the rest of his attributes
 * @param decideEvery   ticks between his checks for an onslaught or a throw
 * @param onslaught     the king's onslaught, weakened, with one axe
 * @param axeThrow      his axe thrown; it flies back to his hand
 * @param spawnShare    share of pillagers in patrols and outposts that come as guards
 */
public record PillagerGuardSettings(Stats stats, int decideEvery, Onslaught onslaught, AxeThrow axeThrow, float spawnShare) {
	public static final PillagerGuardSettings DEFAULT = new PillagerGuardSettings(Stats.DEFAULT, 10, Onslaught.DEFAULT, AxeThrow.DEFAULT, 0.2F);

	public static final Codec<PillagerGuardSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Stats.CODEC.fieldOf("stats").orElse(DEFAULT.stats).forGetter(PillagerGuardSettings::stats),
			Codec.intRange(1, 200).fieldOf("decide_every").orElse(DEFAULT.decideEvery).forGetter(PillagerGuardSettings::decideEvery),
			Onslaught.CODEC.fieldOf("onslaught").orElse(DEFAULT.onslaught).forGetter(PillagerGuardSettings::onslaught),
			AxeThrow.CODEC.fieldOf("throw").orElse(DEFAULT.axeThrow).forGetter(PillagerGuardSettings::axeThrow),
			Codec.floatRange(0.0F, 1.0F).fieldOf("spawn_share").orElse(DEFAULT.spawnShare).forGetter(PillagerGuardSettings::spawnShare)
	).apply(instance, PillagerGuardSettings::new));

	/**
	 * @param health      his health
	 * @param armor       armor points of his helmet and cuirass
	 * @param speed       movement speed attribute
	 * @param followRange how far he notices and follows players
	 * @param xp          experience dropped on death
	 * @param meleeDamageMultiplier his plain axe blows, times their vanilla damage
	 */
	public record Stats(float health, int armor, double speed, double followRange, int xp, float meleeDamageMultiplier) {
		public static final Stats DEFAULT = new Stats(20.0F, 8, 0.35, 16.0, 10, 0.8F);

		public static final Codec<Stats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(1.0F, 1024.0F).fieldOf("health").orElse(DEFAULT.health).forGetter(Stats::health),
				Codec.intRange(0, 30).fieldOf("armor").orElse(DEFAULT.armor).forGetter(Stats::armor),
				Codec.doubleRange(0.01, 2.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Stats::speed),
				Codec.doubleRange(1.0, 128.0).fieldOf("follow_range").orElse(DEFAULT.followRange).forGetter(Stats::followRange),
				Codec.intRange(0, 100000).fieldOf("xp").orElse(DEFAULT.xp).forGetter(Stats::xp),
				Codec.floatRange(0.0F, 10.0F).fieldOf("melee_damage_multiplier").orElse(DEFAULT.meleeDamageMultiplier)
						.forGetter(Stats::meleeDamageMultiplier)
		).apply(instance, Stats::new));
	}

	/**
	 * @param damage           damage of the blow
	 * @param shieldReduction  share of the blow a raised shield takes off
	 * @param rushSpeed        speed of the rush before the leap, blocks per tick
	 * @param reach            how far the blow reaches, flat from his centre to the target's
	 * @param from             nearest target he starts it at
	 * @param to               farthest target he starts it at
	 * @param chance           chance to start it at each check while it is ready
	 * @param cooldown         ticks before the next one
	 * @param parryMultiplier  damage of the hit that parries him, times the hit's own
	 */
	public record Onslaught(float damage, float shieldReduction, double rushSpeed, double reach, double from, double to, double chance,
			int cooldown, float parryMultiplier) {
		public static final Onslaught DEFAULT = new Onslaught(8.0F, 0.25F, 0.55, 2.6, 2.5, 7.0, 0.35, 300, 1.5F);

		public static final Codec<Onslaught> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1024.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Onslaught::damage),
				Codec.floatRange(0.0F, 1.0F).fieldOf("shield_reduction").orElse(DEFAULT.shieldReduction).forGetter(Onslaught::shieldReduction),
				Codec.doubleRange(0.0, 3.0).fieldOf("rush_speed").orElse(DEFAULT.rushSpeed).forGetter(Onslaught::rushSpeed),
				Codec.doubleRange(0.5, 8.0).fieldOf("reach").orElse(DEFAULT.reach).forGetter(Onslaught::reach),
				Codec.doubleRange(0.0, 32.0).fieldOf("from").orElse(DEFAULT.from).forGetter(Onslaught::from),
				Codec.doubleRange(0.0, 32.0).fieldOf("to").orElse(DEFAULT.to).forGetter(Onslaught::to),
				Codec.doubleRange(0.0, 1.0).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Onslaught::chance),
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Onslaught::cooldown),
				Codec.floatRange(1.0F, 10.0F).fieldOf("parry_multiplier").orElse(DEFAULT.parryMultiplier).forGetter(Onslaught::parryMultiplier)
		).apply(instance, Onslaught::new));
	}

	/**
	 * @param damage   damage of the axe
	 * @param minRange nearest target he throws at
	 * @param maxRange farthest target he throws at
	 * @param chance   chance to throw at each check while it is ready
	 * @param cooldown ticks before the next throw
	 * @param speed    flight speed, blocks per tick
	 */
	public record AxeThrow(float damage, double minRange, double maxRange, double chance, int cooldown, double speed) {
		public static final AxeThrow DEFAULT = new AxeThrow(4.0F, 5.0, 12.0, 0.3, 200, 1.3);

		public static final Codec<AxeThrow> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1024.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(AxeThrow::damage),
				Codec.doubleRange(0.0, 64.0).fieldOf("min_range").orElse(DEFAULT.minRange).forGetter(AxeThrow::minRange),
				Codec.doubleRange(0.0, 64.0).fieldOf("max_range").orElse(DEFAULT.maxRange).forGetter(AxeThrow::maxRange),
				Codec.doubleRange(0.0, 1.0).fieldOf("chance").orElse(DEFAULT.chance).forGetter(AxeThrow::chance),
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(AxeThrow::cooldown),
				Codec.doubleRange(0.1, 5.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(AxeThrow::speed)
		).apply(instance, AxeThrow::new));
	}
}
