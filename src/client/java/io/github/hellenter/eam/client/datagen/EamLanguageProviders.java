package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.brewing.BrewingModule;
import io.github.hellenter.eam.content.campfire.CampFireModule;
import io.github.hellenter.eam.content.mithril.MithrilGear;
import io.github.hellenter.eam.content.silver.SilverGear;
import io.github.hellenter.eam.content.tools.FlintTools;
import io.github.hellenter.eam.survival.sleep.SafeRoomModule;
import io.github.hellenter.eam.world.bloodmoon.BloodMoonModule;
import io.github.hellenter.eam.world.crafting.CraftingTags;
import io.github.hellenter.eam.world.forging.ForgingTags;
import io.github.hellenter.eam.world.mithril.MithrilModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

import net.minecraft.core.HolderLookup;

import java.util.concurrent.CompletableFuture;

public final class EamLanguageProviders {
	private EamLanguageProviders() {
	}

	/** A potion's name in each of its containers; vanilla looks them up by the potion's name. */
	private static void potion(FabricLanguageProvider.TranslationBuilder builder, String name, String potion, String splash,
			String lingering, String arrow) {
		builder.add("item.minecraft.potion.effect." + name, potion);
		builder.add("item.minecraft.splash_potion.effect." + name, splash);
		builder.add("item.minecraft.lingering_potion.effect." + name, lingering);
		builder.add("item.minecraft.tipped_arrow.effect." + name, arrow);
	}

	public static class English extends FabricLanguageProvider {
		public English(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
			super(output, "en_us", registries);
		}

		@Override
		public void generateTranslations(HolderLookup.Provider registries, TranslationBuilder builder) {
			builder.add("commands.eam.villages.locked", "Only abandoned villages generate in new chunks");
			builder.add("commands.eam.villages.unlocked", "Normal villages generate in new chunks");
			builder.add("death.attack.eam.dehydration", "%1$s died of thirst");
			builder.add("death.attack.eam.dehydration.player", "%1$s died of thirst while fighting %2$s");
			builder.add("effect.eam.food_poisoning", "Food Poisoning");
			builder.add("effect.eam.malnourished", "Malnourished");
			builder.add("item.eam.leather_thread", "Leather Thread");
			builder.add(EamItems.FLASK_THREADS, "Threads");
			builder.add("item.eam.slingshot", "Slingshot");
			builder.add("item.eam.leather_flask", "Leather Flask");
			builder.add(EamItems.FLINT_HATCHET, "Flint Hatchet");
			builder.add(EamItems.FLINT_AXE, "Flint Axe");
			builder.add(EamItems.FLINT_KNIFE, "Flint Knife");
			builder.add(EamItems.COPPER_KNIFE, "Copper Knife");
			builder.add(EamItems.SILVER_KNIFE, "Silver Knife");
			builder.add(EamItems.IRON_KNIFE, "Iron Knife");
			builder.add(EamBlocks.COPPER_ANVIL, "Copper Anvil");
			builder.add(EamBlocks.SILVER_ANVIL, "Silver Anvil");
			builder.add("block.minecraft.anvil", "Iron Anvil");
			builder.add("block.minecraft.chipped_anvil", "Chipped Iron Anvil");
			builder.add("block.minecraft.damaged_anvil", "Damaged Iron Anvil");
			builder.add(EamItems.COPPER_HAMMER, "Copper Hammer");
			builder.add(EamItems.SILVER_HAMMER, "Silver Hammer");
			builder.add(EamItems.IRON_HAMMER, "Iron Hammer");
			builder.add(ForgingTags.HAMMERS, "Anvil Hammers");
			builder.add(ForgingTags.HAMMER_TIERS.get(0), "Anvil Hammers: Tier 1");
			builder.add(ForgingTags.HAMMER_TIERS.get(1), "Anvil Hammers: Tier 2");
			builder.add(ForgingTags.HAMMER_TIERS.get(2), "Anvil Hammers: Tier 3");
			builder.add("gui.eam.anvil.too_weak", "This anvil is too weak for it");
			builder.add("gui.eam.silver_brewing.recipes", "Silver Stand Recipes");
			builder.add(CraftingTags.KNIFE_TIERS.get(0), "Crafting Table Knives: Tier 1");
			builder.add(CraftingTags.KNIFE_TIERS.get(1), "Crafting Table Knives: Tier 2");
			builder.add(CraftingTags.KNIFE_TIERS.get(2), "Crafting Table Knives: Tier 3");
			builder.add(CraftingTags.SAW_TIERS.get(0), "Saws: Tier 1");
			builder.add(CraftingTags.SAW_TIERS.get(1), "Saws: Tier 2");
			builder.add(CraftingTags.SAW_TIERS.get(2), "Saws: Tier 3");
			builder.add(CraftingTags.SAW_RESULTS, "Crafted Faster with a Saw");
			builder.add(CraftingTags.METALS, "Metals: Crafted Faster with a Hammer");
			builder.add(EamItems.COPPER_SAW, "Copper Saw");
			builder.add(EamItems.SILVER_SAW, "Silver Saw");
			builder.add(EamItems.IRON_SAW, "Iron Saw");
			builder.add(CraftingTags.MATERIAL_TIERS.get(0), "Crafting Materials: Tier 1");
			builder.add(CraftingTags.MATERIAL_TIERS.get(1), "Crafting Materials: Tier 2");
			builder.add(CraftingTags.MATERIAL_TIERS.get(2), "Crafting Materials: Tier 3");
			builder.add(CraftingTags.TIER_FREE, "Crafted from Metal without a Tool");
			builder.add(FlintTools.REPAIR_MATERIALS, "Flint Tool Materials");
			builder.add(CraftingTags.CRAFTING_TABLE_TOOLS, "Crafting Table Tools");
			builder.add(CraftingTags.TEN_SECONDS, "Crafted in 10 Seconds");
			builder.add(CraftingTags.FIVE_SECONDS, "Crafted in 5 Seconds");
			builder.add(CraftingTags.ARMOR, "Crafted in 5 Seconds per Material");
			builder.add("item.eam.filtered_leather_flask", "Leather Flask with Charcoal Filter");
			builder.add("item.eam.flask.water", "Water: %s/%s");
			builder.add("item.eam.flask.filter", "Filter: %s/%s");
			builder.add("item.eam.flask.filter_spent", "Filter is spent");
			builder.add(EamItems.HOT_IRON_INGOT, "Hot Iron");
			builder.add(EamItems.IRON_PLATE, "Iron Plate");
			builder.add(EamItems.SILVER_ORE, "Silver Ore");
			builder.add(EamItems.DEEPSLATE_SILVER_ORE, "Deepslate Silver Ore");
			builder.add(EamItems.SILVER_BLOCK, "Block of Silver");
			builder.add(EamItems.RAW_SILVER, "Raw Silver");
			builder.add(EamItems.SILVER_INGOT, "Silver Ingot");
			builder.add(EamItems.SILVER_NUGGET, "Silver Nugget");
			builder.add(EamItems.SILVER_SWORD, "Silver Sword");
			builder.add(EamItems.SILVER_SHOVEL, "Silver Shovel");
			builder.add(EamItems.SILVER_PICKAXE, "Silver Pickaxe");
			builder.add(EamItems.SILVER_AXE, "Silver Axe");
			builder.add(EamItems.SILVER_HOE, "Silver Hoe");
			builder.add(EamItems.SILVER_HELMET, "Silver Helmet");
			builder.add(EamItems.SILVER_CHESTPLATE, "Silver Chestplate");
			builder.add(EamItems.SILVER_LEGGINGS, "Silver Leggings");
			builder.add(EamItems.SILVER_BOOTS, "Silver Boots");
			builder.add(SilverGear.TOOL_MATERIALS, "Silver Tool Materials");
			builder.add(SilverGear.REPAIRS_ARMOR, "Silver Armor Repair Materials");
			builder.add(EamBlocks.SILVER_BREWING_STAND, "Silver Brewing Stand");
			builder.add("container.eam.silver_brewing_stand", "Silver Brewing Stand");
			builder.add(EamItems.LAPIS_GUNPOWDER_MIX, "Lapis-Gunpowder Mix");
			builder.add(EamItems.SILVER_REDSTONE_DUST, "Silver-Redstone Dust");
			builder.add(EamItems.FLOWER_MIX, "Flower Mix");
			builder.add(EamItems.FERMENTED_ENDER_EYE, "Fermented Ender Eye");
			builder.add(BrewingModule.REAGENTS, "Silver Brewing Reagents");
			builder.add("effect.eam.light_healing", "Light Healing");
			potion(builder, "holy_water", "Holy Water", "Splash Holy Water", "Lingering Holy Water", "Arrow of Holy Water");
			potion(builder, "light_night_vision", "Light Potion of Night Vision", "Light Splash Potion of Night Vision",
					"Light Lingering Potion of Night Vision", "Arrow of Light Night Vision");
			potion(builder, "light_regeneration", "Light Potion of Regeneration", "Light Splash Potion of Regeneration",
					"Light Lingering Potion of Regeneration", "Arrow of Light Regeneration");
			potion(builder, "light_swiftness", "Light Potion of Swiftness", "Light Splash Potion of Swiftness",
					"Light Lingering Potion of Swiftness", "Arrow of Light Swiftness");
			potion(builder, "light_healing", "Light Potion of Healing", "Light Splash Potion of Healing",
					"Light Lingering Potion of Healing", "Arrow of Light Healing");
			potion(builder, "light_weakness", "Light Potion of Weakness", "Light Splash Potion of Weakness",
					"Light Lingering Potion of Weakness", "Arrow of Light Weakness");
			potion(builder, "light_strength", "Light Potion of Strength", "Light Splash Potion of Strength",
					"Light Lingering Potion of Strength", "Arrow of Light Strength");
			builder.add("item.eam.hot_iron_ingot.cools_in", "Cools in %s s");
			builder.add("item.eam.hot_iron_ingot.cooled", "Cooled: it is an iron ingot again");
			builder.add("item.eam.hot_iron_ingot.forge", "Forge on an anvil: 2 make an iron plate");
			builder.add(EamBlocks.PEBBLE, "Pebble");
			builder.add(EamBlocks.FLINT_SHARD, "Flint Shard");
			builder.add(EamBlocks.GROUND_STICK, "Stick");
			builder.add(EamBlocks.CAMP_FIRE, "Camp Fire");
			builder.add("block.eam.camp_fire.needs_sticks", "Put at least %s sticks on the fire to light it");
			builder.add(EamBlocks.SIEVE, "Sieve");
			builder.add(CampFireModule.IGNITERS, "Camp Fire Igniters");
			builder.add(EamEntities.PEBBLE, "Pebble");
			builder.add("item.eam.nutrients", "Protein %s · Fat %s · Carbs %s");
			builder.add("gui.eam.nutrient.protein", "Protein");
			builder.add("gui.eam.nutrient.fat", "Fat");
			builder.add("gui.eam.nutrient.carbs", "Carbs");
			builder.add("gui.eam.nutrient.protein.short", "P");
			builder.add("gui.eam.nutrient.fat.short", "F");
			builder.add("gui.eam.nutrient.carbs.short", "C");
			builder.add("gui.eam.nutrient.value", "%s: %s / %s");
			builder.add("gui.eam.nutrition.balanced", "Balanced diet: hunger is slower, regeneration is faster");
			builder.add("gui.eam.nutrition.unbalanced", "Keep all three at %s or more for a bonus");
			builder.add("gui.eam.crafting_table.tool", "Knives, saws and hammers: a knife is needed, each tool makes its crafts faster");
			builder.add("gui.eam.crafting_table.tool.tiers", "Copper and silver need a flint knife, iron a copper or silver one, mithril an iron one");
			mithril(builder);
		}

		private static void mithril(TranslationBuilder builder) {
			builder.add(EamBlocks.OBSIDIAN_BLAST_FURNACE, "Obsidian Blast Furnace");
			builder.add("container.eam.obsidian_blast_furnace", "Obsidian Blast Furnace");
			builder.add(EamItems.MITHRIL_ORE, "Mithril Ore");
			builder.add(EamItems.DEEPSLATE_MITHRIL_ORE, "Deepslate Mithril Ore");
			builder.add(EamItems.MITHRIL_BLOCK, "Block of Mithril");
			builder.add(EamItems.RAW_MITHRIL, "Raw Mithril");
			builder.add(EamItems.MITHRIL_INGOT, "Mithril Ingot");
			builder.add(EamItems.HOT_MITHRIL_INGOT, "Hot Mithril");
			builder.add(EamItems.MITHRIL_PLATE, "Mithril Plate");
			builder.add(EamItems.MITHRIL_SWORD, "Mithril Sword");
			builder.add(EamItems.MITHRIL_SHOVEL, "Mithril Shovel");
			builder.add(EamItems.MITHRIL_PICKAXE, "Mithril Pickaxe");
			builder.add(EamItems.MITHRIL_AXE, "Mithril Axe");
			builder.add(EamItems.MITHRIL_HOE, "Mithril Hoe");
			builder.add(EamItems.MITHRIL_HELMET, "Mithril Helmet");
			builder.add(EamItems.MITHRIL_CHESTPLATE, "Mithril Chestplate");
			builder.add(EamItems.MITHRIL_LEGGINGS, "Mithril Leggings");
			builder.add(EamItems.MITHRIL_BOOTS, "Mithril Boots");
			builder.add(EamItems.MITHRIL_KNIFE, "Mithril Knife");
			builder.add(EamItems.MITHRIL_SAW, "Mithril Saw");
			builder.add(EamItems.MITHRIL_HAMMER, "Mithril Hammer");
			builder.add(EamBlocks.MITHRIL_ANVIL, "Mithril Anvil");
			builder.add(MithrilGear.TOOL_MATERIALS, "Mithril Tool Materials");
			builder.add(MithrilGear.REPAIRS_ARMOR, "Mithril Armor Repair Materials");
			builder.add(MithrilModule.OBSIDIAN_FURNACE_ONLY, "Smelted Only in the Obsidian Blast Furnace");
			builder.add(ForgingTags.HAMMER_TIERS.get(3), "Anvil Hammers: Tier 4");
			builder.add(CraftingTags.KNIFE_TIERS.get(3), "Crafting Table Knives: Tier 4");
			builder.add(CraftingTags.SAW_TIERS.get(3), "Saws: Tier 4");
			builder.add(CraftingTags.MATERIAL_TIERS.get(3), "Crafting Materials: Tier 4");
			builder.add("item.eam.hot_mithril_ingot.cools_in", "Cools in %s s");
			builder.add("item.eam.hot_mithril_ingot.cooled", "Cooled: it is a mithril ingot again");
			builder.add("item.eam.hot_mithril_ingot.forge", "Forge on a mithril anvil: 2 make a mithril plate");
			builder.add(SafeRoomModule.SAFE_WALLS, "Safe Room Walls");
			builder.add("block.eam.bed.not_enclosed", "You can only sleep in a closed room that is not too big");
			builder.add("block.eam.bed.weak_blocks", "Your room is made of blocks that are too weak");
			builder.add("block.eam.bed.needs_door", "A room this small needs a door");
			builder.add("block.eam.bed.needs_light", "The room needs lighting");
			builder.add("block.eam.bed.blood_moon", "You can't sleep under a blood moon");
			builder.add("event.eam.blood_moon.title", "Blood Moon");
			builder.add("event.eam.blood_moon.start", "The blood moon is rising. Monsters are coming out in armor and won't let you sleep.");
			builder.add("event.eam.blood_moon.end", "The blood moon has set.");
			builder.add("commands.eam.blood_moon.active", "Day %s: the blood moon is up");
			builder.add("commands.eam.blood_moon.next", "Day %s: the next blood moon is on day %s");
			builder.add(BloodMoonModule.ARMORED, "Armored Under a Blood Moon");
			builder.add(BloodMoonModule.ARMED, "Armed Under a Blood Moon");
			builder.add(BloodMoonModule.HUNTERS, "Hunt Animals Under a Blood Moon");
			builder.add("itemGroup.eam.survival", "EAM: Survival");
			builder.add("itemGroup.eam.metals", "EAM: Ores and Metals");
			builder.add("itemGroup.eam.tools", "EAM: Tools");
			builder.add("itemGroup.eam.combat", "EAM: Weapons and Armor");
			builder.add("itemGroup.eam.workshop", "EAM: Workshop");
			builder.add("itemGroup.eam.brewing", "EAM: Brewing");
		}
	}

	public static class Russian extends FabricLanguageProvider {
		public Russian(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
			super(output, "ru_ru", registries);
		}

		@Override
		public void generateTranslations(HolderLookup.Provider registries, TranslationBuilder builder) {
			builder.add("commands.eam.villages.locked", "В новых чанках появляются только заброшенные деревни");
			builder.add("commands.eam.villages.unlocked", "В новых чанках появляются обычные деревни");
			builder.add("death.attack.eam.dehydration", "%1$s умер от жажды");
			builder.add("death.attack.eam.dehydration.player", "%1$s умер от жажды, сражаясь с %2$s");
			builder.add("effect.eam.food_poisoning", "Пищевое отравление");
			builder.add("effect.eam.malnourished", "Истощение");
			builder.add("item.eam.leather_thread", "Кожаная нить");
			builder.add(EamItems.FLASK_THREADS, "Нитки");
			builder.add("item.eam.slingshot", "Рогатка");
			builder.add("item.eam.leather_flask", "Кожаная фляга");
			builder.add(EamItems.FLINT_HATCHET, "Кремнёвый топорик");
			builder.add(EamItems.FLINT_AXE, "Кремнёвый топор");
			builder.add(EamItems.FLINT_KNIFE, "Кремнёвый нож");
			builder.add(EamItems.COPPER_KNIFE, "Медный нож");
			builder.add(EamItems.SILVER_KNIFE, "Серебряный нож");
			builder.add(EamItems.IRON_KNIFE, "Железный нож");
			builder.add(EamBlocks.COPPER_ANVIL, "Медная наковальня");
			builder.add(EamBlocks.SILVER_ANVIL, "Серебряная наковальня");
			builder.add("block.minecraft.anvil", "Железная наковальня");
			builder.add("block.minecraft.chipped_anvil", "Потрескавшаяся железная наковальня");
			builder.add("block.minecraft.damaged_anvil", "Повреждённая железная наковальня");
			builder.add(EamItems.COPPER_HAMMER, "Медный молоток");
			builder.add(EamItems.SILVER_HAMMER, "Серебряный молоток");
			builder.add(EamItems.IRON_HAMMER, "Железный молоток");
			builder.add(ForgingTags.HAMMERS, "Молотки для наковальни");
			builder.add(ForgingTags.HAMMER_TIERS.get(0), "Молотки для наковальни: тир 1");
			builder.add(ForgingTags.HAMMER_TIERS.get(1), "Молотки для наковальни: тир 2");
			builder.add(ForgingTags.HAMMER_TIERS.get(2), "Молотки для наковальни: тир 3");
			builder.add("gui.eam.anvil.too_weak", "Эта наковальня слишком слабая");
			builder.add("gui.eam.silver_brewing.recipes", "Рецепты зельеварки");
			builder.add(CraftingTags.KNIFE_TIERS.get(0), "Ножи для верстака: тир 1");
			builder.add(CraftingTags.KNIFE_TIERS.get(1), "Ножи для верстака: тир 2");
			builder.add(CraftingTags.KNIFE_TIERS.get(2), "Ножи для верстака: тир 3");
			builder.add(CraftingTags.SAW_TIERS.get(0), "Пилы: тир 1");
			builder.add(CraftingTags.SAW_TIERS.get(1), "Пилы: тир 2");
			builder.add(CraftingTags.SAW_TIERS.get(2), "Пилы: тир 3");
			builder.add(CraftingTags.SAW_RESULTS, "Крафтится быстрее с пилой");
			builder.add(CraftingTags.METALS, "Металлы: крафт быстрее с молотком");
			builder.add(EamItems.COPPER_SAW, "Медная пила");
			builder.add(EamItems.SILVER_SAW, "Серебряная пила");
			builder.add(EamItems.IRON_SAW, "Железная пила");
			builder.add(CraftingTags.MATERIAL_TIERS.get(0), "Материалы для крафта: тир 1");
			builder.add(CraftingTags.MATERIAL_TIERS.get(1), "Материалы для крафта: тир 2");
			builder.add(CraftingTags.MATERIAL_TIERS.get(2), "Материалы для крафта: тир 3");
			builder.add(CraftingTags.TIER_FREE, "Крафтится из металла без инструмента");
			builder.add(FlintTools.REPAIR_MATERIALS, "Материалы для кремнёвых инструментов");
			builder.add(CraftingTags.CRAFTING_TABLE_TOOLS, "Инструменты для верстака");
			builder.add(CraftingTags.TEN_SECONDS, "Крафт 10 секунд");
			builder.add(CraftingTags.FIVE_SECONDS, "Крафт 5 секунд");
			builder.add(CraftingTags.ARMOR, "Крафт 5 секунд за материал");
			builder.add("item.eam.filtered_leather_flask", "Кожаная фляга с угольным фильтром");
			builder.add("item.eam.flask.water", "Вода: %s/%s");
			builder.add("item.eam.flask.filter", "Фильтр: %s/%s");
			builder.add("item.eam.flask.filter_spent", "Фильтр забит");
			builder.add(EamItems.HOT_IRON_INGOT, "Раскалённое железо");
			builder.add(EamItems.IRON_PLATE, "Железная пластина");
			builder.add(EamItems.SILVER_ORE, "Серебряная руда");
			builder.add(EamItems.DEEPSLATE_SILVER_ORE, "Глубинносланцевая серебряная руда");
			builder.add(EamItems.SILVER_BLOCK, "Серебряный блок");
			builder.add(EamItems.RAW_SILVER, "Необработанное серебро");
			builder.add(EamItems.SILVER_INGOT, "Серебряный слиток");
			builder.add(EamItems.SILVER_NUGGET, "Кусочек серебра");
			builder.add(EamItems.SILVER_SWORD, "Серебряный меч");
			builder.add(EamItems.SILVER_SHOVEL, "Серебряная лопата");
			builder.add(EamItems.SILVER_PICKAXE, "Серебряная кирка");
			builder.add(EamItems.SILVER_AXE, "Серебряный топор");
			builder.add(EamItems.SILVER_HOE, "Серебряная мотыга");
			builder.add(EamItems.SILVER_HELMET, "Серебряный шлем");
			builder.add(EamItems.SILVER_CHESTPLATE, "Серебряный нагрудник");
			builder.add(EamItems.SILVER_LEGGINGS, "Серебряные поножи");
			builder.add(EamItems.SILVER_BOOTS, "Серебряные ботинки");
			builder.add(SilverGear.TOOL_MATERIALS, "Материалы для серебряных инструментов");
			builder.add(SilverGear.REPAIRS_ARMOR, "Материалы для починки серебряной брони");
			builder.add(EamBlocks.SILVER_BREWING_STAND, "Серебряная варочная стойка");
			builder.add("container.eam.silver_brewing_stand", "Серебряная варочная стойка");
			builder.add(EamItems.LAPIS_GUNPOWDER_MIX, "Лазуритово-пороховая смесь");
			builder.add(EamItems.SILVER_REDSTONE_DUST, "Серебряно-редстоуновый порошок");
			builder.add(EamItems.FLOWER_MIX, "Цветочная смесь");
			builder.add(EamItems.FERMENTED_ENDER_EYE, "Ферментированное око Эндера");
			builder.add(BrewingModule.REAGENTS, "Ингредиенты для серебряной стойки");
			builder.add("effect.eam.light_healing", "Лёгкое исцеление");
			potion(builder, "holy_water", "Святая вода", "Взрывная святая вода", "Оседающая святая вода", "Стрела святой воды");
			potion(builder, "light_night_vision", "Лёгкое зелье ночного зрения", "Взрывное лёгкое зелье ночного зрения",
					"Оседающее лёгкое зелье ночного зрения", "Стрела лёгкого ночного зрения");
			potion(builder, "light_regeneration", "Лёгкое зелье регенерации", "Взрывное лёгкое зелье регенерации",
					"Оседающее лёгкое зелье регенерации", "Стрела лёгкой регенерации");
			potion(builder, "light_swiftness", "Лёгкое зелье стремительности", "Взрывное лёгкое зелье стремительности",
					"Оседающее лёгкое зелье стремительности", "Стрела лёгкой стремительности");
			potion(builder, "light_healing", "Лёгкое зелье исцеления", "Взрывное лёгкое зелье исцеления",
					"Оседающее лёгкое зелье исцеления", "Стрела лёгкого исцеления");
			potion(builder, "light_weakness", "Лёгкое зелье слабости", "Взрывное лёгкое зелье слабости",
					"Оседающее лёгкое зелье слабости", "Стрела лёгкой слабости");
			potion(builder, "light_strength", "Лёгкое зелье силы", "Взрывное лёгкое зелье силы",
					"Оседающее лёгкое зелье силы", "Стрела лёгкой силы");
			builder.add("item.eam.hot_iron_ingot.cools_in", "Остынет через %s с");
			builder.add("item.eam.hot_iron_ingot.cooled", "Остыло и снова стало железным слитком");
			builder.add("item.eam.hot_iron_ingot.forge", "Выкуйте на наковальне: из 2 выйдет железная пластина");
			builder.add(EamBlocks.PEBBLE, "Камушек");
			builder.add(EamBlocks.FLINT_SHARD, "Осколок кремня");
			builder.add(EamBlocks.GROUND_STICK, "Палка");
			builder.add(EamBlocks.CAMP_FIRE, "Походный костёр");
			builder.add("block.eam.camp_fire.needs_sticks", "Чтобы разжечь костёр, положите в него не меньше %s палок");
			builder.add(EamBlocks.SIEVE, "Сито");
			builder.add(CampFireModule.IGNITERS, "Растопка для костра");
			builder.add(EamEntities.PEBBLE, "Камушек");
			builder.add("item.eam.nutrients", "Белки %s · Жиры %s · Углеводы %s");
			builder.add("gui.eam.nutrient.protein", "Белки");
			builder.add("gui.eam.nutrient.fat", "Жиры");
			builder.add("gui.eam.nutrient.carbs", "Углеводы");
			builder.add("gui.eam.nutrient.protein.short", "Б");
			builder.add("gui.eam.nutrient.fat.short", "Ж");
			builder.add("gui.eam.nutrient.carbs.short", "У");
			builder.add("gui.eam.nutrient.value", "%s: %s / %s");
			builder.add("gui.eam.nutrition.balanced", "Питание сбалансировано: голод медленнее, регенерация быстрее");
			builder.add("gui.eam.nutrition.unbalanced", "Держите все три шкалы на %s или выше для бонуса");
			builder.add("gui.eam.crafting_table.tool", "Ножи, пилы и молотки: нужен хотя бы нож, каждый инструмент ускоряет свой крафт");
			builder.add("gui.eam.crafting_table.tool.tiers",
					"Для меди и серебра нужен кремнёвый нож, для железа медный или серебряный, для мифрила железный");
			mithril(builder);
		}

		private static void mithril(TranslationBuilder builder) {
			builder.add(EamBlocks.OBSIDIAN_BLAST_FURNACE, "Обсидиановая доменная печь");
			builder.add("container.eam.obsidian_blast_furnace", "Обсидиановая доменная печь");
			builder.add(EamItems.MITHRIL_ORE, "Мифриловая руда");
			builder.add(EamItems.DEEPSLATE_MITHRIL_ORE, "Глубинносланцевая мифриловая руда");
			builder.add(EamItems.MITHRIL_BLOCK, "Мифриловый блок");
			builder.add(EamItems.RAW_MITHRIL, "Необработанный мифрил");
			builder.add(EamItems.MITHRIL_INGOT, "Мифриловый слиток");
			builder.add(EamItems.HOT_MITHRIL_INGOT, "Раскалённый мифрил");
			builder.add(EamItems.MITHRIL_PLATE, "Мифриловая пластина");
			builder.add(EamItems.MITHRIL_SWORD, "Мифриловый меч");
			builder.add(EamItems.MITHRIL_SHOVEL, "Мифриловая лопата");
			builder.add(EamItems.MITHRIL_PICKAXE, "Мифриловая кирка");
			builder.add(EamItems.MITHRIL_AXE, "Мифриловый топор");
			builder.add(EamItems.MITHRIL_HOE, "Мифриловая мотыга");
			builder.add(EamItems.MITHRIL_HELMET, "Мифриловый шлем");
			builder.add(EamItems.MITHRIL_CHESTPLATE, "Мифриловый нагрудник");
			builder.add(EamItems.MITHRIL_LEGGINGS, "Мифриловые поножи");
			builder.add(EamItems.MITHRIL_BOOTS, "Мифриловые ботинки");
			builder.add(EamItems.MITHRIL_KNIFE, "Мифриловый нож");
			builder.add(EamItems.MITHRIL_SAW, "Мифриловая пила");
			builder.add(EamItems.MITHRIL_HAMMER, "Мифриловый молоток");
			builder.add(EamBlocks.MITHRIL_ANVIL, "Мифриловая наковальня");
			builder.add(MithrilGear.TOOL_MATERIALS, "Материалы мифриловых инструментов");
			builder.add(MithrilGear.REPAIRS_ARMOR, "Материалы для починки мифриловой брони");
			builder.add(MithrilModule.OBSIDIAN_FURNACE_ONLY, "Плавится только в обсидиановой доменной печи");
			builder.add(ForgingTags.HAMMER_TIERS.get(3), "Молотки для наковальни: тир 4");
			builder.add(CraftingTags.KNIFE_TIERS.get(3), "Ножи для верстака: тир 4");
			builder.add(CraftingTags.SAW_TIERS.get(3), "Пилы: тир 4");
			builder.add(CraftingTags.MATERIAL_TIERS.get(3), "Материалы для крафта: тир 4");
			builder.add("item.eam.hot_mithril_ingot.cools_in", "Остынет через %s с");
			builder.add("item.eam.hot_mithril_ingot.cooled", "Остыл: снова мифриловый слиток");
			builder.add("item.eam.hot_mithril_ingot.forge", "Выкуйте на мифриловой наковальне: из 2 выйдет мифриловая пластина");
			builder.add(SafeRoomModule.SAFE_WALLS, "Стены безопасного помещения");
			builder.add("block.eam.bed.not_enclosed", "Спать можно только в замкнутом и не слишком большом помещении");
			builder.add("block.eam.bed.weak_blocks", "Ваше помещение состоит из слишком непрочных блоков");
			builder.add("block.eam.bed.needs_door", "Требуется дверь");
			builder.add("block.eam.bed.needs_light", "Требуется освещение");
			builder.add("block.eam.bed.blood_moon", "Под кровавой луной не уснуть");
			builder.add("event.eam.blood_moon.title", "Кровавая луна");
			builder.add("event.eam.blood_moon.start", "Восходит кровавая луна. Монстры выходят в броне и не дадут уснуть.");
			builder.add("event.eam.blood_moon.end", "Кровавая луна зашла.");
			builder.add("commands.eam.blood_moon.active", "День %s: сейчас кровавая луна");
			builder.add("commands.eam.blood_moon.next", "День %s: следующая кровавая луна в день %s");
			builder.add(BloodMoonModule.ARMORED, "В броне под кровавой луной");
			builder.add(BloodMoonModule.ARMED, "С оружием под кровавой луной");
			builder.add(BloodMoonModule.HUNTERS, "Охотятся на животных под кровавой луной");
			builder.add("itemGroup.eam.survival", "EAM: Выживание");
			builder.add("itemGroup.eam.metals", "EAM: Руды и металлы");
			builder.add("itemGroup.eam.tools", "EAM: Инструменты");
			builder.add("itemGroup.eam.combat", "EAM: Оружие и броня");
			builder.add("itemGroup.eam.workshop", "EAM: Мастерская");
			builder.add("itemGroup.eam.brewing", "EAM: Зельеварение");
		}
	}
}
