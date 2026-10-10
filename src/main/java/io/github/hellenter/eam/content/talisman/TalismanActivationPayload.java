package io.github.hellenter.eam.content.talisman;

import io.github.hellenter.eam.Eam;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Tells a player's client to play the talisman's screen animation, like a totem of undying. */
public record TalismanActivationPayload() implements CustomPacketPayload {
	public static final TalismanActivationPayload INSTANCE = new TalismanActivationPayload();
	public static final Type<TalismanActivationPayload> TYPE = new Type<>(Eam.id("talisman_activation"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TalismanActivationPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<TalismanActivationPayload> type() {
		return TYPE;
	}
}
