package io.github.hellenter.eam.survival.hunger;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Map;

/**
 * MITE-style food values: about 60% of vanilla nutrition and less saturation, so raw food barely helps and
 * cooked meat and stews matter. Every food also gets {@link Nutrients}: meat and fish give protein and fat,
 * plants and baked goods give carbs, so a balanced diet needs both. Golden apples and golden carrots keep
 * their vanilla food values.
 */
final class HungerFoods {
	/** {@code nutrition < 0} keeps the vanilla {@code food} component and only adds nutrients. */
	private record Values(int nutrition, float saturationModifier, boolean alwaysEdible, Nutrients nutrients) {
	}

	private static Values food(int nutrition, float saturationModifier, int protein, int fat, int carbs) {
		return new Values(nutrition, saturationModifier, false, new Nutrients(protein, fat, carbs));
	}

	private static Values alwaysEdible(int nutrition, float saturationModifier, int protein, int fat, int carbs) {
		return new Values(nutrition, saturationModifier, true, new Nutrients(protein, fat, carbs));
	}

	private static Values vanillaFood(int protein, int fat, int carbs) {
		return new Values(-1, 0.0F, false, new Nutrients(protein, fat, carbs));
	}

	//                                                        nutrition, saturation, protein, fat, carbs
	private static final Map<Item, Values> VALUES = Map.ofEntries(
			// Meat and fish: raw is a last resort, cooking roughly doubles the value.
			Map.entry(Items.BEEF, food(2, 0.2F, 10, 6, 0)),
			Map.entry(Items.COOKED_BEEF, food(5, 0.6F, 25, 15, 0)),
			Map.entry(Items.PORKCHOP, food(2, 0.2F, 8, 8, 0)),
			Map.entry(Items.COOKED_PORKCHOP, food(5, 0.6F, 20, 20, 0)),
			Map.entry(Items.MUTTON, food(1, 0.2F, 5, 5, 0)),
			Map.entry(Items.COOKED_MUTTON, food(4, 0.6F, 16, 16, 0)),
			Map.entry(Items.CHICKEN, food(1, 0.2F, 6, 2, 0)),
			Map.entry(Items.COOKED_CHICKEN, food(4, 0.5F, 24, 6, 0)),
			Map.entry(Items.RABBIT, food(2, 0.2F, 10, 2, 0)),
			Map.entry(Items.COOKED_RABBIT, food(3, 0.5F, 18, 3, 0)),
			Map.entry(Items.COD, food(1, 0.1F, 6, 1, 0)),
			Map.entry(Items.COOKED_COD, food(3, 0.5F, 18, 3, 0)),
			Map.entry(Items.SALMON, food(1, 0.1F, 5, 4, 0)),
			Map.entry(Items.COOKED_SALMON, food(4, 0.6F, 18, 12, 0)),
			Map.entry(Items.TROPICAL_FISH, food(1, 0.1F, 4, 1, 0)),
			Map.entry(Items.PUFFERFISH, food(1, 0.1F, 4, 1, 0)),
			Map.entry(Items.ROTTEN_FLESH, food(2, 0.1F, 6, 2, 0)),
			Map.entry(Items.SPIDER_EYE, food(1, 0.4F, 4, 1, 0)),
			// Crops and plants.
			Map.entry(Items.APPLE, food(2, 0.3F, 0, 0, 14)),
			Map.entry(Items.BREAD, food(3, 0.5F, 4, 1, 22)),
			Map.entry(Items.CARROT, food(2, 0.4F, 1, 0, 12)),
			Map.entry(Items.POTATO, food(1, 0.2F, 1, 0, 8)),
			Map.entry(Items.BAKED_POTATO, food(3, 0.5F, 2, 0, 22)),
			Map.entry(Items.POISONOUS_POTATO, food(1, 0.2F, 0, 0, 4)),
			Map.entry(Items.BEETROOT, food(1, 0.4F, 0, 0, 8)),
			Map.entry(Items.MELON_SLICE, food(1, 0.2F, 0, 0, 8)),
			Map.entry(Items.SWEET_BERRIES, food(1, 0.1F, 0, 0, 6)),
			Map.entry(Items.GLOW_BERRIES, food(1, 0.1F, 0, 0, 6)),
			Map.entry(Items.DRIED_KELP, food(1, 0.2F, 2, 0, 4)),
			Map.entry(Items.CHORUS_FRUIT, alwaysEdible(2, 0.3F, 0, 0, 10)),
			Map.entry(Items.GOLDEN_CARROT, vanillaFood(2, 0, 16)),
			Map.entry(Items.GOLDEN_APPLE, vanillaFood(0, 0, 20)),
			Map.entry(Items.ENCHANTED_GOLDEN_APPLE, vanillaFood(10, 10, 20)),
			// Cooked dishes.
			Map.entry(Items.COOKIE, food(1, 0.1F, 0, 3, 8)),
			Map.entry(Items.PUMPKIN_PIE, food(5, 0.3F, 4, 10, 24)),
			Map.entry(Items.HONEY_BOTTLE, alwaysEdible(3, 0.1F, 0, 0, 24)),
			Map.entry(Items.MUSHROOM_STEW, food(4, 0.5F, 8, 4, 16)),
			Map.entry(Items.BEETROOT_SOUP, food(4, 0.5F, 2, 2, 22)),
			Map.entry(Items.SUSPICIOUS_STEW, alwaysEdible(4, 0.5F, 8, 4, 16)),
			Map.entry(Items.RABBIT_STEW, food(6, 0.5F, 24, 8, 18))
	);

	private HungerFoods() {
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
		}));
	}
}
