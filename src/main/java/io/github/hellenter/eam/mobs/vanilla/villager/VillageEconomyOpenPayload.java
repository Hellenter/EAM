package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent by the upgrades button of the village menu: the server sends the village's economy. */
public record VillageEconomyOpenPayload(int elderId) implements CustomPacketPayload {
	public static final Type<VillageEconomyOpenPayload> TYPE = new Type<>(Eam.id("village_economy_open"));
	public static final StreamCodec<RegistryFriendlyByteBuf, VillageEconomyOpenPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, VillageEconomyOpenPayload::elderId,
			VillageEconomyOpenPayload::new);

	@Override
	public Type<VillageEconomyOpenPayload> type() {
		return TYPE;
	}
}
