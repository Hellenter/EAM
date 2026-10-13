package io.github.hellenter.eam.survival.hunger;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.campfire.CampFireLogic;
import io.github.hellenter.eam.content.campfire.Overcooking;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * MITE-style food values: about 60% of vanilla nutrition and less saturation, so raw food barely helps and
 * cooked meat and stews matter. Every food also gets {@link Nutrients}: meat and fish give protein and fat,
 * plants and baked goods give carbs, so a balanced diet needs both. Golden apples and golden carrots keep
 * their vanilla food values. Seeds are edible too: a quick snack worth one point, mostly fat and protein.
 * They stay plantable, since vanilla tries to place a block item before eating it.
 */
final class HungerFoods {
	/** {@code nutrition < 0} keeps the vanilla {@code food} component and only adds nutrients. */
	/** {@code consumable != null} adds a {@code consumable} component to items that are not food in vanilla. */
	private record Values(int nutrition, float saturationModifier, boolean alwaysEdible, Nutrients nutrients,
			@Nullable Consumable consumable) {
	}

	private static Values food(int nutrition, float saturationModifier, int protein, int fat, int carbs) {
		return new Values(nutrition, saturationModifier, false, new Nutrients(protein, fat, carbs), null);
	}

	private static Values alwaysEdible(int nutrition, float saturationModifier, int protein, int fat, int carbs) {
		return new Values(nutrition, saturationModifier, true, new Nutrients(protein, fat, carbs), null);
	}

	private static Values vanillaFood(int protein, int fat, int carbs) {
		return new Values(-1, 0.0F, false, new Nutrients(protein, fat, carbs), null);
	}

	/** Seeds are eaten as fast as dried kelp. */
	private static Values seed(int protein, int fat, int carbs) {
		return new Values(1, 0.1F, false, new Nutrients(protein, fat, carbs), Consumables.DRIED_KELP);
	}

	//                                                        nutrition, saturation, protein, fat, carbs
	private static final Map<Item, Values> COMMON = Map.ofEntries(
			// Meat and fish: raw is a last resort, cooking roughly doubles the value.
			Map.entry(Items.BEEF, food(2, 0.2F, 5, 3, 0)),
			Map.entry(Items.COOKED_BEEF, food(5, 0.6F, 13, 8, 0)),
			Map.entry(Items.PORKCHOP, food(2, 0.2F, 4, 4, 0)),
			Map.entry(Items.COOKED_PORKCHOP, food(5, 0.6F, 10, 10, 0)),
			Map.entry(Items.MUTTON, food(1, 0.2F, 3, 3, 0)),
			Map.entry(Items.COOKED_MUTTON, food(4, 0.6F, 8, 8, 0)),
			Map.entry(Items.CHICKEN, food(1, 0.2F, 3, 1, 0)),
			Map.entry(Items.COOKED_CHICKEN, food(4, 0.5F, 12, 3, 0)),
			Map.entry(Items.RABBIT, food(2, 0.2F, 5, 1, 0)),
			Map.entry(Items.COOKED_RABBIT, food(3, 0.5F, 9, 2, 0)),
			// Warg meat: lean and tough, a bit less than mutton but more protein.
			Map.entry(EamItems.WOLF_MEAT, food(1, 0.2F, 4, 2, 0)),
			Map.entry(EamItems.COOKED_WOLF_MEAT, food(4, 0.6F, 10, 5, 0)),
			Map.entry(Items.COD, food(1, 0.1F, 3, 1, 0)),
			Map.entry(Items.COOKED_COD, food(3, 0.5F, 9, 2, 0)),
			Map.entry(Items.SALMON, food(1, 0.1F, 3, 2, 0)),
			Map.entry(Items.COOKED_SALMON, food(4, 0.6F, 9, 6, 0)),
			Map.entry(Items.TROPICAL_FISH, food(1, 0.1F, 2, 1, 0)),
			Map.entry(Items.PUFFERFISH, food(1, 0.1F, 2, 1, 0)),
			Map.entry(Items.ROTTEN_FLESH, food(2, 0.1F, 3, 1, 0)),
			Map.entry(Items.SPIDER_EYE, food(1, 0.4F, 2, 1, 0)),
			// Crops and plants.
			Map.entry(Items.APPLE, food(2, 0.3F, 0, 0, 7)),
			Map.entry(Items.BREAD, food(3, 0.5F, 2, 1, 11)),
			Map.entry(Items.CARROT, food(2, 0.4F, 1, 0, 6)),
			Map.entry(Items.POTATO, food(1, 0.2F, 1, 0, 4)),
			Map.entry(Items.BAKED_POTATO, food(3, 0.5F, 1, 0, 11)),
			Map.entry(Items.POISONOUS_POTATO, food(1, 0.2F, 0, 0, 2)),
			Map.entry(Items.BEETROOT, food(1, 0.4F, 0, 0, 4)),
			Map.entry(Items.MELON_SLICE, food(1, 0.2F, 0, 0, 4)),
			Map.entry(Items.SWEET_BERRIES, food(1, 0.1F, 0, 0, 3)),
			Map.entry(Items.GLOW_BERRIES, food(1, 0.1F, 0, 0, 3)),
			Map.entry(Items.DRIED_KELP, food(1, 0.2F, 1, 0, 2)),
			// Seeds: a handful barely takes the edge off hunger.
			Map.entry(Items.WHEAT_SEEDS, seed(1, 1, 1)),
			Map.entry(Items.BEETROOT_SEEDS, seed(1, 1, 1)),
			Map.entry(Items.MELON_SEEDS, seed(1, 2, 0)),
			Map.entry(Items.PUMPKIN_SEEDS, seed(1, 2, 0)),
			Map.entry(Items.TORCHFLOWER_SEEDS, seed(1, 1, 0)),
			Map.entry(Items.PITCHER_POD, seed(1, 1, 1)),
			Map.entry(Items.CHORUS_FRUIT, alwaysEdible(2, 0.3F, 0, 0, 5)),
			Map.entry(Items.GOLDEN_CARROT, vanillaFood(1, 0, 8)),
			Map.entry(Items.GOLDEN_APPLE, vanillaFood(0, 0, 10)),
			Map.entry(Items.ENCHANTED_GOLDEN_APPLE, vanillaFood(5, 5, 10)),
			// Cooked dishes.
			Map.entry(Items.COOKIE, food(1, 0.1F, 0, 2, 4)),
			Map.entry(Items.PUMPKIN_PIE, food(5, 0.3F, 2, 5, 12)),
			Map.entry(Items.HONEY_BOTTLE, alwaysEdible(3, 0.1F, 0, 0, 12)),
			Map.entry(Items.MUSHROOM_STEW, food(4, 0.5F, 4, 2, 8)),
			Map.entry(Items.BEETROOT_SOUP, food(4, 0.5F, 1, 1, 11)),
			Map.entry(Items.SUSPICIOUS_STEW, alwaysEdible(4, 0.5F, 4, 2, 8)),
			Map.entry(Items.RABBIT_STEW, food(6, 0.5F, 12, 4, 9))
	);

	private static final Map<Item, Values> VALUES = withBurntFood(COMMON);

	private HungerFoods() {
	}

	/** Food burnt on a camp fire gives two thirds of the cooked food's nutrition and nutrients. */
	private static Map<Item, Values> withBurntFood(Map<Item, Values> values) {
		Map<Item, Values> all = new HashMap<>(values);
		Overcooking.BURNT.forEach((cooked, burnt) -> {
			Values food = values.get(cooked);
			Nutrients nutrients = food.nutrients();
			all.put(burnt, food(CampFireLogic.burntValue(food.nutrition()), food.saturationModifier(),
					CampFireLogic.burntValue(nutrients.protein()), CampFireLogic.burntValue(nutrients.fat()),
					CampFireLogic.burntValue(nutrients.carbs())));
		});
		return Map.copyOf(all);
	}

	static void register() {
		DefaultItemComponentEvents.MODIFY.register(context -> context.modify(VALUES::containsKey, (builder, item) -> {
			Values values = VALUES.get(item);
			builder.set(HungerModule.NUTRIENTS, values.nutrients());
			if (values.nutrition() < 0) {
				return;
			}

			FoodProperties.Builder food = new FoodProperties.Builder()
					.nutrition(values.nutrition())
					.saturationModifier(values.saturationModifier());
			if (values.alwaysEdible()) {
				food.alwaysEdible();
			}
			builder.set(DataComponents.FOOD, food.build());
			if (values.consumable() != null) {
				builder.set(DataComponents.CONSUMABLE, values.consumable());
			}
		}));
	}
}
