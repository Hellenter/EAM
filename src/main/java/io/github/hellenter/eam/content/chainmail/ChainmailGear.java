package io.github.hellenter.eam.content.chainmail;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.mithril.MithrilGear;
import io.github.hellenter.eam.content.silver.SilverGear;

import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

/**
 * Chainmail of rings, one set per metal. Each set protects a little better than the plate armor of the tier below and
 * lasts as long as the plate armor of its own metal; it is repaired with its own metal. The vanilla chainmail is the
 * iron set. The ancient metal set is {@link io.github.hellenter.eam.content.ancient.AncientMetalGear#CHAINMAIL_ARMOR}.
 * Defense values are before the gear module's cut; the totals after it are in each comment.
 */
public final class ChainmailGear {
	public static final ResourceKey<EquipmentAsset> COPPER_ASSET = asset("copper_chainmail");
	public static final ResourceKey<EquipmentAsset> SILVER_ASSET = asset("silver_chainmail");
	public static final ResourceKey<EquipmentAsset> MITHRIL_ASSET = asset("mithril_chainmail");

	/** 1/3/2/1 = 7 after the cut (leather 6); copper's durability, halved by the gear module like copper armor. */
	public static final ArmorMaterial COPPER = new ArmorMaterial(ArmorMaterials.COPPER.durability(), defense(1, 4, 3, 1, 4),
			ArmorMaterials.COPPER.enchantmentValue(), ArmorMaterials.CHAINMAIL.equipSound(), 0.0F, 0.0F,
			ItemTags.REPAIRS_COPPER_ARMOR, COPPER_ASSET);

	/** Protects like copper chainmail and lasts like silver armor. */
	public static final ArmorMaterial SILVER = new ArmorMaterial(ArmorMaterials.COPPER.durability(), defense(1, 4, 3, 1, 4),
			ArmorMaterials.COPPER.enchantmentValue(), ArmorMaterials.CHAINMAIL.equipSound(), 0.0F, 0.0F,
			SilverGear.REPAIRS_ARMOR, SILVER_ASSET);

	/**
	 * The vanilla chainmail: 2/4/2/1 = 9 after the cut (copper and silver armor 8), iron's durability. Only its defense
	 * is used: the gear module puts these armor points on the vanilla items.
	 */
	public static final ArmorMaterial IRON = new ArmorMaterial(ArmorMaterials.CHAINMAIL.durability(), defense(2, 5, 3, 1, 4),
			ArmorMaterials.CHAINMAIL.enchantmentValue(), ArmorMaterials.CHAINMAIL.equipSound(), 0.0F, 0.0F,
			ItemTags.REPAIRS_CHAIN_ARMOR, EquipmentAssets.CHAINMAIL);

	/** 2/6/5/2 = 15 after the cut (ancient metal plate 14, mithril plate 16); mithril's durability. */
	public static final ArmorMaterial MITHRIL = new ArmorMaterial(ArmorMaterials.DIAMOND.durability(), defense(3, 7, 6, 3, 9),
			ArmorMaterials.DIAMOND.enchantmentValue(), ArmorMaterials.CHAINMAIL.equipSound(), 1.0F, 0.0F,
			MithrilGear.REPAIRS_ARMOR, MITHRIL_ASSET);

	private ChainmailGear() {
	}

	private static ResourceKey<EquipmentAsset> asset(String name) {
		return ResourceKey.create(EquipmentAssets.ROOT_ID, Eam.id(name));
	}

	public static Map<ArmorType, Integer> defense(int helmet, int chestplate, int leggings, int boots, int body) {
		return Map.of(ArmorType.HELMET, helmet, ArmorType.CHESTPLATE, chestplate, ArmorType.LEGGINGS, leggings,
				ArmorType.BOOTS, boots, ArmorType.BODY, body);
	}
}
