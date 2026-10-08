package io.github.hellenter.eam.world.smelting;

import io.github.hellenter.eam.Eam;

import com.mojang.serialization.MapCodec;

import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Iron is smelted only in the blast furnace: the furnace recipes for iron ores, raw iron and iron gear are overridden by
 * datagen copies that load only while this module is off ({@link #IRON_ONLY}). The blast furnace itself is crafted from
 * any ingot in {@link #INGOTS} instead of iron ingots only. Everything here is data; the flag is {@code blast_furnace_iron}.
 */
public final class BlastFurnaceModule {
	/** Ingots that build the blast furnace: iron and copper, other metals add themselves. */
	public static final TagKey<Item> INGOTS = TagKey.create(Registries.ITEM, Eam.id("blast_furnace_ingots"));

	/** True while the {@code blast_furnace_iron} flag is on; the vanilla recipe copies load under {@code not} of it. */
	public static final ResourceCondition IRON_ONLY = new IronOnlyCondition();

	private static final ResourceConditionType<IronOnlyCondition> IRON_ONLY_TYPE =
			ResourceConditionType.create(Eam.id("blast_furnace_iron"), MapCodec.unit(IronOnlyCondition::new));

	private BlastFurnaceModule() {
	}

	/** The condition type must be known even with the module off, or the recipe files fail to parse. */
	public static void register() {
		ResourceConditions.register(IRON_ONLY_TYPE);
	}

	private record IronOnlyCondition() implements ResourceCondition {
		@Override
		public ResourceConditionType<?> getType() {
			return IRON_ONLY_TYPE;
		}

		@Override
		public boolean test(RegistryOps.RegistryInfoLookup registryInfo) {
			return Eam.config().blastFurnaceIron();
		}
	}
}
