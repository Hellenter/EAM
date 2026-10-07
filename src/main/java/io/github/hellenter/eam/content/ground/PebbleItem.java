package io.github.hellenter.eam.content.ground;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Pebble: use throws it like a snowball, sneak-use on a block lays it down. */
public class PebbleItem extends BlockItem {
	/** Default delay between throws (the item's use_cooldown component), rounded down to whole ticks: 13 ticks = 650 ms. */
	public static final float THROW_COOLDOWN_SECONDS = 0.667F;

	public PebbleItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player != null && !player.isSecondaryUseActive()) {
			// Not sneaking: fall through to use(), which throws.
			return InteractionResult.PASS;
		}
		return super.useOn(context);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack itemStack = player.getItemInHand(hand);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL,
				0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
		if (level instanceof ServerLevel serverLevel) {
			Projectile.spawnProjectileFromRotation(ThrownPebble::new, serverLevel, itemStack, player, 0.0F, 1.5F, 1.0F);
		}
		player.awardStat(Stats.ITEM_USED.get(this));
		itemStack.consume(1, player);
		return InteractionResult.SUCCESS;
	}
}
