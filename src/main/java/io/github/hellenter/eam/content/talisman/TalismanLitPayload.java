package io.github.hellenter.eam.content.talisman;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Tells the client the talisman lit the portal at {@code portal}: its crystal lights up as the portal does. */
public record TalismanLitPayload(BlockPos portal) implements CustomPacketPayload {
	public static final Type<TalismanLitPayload> TYPE = new Type<>(Eam.id("talisman_lit"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TalismanLitPayload> STREAM_CODEC =
			BlockPos.STREAM_CODEC.map(TalismanLitPayload::new, TalismanLitPayload::portal).cast();

	@Override
	public Type<TalismanLitPayload> type() {
		return TYPE;
	}
}
