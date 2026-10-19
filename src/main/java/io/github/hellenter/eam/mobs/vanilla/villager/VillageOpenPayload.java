package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent by the village button of the elder's inventory: the server closes it and sends the village list. */
public record VillageOpenPayload() implements CustomPacketPayload {
	public static final VillageOpenPayload INSTANCE = new VillageOpenPayload();
	public static final Type<VillageOpenPayload> TYPE = new Type<>(Eam.id("village_open"));
	public static final StreamCodec<RegistryFriendlyByteBuf, VillageOpenPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<VillageOpenPayload> type() {
		return TYPE;
	}
}
