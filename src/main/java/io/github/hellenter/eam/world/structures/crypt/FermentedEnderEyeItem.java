package io.github.hellenter.eam.world.structures.crypt;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/**
 * Thrown like an ender eye, but flies to the nearest crypt entrance. The flying eye is the vanilla one carrying this
 * stack, so it shows this item and drops it back four times in five.
 */
public class FermentedEnderEyeItem extends Item {
	public FermentedEnderEyeItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		player.startUsingItem(hand);
		if (level instanceof ServerLevel serverLevel) {
			BlockPos entrance = CryptLocator.nearestEntrance(serverLevel, player.blockPosition());
			if (entrance == null) return InteractionResult.CONSUME;

			EyeOfEnder eye = new EyeOfEnder(level, player.getX(), player.getY(0.5), player.getZ());
			eye.setItem(stack);
			eye.signalTo(Vec3.atLowerCornerOf(entrance));
			level.gameEvent(GameEvent.PROJECTILE_SHOOT, eye.position(), GameEvent.Context.of(player));
			level.addFreshEntity(eye);
			float pitch = Mth.lerp(level.getRandom().nextFloat(), 0.33F, 0.5F);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.NEUTRAL, 1.0F, pitch);
			stack.consume(1, player);
			player.awardStat(Stats.ITEM_USED.get(this));
		}
		return InteractionResult.SUCCESS_SERVER;
	}
}
