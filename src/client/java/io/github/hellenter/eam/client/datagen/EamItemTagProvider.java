package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.campfire.CampFireModule;
import io.github.hellenter.eam.content.silver.SilverGear;
import io.github.hellenter.eam.content.tools.FlintTools;
import io.github.hellenter.eam.world.crafting.CraftingTags;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EamItemTagProvider extends FabricTagsProvider.ItemTagsProvider {
	public EamItemTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		builder(EamItems.FLASK_THREADS).add(EamItems.LEATHER_THREAD.builtInRegistryHolder().key(), Items.STRING.builtInRegistryHolder().key());
		builder(CampFireModule.IGNITERS).add(
				Items.FLINT_AND_STEEL.builtInRegistryHolder().key(),
				Items.FIRE_CHARGE.builtInRegistryHolder().key(),
				Items.FLINT.builtInRegistryHolder().key(),
				EamItems.FLINT_SHARD.builtInRegistryHolder().key());
		builder(FlintTools.REPAIR_MATERIALS).add(Items.FLINT.builtInRegistryHolder().key());
		builder(ItemTags.AXES).add(EamItems.FLINT_HATCHET.builtInRegistryHolder().key(), EamItems.FLINT_AXE.builtInRegistryHolder().key(),
				EamItems.SILVER_AXE.builtInRegistryHolder().key());

		builder(SilverGear.TOOL_MATERIALS).add(EamItems.SILVER_INGOT.builtInRegistryHolder().key());
		builder(SilverGear.REPAIRS_ARMOR).add(EamItems.SILVER_INGOT.builtInRegistryHolder().key());
		// Ingots the blast furnace recipe accepts; the iron-only-in-blast-furnace change adds iron and copper here.
		builder(TagKey.create(Registries.ITEM, Eam.id("blast_furnace_ingots"))).add(EamItems.SILVER_INGOT.builtInRegistryHolder().key());
		// Vanilla tool and armor tags: enchanting, and the 5 s craft times below.
		builder(ItemTags.SWORDS).add(EamItems.SILVER_SWORD.builtInRegistryHolder().key());
		builder(ItemTags.SHOVELS).add(EamItems.SILVER_SHOVEL.builtInRegistryHolder().key());
		builder(ItemTags.PICKAXES).add(EamItems.SILVER_PICKAXE.builtInRegistryHolder().key());
		builder(ItemTags.HOES).add(EamItems.SILVER_HOE.builtInRegistryHolder().key());
		builder(ItemTags.HEAD_ARMOR).add(EamItems.SILVER_HELMET.builtInRegistryHolder().key());
		builder(ItemTags.CHEST_ARMOR).add(EamItems.SILVER_CHESTPLATE.builtInRegistryHolder().key());
		builder(ItemTags.LEG_ARMOR).add(EamItems.SILVER_LEGGINGS.builtInRegistryHolder().key());
		builder(ItemTags.FOOT_ARMOR).add(EamItems.SILVER_BOOTS.builtInRegistryHolder().key());

		builder(CraftingTags.CRAFTING_TABLE_TOOLS).add(EamItems.FLINT_KNIFE.builtInRegistryHolder().key());
		builder(CraftingTags.TEN_SECONDS).addAll(keys(Items.CRAFTING_TABLE, Items.CRAFTER, Items.ANVIL, Items.ENCHANTING_TABLE,
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
				.addAll(keys(EamItems.FLINT_KNIFE, Items.SHEARS, Items.FLINT_AND_STEEL, Items.FISHING_ROD, Items.CARROT_ON_A_STICK,
						Items.WARPED_FUNGUS_ON_A_STICK, Items.BOW, Items.CROSSBOW, Items.SHIELD, Items.MACE, Items.BRUSH, Items.SPYGLASS,
						Items.COMPASS, Items.RECOVERY_COMPASS, Items.CLOCK))
				// Fires.
				.addAll(keys(EamBlocks.CAMP_FIRE.asItem(), Items.CAMPFIRE, Items.SOUL_CAMPFIRE))
				// Workstations and containers.
				.addAll(keys(Items.FURNACE, Items.BLAST_FURNACE, Items.SMOKER, Items.CHEST, Items.TRAPPED_CHEST, Items.BARREL,
						Items.ENDER_CHEST, Items.SHULKER_BOX, Items.SMITHING_TABLE, Items.FLETCHING_TABLE, Items.CARTOGRAPHY_TABLE,
						Items.LOOM, Items.STONECUTTER, Items.GRINDSTONE, Items.LECTERN, Items.COMPOSTER, Items.BREWING_STAND,
						Items.CAULDRON, Items.JUKEBOX, Items.NOTE_BLOCK, Items.CHISELED_BOOKSHELF, Items.BEEHIVE, Items.ARMOR_STAND))
				// Redstone machines.
				.addAll(keys(Items.PISTON, Items.STICKY_PISTON, Items.OBSERVER, Items.DISPENSER, Items.DROPPER, Items.HOPPER,
						Items.DAYLIGHT_DETECTOR, Items.COMPARATOR, Items.REPEATER))
				// Vehicles.
				.addOptionalTag(ItemTags.BOATS)
				.addOptionalTag(ItemTags.CHEST_BOATS)
				.addAll(keys(Items.MINECART, Items.CHEST_MINECART, Items.FURNACE_MINECART, Items.HOPPER_MINECART, Items.TNT_MINECART));
	}

	private static List<ResourceKey<Item>> keys(Item... items) {
		return Arrays.stream(items).map(item -> BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow()).toList();
	}
}
