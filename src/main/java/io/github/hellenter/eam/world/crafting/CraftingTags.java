package io.github.hellenter.eam.world.crafting;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Item tags for crafting. The craft time tags are only referenced by the default {@code times.json}. */
public final class CraftingTags {
	/** Results that take 10 seconds: the crafting table and heavy stations. */
	public static final TagKey<Item> TEN_SECONDS = tag("craft_time/ten_seconds");
	/** Armor: every item in the recipe adds 5 seconds. */
	public static final TagKey<Item> ARMOR = tag("craft_time/armor");
	/** Results that take 5 seconds: tools, fires, workstations, containers, redstone machines, vehicles. */
	public static final TagKey<Item> FIVE_SECONDS = tag("craft_time/five_seconds");
	/** Tools that go in the crafting table's tool slot. Tier 1 is the flint knife; higher tiers come later. */
	public static final TagKey<Item> CRAFTING_TABLE_TOOLS = tag("crafting_table_tools");

	private CraftingTags() {
	}

	private static TagKey<Item> tag(String path) {
		return TagKey.create(Registries.ITEM, Eam.id(path));
	}
}
