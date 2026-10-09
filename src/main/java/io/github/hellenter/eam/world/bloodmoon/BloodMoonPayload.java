package io.github.hellenter.eam.world.bloodmoon;

import io.github.hellenter.eam.Eam;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Tells a client whether a blood moon is up, so it can tint the sky. Sent on join and whenever the event starts or ends. */
public record BloodMoonPayload(boolean active) implements CustomPacketPayload {
	public static final Type<BloodMoonPayload> TYPE = new Type<>(Eam.id("blood_moon"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BloodMoonPayload> STREAM_CODEC =
			ByteBufCodecs.BOOL.map(BloodMoonPayload::new, BloodMoonPayload::active).cast();

	@Override
	public Type<BloodMoonPayload> type() {
		return TYPE;
	}
}
