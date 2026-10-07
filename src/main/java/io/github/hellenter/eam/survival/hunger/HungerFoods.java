package io.github.hellenter.eam.survival.hunger;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Map;

/**
 * MITE-style food values: about 60% of vanilla nutrition and less saturation, so raw food barely helps and
 * cooked meat and stews matter. Golden apples and golden carrots keep their vanilla values.
 */
final class HungerFoods {
	private record Values(int nutrition, float saturationModifier, boolean alwaysEdible) {
		Values(int nutrition, float saturationModifier) {
			this(nutrition, saturationModifier, false);
		}
	}

	private static final Map<Item, Values> VALUES = Map.ofEntries(
			// Meat and fish: raw is a last resort, cooking roughly doubles the value.
			Map.entry(Items.BEEF, new Values(2, 0.2F)),
			Map.entry(Items.COOKED_BEEF, new Values(5, 0.6F)),
			Map.entry(Items.PORKCHOP, new Values(2, 0.2F)),
			Map.entry(Items.COOKED_PORKCHOP, new Values(5, 0.6F)),
			Map.entry(Items.MUTTON, new Values(1, 0.2F)),
			Map.entry(Items.COOKED_MUTTON, new Values(4, 0.6F)),
			Map.entry(Items.CHICKEN, new Values(1, 0.2F)),
			Map.entry(Items.COOKED_CHICKEN, new Values(4, 0.5F)),
			Map.entry(Items.RABBIT, new Values(2, 0.2F)),
			Map.entry(Items.COOKED_RABBIT, new Values(3, 0.5F)),
			Map.entry(Items.COD, new Values(1, 0.1F)),
			Map.entry(Items.COOKED_COD, new Values(3, 0.5F)),
			Map.entry(Items.SALMON, new Values(1, 0.1F)),
			Map.entry(Items.COOKED_SALMON, new Values(4, 0.6F)),
			Map.entry(Items.TROPICAL_FISH, new Values(1, 0.1F)),
			Map.entry(Items.PUFFERFISH, new Values(1, 0.1F)),
			Map.entry(Items.ROTTEN_FLESH, new Values(2, 0.1F)),
			Map.entry(Items.SPIDER_EYE, new Values(1, 0.4F)),
			// Crops and plants.
			Map.entry(Items.APPLE, new Values(2, 0.3F)),
			Map.entry(Items.BREAD, new Values(3, 0.5F)),
			Map.entry(Items.CARROT, new Values(2, 0.4F)),
			Map.entry(Items.POTATO, new Values(1, 0.2F)),
			Map.entry(Items.BAKED_POTATO, new Values(3, 0.5F)),
			Map.entry(Items.POISONOUS_POTATO, new Values(1, 0.2F)),
			Map.entry(Items.BEETROOT, new Values(1, 0.4F)),
			Map.entry(Items.MELON_SLICE, new Values(1, 0.2F)),
			Map.entry(Items.SWEET_BERRIES, new Values(1, 0.1F)),
			Map.entry(Items.GLOW_BERRIES, new Values(1, 0.1F)),
			Map.entry(Items.DRIED_KELP, new Values(1, 0.2F)),
			Map.entry(Items.CHORUS_FRUIT, new Values(2, 0.3F, true)),
			// Cooked dishes.
			Map.entry(Items.COOKIE, new Values(1, 0.1F)),
			Map.entry(Items.PUMPKIN_PIE, new Values(5, 0.3F)),
			Map.entry(Items.HONEY_BOTTLE, new Values(3, 0.1F, true)),
			Map.entry(Items.MUSHROOM_STEW, new Values(4, 0.5F)),
			Map.entry(Items.BEETROOT_SOUP, new Values(4, 0.5F)),
			Map.entry(Items.SUSPICIOUS_STEW, new Values(4, 0.5F, true)),
			Map.entry(Items.RABBIT_STEW, new Values(6, 0.5F))
	);

	private HungerFoods() {
	}

	static void register() {
		DefaultItemComponentEvents.MODIFY.register(context -> context.modify(VALUES::containsKey, (builder, item) -> {
			Values values = VALUES.get(item);
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
