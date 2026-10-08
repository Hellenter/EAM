package io.github.hellenter.eam.content.silver;

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
 * Silver tools and armor: copies of the copper materials with their own repair items and look. Durability and armor
 * points follow copper's, including the {@code gear} module's cuts; the undead bonuses are item components set in
 * {@link io.github.hellenter.eam.content.EamItems}.
 */
public final class SilverGear {
	/** Items that repair silver tools in an anvil. */
	public static final TagKey<Item> TOOL_MATERIALS = TagKey.create(Registries.ITEM, Eam.id("silver_tool_materials"));
	/** Items that repair silver armor in an anvil. */
	public static final TagKey<Item> REPAIRS_ARMOR = TagKey.create(Registries.ITEM, Eam.id("repairs_silver_armor"));

	public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Eam.id("silver"));

	/** Mines like copper. */
	public static final ToolMaterial TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_COPPER_TOOL, ToolMaterial.COPPER.durability(),
			ToolMaterial.COPPER.speed(), ToolMaterial.COPPER.attackDamageBonus(), ToolMaterial.COPPER.enchantmentValue(), TOOL_MATERIALS);

	/** Protects like copper. */
	public static final ArmorMaterial ARMOR = new ArmorMaterial(ArmorMaterials.COPPER.durability(), ArmorMaterials.COPPER.defense(),
			ArmorMaterials.COPPER.enchantmentValue(), ArmorMaterials.COPPER.equipSound(), ArmorMaterials.COPPER.toughness(),
			ArmorMaterials.COPPER.knockbackResistance(), REPAIRS_ARMOR, ASSET);

	/** Extra damage to undead, as a share of the weapon's base damage. */
	public static final float UNDEAD_DAMAGE_BONUS = 0.5F;
	/** Share of an undead mob's hit that each silver armor piece sends back to it: 33% for a full set. */
	public static final float UNDEAD_THORNS_PER_PIECE = 0.0825F;

	private SilverGear() {
	}
}
