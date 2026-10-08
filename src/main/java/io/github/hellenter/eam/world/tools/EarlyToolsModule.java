package io.github.hellenter.eam.world.tools;

import io.github.hellenter.eam.Eam;

import com.mojang.serialization.MapCodec;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;

import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Predicate;

/**
 * Stone tools and the wooden pickaxe and axe cannot be crafted: flint tools come first. The items stay registered so old
 * worlds and loot keep working. Their vanilla recipes are overridden by datagen copies that load only while this module is
 * off ({@link #REMOVED}), and the items are hidden from the creative tabs.
 */
public final class EarlyToolsModule {
	public static final List<Item> REMOVED_TOOLS = List.of(
			Items.WOODEN_PICKAXE,
			Items.WOODEN_AXE,
			Items.STONE_SWORD,
			Items.STONE_SHOVEL,
			Items.STONE_PICKAXE,
			Items.STONE_AXE,
			Items.STONE_HOE,
			Items.STONE_SPEAR
	);

	/** True while the {@code remove_early_tools} flag is on; the recipe copies load under {@code not} of it. */
	public static final ResourceCondition REMOVED = new RemovedCondition();

	private static final ResourceConditionType<RemovedCondition> REMOVED_TYPE =
			ResourceConditionType.create(Eam.id("early_tools_removed"), MapCodec.unit(RemovedCondition::new));

	private EarlyToolsModule() {
	}

	/** The condition type must be known even with the module off, or the recipe files fail to parse. */
	public static void register() {
		ResourceConditions.register(REMOVED_TYPE);
	}

	public static void init() {
		Predicate<ItemStack> removed = stack -> REMOVED_TOOLS.stream().anyMatch(stack::is);
		CreativeModeTabEvents.MODIFY_OUTPUT_ALL.register((tab, output) -> {
			output.getDisplayStacks().removeIf(removed);
			output.getSearchTabStacks().removeIf(removed);
		});
	}

	private record RemovedCondition() implements ResourceCondition {
		@Override
		public ResourceConditionType<?> getType() {
			return REMOVED_TYPE;
		}

		@Override
		public boolean test(RegistryOps.RegistryInfoLookup registryInfo) {
			return Eam.config().removeEarlyTools();
		}
	}
}
