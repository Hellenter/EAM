package io.github.hellenter.eam.survival.thirst;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.function.Consumer;

/**
 * Leather flask: use on a water source to fill it, use elsewhere to drink. A filtered flask carries the
 * {@link ThirstModule#FILTER} component; the rules are in {@link FlaskLogic}.
 */
public class FlaskItem extends Item {
	private static final int DRINK_TICKS = 24;
	private static final int WATER_BAR_COLOR = 0x3478DC;

	public FlaskItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack flask = player.getItemInHand(hand);
		FlaskWater water = flask.getOrDefault(ThirstModule.FLASK_WATER, FlaskWater.EMPTY);

		BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
		if (hit.getType() == HitResult.Type.BLOCK && level.mayInteract(player, hit.getBlockPos())
				&& level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
			if (water.isFull()) {
				return InteractionResult.PASS;
			}
			if (player instanceof ServerPlayer serverPlayer) {
				ThirstModule.fillFlask(serverPlayer, flask);
			}
			level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
			level.gameEvent(player, GameEvent.FLUID_PICKUP, hit.getBlockPos());
			return InteractionResult.SUCCESS;
		}

		if (water.isEmpty() || !ThirstModule.canDrink(player)) {
			return InteractionResult.FAIL;
		}
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack flask, Level level, LivingEntity entity) {
		if (entity instanceof ServerPlayer player) {
			ThirstModule.drinkFromFlask(player, flask);
		}
		level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS,
				0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
		entity.gameEvent(GameEvent.DRINK);
		return flask;
	}

	@Override
	public int getUseDuration(ItemStack flask, LivingEntity user) {
		return DRINK_TICKS;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack flask) {
		return ItemUseAnimation.DRINK;
	}

	/** A filtered flask shows its filter like durability; a plain one shows how much water is left. */
	@Override
	public boolean isBarVisible(ItemStack flask) {
		Integer filter = flask.get(ThirstModule.FILTER);
		return filter != null ? filter < FlaskLogic.FILTER_USES : !flask.getOrDefault(ThirstModule.FLASK_WATER, FlaskWater.EMPTY).isEmpty();
	}

	@Override
	public int getBarWidth(ItemStack flask) {
		Integer filter = flask.get(ThirstModule.FILTER);
		if (filter != null) {
			return Mth.clamp(Math.round(13.0F * filter / FlaskLogic.FILTER_USES), 0, 13);
		}
		return Math.round(13.0F * flask.getOrDefault(ThirstModule.FLASK_WATER, FlaskWater.EMPTY).amount() / FlaskLogic.CAPACITY);
	}

	@Override
	public int getBarColor(ItemStack flask) {
		Integer filter = flask.get(ThirstModule.FILTER);
		if (filter != null) {
			return Mth.hsvToRgb(Math.max(0.0F, (float) filter / FlaskLogic.FILTER_USES) / 3.0F, 1.0F, 1.0F);
		}
		return WATER_BAR_COLOR;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(ItemStack flask, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		FlaskWater water = flask.getOrDefault(ThirstModule.FLASK_WATER, FlaskWater.EMPTY);
		builder.accept(Component.translatable("item.eam.flask.water", water.amount(), FlaskLogic.CAPACITY).withStyle(ChatFormatting.BLUE));

		Integer filter = flask.get(ThirstModule.FILTER);
		if (filter != null && filter > 0) {
			builder.accept(Component.translatable("item.eam.flask.filter", filter, FlaskLogic.FILTER_USES).withStyle(ChatFormatting.GRAY));
		} else if (filter != null) {
			builder.accept(Component.translatable("item.eam.flask.filter_spent").withStyle(ChatFormatting.RED));
		}
	}
}
