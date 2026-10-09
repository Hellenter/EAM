package io.github.hellenter.eam.content.arrows;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import java.util.function.Consumer;

/** Item component {@code eam:arrow_head}: an arrow's extra damage as a share of its base damage, 0.2 is +20%. */
public record ArrowHead(float damageBonus) implements TooltipProvider {
	public static final Codec<ArrowHead> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ExtraCodecs.NON_NEGATIVE_FLOAT.fieldOf("damage_bonus").forGetter(ArrowHead::damageBonus)
	).apply(instance, ArrowHead::new));

	public static final StreamCodec<ByteBuf, ArrowHead> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, ArrowHead::damageBonus,
			ArrowHead::new
	);

	@Override
	public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
		consumer.accept(Component.translatable("item.eam.arrow_head", Math.round(damageBonus * 100.0F)).withStyle(ChatFormatting.BLUE));
	}
}
