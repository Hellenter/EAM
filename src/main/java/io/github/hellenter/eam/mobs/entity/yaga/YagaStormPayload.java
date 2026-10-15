package io.github.hellenter.eam.mobs.entity.yaga;

import io.github.hellenter.eam.Eam;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: whether the player stands in a Baba Yaga's storm, so the client shows its rain and thunder. */
public record YagaStormPayload(boolean inStorm) implements CustomPacketPayload {
	public static final Type<YagaStormPayload> TYPE = new Type<>(Eam.id("yaga_storm"));
	public static final StreamCodec<RegistryFriendlyByteBuf, YagaStormPayload> STREAM_CODEC =
			ByteBufCodecs.BOOL.map(YagaStormPayload::new, YagaStormPayload::inStorm).cast();

	@Override
	public Type<YagaStormPayload> type() {
		return TYPE;
	}
}
