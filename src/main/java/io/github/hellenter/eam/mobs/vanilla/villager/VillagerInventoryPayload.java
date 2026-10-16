package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent by the inventory button beside a villager's trades: the server closes them and opens his inventory. */
public record VillagerInventoryPayload() implements CustomPacketPayload {
	public static final VillagerInventoryPayload INSTANCE = new VillagerInventoryPayload();
	public static final Type<VillagerInventoryPayload> TYPE = new Type<>(Eam.id("villager_inventory"));
	public static final StreamCodec<RegistryFriendlyByteBuf, VillagerInventoryPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<VillagerInventoryPayload> type() {
		return TYPE;
	}
}
