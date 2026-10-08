package io.github.hellenter.eam.world.crafting;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.List;

/** Item tags for crafting. The craft time tags are only referenced by the default {@code times.json}. */
public final class CraftingTags {
	/** Results that take 10 seconds: the crafting table and heavy stations. */
	public static final TagKey<Item> TEN_SECONDS = tag("craft_time/ten_seconds");
	/** Armor: every item in the recipe adds 5 seconds. */
	public static final TagKey<Item> ARMOR = tag("craft_time/armor");
	/** Results that take 5 seconds: tools, fires, workstations, containers, redstone machines, vehicles. */
	public static final TagKey<Item> FIVE_SECONDS = tag("craft_time/five_seconds");
	/** Tools that go in the crafting table's tool slots: every tool of the tier tags below. */
	public static final TagKey<Item> CRAFTING_TABLE_TOOLS = tag("crafting_table_tools");
	/** Table tools by tier: flint 1, copper and silver 2, iron 3. */
	public static final List<TagKey<Item>> TOOL_TIERS = List.of(
			tag("crafting_table_tools/tier_1"), tag("crafting_table_tools/tier_2"), tag("crafting_table_tools/tier_3"));
	/** Materials by tier; tier 1 (flint) needs no tool beyond the one any table craft needs. */
	public static final List<TagKey<Item>> MATERIAL_TIERS = List.of(
			tag("crafting_materials/tier_1"), tag("crafting_materials/tier_2"), tag("crafting_materials/tier_3"));
	/** Results crafted from metal with no tool tier needed: ingots, nuggets and storage blocks. */
	public static final TagKey<Item> TIER_FREE = tag("crafting_materials/tier_free");

	private CraftingTags() {
	}

	private static TagKey<Item> tag(String path) {
		return TagKey.create(Registries.ITEM, Eam.id(path));
	}
}
