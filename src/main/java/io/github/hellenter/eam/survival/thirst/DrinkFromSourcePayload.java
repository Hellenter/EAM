package io.github.hellenter.eam.survival.thirst;

import io.github.hellenter.eam.Eam;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent by the client while the player holds use with an empty hand and looks at water. Vanilla sends nothing for an
 * empty-hand click into the air, so the client has to ask; the server re-checks everything.
 */
public record DrinkFromSourcePayload() implements CustomPacketPayload {
	public static final DrinkFromSourcePayload INSTANCE = new DrinkFromSourcePayload();
	public static final Type<DrinkFromSourcePayload> TYPE = new Type<>(Eam.id("drink_from_source"));
	public static final StreamCodec<RegistryFriendlyByteBuf, DrinkFromSourcePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<DrinkFromSourcePayload> type() {
		return TYPE;
	}
}
