package io.github.hellenter.eam.mobs.vanilla.villager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * What the elder keeps for his village: the resources his villagers brought and the order they save up for, if any.
 * Immutable; every change makes a new one.
 *
 * @param stock what the village has of each resource
 * @param goal  the order the village saves up for: it is carried out by itself once the stock covers it
 */
public record VillageTreasury(Map<VillageResource, Double> stock, Optional<String> goal) {
	public static final VillageTreasury EMPTY = new VillageTreasury(Map.of(), Optional.empty());

	public static final Codec<VillageTreasury> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.unboundedMap(VillageResource.CODEC, Codec.DOUBLE).optionalFieldOf("stock", Map.of()).forGetter(VillageTreasury::stock),
			Codec.STRING.optionalFieldOf("goal").forGetter(VillageTreasury::goal)
	).apply(instance, VillageTreasury::new));

	public double get(VillageResource resource) {
		return stock.getOrDefault(resource, 0.0);
	}

	/** This treasury with {@code amounts} added to the stock; a resource never drops below nothing. */
	public VillageTreasury add(Map<VillageResource, Double> amounts) {
		Map<VillageResource, Double> next = new EnumMap<>(VillageResource.class);
		next.putAll(stock);
		amounts.forEach((resource, amount) -> next.put(resource, Math.max(0.0, get(resource) + amount)));
		return new VillageTreasury(Map.copyOf(next), goal);
	}

	/** This treasury with {@code cost} taken from the stock. */
	public VillageTreasury pay(Map<VillageResource, Double> cost) {
		Map<VillageResource, Double> negative = new EnumMap<>(VillageResource.class);
		cost.forEach((resource, amount) -> negative.put(resource, -amount));
		return add(negative);
	}

	public VillageTreasury withGoal(Optional<String> next) {
		return new VillageTreasury(stock, next);
	}
}
