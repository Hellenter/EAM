package io.github.hellenter.eam.world.enchanting;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;

public final class EnchantingTags {
	/** Attack enchantments: chosen at the table for the offensive resource (diamonds). */
	public static final TagKey<Enchantment> OFFENSIVE = TagKey.create(Registries.ENCHANTMENT, Eam.id("offensive"));
	/** Tool, armor and utility enchantments: chosen at the table for the supportive resource (emeralds). */
	public static final TagKey<Enchantment> SUPPORTIVE = TagKey.create(Registries.ENCHANTMENT, Eam.id("supportive"));

	private EnchantingTags() {
	}
}
