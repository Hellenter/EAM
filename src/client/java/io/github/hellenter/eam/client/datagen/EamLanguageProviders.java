package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamItems;

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
			builder.add("item.eam.leather_thread", "Leather Thread");
			builder.add(EamItems.FLASK_THREADS, "Flask Threads");
			builder.add("item.eam.leather_flask", "Leather Flask");
			builder.add("item.eam.filtered_leather_flask", "Leather Flask with Charcoal Filter");
			builder.add("item.eam.flask.water", "Water: %s/%s");
			builder.add("item.eam.flask.filter", "Filter: %s/%s");
			builder.add("item.eam.flask.filter_spent", "Filter is spent");
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
			builder.add("item.eam.leather_thread", "Кожаная нить");
			builder.add(EamItems.FLASK_THREADS, "Нити для фляги");
			builder.add("item.eam.leather_flask", "Кожаная фляга");
			builder.add("item.eam.filtered_leather_flask", "Кожаная фляга с угольным фильтром");
			builder.add("item.eam.flask.water", "Вода: %s/%s");
			builder.add("item.eam.flask.filter", "Фильтр: %s/%s");
			builder.add("item.eam.flask.filter_spent", "Фильтр забит");
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
		}
	}
}
