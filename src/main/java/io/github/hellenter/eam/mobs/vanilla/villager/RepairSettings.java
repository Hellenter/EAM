package io.github.hellenter.eam.mobs.vanilla.villager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for villagers mending their gear, read from {@code data/eam/villagers/repair.json} so datapacks can
 * override them.
 *
 * @param intervalTicks    how often, in ticks, a villager checks his gear
 * @param villageRadius    how far, in blocks, a master can be and still count as in the same village
 * @param clientsPerMaster how many villagers with worn gear one master serves; more need another master
 * @param repairShare      share of an item's full durability one check mends
 */
public record RepairSettings(int intervalTicks, int villageRadius, int clientsPerMaster, double repairShare) {
	public static final RepairSettings DEFAULT = new RepairSettings(1200, 48, 12, 0.25);

	public static final Codec<RepairSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(20, 1_000_000).fieldOf("interval_ticks").orElse(DEFAULT.intervalTicks).forGetter(RepairSettings::intervalTicks),
			Codec.intRange(1, 256).fieldOf("village_radius").orElse(DEFAULT.villageRadius).forGetter(RepairSettings::villageRadius),
			Codec.intRange(1, 1000).fieldOf("clients_per_master").orElse(DEFAULT.clientsPerMaster).forGetter(RepairSettings::clientsPerMaster),
			Codec.doubleRange(0.0, 1.0).fieldOf("repair_share").orElse(DEFAULT.repairShare).forGetter(RepairSettings::repairShare)
	).apply(instance, RepairSettings::new));
}
