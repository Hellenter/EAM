package io.github.hellenter.eam.core.link;

import io.github.hellenter.eam.Eam;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The EAM handshake, sent during configuration. The server sends its version and protocol; a client with EAM answers
 * with its own on the reply channel. Clients without EAM never register the channel, so the server can tell them apart.
 */
public record HelloPayload(int protocol, String version) implements CustomPacketPayload {
	public static final Type<HelloPayload> TYPE = new Type<>(Eam.id("hello"));
	public static final StreamCodec<FriendlyByteBuf, HelloPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, HelloPayload::protocol,
			ByteBufCodecs.STRING_UTF8, HelloPayload::version,
			HelloPayload::new);

	@Override
	public Type<HelloPayload> type() {
		return TYPE;
	}

	/** The same data on the serverbound channel. */
	public record Reply(int protocol, String version) implements CustomPacketPayload {
		public static final StreamCodec<FriendlyByteBuf, Reply> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Reply::protocol,
				ByteBufCodecs.STRING_UTF8, Reply::version,
				Reply::new);
		public static final Type<Reply> TYPE = new Type<>(Eam.id("hello_reply"));

		@Override
		public Type<Reply> type() {
			return TYPE;
		}
	}
}
