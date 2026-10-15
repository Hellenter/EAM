package io.github.hellenter.eam.world.bluemoon;

import io.github.hellenter.eam.Eam;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Tells a client whether a blue moon is up, so it can tint the sky. Sent on join and whenever the event starts or ends. */
public record BlueMoonPayload(boolean active) implements CustomPacketPayload {
	public static final Type<BlueMoonPayload> TYPE = new Type<>(Eam.id("blue_moon"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BlueMoonPayload> STREAM_CODEC =
			ByteBufCodecs.BOOL.map(BlueMoonPayload::new, BlueMoonPayload::active).cast();

	@Override
	public Type<BlueMoonPayload> type() {
		return TYPE;
	}
}
