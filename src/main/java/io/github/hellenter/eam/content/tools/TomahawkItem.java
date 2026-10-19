package io.github.hellenter.eam.content.tools;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * A duergar rider's tomahawk: a light iron axe. It hits and chops like an axe, a little weaker but quicker; hold use to
 * draw it back over the shoulder and let go to throw it, like a trident. It flies about as far as a trident and is
 * picked up where it lands. Only the riders carry them.
 */
public class TomahawkItem extends Item {
	/** Iron tier, a little less durable than an iron axe. */
	public static final ToolMaterial MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 200, 5.5F, 0.0F,
			ToolMaterial.IRON.enchantmentValue(), ToolMaterial.IRON.repairItems());
	/** Attack damage added to the base 1: hits for 6. */
	public static final float ATTACK_DAMAGE = 5.0F;
	public static final float ATTACK_SPEED = -2.6F;
	/** Damage of a player's throw. */
	public static final float THROWN_DAMAGE = 6.0F;
	/** Ticks use must be held before a release throws. */
	private static final int THROW_THRESHOLD = 8;
	private static final float THROW_SPEED = 1.8F;

	public TomahawkItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Component.translatable("item.eam.tomahawk.usage").withStyle(ChatFormatting.GRAY));
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.TRIDENT;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user) {
		return 72000;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player.getItemInHand(hand).nextDamageWillBreak()) {
			return InteractionResult.FAIL;
		}
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
		if (!(entity instanceof Player player) || getUseDuration(stack, entity) - remainingTime < THROW_THRESHOLD
				|| stack.nextDamageWillBreak()) {
			return false;
		}
		player.awardStat(Stats.ITEM_USED.get(this));
		if (level instanceof ServerLevel serverLevel) {
			stack.hurtWithoutBreaking(1, player);
			ItemStack thrown = stack.consumeAndReturn(1, player);
			ThrownTomahawk tomahawk = Projectile.spawnProjectileFromRotation(
					(l, owner, item) -> new ThrownTomahawk(l, owner, item, THROWN_DAMAGE), serverLevel, thrown, player, 0.0F, THROW_SPEED, 1.0F);
			if (player.hasInfiniteMaterials()) {
				tomahawk.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
			}
			level.playSound(null, tomahawk, SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0F, 1.3F);
		}
		return true;
	}
}
