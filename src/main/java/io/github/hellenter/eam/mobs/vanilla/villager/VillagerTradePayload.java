package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent by the trade button of a villager's inventory: the server closes it and opens the villager's trades. */
public record VillagerTradePayload() implements CustomPacketPayload {
	public static final VillagerTradePayload INSTANCE = new VillagerTradePayload();
	public static final Type<VillagerTradePayload> TYPE = new Type<>(Eam.id("villager_trade"));
	public static final StreamCodec<RegistryFriendlyByteBuf, VillagerTradePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<VillagerTradePayload> type() {
		return TYPE;
	}
}
