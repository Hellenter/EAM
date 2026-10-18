package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent by a guard button of the village menu: the server makes the villager a guard, or an ordinary villager again, if
 * he still lives in the elder's village.
 */
public record VillageGuardPayload(int elderId, int villagerId, boolean guard) implements CustomPacketPayload {
	public static final Type<VillageGuardPayload> TYPE = new Type<>(Eam.id("village_guard"));
	public static final StreamCodec<RegistryFriendlyByteBuf, VillageGuardPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, VillageGuardPayload::elderId,
			ByteBufCodecs.VAR_INT, VillageGuardPayload::villagerId,
			ByteBufCodecs.BOOL, VillageGuardPayload::guard,
			VillageGuardPayload::new);

	@Override
	public Type<VillageGuardPayload> type() {
		return TYPE;
	}
}
