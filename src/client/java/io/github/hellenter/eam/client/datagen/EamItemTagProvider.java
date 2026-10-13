package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.ancient.AncientMetalGear;
import io.github.hellenter.eam.content.brewing.BrewingModule;
import io.github.hellenter.eam.content.bucket.BucketContent;
import io.github.hellenter.eam.content.bucket.MetalBuckets;
import io.github.hellenter.eam.content.campfire.CampFireModule;
import io.github.hellenter.eam.content.mithril.MithrilGear;
import io.github.hellenter.eam.content.silver.SilverGear;
import io.github.hellenter.eam.content.tools.FlintTools;
import io.github.hellenter.eam.dimension.UnderworldDimension;
import io.github.hellenter.eam.world.crafting.CraftingTags;
import io.github.hellenter.eam.world.forging.ForgingTags;
import io.github.hellenter.eam.world.mining.HandBreakingModule;
import io.github.hellenter.eam.world.mithril.MithrilModule;
import io.github.hellenter.eam.world.smelting.BlastFurnaceModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class EamItemTagProvider extends FabricTagsProvider.ItemTagsProvider {
	public EamItemTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		builder(EamItems.WATER_BOWLS).add(EamItems.WATER_BOWL.builtInRegistryHolder().key(),
				EamItems.BOILED_WATER_BOWL.builtInRegistryHolder().key());
		// Metal buckets join the conventional bucket tags, so recipes asking for a bucket of milk or water take any metal.
		Map<BucketContent, TagKey<Item>> bucketTags = Map.of(BucketContent.EMPTY, ConventionalItemTags.EMPTY_BUCKETS,
				BucketContent.WATER, ConventionalItemTags.WATER_BUCKETS, BucketContent.LAVA, ConventionalItemTags.LAVA_BUCKETS,
				BucketContent.POWDER_SNOW, ConventionalItemTags.POWDER_SNOW_BUCKETS, BucketContent.MILK, ConventionalItemTags.MILK_BUCKETS);
		for (Map<BucketContent, Item> set : MetalBuckets.metalSets()) {
			set.forEach((content, item) -> builder(bucketTags.get(content)).add(item.builtInRegistryHolder().key()));
		}
		builder(EamItems.FLASK_THREADS).add(EamItems.LEATHER_THREAD.builtInRegistryHolder().key(), Items.STRING.builtInRegistryHolder().key());
		builder(CampFireModule.IGNITERS).add(
				Items.FLINT_AND_STEEL.builtInRegistryHolder().key(),
				Items.FIRE_CHARGE.builtInRegistryHolder().key(),
				Items.FLINT.builtInRegistryHolder().key(),
				EamItems.FLINT_SHARD.builtInRegistryHolder().key());
		builder(FlintTools.REPAIR_MATERIALS).add(Items.FLINT.builtInRegistryHolder().key());
		builder(ItemTags.AXES).add(EamItems.FLINT_HATCHET.builtInRegistryHolder().key(), EamItems.FLINT_AXE.builtInRegistryHolder().key(),
				EamItems.SILVER_AXE.builtInRegistryHolder().key());

		// Axes take the melee weapon enchantments too: looting, knockback, fire aspect.
		builder(ItemTags.MELEE_WEAPON_ENCHANTABLE).addOptionalTag(ItemTags.AXES);

		// Logs break only with these; a battle axe goes here too.
		builder(HandBreakingModule.CHOPS_LOGS).forceAddTag(ItemTags.AXES);

		// The only thing that breaks the underworld's deep bedrock.
		builder(UnderworldDimension.BREAKS_DEEP_BEDROCK).add(EamItems.BEDROCK_BREAKER.builtInRegistryHolder().key());

		// Bows, crossbows and dispensers take anything in this tag as arrows.
		builder(ItemTags.ARROWS).addAll(keys(EamItems.COPPER_ARROW, EamItems.SILVER_ARROW, EamItems.IRON_ARROW, EamItems.ANCIENT_METAL_ARROW,
				EamItems.MITHRIL_ARROW));

		builder(SilverGear.TOOL_MATERIALS).add(EamItems.SILVER_INGOT.builtInRegistryHolder().key());
		builder(SilverGear.REPAIRS_ARMOR).add(EamItems.SILVER_INGOT.builtInRegistryHolder().key());
		// Vanilla tool and armor tags: enchanting, and the 5 s craft times below.
		builder(ItemTags.SWORDS).add(EamItems.SILVER_SWORD.builtInRegistryHolder().key());
		builder(ItemTags.SHOVELS).add(EamItems.SILVER_SHOVEL.builtInRegistryHolder().key());
		builder(ItemTags.PICKAXES).add(EamItems.SILVER_PICKAXE.builtInRegistryHolder().key());
		builder(ItemTags.HOES).add(EamItems.SILVER_HOE.builtInRegistryHolder().key());
		builder(ItemTags.HEAD_ARMOR).add(EamItems.SILVER_HELMET.builtInRegistryHolder().key());
		builder(ItemTags.CHEST_ARMOR).add(EamItems.SILVER_CHESTPLATE.builtInRegistryHolder().key());
		builder(ItemTags.LEG_ARMOR).add(EamItems.SILVER_LEGGINGS.builtInRegistryHolder().key());
		builder(ItemTags.FOOT_ARMOR).add(EamItems.SILVER_BOOTS.builtInRegistryHolder().key());

		builder(BlastFurnaceModule.INGOTS).addAll(keys(Items.IRON_INGOT, Items.COPPER_INGOT, EamItems.SILVER_INGOT, EamItems.MITHRIL_INGOT));

		builder(MithrilGear.TOOL_MATERIALS).add(EamItems.MITHRIL_INGOT.builtInRegistryHolder().key());
		builder(MithrilGear.REPAIRS_ARMOR).add(EamItems.MITHRIL_INGOT.builtInRegistryHolder().key());
		builder(ItemTags.SWORDS).add(EamItems.MITHRIL_SWORD.builtInRegistryHolder().key());
		builder(ItemTags.SHOVELS).add(EamItems.MITHRIL_SHOVEL.builtInRegistryHolder().key());
		builder(ItemTags.PICKAXES).add(EamItems.MITHRIL_PICKAXE.builtInRegistryHolder().key());
		builder(ItemTags.AXES).add(EamItems.MITHRIL_AXE.builtInRegistryHolder().key());
		builder(ItemTags.HOES).add(EamItems.MITHRIL_HOE.builtInRegistryHolder().key());
		builder(ItemTags.HEAD_ARMOR).add(EamItems.MITHRIL_HELMET.builtInRegistryHolder().key());
		builder(ItemTags.CHEST_ARMOR).add(EamItems.MITHRIL_CHESTPLATE.builtInRegistryHolder().key());
		builder(ItemTags.LEG_ARMOR).add(EamItems.MITHRIL_LEGGINGS.builtInRegistryHolder().key());
		builder(ItemTags.FOOT_ARMOR).add(EamItems.MITHRIL_BOOTS.builtInRegistryHolder().key());

		builder(ItemTags.SWORDS).add(EamItems.RUSTY_IRON_SWORD.builtInRegistryHolder().key());
		builder(ItemTags.SHOVELS).add(EamItems.RUSTY_IRON_SHOVEL.builtInRegistryHolder().key());
		builder(ItemTags.PICKAXES).add(EamItems.RUSTY_IRON_PICKAXE.builtInRegistryHolder().key());
		builder(ItemTags.AXES).add(EamItems.RUSTY_IRON_AXE.builtInRegistryHolder().key());
		builder(ItemTags.HOES).add(EamItems.RUSTY_IRON_HOE.builtInRegistryHolder().key());
		builder(ItemTags.HEAD_ARMOR).addAll(keys(EamItems.RUSTY_IRON_HELMET, EamItems.RUSTY_CHAINMAIL_HELMET));
		builder(ItemTags.CHEST_ARMOR).addAll(keys(EamItems.RUSTY_IRON_CHESTPLATE, EamItems.RUSTY_CHAINMAIL_CHESTPLATE));
		builder(ItemTags.LEG_ARMOR).addAll(keys(EamItems.RUSTY_IRON_LEGGINGS, EamItems.RUSTY_CHAINMAIL_LEGGINGS));
		builder(ItemTags.FOOT_ARMOR).addAll(keys(EamItems.RUSTY_IRON_BOOTS, EamItems.RUSTY_CHAINMAIL_BOOTS));
		builder(AncientMetalGear.TOOL_MATERIALS).add(EamItems.ANCIENT_METAL_INGOT.builtInRegistryHolder().key());
		builder(AncientMetalGear.REPAIRS_ARMOR).add(EamItems.ANCIENT_METAL_INGOT.builtInRegistryHolder().key());
		builder(ItemTags.SWORDS).add(EamItems.ANCIENT_METAL_SWORD.builtInRegistryHolder().key());
		builder(ItemTags.SHOVELS).add(EamItems.ANCIENT_METAL_SHOVEL.builtInRegistryHolder().key());
		builder(ItemTags.PICKAXES).add(EamItems.ANCIENT_METAL_PICKAXE.builtInRegistryHolder().key());
		builder(ItemTags.AXES).add(EamItems.ANCIENT_METAL_AXE.builtInRegistryHolder().key());
		builder(ItemTags.HOES).add(EamItems.ANCIENT_METAL_HOE.builtInRegistryHolder().key());
		builder(ItemTags.HEAD_ARMOR).addAll(keys(EamItems.ANCIENT_METAL_HELMET, EamItems.ANCIENT_METAL_CHAINMAIL_HELMET));
		builder(ItemTags.CHEST_ARMOR).addAll(keys(EamItems.ANCIENT_METAL_CHESTPLATE, EamItems.ANCIENT_METAL_CHAINMAIL_CHESTPLATE));
		builder(ItemTags.LEG_ARMOR).addAll(keys(EamItems.ANCIENT_METAL_LEGGINGS, EamItems.ANCIENT_METAL_CHAINMAIL_LEGGINGS));
		builder(ItemTags.FOOT_ARMOR).addAll(keys(EamItems.ANCIENT_METAL_BOOTS, EamItems.ANCIENT_METAL_CHAINMAIL_BOOTS));
		builder(ItemTags.HEAD_ARMOR).addAll(keys(EamItems.COPPER_CHAINMAIL_HELMET, EamItems.SILVER_CHAINMAIL_HELMET,
				EamItems.MITHRIL_CHAINMAIL_HELMET));
		builder(ItemTags.CHEST_ARMOR).addAll(keys(EamItems.COPPER_CHAINMAIL_CHESTPLATE, EamItems.SILVER_CHAINMAIL_CHESTPLATE,
				EamItems.MITHRIL_CHAINMAIL_CHESTPLATE));
		builder(ItemTags.LEG_ARMOR).addAll(keys(EamItems.COPPER_CHAINMAIL_LEGGINGS, EamItems.SILVER_CHAINMAIL_LEGGINGS,
				EamItems.MITHRIL_CHAINMAIL_LEGGINGS));
		builder(ItemTags.FOOT_ARMOR).addAll(keys(EamItems.COPPER_CHAINMAIL_BOOTS, EamItems.SILVER_CHAINMAIL_BOOTS,
				EamItems.MITHRIL_CHAINMAIL_BOOTS));
		// Too hot for the vanilla blast furnace: these cook only in the obsidian one.
		builder(MithrilModule.OBSIDIAN_FURNACE_ONLY).addAll(keys(EamItems.MITHRIL_ORE, EamItems.DEEPSLATE_MITHRIL_ORE, EamItems.RAW_MITHRIL,
				EamItems.MITHRIL_INGOT));

		// What the silver brewing stand takes in its reagent slot; its recipes are data/eam/recipe/silver_brewing/.
		builder(BrewingModule.REAGENTS).addAll(keys(EamItems.SILVER_REDSTONE_DUST, Items.GLOW_BERRIES, EamItems.FLOWER_MIX,
				Items.SUGAR, Items.GLISTERING_MELON_SLICE, Items.FERMENTED_SPIDER_EYE, EamItems.WOLF_MEAT, EamItems.GARLIC_ORCHID_MIX, EamItems.HERBAL_HONEY,
				Items.GUNPOWDER));

		builder(CraftingTags.KNIFE_TIERS.get(0)).add(EamItems.FLINT_KNIFE.builtInRegistryHolder().key());
		builder(CraftingTags.KNIFE_TIERS.get(1)).addAll(keys(EamItems.COPPER_KNIFE, EamItems.SILVER_KNIFE));
		builder(CraftingTags.KNIFE_TIERS.get(2)).addAll(keys(EamItems.IRON_KNIFE, EamItems.ANCIENT_METAL_KNIFE));
		builder(CraftingTags.KNIFE_TIERS.get(3)).add(EamItems.MITHRIL_KNIFE.builtInRegistryHolder().key());
		builder(CraftingTags.SAW_TIERS.get(0));
		builder(CraftingTags.SAW_TIERS.get(1)).addAll(keys(EamItems.COPPER_SAW, EamItems.SILVER_SAW));
		builder(CraftingTags.SAW_TIERS.get(2)).addAll(keys(EamItems.IRON_SAW, EamItems.ANCIENT_METAL_SAW));
		builder(CraftingTags.SAW_TIERS.get(3)).add(EamItems.MITHRIL_SAW.builtInRegistryHolder().key());
		// Knives, saws and hammers go in the crafting table's tool slots in any mix.
		builder(CraftingTags.CRAFTING_TABLE_TOOLS)
				.addTag(CraftingTags.KNIFE_TIERS.get(0)).addTag(CraftingTags.KNIFE_TIERS.get(1)).addTag(CraftingTags.KNIFE_TIERS.get(2))
				.addTag(CraftingTags.KNIFE_TIERS.get(3))
				.addTag(CraftingTags.SAW_TIERS.get(0)).addTag(CraftingTags.SAW_TIERS.get(1)).addTag(CraftingTags.SAW_TIERS.get(2))
				.addTag(CraftingTags.SAW_TIERS.get(3))
				.addTag(ForgingTags.HAMMERS);
		// A hammer in the table speeds up crafts with any of these in the grid.
		builder(CraftingTags.METALS)
				.addTag(CraftingTags.MATERIAL_TIERS.get(1)).addTag(CraftingTags.MATERIAL_TIERS.get(2)).addTag(CraftingTags.MATERIAL_TIERS.get(3))
				.addOptionalTag(ItemTags.METAL_NUGGETS)
				.addAll(keys(EamItems.SILVER_NUGGET, EamItems.MITHRIL_NUGGET, EamItems.ANCIENT_METAL_NUGGET, Items.GOLD_INGOT, Items.GOLD_NUGGET, Items.IRON_NUGGET, Items.COPPER_NUGGET));
		// A saw in the table speeds up crafting these: wooden things and decorative blocks.
		builder(CraftingTags.SAW_RESULTS)
				.addOptionalTag(ItemTags.PLANKS)
				.addOptionalTag(ItemTags.WOODEN_STAIRS)
				.addOptionalTag(ItemTags.WOODEN_SLABS)
				.addOptionalTag(ItemTags.WOODEN_FENCES)
				.addOptionalTag(ItemTags.FENCE_GATES)
				.addOptionalTag(ItemTags.WOODEN_DOORS)
				.addOptionalTag(ItemTags.WOODEN_TRAPDOORS)
				.addOptionalTag(ItemTags.WOODEN_BUTTONS)
				.addOptionalTag(ItemTags.WOODEN_PRESSURE_PLATES)
				.addOptionalTag(ItemTags.WOODEN_SHELVES)
				.addOptionalTag(ItemTags.SIGNS)
				.addOptionalTag(ItemTags.HANGING_SIGNS)
				.addOptionalTag(ItemTags.BOATS)
				.addOptionalTag(ItemTags.CHEST_BOATS)
				.addAll(keys(Items.STICK, Items.BOWL, Items.LADDER, Items.CHEST, Items.TRAPPED_CHEST, Items.BARREL, Items.CRAFTING_TABLE,
						Items.BOOKSHELF, Items.CHISELED_BOOKSHELF, Items.LECTERN, Items.COMPOSTER, Items.NOTE_BLOCK, Items.JUKEBOX,
						Items.LOOM, Items.CARTOGRAPHY_TABLE, Items.FLETCHING_TABLE, Items.BEEHIVE, Items.ITEM_FRAME, Items.PAINTING,
						Items.ARMOR_STAND, Items.BOW, Items.WOODEN_SWORD, Items.WOODEN_SHOVEL, Items.WOODEN_PICKAXE, Items.WOODEN_AXE,
						Items.WOODEN_HOE, Items.WOODEN_SPEAR, EamItems.SIEVE))
				// Decorative blocks of any material.
				.addOptionalTag(vanilla("stairs"))
				.addOptionalTag(vanilla("slabs"))
				.addOptionalTag(ItemTags.WALLS)
				.addOptionalTag(vanilla("fences"))
				.addOptionalTag(ItemTags.WOOL_CARPETS)
				.addOptionalTag(ItemTags.BANNERS)
				.addAll(keys(Items.FLOWER_POT, Items.DECORATED_POT));
		// Crafting with these needs a tool of the tier below in the table; iron plates count as iron, ancient metal as iron too.
		// Rings count as their metal, and so does making them from (untiered) nuggets.
		builder(CraftingTags.MATERIAL_TIERS.get(0)).add(Items.FLINT.builtInRegistryHolder().key());
		builder(CraftingTags.MATERIAL_TIERS.get(1)).addAll(keys(Items.COPPER_INGOT, EamItems.SILVER_INGOT, EamItems.COPPER_RING, EamItems.SILVER_RING));
		builder(CraftingTags.MATERIAL_TIERS.get(2)).addAll(keys(Items.IRON_INGOT, EamItems.IRON_PLATE, EamItems.ANCIENT_METAL_INGOT,
				EamItems.ANCIENT_METAL_PLATE, EamItems.IRON_RING, EamItems.ANCIENT_METAL_RING));
		builder(CraftingTags.MATERIAL_TIERS.get(3)).addAll(keys(EamItems.MITHRIL_INGOT, EamItems.MITHRIL_PLATE, EamItems.MITHRIL_RING));
		builder(CraftingTags.TIER_FREE).addAll(keys(Items.COPPER_INGOT, Items.COPPER_NUGGET, Items.RAW_COPPER_BLOCK,
				EamItems.SILVER_INGOT, EamItems.SILVER_NUGGET, EamItems.SILVER_BLOCK, Items.IRON_INGOT, Items.IRON_NUGGET, Items.IRON_BLOCK,
				Items.RAW_IRON_BLOCK, EamItems.MITHRIL_INGOT, EamItems.MITHRIL_NUGGET, EamItems.MITHRIL_BLOCK,
				EamItems.ANCIENT_METAL_INGOT, EamItems.ANCIENT_METAL_NUGGET, EamItems.ANCIENT_METAL_BLOCK))
				.addAll(keys(Items.COPPER_BLOCK.asList().toArray(Item[]::new)));
		builder(ForgingTags.HAMMER_TIERS.get(0));
		builder(ForgingTags.HAMMER_TIERS.get(1)).addAll(keys(EamItems.COPPER_HAMMER, EamItems.SILVER_HAMMER));
		builder(ForgingTags.HAMMER_TIERS.get(2)).addAll(keys(EamItems.IRON_HAMMER, EamItems.ANCIENT_METAL_HAMMER));
		builder(ForgingTags.HAMMER_TIERS.get(3)).add(EamItems.MITHRIL_HAMMER.builtInRegistryHolder().key());
		builder(ForgingTags.HAMMERS).addTag(ForgingTags.HAMMER_TIERS.get(0)).addTag(ForgingTags.HAMMER_TIERS.get(1))
				.addTag(ForgingTags.HAMMER_TIERS.get(2)).addTag(ForgingTags.HAMMER_TIERS.get(3));
		builder(vanilla("bars")).addAll(keys(EamItems.SILVER_BARS, EamItems.MITHRIL_BARS));
		builder(vanilla("small_flowers")).addAll(keys(EamItems.MARSH_MARIGOLD));
		builder(vanilla("flowers")).addAll(keys(EamItems.FIREWEED));
		builder(vanilla("doors")).addAll(keys(EamItems.SILVER_DOOR, EamItems.MITHRIL_DOOR));
		builder(vanilla("trapdoors")).addAll(keys(EamItems.SILVER_TRAPDOOR, EamItems.MITHRIL_TRAPDOOR));
		builder(ItemTags.ANVIL).addAll(keys(EamItems.COPPER_ANVIL, EamItems.SILVER_ANVIL, EamItems.MITHRIL_ANVIL));

		builder(CraftingTags.TEN_SECONDS).addAll(keys(Items.CRAFTING_TABLE, Items.CRAFTER, Items.ANVIL, EamItems.COPPER_ANVIL,
				EamItems.SILVER_ANVIL, EamItems.MITHRIL_ANVIL, EamItems.OBSIDIAN_BLAST_FURNACE, Items.ENCHANTING_TABLE,
				Items.BEACON, Items.CONDUIT, Items.RESPAWN_ANCHOR, Items.LODESTONE));
		// Vanilla tags are not generated here, so the datagen cannot check them: refer to them as optional.
		builder(CraftingTags.ARMOR)
				.addOptionalTag(ItemTags.HEAD_ARMOR)
				.addOptionalTag(ItemTags.CHEST_ARMOR)
				.addOptionalTag(ItemTags.LEG_ARMOR)
				.addOptionalTag(ItemTags.FOOT_ARMOR);
		builder(CraftingTags.FIVE_SECONDS)
				// Tools and weapons.
				.addOptionalTag(ItemTags.PICKAXES)
				.addOptionalTag(ItemTags.AXES)
				.addOptionalTag(ItemTags.SHOVELS)
				.addOptionalTag(ItemTags.HOES)
				.addOptionalTag(ItemTags.SWORDS)
				.addOptionalTag(ItemTags.SPEARS)
				.addAll(keys(EamItems.FLINT_KNIFE, EamItems.COPPER_KNIFE, EamItems.SILVER_KNIFE, EamItems.IRON_KNIFE, EamItems.COPPER_HAMMER,
						EamItems.SILVER_HAMMER, EamItems.IRON_HAMMER, EamItems.COPPER_SAW, EamItems.SILVER_SAW, EamItems.IRON_SAW,
						EamItems.MITHRIL_KNIFE, EamItems.MITHRIL_HAMMER, EamItems.MITHRIL_SAW, EamItems.ANCIENT_METAL_KNIFE,
						EamItems.ANCIENT_METAL_HAMMER, EamItems.ANCIENT_METAL_SAW, Items.SHEARS, Items.FLINT_AND_STEEL, Items.FISHING_ROD, Items.CARROT_ON_A_STICK,
						Items.WARPED_FUNGUS_ON_A_STICK, Items.BOW, Items.CROSSBOW, Items.SHIELD, Items.MACE, Items.BRUSH, Items.SPYGLASS,
						Items.COMPASS, Items.RECOVERY_COMPASS, Items.CLOCK))
				// Fires.
				.addAll(keys(EamBlocks.CAMP_FIRE.asItem(), Items.CAMPFIRE, Items.SOUL_CAMPFIRE))
				// Workstations and containers.
				.addAll(keys(Items.FURNACE, Items.BLAST_FURNACE, Items.SMOKER, Items.CHEST, Items.TRAPPED_CHEST, Items.BARREL,
						Items.ENDER_CHEST, Items.SHULKER_BOX, Items.SMITHING_TABLE, Items.FLETCHING_TABLE, Items.CARTOGRAPHY_TABLE,
						Items.LOOM, Items.STONECUTTER, Items.GRINDSTONE, Items.LECTERN, Items.COMPOSTER, Items.BREWING_STAND, EamItems.SILVER_BREWING_STAND,
						Items.CAULDRON, Items.JUKEBOX, Items.NOTE_BLOCK, Items.CHISELED_BOOKSHELF, Items.BEEHIVE, Items.ARMOR_STAND))
				// Redstone machines.
				.addAll(keys(Items.PISTON, Items.STICKY_PISTON, Items.OBSERVER, Items.DISPENSER, Items.DROPPER, Items.HOPPER,
						Items.DAYLIGHT_DETECTOR, Items.COMPARATOR, Items.REPEATER))
				// Vehicles.
				.addOptionalTag(ItemTags.BOATS)
				.addOptionalTag(ItemTags.CHEST_BOATS)
				.addAll(keys(Items.MINECART, Items.CHEST_MINECART, Items.FURNACE_MINECART, Items.HOPPER_MINECART, Items.TNT_MINECART));
	}

	/** A vanilla item tag that {@link ItemTags} has no field for. */
	private static TagKey<Item> vanilla(String path) {
		return TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace(path));
	}

	private static List<ResourceKey<Item>> keys(Item... items) {
		return Arrays.stream(items).map(item -> BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow()).toList();
	}
}
