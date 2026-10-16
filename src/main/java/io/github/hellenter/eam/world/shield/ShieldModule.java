package io.github.hellenter.eam.world.shield;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;

/**
 * Shields can be enchanted at the table, and Light as a Feather shortens the time a shield stays disabled after an axe
 * or a boss knocks it out. The effect is the {@code eam:shield_disable_time} enchantment effect, so its numbers are in
 * the enchantment JSON.
 */
public final class ShieldModule {
	public static final ResourceKey<Enchantment> LIGHT_AS_A_FEATHER = ResourceKey.create(Registries.ENCHANTMENT, Eam.id("light_as_a_feather"));

	/** Items Light as a Feather goes on. */
	public static final TagKey<Item> ENCHANTABLE = TagKey.create(Registries.ITEM, Eam.id("enchantable/shield"));

	/** Changes the seconds a shield stays disabled; vanilla's value effects (add, multiply, set) work on it. */
	public static final DataComponentType<EnchantmentValueEffect> DISABLE_TIME = Registry.register(
			BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE, Eam.id("shield_disable_time"),
			DataComponentType.<EnchantmentValueEffect>builder().persistent(EnchantmentValueEffect.CODEC).build());

	/** As enchantable as wooden tools: a shield is mostly planks. */
	private static final int ENCHANTABILITY = 15;

	private ShieldModule() {
	}

	/** The effect component is registered whatever the config says, so the enchantment always loads. */
	public static void register() {
	}

	public static void init() {
		DefaultItemComponentEvents.MODIFY.register(context -> context.modify(item -> item == Items.SHIELD,
				(builder, item) -> builder.set(DataComponents.ENCHANTABLE, new Enchantable(ENCHANTABILITY))));
	}
}
