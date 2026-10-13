package io.github.hellenter.eam.mobs.entity.lost;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The shield wall's numbers, the {@code shield_wall} section of {@code data/eam/mobs/lost.json}. Spearmen and their
 * commander have the health and spear fighting of the Lost's followers and leader.
 *
 * @param spearmen      how many spearmen a commander brings
 * @param spacing       flat distance between neighbours in the row, centre to centre
 * @param frontDistance the row stands this far in front of the commanders, towards the enemy, or halfway to it when closer
 * @param joinRange     commanders this close to each other, fighting the same enemy, put their spearmen in one row
 * @param crossbow      the commander's shooting
 */
public record ShieldWallSettings(int spearmen, double spacing, double frontDistance, double joinRange, Crossbow crossbow) {
	public static final ShieldWallSettings DEFAULT = new ShieldWallSettings(4, 0.85, 3.0, 24.0, Crossbow.DEFAULT);

	/**
	 * @param minRange   closer than this the commander backs away from the enemy
	 * @param maxRange   further than this, or out of sight, he walks closer
	 * @param loadTicks  ticks he loads the crossbow
	 * @param aimTicks   loaded, he stops, turns to the enemy and holds his aim this long before he shoots
	 * @param cooldown   ticks from a shot to the next loading
	 * @param damage     a bolt's damage
	 * @param speed      a bolt's speed, blocks per tick
	 * @param inaccuracy spread of his shots; a player's full-power shot has 1
	 */
	public record Crossbow(double minRange, double maxRange, int loadTicks, int aimTicks, int cooldown, float damage, float speed,
			float inaccuracy) {
		public static final Crossbow DEFAULT = new Crossbow(8.0, 20.0, 25, 8, 30, 5.0F, 2.0F, 2.0F);

		public static final Codec<Crossbow> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(0.0, 32.0).fieldOf("min_range").orElse(DEFAULT.minRange).forGetter(Crossbow::minRange),
				Codec.doubleRange(1.0, 64.0).fieldOf("max_range").orElse(DEFAULT.maxRange).forGetter(Crossbow::maxRange),
				Codec.intRange(1, 200).fieldOf("load_ticks").orElse(DEFAULT.loadTicks).forGetter(Crossbow::loadTicks),
				Codec.intRange(0, 100).fieldOf("aim_ticks").orElse(DEFAULT.aimTicks).forGetter(Crossbow::aimTicks),
				Codec.intRange(0, 400).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Crossbow::cooldown),
				Codec.floatRange(0.0F, 100.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Crossbow::damage),
				Codec.floatRange(0.1F, 5.0F).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Crossbow::speed),
				Codec.floatRange(0.0F, 20.0F).fieldOf("inaccuracy").orElse(DEFAULT.inaccuracy).forGetter(Crossbow::inaccuracy)
		).apply(instance, Crossbow::new));
	}

	public static final Codec<ShieldWallSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(0, 12).fieldOf("spearmen").orElse(DEFAULT.spearmen).forGetter(ShieldWallSettings::spearmen),
			Codec.doubleRange(0.5, 3.0).fieldOf("spacing").orElse(DEFAULT.spacing).forGetter(ShieldWallSettings::spacing),
			Codec.doubleRange(0.0, 16.0).fieldOf("front_distance").orElse(DEFAULT.frontDistance).forGetter(ShieldWallSettings::frontDistance),
			Codec.doubleRange(0.0, 64.0).fieldOf("join_range").orElse(DEFAULT.joinRange).forGetter(ShieldWallSettings::joinRange),
			Crossbow.CODEC.fieldOf("crossbow").orElse(DEFAULT.crossbow).forGetter(ShieldWallSettings::crossbow)
	).apply(instance, ShieldWallSettings::new));
}
