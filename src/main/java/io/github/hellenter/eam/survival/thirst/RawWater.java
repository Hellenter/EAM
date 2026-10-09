package io.github.hellenter.eam.survival.thirst;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.ConsumableListener;
import net.minecraft.world.level.Level;

/**
 * Item component {@code eam:raw_water}: the item holds unboiled water, so drinking it may cause food poisoning
 * like a sip straight from a water source.
 */
public record RawWater() implements ConsumableListener {
	public static final RawWater INSTANCE = new RawWater();

	public static final Codec<RawWater> CODEC = MapCodec.unitCodec(INSTANCE);
	public static final StreamCodec<ByteBuf, RawWater> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public void onConsume(Level level, LivingEntity user, ItemStack stack, Consumable consumable) {
		if (user instanceof ServerPlayer player) {
			ThirstModule.drinkRawWater(player);
		}
	}
}
