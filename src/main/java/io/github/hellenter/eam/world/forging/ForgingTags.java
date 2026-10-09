package io.github.hellenter.eam.world.forging;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.List;

/** Item tags for the anvil. */
public final class ForgingTags {
	/** Hammers that go in the anvil's hammer slot: every hammer of the tier tags below. */
	public static final TagKey<Item> HAMMERS = tag("anvil_hammers");
	/** Hammers by tier: copper and silver 2, iron 3, mithril 4. Tier 1 is empty for now. */
	public static final List<TagKey<Item>> HAMMER_TIERS = List.of(
			tag("anvil_hammers/tier_1"), tag("anvil_hammers/tier_2"), tag("anvil_hammers/tier_3"), tag("anvil_hammers/tier_4"));

	private ForgingTags() {
	}

	private static TagKey<Item> tag(String path) {
		return TagKey.create(Registries.ITEM, Eam.id(path));
	}
}
