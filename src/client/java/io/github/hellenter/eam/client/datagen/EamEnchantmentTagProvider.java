package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.arrows.ArrowRecoveryModule;
import io.github.hellenter.eam.world.enchanting.EnchantingTags;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.concurrent.CompletableFuture;

/**
 * Which enchantments a player can choose at the table, and whether they cost diamonds or emeralds. Treasure ones
 * (mending, frost walker, soul speed, swift sneak, wind burst) and curses are left out: they come only from loot.
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
				Enchantments.BREACH, Enchantments.LUNGE);
		builder(EnchantingTags.SUPPORTIVE).add(
				Enchantments.PROTECTION, Enchantments.FIRE_PROTECTION, Enchantments.BLAST_PROTECTION,
				Enchantments.PROJECTILE_PROTECTION, Enchantments.FEATHER_FALLING, Enchantments.RESPIRATION,
				Enchantments.AQUA_AFFINITY, Enchantments.DEPTH_STRIDER, Enchantments.EFFICIENCY, Enchantments.SILK_TOUCH,
				Enchantments.UNBREAKING, Enchantments.FORTUNE, Enchantments.LUCK_OF_THE_SEA, Enchantments.LURE,
				Enchantments.INFINITY, Enchantments.LOYALTY, Enchantments.RIPTIDE, ArrowRecoveryModule.ARROW_RECOVERY);
		// Arrow Recovery is an ordinary enchantment: on the table, on books, in loot and trades.
		builder(EnchantmentTags.NON_TREASURE).add(ArrowRecoveryModule.ARROW_RECOVERY);
		// Villagers trade every book vanilla lets them but Mending: only a duergar runecarver sells that one.
		builder(EnchantmentTags.TRADEABLE).setReplace(true)
				.forceAddTag(EnchantmentTags.NON_TREASURE)
				.add(Enchantments.BINDING_CURSE, Enchantments.VANISHING_CURSE, Enchantments.FROST_WALKER);
	}
}
