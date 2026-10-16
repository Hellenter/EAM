package io.github.hellenter.eam.content.adamantium;

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
 * Adamantium tools and armor: copies of the vanilla netherite materials with their own repair items and look. Adamantium
 * is tier 5, above mithril, and netherite is now its upgrade: a smithing table turns adamantium gear into netherite gear,
 * which lasts three times as long ({@link io.github.hellenter.eam.world.tools.GearLogic}). The {@code gear} module scales
 * the armor like every other set.
 */
public final class AdamantiumGear {
	/** Items that repair adamantium tools in an anvil. */
	public static final TagKey<Item> TOOL_MATERIALS = TagKey.create(Registries.ITEM, Eam.id("adamantium_tool_materials"));
	/** Items that repair adamantium armor in an anvil. */
	public static final TagKey<Item> REPAIRS_ARMOR = TagKey.create(Registries.ITEM, Eam.id("repairs_adamantium_armor"));

	public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Eam.id("adamantium"));

	/** Mines, hits and lasts like vanilla netherite. */
	public static final ToolMaterial TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, ToolMaterial.NETHERITE.durability(),
			ToolMaterial.NETHERITE.speed(), ToolMaterial.NETHERITE.attackDamageBonus(), ToolMaterial.NETHERITE.enchantmentValue(),
			TOOL_MATERIALS);

	/** The adamantium knife: the iron knife's speed, twice the mithril knife's uses. */
	public static final ToolMaterial KNIFE = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 640, 1.5F, 0.0F,
			ToolMaterial.NETHERITE.enchantmentValue(), TOOL_MATERIALS);

	/** Attack damage added to the player's base 1: the adamantium knife hits for 7 (mithril: 6). */
	public static final float KNIFE_DAMAGE = 6.0F;

	/** Protects and lasts like vanilla netherite, knockback resistance included. */
	public static final ArmorMaterial ARMOR = new ArmorMaterial(ArmorMaterials.NETHERITE.durability(), ArmorMaterials.NETHERITE.defense(),
			ArmorMaterials.NETHERITE.enchantmentValue(), ArmorMaterials.NETHERITE.equipSound(), ArmorMaterials.NETHERITE.toughness(),
			ArmorMaterials.NETHERITE.knockbackResistance(), REPAIRS_ARMOR, ASSET);

	private AdamantiumGear() {
	}
}
