package io.github.hellenter.eam.mobs.entity.lost;

import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightSettings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for the Lost, read from {@code data/eam/mobs/lost.json} so datapacks can override them. A missing or
 * invalid field falls back to its default. Melee fighting uses the skeleton knight's numbers, one set for the leader and
 * one for his followers.
 *
 * @param leader        the leader's sword fighting
 * @param follower      the followers' sword and spear fighting
 * @param spearReach    how far a spearman's jab reaches, flat from his centre to the target's
 * @param guardDistance shield bearers stand this far from the player, on the side of their archers
 * @param squad         how big squads are and how often they spawn
 * @param archer        how archers keep their distance and shoot
 * @param centurion     the centurion who comes with some squads
 * @param commanderHorn the commander's horn and the Lost who serve whoever carries it
 * @param shieldWall    the shield wall: spearmen in a row in front of their crossbow commander
 * @param longbowman    the longbowman who comes with some squads and marks the player for them
 * @param straggler     the lone straggler of the stone wastes, who calls a squad when a fight drags on
 */
public record LostSettings(SkeletonKnightSettings leader, SkeletonKnightSettings follower, double spearReach, double guardDistance,
		Squad squad, Archer archer, LostCenturionSettings centurion, CommanderHorn commanderHorn, ShieldWallSettings shieldWall,
		LongbowmanSettings longbowman, LostStragglerSettings straggler) {
	public static final SkeletonKnightSettings LEADER_DEFAULT = new SkeletonKnightSettings(10, 2.7, 12.0, 1.0F,
			0.45, 80, 0.6F, 0.2, 0.6,
			160, 1.7F, 0.5, 0.5F, 4.0F,
			0.55, new SkeletonKnightSettings.Dash(220, 4, 0.85));
	public static final SkeletonKnightSettings FOLLOWER_DEFAULT = new SkeletonKnightSettings(24, 2.6, 12.0, 1.0F,
			0.15, 140, 0.6F, 0.0, 0.0,
			400, 1.5F, 0.5, 0.5F, 3.0F,
			0.5, new SkeletonKnightSettings.Dash(300, 3, 0.8));
	public static final LostSettings DEFAULT = new LostSettings(LEADER_DEFAULT, FOLLOWER_DEFAULT, 3.4, 2.6, Squad.DEFAULT, Archer.DEFAULT,
			LostCenturionSettings.DEFAULT, CommanderHorn.DEFAULT, ShieldWallSettings.DEFAULT, LongbowmanSettings.DEFAULT,
			LostStragglerSettings.DEFAULT);

	/**
	 * @param minFollowers    fewest followers a leader brings (a squad is one more)
	 * @param maxFollowers    most followers
	 * @param leaderHealth    the leader's health
	 * @param followerHealth  a follower's health
	 * @param spawnSpacing    no squad spawns naturally this close to another of the Lost
	 * @param spawnCooldownMin after a squad spawns naturally near a player, no other spawns near them for at least this many ticks
	 * @param spawnCooldownMax and at most this many
	 * @param leaderDropChance   chance for each piece of the leader's gear to drop, battered
	 * @param followerDropChance the same for a follower
	 */
	public record Squad(int minFollowers, int maxFollowers, double leaderHealth, double followerHealth, double spawnSpacing,
			int spawnCooldownMin, int spawnCooldownMax, float leaderDropChance, float followerDropChance) {
		public static final Squad DEFAULT = new Squad(2, 4, 33.0, 23.0, 64.0, 2400, 4800, 0.1F, 0.04F);

		public static final Codec<Squad> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, 8).fieldOf("min_followers").orElse(DEFAULT.minFollowers).forGetter(Squad::minFollowers),
				Codec.intRange(0, 8).fieldOf("max_followers").orElse(DEFAULT.maxFollowers).forGetter(Squad::maxFollowers),
				Codec.doubleRange(1.0, 1024.0).fieldOf("leader_health").orElse(DEFAULT.leaderHealth).forGetter(Squad::leaderHealth),
				Codec.doubleRange(1.0, 1024.0).fieldOf("follower_health").orElse(DEFAULT.followerHealth).forGetter(Squad::followerHealth),
				Codec.doubleRange(0.0, 256.0).fieldOf("spawn_spacing").orElse(DEFAULT.spawnSpacing).forGetter(Squad::spawnSpacing),
				Codec.intRange(0, 72000).fieldOf("spawn_cooldown_min").orElse(DEFAULT.spawnCooldownMin).forGetter(Squad::spawnCooldownMin),
				Codec.intRange(0, 72000).fieldOf("spawn_cooldown_max").orElse(DEFAULT.spawnCooldownMax).forGetter(Squad::spawnCooldownMax),
				Codec.floatRange(0.0F, 1.0F).fieldOf("leader_drop_chance").orElse(DEFAULT.leaderDropChance).forGetter(Squad::leaderDropChance),
				Codec.floatRange(0.0F, 1.0F).fieldOf("follower_drop_chance").orElse(DEFAULT.followerDropChance)
						.forGetter(Squad::followerDropChance)
		).apply(instance, Squad::new));
	}

	/**
	 * @param minRange  closer than this the archer backs away from the player
	 * @param maxRange  further than this, or out of sight, he walks closer
	 * @param drawTicks ticks he draws the bow before a shot
	 * @param cooldown  ticks between the end of one shot and the next draw
	 * @param aimTicks  with the bow drawn he stops, turns to face the target and holds still this long before he looses
	 */
	public record Archer(double minRange, double maxRange, int drawTicks, int cooldown, int aimTicks) {
		public static final Archer DEFAULT = new Archer(7.0, 15.0, 20, 25, 6);

		public static final Codec<Archer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(0.0, 32.0).fieldOf("min_range").orElse(DEFAULT.minRange).forGetter(Archer::minRange),
				Codec.doubleRange(1.0, 48.0).fieldOf("max_range").orElse(DEFAULT.maxRange).forGetter(Archer::maxRange),
				Codec.intRange(1, 100).fieldOf("draw_ticks").orElse(DEFAULT.drawTicks).forGetter(Archer::drawTicks),
				Codec.intRange(0, 400).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Archer::cooldown),
				Codec.intRange(0, 60).fieldOf("aim_ticks").orElse(DEFAULT.aimTicks).forGetter(Archer::aimTicks)
		).apply(instance, Archer::new));
	}

	/**
	 * @param cooldown    ticks between blasts of the horn
	 * @param minDistance a called wave comes this far from the player at least
	 * @param maxDistance and this far at most
	 * @param guardRange  the Lost serve a player with the horn this close
	 * @param enemyRange  they go for hostile mobs this close to themselves
	 * @param memoryTicks they go for whoever the player hit, or was hit by, this many ticks ago at most
	 */
	public record CommanderHorn(int cooldown, double minDistance, double maxDistance, double guardRange, double enemyRange, int memoryTicks) {
		public static final CommanderHorn DEFAULT = new CommanderHorn(40, 5.0, 10.0, 32.0, 16.0, 200);

		public static final Codec<CommanderHorn> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, 24000).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(CommanderHorn::cooldown),
				Codec.doubleRange(0.0, 64.0).fieldOf("min_distance").orElse(DEFAULT.minDistance).forGetter(CommanderHorn::minDistance),
				Codec.doubleRange(1.0, 64.0).fieldOf("max_distance").orElse(DEFAULT.maxDistance).forGetter(CommanderHorn::maxDistance),
				Codec.doubleRange(1.0, 128.0).fieldOf("guard_range").orElse(DEFAULT.guardRange).forGetter(CommanderHorn::guardRange),
				Codec.doubleRange(1.0, 64.0).fieldOf("enemy_range").orElse(DEFAULT.enemyRange).forGetter(CommanderHorn::enemyRange),
				Codec.intRange(1, 24000).fieldOf("memory_ticks").orElse(DEFAULT.memoryTicks).forGetter(CommanderHorn::memoryTicks)
		).apply(instance, CommanderHorn::new));
	}

	public static final Codec<LostSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			SkeletonKnightSettings.CODEC.fieldOf("leader").orElse(DEFAULT.leader).forGetter(LostSettings::leader),
			SkeletonKnightSettings.CODEC.fieldOf("follower").orElse(DEFAULT.follower).forGetter(LostSettings::follower),
			Codec.doubleRange(1.0, 6.0).fieldOf("spear_reach").orElse(DEFAULT.spearReach).forGetter(LostSettings::spearReach),
			Codec.doubleRange(1.0, 8.0).fieldOf("guard_distance").orElse(DEFAULT.guardDistance).forGetter(LostSettings::guardDistance),
			Squad.CODEC.fieldOf("squad").orElse(DEFAULT.squad).forGetter(LostSettings::squad),
			Archer.CODEC.fieldOf("archer").orElse(DEFAULT.archer).forGetter(LostSettings::archer),
			LostCenturionSettings.CODEC.fieldOf("centurion").orElse(DEFAULT.centurion).forGetter(LostSettings::centurion),
			CommanderHorn.CODEC.fieldOf("commander_horn").orElse(DEFAULT.commanderHorn).forGetter(LostSettings::commanderHorn),
			ShieldWallSettings.CODEC.fieldOf("shield_wall").orElse(DEFAULT.shieldWall).forGetter(LostSettings::shieldWall),
			LongbowmanSettings.CODEC.fieldOf("longbowman").orElse(DEFAULT.longbowman).forGetter(LostSettings::longbowman),
			LostStragglerSettings.CODEC.fieldOf("straggler").orElse(DEFAULT.straggler).forGetter(LostSettings::straggler)
	).apply(instance, LostSettings::new));
}
