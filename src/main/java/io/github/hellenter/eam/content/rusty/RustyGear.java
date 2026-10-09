package io.github.hellenter.eam.content.rusty;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
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
 * Rusty iron tools, rusty iron armor and rusty chainmail: found, never crafted, a little worse than the iron gear they
 * copy. They mine what iron mines, wear out about a quarter sooner, hit one point softer and protect one point less on
 * the chestplate and leggings. They mend with iron on an iron anvil like iron gear.
 */
public final class RustyGear {
	/** Items that repair rusty iron tools in an anvil: iron, like iron tools. */
	public static final TagKey<Item> TOOL_MATERIALS = ItemTags.IRON_TOOL_MATERIALS;
	/** Items that repair rusty armor in an anvil: iron, like iron and chainmail armor. */
	public static final TagKey<Item> REPAIRS_ARMOR = ItemTags.REPAIRS_IRON_ARMOR;

	public static final ResourceKey<EquipmentAsset> IRON_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Eam.id("rusty_iron"));
	public static final ResourceKey<EquipmentAsset> CHAINMAIL_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Eam.id("rusty_chainmail"));

	/** Iron's mining level, a bit slower, one point less damage and fewer uses; the gear module sets each tool's uses. */
	public static final ToolMaterial TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 188, 5.0F, 1.0F, 10, TOOL_MATERIALS);

	/** Durability multiplier of both rusty armor sets: iron and chainmail have 15. */
	private static final int ARMOR_DURABILITY = 11;

	/** Before the gear module's cut, which leaves 2/4/3/1 = 10 (iron: 2/5/4/1 = 12). */
	public static final ArmorMaterial IRON_ARMOR = new ArmorMaterial(ARMOR_DURABILITY, defense(2, 5, 4, 2, 4), 7,
			ArmorMaterials.IRON.equipSound(), 0.0F, 0.0F, REPAIRS_ARMOR, IRON_ASSET);

	/** Before the gear module's cut, which leaves 2/3/2/1 = 8 (iron chainmail: 2/4/2/1 = 9). */
	public static final ArmorMaterial CHAINMAIL_ARMOR = new ArmorMaterial(ARMOR_DURABILITY, defense(2, 4, 3, 1, 3), 9,
			ArmorMaterials.CHAINMAIL.equipSound(), 0.0F, 0.0F, REPAIRS_ARMOR, CHAINMAIL_ASSET);

	private RustyGear() {
	}

	private static Map<ArmorType, Integer> defense(int helmet, int chestplate, int leggings, int boots, int body) {
		return Map.of(ArmorType.HELMET, helmet, ArmorType.CHESTPLATE, chestplate, ArmorType.LEGGINGS, leggings,
				ArmorType.BOOTS, boots, ArmorType.BODY, body);
	}
}
