package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * What a player's items are worth to a village when he gives them to the elder from the upgrades menu, read from
 * {@code data/eam/villagers/donations.json} so datapacks can override them. An item counts by the first entry whose tag
 * holds it.
 */
public record DonationSettings(List<Donation> donations) {
	/**
	 * @param items    the items this entry takes
	 * @param resource the resource they become
	 * @param value    how much of it each item is
	 */
	public record Donation(TagKey<Item> items, VillageResource resource, double value) {
		public static final Codec<Donation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				TagKey.codec(Registries.ITEM).fieldOf("items").forGetter(Donation::items),
				VillageResource.CODEC.fieldOf("resource").forGetter(Donation::resource),
				Codec.doubleRange(0.0, 1000.0).fieldOf("value").forGetter(Donation::value)
		).apply(instance, Donation::new));
	}

	public static final DonationSettings DEFAULT = new DonationSettings(List.of(
			new Donation(tag("wood_logs"), VillageResource.WOOD, 1.0),
			new Donation(tag("wood_planks"), VillageResource.WOOD, 0.25),
			new Donation(tag("metal_ingots"), VillageResource.METAL, 1.0),
			new Donation(tag("metal_soft_ingots"), VillageResource.METAL, 0.5),
			new Donation(tag("metal_nuggets"), VillageResource.METAL, 0.1),
			new Donation(tag("stone"), VillageResource.STONE, 0.25),
			new Donation(tag("money_gems"), VillageResource.MONEY, 2.0),
			new Donation(tag("money_gold"), VillageResource.MONEY, 1.0),
			new Donation(tag("money_nuggets"), VillageResource.MONEY, 0.1),
			new Donation(tag("provisions_meals"), VillageResource.PROVISIONS, 1.0),
			new Donation(tag("provisions_crops"), VillageResource.PROVISIONS, 0.25)));

	public static final Codec<DonationSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Donation.CODEC.listOf().fieldOf("donations").orElse(DEFAULT.donations).forGetter(DonationSettings::donations)
	).apply(instance, DonationSettings::new));

	/** The tag {@code eam:village_donations/<name>}. */
	public static TagKey<Item> tag(String name) {
		return TagKey.create(Registries.ITEM, Eam.id("village_donations/" + name));
	}

	/** The entry that takes the stack, if any. */
	public Optional<Donation> of(ItemStack stack) {
		if (stack.isEmpty()) return Optional.empty();
		return donations.stream().filter(donation -> stack.is(donation.items())).findFirst();
	}
}
