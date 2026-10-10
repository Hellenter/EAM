package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.world.enchanting.EnchantingTags;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.concurrent.CompletableFuture;

/**
 * Which enchantments a player can choose at the table, and whether they cost diamonds or emeralds. Every enchantment but
 * the curses is here, treasure ones included: the table offers all an item supports, like books on an anvil.
 */
public class EamEnchantmentTagProvider extends FabricTagsProvider<Enchantment> {
	public EamEnchantmentTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, Registries.ENCHANTMENT, registries);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		builder(EnchantingTags.OFFENSIVE).add(
				Enchantments.SHARPNESS, Enchantments.SMITE, Enchantments.BANE_OF_ARTHROPODS, Enchantments.KNOCKBACK,
				Enchantments.FIRE_ASPECT, Enchantments.LOOTING, Enchantments.SWEEPING_EDGE, Enchantments.THORNS,
				Enchantments.POWER, Enchantments.PUNCH, Enchantments.FLAME, Enchantments.MULTISHOT, Enchantments.PIERCING,
				Enchantments.QUICK_CHARGE, Enchantments.IMPALING, Enchantments.CHANNELING, Enchantments.DENSITY,
				Enchantments.BREACH, Enchantments.LUNGE, Enchantments.WIND_BURST);
		builder(EnchantingTags.SUPPORTIVE).add(
				Enchantments.PROTECTION, Enchantments.FIRE_PROTECTION, Enchantments.BLAST_PROTECTION,
				Enchantments.PROJECTILE_PROTECTION, Enchantments.FEATHER_FALLING, Enchantments.RESPIRATION,
				Enchantments.AQUA_AFFINITY, Enchantments.DEPTH_STRIDER, Enchantments.EFFICIENCY, Enchantments.SILK_TOUCH,
				Enchantments.UNBREAKING, Enchantments.FORTUNE, Enchantments.LUCK_OF_THE_SEA, Enchantments.LURE,
				Enchantments.INFINITY, Enchantments.LOYALTY, Enchantments.RIPTIDE, Enchantments.MENDING,
				Enchantments.FROST_WALKER, Enchantments.SOUL_SPEED, Enchantments.SWIFT_SNEAK);
	}
}
