package io.github.hellenter.eam.world.items;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/**
 * Changes default stack sizes MITE-style by editing the {@code max_stack_size} item component.
 * The rules are in {@link StackSizeLogic}.
 */
public final class StackSizeModule {
	private static boolean enabled;

	private StackSizeModule() {
	}

	public static void init() {
		enabled = true;
		DefaultItemComponentEvents.MODIFY.register(context -> context.modify(item -> true, (builder, item) -> {
			Integer vanillaMax = builder.get(DataComponents.MAX_STACK_SIZE);
			if (vanillaMax == null) {
				return;
			}

			int max = StackSizeLogic.maxStackSize(
					BuiltInRegistries.ITEM.getKey(item).getPath(),
					placesSameNamedBlock(item),
					builder.get(DataComponents.FOOD) != null,
					vanillaMax
			);
			if (max != vanillaMax) {
				builder.set(DataComponents.MAX_STACK_SIZE, max);
			}
		}));
	}

	/** Per-stack limit, for stacks whose limit depends on more than the item (water bottles). */
	public static int maxStackSize(ItemInstance stack, int current) {
		if (!enabled || !stack.is(Items.POTION)) {
			return current;
		}
		PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
		return StackSizeLogic.stackMaxStackSize(contents != null && contents.is(Potions.WATER), current);
	}

	private static boolean placesSameNamedBlock(Item item) {
		return item instanceof BlockItem blockItem
				&& BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).equals(BuiltInRegistries.ITEM.getKey(item));
	}
}
