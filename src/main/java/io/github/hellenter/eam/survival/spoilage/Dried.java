package io.github.hellenter.eam.survival.spoilage;

import io.github.hellenter.eam.survival.thirst.Thirst;
import io.github.hellenter.eam.survival.thirst.ThirstModule;

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
 * Item component {@code eam:dried}: the food was dried on a dryer. It keeps longer (see {@link SpoilageModule#dry}) and
 * eating it costs {@code water} points of thirst. Vanilla calls every {@link ConsumableListener} component when a
 * consumable finishes, so no mixin is needed.
 */
public record Dried(int water) implements ConsumableListener, TooltipProvider {
	public static final Codec<Dried> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(0, Thirst.MAX).optionalFieldOf("water", 1).forGetter(Dried::water)
	).apply(instance, Dried::new));

	public static final StreamCodec<ByteBuf, Dried> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(Dried::new, Dried::water);

	@Override
	public void onConsume(Level level, LivingEntity user, ItemStack stack, Consumable consumable) {
		if (water > 0 && user instanceof ServerPlayer player) {
			ThirstModule.drain(player, water);
		}
	}

	@Override
	public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
		consumer.accept(Component.translatable("tooltip.eam.dried").withStyle(ChatFormatting.GOLD));
	}
}
