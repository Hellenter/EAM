package io.github.hellenter.eam.content.bucket;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;

/** A metal bucket of powder snow: places the snow and leaves this metal's empty bucket. */
public class MetalPowderSnowBucketItem extends SolidBucketItem {
	public MetalPowderSnowBucketItem(Item.Properties properties) {
		super(Blocks.POWDER_SNOW, SoundEvents.BUCKET_EMPTY_POWDER_SNOW, properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		InteractionResult placeResult = super.useOn(context);
		Player player = context.getPlayer();
		// The vanilla call above already swapped in a vanilla bucket; put this metal's one there instead.
		if (placeResult.consumesAction() && player != null && !player.hasInfiniteMaterials()) {
			player.setItemInHand(context.getHand(), new ItemStack(MetalBucketItem.emptyOf(this)));
		}
		return placeResult;
	}

	/** Picking powder snow in creative still gives the vanilla bucket. */
	@Override
	public void registerBlocks(Map<Block, Item> map, Item item) {
	}
}
