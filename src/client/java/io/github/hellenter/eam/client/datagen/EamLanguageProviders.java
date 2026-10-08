package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.brewing.BrewingModule;
import io.github.hellenter.eam.content.campfire.CampFireModule;
import io.github.hellenter.eam.content.silver.SilverGear;
import io.github.hellenter.eam.content.tools.FlintTools;
import io.github.hellenter.eam.world.crafting.CraftingTags;

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
			builder.add("gui.eam.crafting_table.tool", "Put a crafting tool here: a flint knife");
		}
	}

	public static class Russian extends FabricLanguageProvider {
		public Russian(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
			super(output, "ru_ru", registries);
		}

		@Override
		public void generateTranslations(HolderLookup.Provider registries, TranslationBuilder builder) {
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
			builder.add("gui.eam.crafting_table.tool", "Положите сюда инструмент для крафта: кремнёвый нож");
		}
	}
}
