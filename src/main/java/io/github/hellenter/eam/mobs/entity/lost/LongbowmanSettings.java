package io.github.hellenter.eam.mobs.entity.lost;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The longbowman's numbers, the {@code longbowman} section of {@code data/eam/mobs/lost.json}. Draw time, damage and
 * arrow speed are given against a squad archer's: his bow, at the speed of a skeleton's arrow.
 *
 * @param squadChance      chance that a squad led by one of the Lost brings a longbowman along
 * @param health           his health
 * @param followRange      how far he sees and picks his targets; a squad archer sees 32
 * @param minRange         closer than this he backs away
 * @param maxRange         further than this, or out of sight, he walks closer
 * @param drawTicks        ticks he draws the longbow before a shot
 * @param aimTicks         drawn, he stops, turns to the target and holds still this long before he looses
 * @param cooldown         ticks from a shot to the next draw
 * @param damageMultiplier his arrows hurt this many times more than a squad archer's
 * @param speedMultiplier  and fly this many times faster
 * @param inaccuracy       spread of his shots; a player's full-power shot has 1
 * @param retargetTicks    how often he looks again for the weakest player in sight
 * @param dropChance       chance his longbow drops
 * @param mark             the marking arrow
 * @param barrage          the quick volley that drives back a player who came too close
 * @param powerShot        the rare warned shot with a red arrow that goes through a shield
 */
public record LongbowmanSettings(double squadChance, double health, double followRange, double minRange, double maxRange, int drawTicks,
		int aimTicks, int cooldown, float damageMultiplier, float speedMultiplier, float inaccuracy, int retargetTicks, float dropChance,
		Mark mark, Barrage barrage, PowerShot powerShot) {
	public static final LongbowmanSettings DEFAULT = new LongbowmanSettings(0.3, 23.0, 96.0, 10.0, 40.0, 40, 8, 25, 2.5F, 2.0F, 1.5F, 20,
			0.1F, Mark.DEFAULT, Barrage.DEFAULT, PowerShot.DEFAULT);

	/**
	 * @param every         at most one shot in this many is a marking arrow
	 * @param seconds       how long the mark lasts
	 * @param damageBonus   the marked one takes this much more damage from everything (0.2 = +20%)
	 * @param priorityRange the Lost this close to a marked player in sight leave whoever they fight and go for him
	 */
	public record Mark(int every, float seconds, float damageBonus, double priorityRange) {
		public static final Mark DEFAULT = new Mark(2, 7.0F, 0.2F, 32.0);

		public static final Codec<Mark> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(1, 64).fieldOf("every").orElse(DEFAULT.every).forGetter(Mark::every),
				Codec.floatRange(0.0F, 600.0F).fieldOf("seconds").orElse(DEFAULT.seconds).forGetter(Mark::seconds),
				Codec.floatRange(0.0F, 10.0F).fieldOf("damage_bonus").orElse(DEFAULT.damageBonus).forGetter(Mark::damageBonus),
				Codec.doubleRange(0.0, 128.0).fieldOf("priority_range").orElse(DEFAULT.priorityRange).forGetter(Mark::priorityRange)
		).apply(instance, Mark::new));
	}

	/**
	 * @param range      a target this close sets off the volley
	 * @param arrows     arrows in one volley
	 * @param interval   ticks between them
	 * @param cooldown   ticks from the end of one volley to the next
	 * @param damage     each arrow's damage
	 * @param speed      each arrow's speed, blocks per tick
	 * @param knockback  how hard each arrow pushes back; Punch I is 1
	 * @param inaccuracy spread of the volley
	 */
	public record Barrage(double range, int arrows, int interval, int cooldown, float damage, float speed, float knockback, float inaccuracy) {
		public static final Barrage DEFAULT = new Barrage(5.0, 4, 3, 120, 1.5F, 2.4F, 0.6F, 1.5F);

		public static final Codec<Barrage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.doubleRange(0.0, 32.0).fieldOf("range").orElse(DEFAULT.range).forGetter(Barrage::range),
				Codec.intRange(0, 16).fieldOf("arrows").orElse(DEFAULT.arrows).forGetter(Barrage::arrows),
				Codec.intRange(1, 40).fieldOf("interval").orElse(DEFAULT.interval).forGetter(Barrage::interval),
				Codec.intRange(0, 2400).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(Barrage::cooldown),
				Codec.floatRange(0.0F, 100.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Barrage::damage),
				Codec.floatRange(0.1F, 5.0F).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Barrage::speed),
				Codec.floatRange(0.0F, 10.0F).fieldOf("knockback").orElse(DEFAULT.knockback).forGetter(Barrage::knockback),
				Codec.floatRange(0.0F, 20.0F).fieldOf("inaccuracy").orElse(DEFAULT.inaccuracy).forGetter(Barrage::inaccuracy)
		).apply(instance, Barrage::new));
	}

	/**
	 * A rare shot with a clear warning: he stops, draws with red sparks around him and a warning sound, and looses a very
	 * fast red arrow exactly {@code warningTicks} after the sound. A raised shield lets part of the damage through and is
	 * knocked out for a while.
	 *
	 * @param cooldownMin      fewest ticks from one power shot to the next
	 * @param cooldownMax      most ticks; each time a number in between is picked
	 * @param warningTicks     ticks from the warning sound to the shot
	 * @param minRange         only at a target at least this far away
	 * @param damageMultiplier his red arrow hurts this many times more than his full-draw arrow
	 * @param speed            the red arrow's speed, blocks per tick; his normal arrow flies 3.2
	 * @param shieldMultiplier share of the damage that goes through a raised shield
	 * @param shieldSeconds    how long the shield it hits stays down
	 */
	public record PowerShot(int cooldownMin, int cooldownMax, int warningTicks, double minRange, float damageMultiplier, float speed,
			float shieldMultiplier, float shieldSeconds) {
		public static final PowerShot DEFAULT = new PowerShot(500, 800, 30, 8.0, 1.5F, 5.0F, 0.33F, 5.0F);

		public static final Codec<PowerShot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, 24000).fieldOf("cooldown_min").orElse(DEFAULT.cooldownMin).forGetter(PowerShot::cooldownMin),
				Codec.intRange(0, 24000).fieldOf("cooldown_max").orElse(DEFAULT.cooldownMax).forGetter(PowerShot::cooldownMax),
				Codec.intRange(1, 200).fieldOf("warning_ticks").orElse(DEFAULT.warningTicks).forGetter(PowerShot::warningTicks),
				Codec.doubleRange(0.0, 128.0).fieldOf("min_range").orElse(DEFAULT.minRange).forGetter(PowerShot::minRange),
				Codec.floatRange(0.0F, 20.0F).fieldOf("damage_multiplier").orElse(DEFAULT.damageMultiplier).forGetter(PowerShot::damageMultiplier),
				Codec.floatRange(0.1F, 10.0F).fieldOf("speed").orElse(DEFAULT.speed).forGetter(PowerShot::speed),
				Codec.floatRange(0.0F, 1.0F).fieldOf("shield_multiplier").orElse(DEFAULT.shieldMultiplier).forGetter(PowerShot::shieldMultiplier),
				Codec.floatRange(0.0F, 60.0F).fieldOf("shield_seconds").orElse(DEFAULT.shieldSeconds).forGetter(PowerShot::shieldSeconds)
		).apply(instance, PowerShot::new));
	}

	public static final Codec<LongbowmanSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.doubleRange(0.0, 1.0).fieldOf("squad_chance").orElse(DEFAULT.squadChance).forGetter(LongbowmanSettings::squadChance),
			Codec.doubleRange(1.0, 1024.0).fieldOf("health").orElse(DEFAULT.health).forGetter(LongbowmanSettings::health),
			Codec.doubleRange(1.0, 256.0).fieldOf("follow_range").orElse(DEFAULT.followRange).forGetter(LongbowmanSettings::followRange),
			Codec.doubleRange(0.0, 64.0).fieldOf("min_range").orElse(DEFAULT.minRange).forGetter(LongbowmanSettings::minRange),
			Codec.doubleRange(1.0, 128.0).fieldOf("max_range").orElse(DEFAULT.maxRange).forGetter(LongbowmanSettings::maxRange),
			Codec.intRange(1, 200).fieldOf("draw_ticks").orElse(DEFAULT.drawTicks).forGetter(LongbowmanSettings::drawTicks),
			Codec.intRange(0, 100).fieldOf("aim_ticks").orElse(DEFAULT.aimTicks).forGetter(LongbowmanSettings::aimTicks),
			Codec.intRange(0, 400).fieldOf("cooldown").orElse(DEFAULT.cooldown).forGetter(LongbowmanSettings::cooldown),
			Codec.floatRange(0.0F, 20.0F).fieldOf("damage_multiplier").orElse(DEFAULT.damageMultiplier).forGetter(LongbowmanSettings::damageMultiplier),
			Codec.floatRange(0.1F, 4.0F).fieldOf("speed_multiplier").orElse(DEFAULT.speedMultiplier).forGetter(LongbowmanSettings::speedMultiplier),
			Codec.floatRange(0.0F, 20.0F).fieldOf("inaccuracy").orElse(DEFAULT.inaccuracy).forGetter(LongbowmanSettings::inaccuracy),
			Codec.intRange(1, 400).fieldOf("retarget_ticks").orElse(DEFAULT.retargetTicks).forGetter(LongbowmanSettings::retargetTicks),
			Codec.floatRange(0.0F, 1.0F).fieldOf("drop_chance").orElse(DEFAULT.dropChance).forGetter(LongbowmanSettings::dropChance),
			Mark.CODEC.fieldOf("mark").orElse(DEFAULT.mark).forGetter(LongbowmanSettings::mark),
			Barrage.CODEC.fieldOf("barrage").orElse(DEFAULT.barrage).forGetter(LongbowmanSettings::barrage),
			PowerShot.CODEC.fieldOf("power_shot").orElse(DEFAULT.powerShot).forGetter(LongbowmanSettings::powerShot)
	).apply(instance, LongbowmanSettings::new));
}
