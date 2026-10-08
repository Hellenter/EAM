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
 * Diamond tools and armor cannot be crafted. The items stay registered so old worlds and loot keep working. Their vanilla
 * recipes are overridden by datagen copies that load only while this module is off ({@link #REMOVED}), and the items are
 * hidden from the creative tabs. The netherite smithing upgrades stay, but have no diamond gear to start from.
 */
public final class DiamondGearModule {
	public static final List<Item> REMOVED_GEAR = List.of(
			Items.DIAMOND_SWORD,
			Items.DIAMOND_SHOVEL,
			Items.DIAMOND_PICKAXE,
			Items.DIAMOND_AXE,
			Items.DIAMOND_HOE,
			Items.DIAMOND_SPEAR,
			Items.DIAMOND_HELMET,
			Items.DIAMOND_CHESTPLATE,
			Items.DIAMOND_LEGGINGS,
			Items.DIAMOND_BOOTS
	);

	/** True while the {@code remove_diamond_gear} flag is on; the recipe copies load under {@code not} of it. */
	public static final ResourceCondition REMOVED = new RemovedCondition();

	private static final ResourceConditionType<RemovedCondition> REMOVED_TYPE =
			ResourceConditionType.create(Eam.id("diamond_gear_removed"), MapCodec.unit(RemovedCondition::new));

	private DiamondGearModule() {
	}

	/** The condition type must be known even with the module off, or the recipe files fail to parse. */
	public static void register() {
		ResourceConditions.register(REMOVED_TYPE);
	}

	public static void init() {
		Predicate<ItemStack> removed = stack -> REMOVED_GEAR.stream().anyMatch(stack::is);
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
			return Eam.config().removeDiamondGear();
		}
	}
}
