package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.campfire.CampFireModule;
import io.github.hellenter.eam.content.tools.FlintTools;
import io.github.hellenter.eam.world.crafting.CraftingTags;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

import net.minecraft.core.HolderLookup;

import java.util.concurrent.CompletableFuture;

public final class EamLanguageProviders {
	private EamLanguageProviders() {
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
			builder.add(EamBlocks.PEBBLE, "Pebble");
			builder.add(EamBlocks.FLINT_SHARD, "Flint Shard");
			builder.add(EamBlocks.GROUND_STICK, "Stick");
			builder.add(EamBlocks.CAMP_FIRE, "Camp Fire");
			builder.add("block.eam.camp_fire.needs_sticks", "Put at least %s sticks on the fire to light it");
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
			builder.add(EamBlocks.PEBBLE, "Камушек");
			builder.add(EamBlocks.FLINT_SHARD, "Осколок кремня");
			builder.add(EamBlocks.GROUND_STICK, "Палка");
			builder.add(EamBlocks.CAMP_FIRE, "Походный костёр");
			builder.add("block.eam.camp_fire.needs_sticks", "Чтобы разжечь костёр, положите в него не меньше %s палок");
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
