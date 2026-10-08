package io.github.hellenter.eam.content.tools;

import io.github.hellenter.eam.content.silver.SilverGear;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ToolMaterial;

/**
 * Knives after flint: copper and silver (tier 2), iron (tier 3). They hit like the flint knife scaled by tier, backstab
 * the same way, and go in the crafting table's tool slots. Durability counts hits.
 */
public final class MetalKnives {
	public static final ToolMaterial COPPER = new ToolMaterial(BlockTags.INCORRECT_FOR_COPPER_TOOL, 80, 1.5F, 0.0F,
			ToolMaterial.COPPER.enchantmentValue(), ToolMaterial.COPPER.repairItems());
	public static final ToolMaterial SILVER = new ToolMaterial(BlockTags.INCORRECT_FOR_COPPER_TOOL, 80, 1.5F, 0.0F,
			ToolMaterial.COPPER.enchantmentValue(), SilverGear.TOOL_MATERIALS);
	public static final ToolMaterial IRON = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 160, 1.5F, 0.0F,
			ToolMaterial.IRON.enchantmentValue(), ToolMaterial.IRON.repairItems());

	/** Attack damage added to the player's base 1: copper and silver hit for 4, iron for 5 (flint: 3). */
	public static final float TIER_2_DAMAGE = 3.0F;
	public static final float IRON_DAMAGE = 4.0F;

	private MetalKnives() {
	}
}
