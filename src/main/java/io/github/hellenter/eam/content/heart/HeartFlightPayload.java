package io.github.hellenter.eam.content.heart;

import io.github.hellenter.eam.Eam;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Tells a client that {@code playerId} squeezed a heart in a jar in the hand on the {@code rightArm} side. The client
 * flies the heart itself, from the player's own position every tick, so it keeps up with a walking or riding player.
 */
public record HeartFlightPayload(int playerId, boolean rightArm) implements CustomPacketPayload {
	public static final Type<HeartFlightPayload> TYPE = new Type<>(Eam.id("heart_flight"));
	public static final StreamCodec<RegistryFriendlyByteBuf, HeartFlightPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, HeartFlightPayload::playerId,
			ByteBufCodecs.BOOL, HeartFlightPayload::rightArm,
			HeartFlightPayload::new);

	@Override
	public Type<HeartFlightPayload> type() {
		return TYPE;
	}
}
