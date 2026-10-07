package io.github.hellenter.eam.world.items;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

/**
 * Changes default stack sizes MITE-style by editing the {@code max_stack_size} item component.
 * The rules are in {@link StackSizeLogic}.
 */
public final class StackSizeModule {
	private StackSizeModule() {
	}

	public static void init() {
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

	private static boolean placesSameNamedBlock(Item item) {
		return item instanceof BlockItem blockItem
				&& BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).equals(BuiltInRegistries.ITEM.getKey(item));
	}
}
