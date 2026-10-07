package io.github.hellenter.eam.survival.hunger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.ConsumableListener;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Item component {@code eam:nutrients}: protein, fat and carbs a food adds to the player's {@link Nutrition}.
 * Vanilla calls every {@link ConsumableListener} component when a consumable finishes, so no mixin is needed.
 */
public record Nutrients(int protein, int fat, int carbs) implements ConsumableListener, TooltipProvider {
	private static final Codec<Integer> AMOUNT = Codec.intRange(0, Nutrition.MAX);

	public static final Codec<Nutrients> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			AMOUNT.optionalFieldOf("protein", 0).forGetter(Nutrients::protein),
			AMOUNT.optionalFieldOf("fat", 0).forGetter(Nutrients::fat),
			AMOUNT.optionalFieldOf("carbs", 0).forGetter(Nutrients::carbs)
	).apply(instance, Nutrients::new));

	public static final StreamCodec<ByteBuf, Nutrients> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Nutrients::protein,
			ByteBufCodecs.VAR_INT, Nutrients::fat,
			ByteBufCodecs.VAR_INT, Nutrients::carbs,
			Nutrients::new
	);

	@Override
	public void onConsume(Level level, LivingEntity user, ItemStack stack, Consumable consumable) {
		if (user instanceof ServerPlayer player) {
			HungerModule.eat(player, this);
		}
	}

	@Override
	public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
		consumer.accept(Component.translatable("item.eam.nutrients", protein, fat, carbs).withStyle(ChatFormatting.GRAY));
	}
}
