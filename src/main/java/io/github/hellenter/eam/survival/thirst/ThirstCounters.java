package io.github.hellenter.eam.survival.thirst;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Server-only counters that change every tick, kept apart from {@link Thirst} so they are never synced.
 *
 * @param exhaustion   accumulated thirst exhaustion; every {@link ThirstSettings#exhaustionPerPoint()} costs one water
 * @param damageTimer  ticks spent at zero water since the last dehydration damage, or since the last refill tick
 *                     in peaceful
 * @param sipCooldown  ticks until the player may sip from a water source again
 * @param immunity     ticks left during which sips from a water source cannot poison; held at full while poisoned
 */
public record ThirstCounters(float exhaustion, int damageTimer, int sipCooldown, int immunity) {
	public static final ThirstCounters ZERO = new ThirstCounters(0.0F, 0, 0, 0);

	public static final Codec<ThirstCounters> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.FLOAT.optionalFieldOf("exhaustion", 0.0F).forGetter(ThirstCounters::exhaustion),
			Codec.INT.optionalFieldOf("damage_timer", 0).forGetter(ThirstCounters::damageTimer),
			Codec.INT.optionalFieldOf("sip_cooldown", 0).forGetter(ThirstCounters::sipCooldown),
			Codec.INT.optionalFieldOf("immunity", 0).forGetter(ThirstCounters::immunity)
	).apply(instance, ThirstCounters::new));

	public ThirstCounters withExhaustion(float value) {
		return new ThirstCounters(value, damageTimer, sipCooldown, immunity);
	}

	public ThirstCounters withSipCooldown(int value) {
		return new ThirstCounters(exhaustion, damageTimer, value, immunity);
	}
}
