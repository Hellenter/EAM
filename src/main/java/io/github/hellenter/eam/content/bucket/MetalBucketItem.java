package io.github.hellenter.eam.content.bucket;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A copper, silver, gold or mithril bucket for water and lava, or the empty one. Works like the vanilla bucket, but
 * fills into and empties back into the same metal's bucket instead of the vanilla (iron) one.
 */
public class MetalBucketItem extends BucketItem {
	public MetalBucketItem(Fluid content, Item.Properties properties) {
		super(content, properties);
	}

	/** Vanilla {@link BucketItem#use} with this metal's buckets as the results. */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack itemStack = player.getItemInHand(hand);
		BlockHitResult hitResult = getPlayerPOVHitResult(level, player, this.getFluidContext());
		if (hitResult.getType() != HitResult.Type.BLOCK) {
			return InteractionResult.PASS;
		}

		BlockPos pos = hitResult.getBlockPos();
		Direction direction = hitResult.getDirection();
		BlockPos directionOffsetPos = pos.relative(direction);
		if (!level.mayInteract(player, pos) || !player.mayUseItemAt(directionOffsetPos, direction, itemStack)) {
			return InteractionResult.FAIL;
		}

		BlockState clicked = level.getBlockState(pos);
		BlockPos placePos = clicked.getBlock() instanceof LiquidBlockContainer && this.content == Fluids.WATER ? pos : directionOffsetPos;
		if (this.emptyContents(player, level, placePos, hitResult)) {
			this.checkExtraContent(player, level, itemStack, placePos);
			if (player instanceof ServerPlayer serverPlayer && this.content != Fluids.EMPTY) {
				CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, placePos, itemStack);
			}

			player.awardStat(Stats.ITEM_USED.get(this));
			ItemStack emptyResult = ItemUtils.createFilledResult(itemStack, player, emptyAfterUse(itemStack, player));
			return InteractionResult.SUCCESS.heldItemTransformedTo(emptyResult);
		}

		if (this.content == Fluids.EMPTY && clicked.getBlock() instanceof BucketPickup bucketPickupBlock) {
			ItemStack taken = sameMetal(bucketPickupBlock.pickupBlock(player, level, pos, clicked));
			if (!taken.isEmpty()) {
				player.awardStat(Stats.ITEM_USED.get(this));
				bucketPickupBlock.getPickupSound().ifPresent(soundEvent -> player.playSound(soundEvent, 1.0F, 1.0F));
				level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
				ItemStack result = ItemUtils.createFilledResult(itemStack, player, taken);
				if (player instanceof ServerPlayer serverPlayer) {
					CriteriaTriggers.FILLED_BUCKET.trigger(serverPlayer, taken);
				}

				return InteractionResult.SUCCESS.heldItemTransformedTo(result);
			}
		}

		return InteractionResult.FAIL;
	}

	/** The empty bucket of this metal, or the stack itself for a player who doesn't use items up. */
	public ItemStack emptyAfterUse(ItemStack itemStack, Player player) {
		return player.hasInfiniteMaterials() ? itemStack : new ItemStack(emptyOf(this));
	}

	/** Turns a vanilla filled bucket from a pickup into this metal's one with the same contents. */
	private ItemStack sameMetal(ItemStack vanilla) {
		BucketContent content = MetalBuckets.contentOf(vanilla.getItem());
		Item filled = content == null ? null : MetalBuckets.withContent(this, content);
		return filled == null ? vanilla : new ItemStack(filled, vanilla.getCount());
	}

	static Item emptyOf(Item bucket) {
		Item empty = MetalBuckets.withContent(bucket, BucketContent.EMPTY);
		return empty == null ? bucket : empty;
	}
}
