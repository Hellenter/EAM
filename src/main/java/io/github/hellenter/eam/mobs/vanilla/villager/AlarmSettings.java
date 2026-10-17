package io.github.hellenter.eam.mobs.vanilla.villager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for the village alarm, read from {@code data/eam/villagers/alarm.json} so datapacks can override them.
 *
 * @param bellRadius       how far, in blocks, a hurt villager looks for a bell, and how far the alarm reaches from it
 * @param runSpeed         walk speed of the villager running to the bell, as a multiple of his speed
 * @param runTimeoutTicks  ticks he tries to reach the bell before giving up
 * @param ringReach        how close, in blocks, he must be to ring it
 * @param rings            times he strikes the bell
 * @param ringIntervalTicks ticks between strikes
 * @param alarmTicks       ticks the alarm lasts after the bell rings or another villager near it is hurt
 * @param gatherDistance   how close, in blocks, guards gather around the bell
 * @param gatherSpeed      walk speed of guards and clerics answering the alarm, as a multiple of their speed
 * @param clericHeal       health a cleric gives a hurt guard he reaches during the alarm
 * @param clericRestTicks  ticks a cleric waits after tending a guard before he goes to the next
 */
public record AlarmSettings(int bellRadius, double runSpeed, int runTimeoutTicks, double ringReach, int rings, int ringIntervalTicks,
		int alarmTicks, double gatherDistance, double gatherSpeed, float clericHeal, int clericRestTicks) {
	public static final AlarmSettings DEFAULT = new AlarmSettings(48, 0.8, 400, 2.5, 3, 15, 1200, 5.0, 0.8, 4.0F, 40);

	public static final Codec<AlarmSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(4, 128).fieldOf("bell_radius").orElse(DEFAULT.bellRadius).forGetter(AlarmSettings::bellRadius),
			Codec.doubleRange(0.1, 4.0).fieldOf("run_speed").orElse(DEFAULT.runSpeed).forGetter(AlarmSettings::runSpeed),
			Codec.intRange(20, 6000).fieldOf("run_timeout_ticks").orElse(DEFAULT.runTimeoutTicks).forGetter(AlarmSettings::runTimeoutTicks),
			Codec.doubleRange(1.0, 8.0).fieldOf("ring_reach").orElse(DEFAULT.ringReach).forGetter(AlarmSettings::ringReach),
			Codec.intRange(1, 20).fieldOf("rings").orElse(DEFAULT.rings).forGetter(AlarmSettings::rings),
			Codec.intRange(1, 200).fieldOf("ring_interval_ticks").orElse(DEFAULT.ringIntervalTicks).forGetter(AlarmSettings::ringIntervalTicks),
			Codec.intRange(20, 72000).fieldOf("alarm_ticks").orElse(DEFAULT.alarmTicks).forGetter(AlarmSettings::alarmTicks),
			Codec.doubleRange(1.0, 32.0).fieldOf("gather_distance").orElse(DEFAULT.gatherDistance).forGetter(AlarmSettings::gatherDistance),
			Codec.doubleRange(0.1, 4.0).fieldOf("gather_speed").orElse(DEFAULT.gatherSpeed).forGetter(AlarmSettings::gatherSpeed),
			Codec.floatRange(0.0F, 1000.0F).fieldOf("cleric_heal").orElse(DEFAULT.clericHeal).forGetter(AlarmSettings::clericHeal),
			Codec.intRange(1, 1200).fieldOf("cleric_rest_ticks").orElse(DEFAULT.clericRestTicks).forGetter(AlarmSettings::clericRestTicks)
	).apply(instance, AlarmSettings::new));
}
