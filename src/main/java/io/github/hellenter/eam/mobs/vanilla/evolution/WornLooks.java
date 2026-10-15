package io.github.hellenter.eam.mobs.vanilla.evolution;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.chainmail.ChainmailGear;
import io.github.hellenter.eam.content.rusty.RustyGear;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

import java.util.Map;
import java.util.Optional;

/**
 * Gear handed out by monster evolution is drawn worn: chipped, notched and stained weapons, armor scuffed, torn and
 * holed. Only the look changes (the item_model and the equippable asset of the stack); the moment the item drops it is
 * mended back to the plain item.
 */
public final class WornLooks {
	/** Worn weapon looks: {@code assets/eam/items/<id>.json}. */
	public static final Map<Item, Identifier> WEAPONS = Map.of(
			Items.WOODEN_SWORD, Eam.id("worn_wooden_sword"),
			Items.WOODEN_AXE, Eam.id("worn_wooden_axe"),
			Items.WOODEN_SHOVEL, Eam.id("worn_wooden_shovel"),
			Items.COPPER_SWORD, Eam.id("worn_copper_sword"),
			Items.COPPER_AXE, Eam.id("worn_copper_axe"),
			Items.COPPER_SHOVEL, Eam.id("worn_copper_shovel"),
			EamItems.RUSTY_IRON_SWORD, Eam.id("worn_rusty_iron_sword"),
			EamItems.RUSTY_IRON_AXE, Eam.id("worn_rusty_iron_axe"),
			EamItems.RUSTY_IRON_SHOVEL, Eam.id("worn_rusty_iron_shovel"));

	/** Worn armor looks by the armor's own equipment asset: {@code assets/eam/equipment/<id>.json}. */
	public static final Map<ResourceKey<EquipmentAsset>, ResourceKey<EquipmentAsset>> ARMOR = Map.of(
			EquipmentAssets.LEATHER, asset("worn_leather"),
			EquipmentAssets.COPPER, asset("worn_copper"),
			ChainmailGear.COPPER_ASSET, asset("worn_copper_chainmail"),
			RustyGear.IRON_ASSET, asset("worn_rusty_iron"),
			RustyGear.CHAINMAIL_ASSET, asset("worn_rusty_chainmail"));

	private WornLooks() {
	}

	private static ResourceKey<EquipmentAsset> asset(String name) {
		return ResourceKey.create(EquipmentAssets.ROOT_ID, Eam.id(name));
	}

	/** {@code stack} drawn worn, if it has a worn look; it stays the same item in every other way. */
	public static ItemStack wear(ItemStack stack) {
		Identifier model = WEAPONS.get(stack.getItem());
		if (model != null) {
			stack.set(DataComponents.ITEM_MODEL, model);
		}
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		if (equippable != null && equippable.assetId().isPresent()) {
			ResourceKey<EquipmentAsset> worn = ARMOR.get(equippable.assetId().get());
			if (worn != null) {
				stack.set(DataComponents.EQUIPPABLE, withAsset(equippable, worn));
			}
		}
		return stack;
	}

	/** True if {@code stack} carries one of the worn looks. */
	public static boolean isWorn(ItemStack stack) {
		Identifier model = stack.get(DataComponents.ITEM_MODEL);
		if (model != null && WEAPONS.containsValue(model)) {
			return true;
		}
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		return equippable != null && equippable.assetId().filter(ARMOR::containsValue).isPresent();
	}

	/** Puts back the plain look of the item, keeping its damage, enchantments and everything else. */
	public static void mend(ItemStack stack) {
		// Setting the item's own value drops the stack's override.
		Item item = stack.getItem();
		stack.set(DataComponents.ITEM_MODEL, item.components().get(DataComponents.ITEM_MODEL));
		stack.set(DataComponents.EQUIPPABLE, item.components().get(DataComponents.EQUIPPABLE));
	}

	private static Equippable withAsset(Equippable equippable, ResourceKey<EquipmentAsset> asset) {
		return new Equippable(equippable.slot(), equippable.equipSound(), Optional.of(asset), equippable.cameraOverlay(),
				equippable.allowedEntities(), equippable.dispensable(), equippable.swappable(), equippable.damageOnHurt(),
				equippable.equipOnInteract(), equippable.canBeSheared(), equippable.shearingSound());
	}
}
