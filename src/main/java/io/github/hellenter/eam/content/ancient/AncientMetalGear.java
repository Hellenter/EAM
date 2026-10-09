package io.github.hellenter.eam.content.ancient;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

/**
 * Ancient metal tools, plate armor and chainmail: between iron and mithril. The metal shares iron's tier 3 (crafting,
 * anvils, mining level), so it fits the tier tags without renumbering mithril; it is better than iron in uses, speed,
 * damage and protection. It has no source yet: it will come from another dimension.
 */
public final class AncientMetalGear {
	/** Items that repair ancient metal tools in an anvil. */
	public static final TagKey<Item> TOOL_MATERIALS = TagKey.create(Registries.ITEM, Eam.id("ancient_metal_tool_materials"));
	/** Items that repair ancient metal armor and chainmail in an anvil. */
	public static final TagKey<Item> REPAIRS_ARMOR = TagKey.create(Registries.ITEM, Eam.id("repairs_ancient_metal_armor"));

	public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Eam.id("ancient_metal"));
	public static final ResourceKey<EquipmentAsset> CHAINMAIL_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID,
			Eam.id("ancient_metal_chainmail"));

	/** Iron's mining level, faster than iron (6) and slower than mithril (8); the gear module sets each tool's uses. */
	public static final ToolMaterial TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 500, 7.0F, 2.5F, 12, TOOL_MATERIALS);

	/** The ancient metal knife: between the iron (160) and mithril (320) knives. */
	public static final ToolMaterial KNIFE = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 240, 1.5F, 0.0F, 12, TOOL_MATERIALS);

	/** Attack damage added to the player's base 1: the ancient metal knife hits for 5.5 (iron 5, mithril 6). */
	public static final float KNIFE_DAMAGE = 4.5F;

	/** Durability multiplier of both sets: iron has 15, mithril (diamond) 33. */
	private static final int ARMOR_DURABILITY = 24;

	/** Before the gear module's cut, which leaves 2/6/4/2 = 14 (iron 12, mithril 16). */
	public static final ArmorMaterial ARMOR = new ArmorMaterial(ARMOR_DURABILITY, defense(3, 7, 5, 2, 8), 12,
			ArmorMaterials.IRON.equipSound(), 1.0F, 0.0F, REPAIRS_ARMOR, ASSET);

	/** Before the gear module's cut, which leaves 2/5/3/1 = 11 (chainmail 10, ancient plate 14). */
	public static final ArmorMaterial CHAINMAIL_ARMOR = new ArmorMaterial(ARMOR_DURABILITY, defense(2, 6, 4, 2, 6), 14,
			ArmorMaterials.CHAINMAIL.equipSound(), 0.0F, 0.0F, REPAIRS_ARMOR, CHAINMAIL_ASSET);

	private AncientMetalGear() {
	}

	private static Map<ArmorType, Integer> defense(int helmet, int chestplate, int leggings, int boots, int body) {
		return Map.of(ArmorType.HELMET, helmet, ArmorType.CHESTPLATE, chestplate, ArmorType.LEGGINGS, leggings,
				ArmorType.BOOTS, boots, ArmorType.BODY, body);
	}
}
