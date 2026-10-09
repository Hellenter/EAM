package io.github.hellenter.eam.content.mithril;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * Mithril tools and armor: copies of the vanilla diamond materials with their own repair items and look. Diamond gear
 * cannot be crafted, so mithril takes its place at the top. The {@code gear} module scales the armor like every other
 * set: a full mithril set gives 16 armor instead of diamond's 20.
 */
public final class MithrilGear {
	/** Items that repair mithril tools in an anvil. */
	public static final TagKey<Item> TOOL_MATERIALS = TagKey.create(Registries.ITEM, Eam.id("mithril_tool_materials"));
	/** Items that repair mithril armor in an anvil. */
	public static final TagKey<Item> REPAIRS_ARMOR = TagKey.create(Registries.ITEM, Eam.id("repairs_mithril_armor"));

	public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Eam.id("mithril"));

	/** Mines, hits and lasts like diamond. */
	public static final ToolMaterial TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, ToolMaterial.DIAMOND.durability(),
			ToolMaterial.DIAMOND.speed(), ToolMaterial.DIAMOND.attackDamageBonus(), ToolMaterial.DIAMOND.enchantmentValue(), TOOL_MATERIALS);

	/** The mithril knife: the iron knife's speed, twice its uses. */
	public static final ToolMaterial KNIFE = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 320, 1.5F, 0.0F,
			ToolMaterial.DIAMOND.enchantmentValue(), TOOL_MATERIALS);

	/** Attack damage added to the player's base 1: the mithril knife hits for 6 (iron: 5). */
	public static final float KNIFE_DAMAGE = 5.0F;

	/** Protects and lasts like diamond. */
	public static final ArmorMaterial ARMOR = new ArmorMaterial(ArmorMaterials.DIAMOND.durability(), ArmorMaterials.DIAMOND.defense(),
			ArmorMaterials.DIAMOND.enchantmentValue(), ArmorMaterials.DIAMOND.equipSound(), ArmorMaterials.DIAMOND.toughness(),
			ArmorMaterials.DIAMOND.knockbackResistance(), REPAIRS_ARMOR, ASSET);

	private MithrilGear() {
	}
}
