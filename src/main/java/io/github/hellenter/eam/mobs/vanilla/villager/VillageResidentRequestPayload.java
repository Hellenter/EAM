package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent by a right click on a row of the village menu, and again now and then while his page is open: the server sends
 * what that villager has, if he still lives in the elder's village.
 */
public record VillageResidentRequestPayload(int elderId, int villagerId) implements CustomPacketPayload {
	public static final Type<VillageResidentRequestPayload> TYPE = new Type<>(Eam.id("village_resident_request"));
	public static final StreamCodec<RegistryFriendlyByteBuf, VillageResidentRequestPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, VillageResidentRequestPayload::elderId,
			ByteBufCodecs.VAR_INT, VillageResidentRequestPayload::villagerId,
			VillageResidentRequestPayload::new);

	@Override
	public Type<VillageResidentRequestPayload> type() {
		return TYPE;
	}
}
