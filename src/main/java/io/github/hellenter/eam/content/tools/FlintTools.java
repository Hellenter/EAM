package io.github.hellenter.eam.content.tools;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

/**
 * Flint tools: the first tools a player can make, needed to chop wood since logs cannot be broken by hand.
 * Durability counts blocks: each block mined costs one point (an attack costs two, as for vanilla axes).
 */
public final class FlintTools {
	/** Items that repair flint tools in an anvil. */
	public static final TagKey<Item> REPAIR_MATERIALS = TagKey.create(Registries.ITEM, Eam.id("flint_tool_materials"));

	/** Breaks 3 logs, slower than a wooden axe (speed 2). */
	public static final ToolMaterial HATCHET = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 3, 1.5F, 0.0F, 5, REPAIR_MATERIALS);
	/** Breaks 10 logs, a bit faster than the hatchet but still slower than a wooden axe. */
	public static final ToolMaterial AXE = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 10, 1.75F, 0.0F, 5, REPAIR_MATERIALS);

	/** Lasts 24 hits; mines plants and cobwebs like a sword. */
	public static final ToolMaterial KNIFE = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 24, 1.5F, 0.0F, 5, REPAIR_MATERIALS);

	/** Attack damage added to the player's base 1; a wooden axe adds 6. */
	public static final float HATCHET_DAMAGE = 3.0F;
	public static final float AXE_DAMAGE = 4.0F;
	/** 3 HP per hit, attacks as fast as a sword. */
	public static final float KNIFE_DAMAGE = 2.0F;
	public static final float KNIFE_ATTACK_SPEED = -2.4F;
	public static final float HATCHET_ATTACK_SPEED = -3.0F;
	public static final float AXE_ATTACK_SPEED = -3.2F;

	private FlintTools() {
	}
}
