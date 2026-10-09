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
	/** Tools that go in the crafting table's tool slots: the knives and saws below and the anvil hammers. */
	public static final TagKey<Item> CRAFTING_TABLE_TOOLS = tag("crafting_table_tools");
	/** Knives by tier: flint 1, copper and silver 2, iron 3. The table needs one; they speed up every craft. */
	public static final List<TagKey<Item>> KNIFE_TIERS = List.of(
			tag("crafting_table_knives/tier_1"), tag("crafting_table_knives/tier_2"), tag("crafting_table_knives/tier_3"));
	/** Saws by tier: copper and silver 2, iron 3. They speed up crafting {@link #SAW_RESULTS}. */
	public static final List<TagKey<Item>> SAW_TIERS = List.of(
			tag("crafting_table_saws/tier_1"), tag("crafting_table_saws/tier_2"), tag("crafting_table_saws/tier_3"));
	/** Wooden and decorative results, which a saw in the table crafts faster. */
	public static final TagKey<Item> SAW_RESULTS = tag("craft_bonus/saw_results");
	/** Metal ingredients: a grid holding one is metalwork, which a hammer in the table crafts faster. */
	public static final TagKey<Item> METALS = tag("craft_bonus/metals");
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
