package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** The payloads a client holding a blueprint asks for its template with, and gets it back with. */
public final class BlueprintTemplatePayloads {
	private BlueprintTemplatePayloads() {
	}

	/** Sent by a client that holds a blueprint whose template it has not got yet. */
	public record Request(Identifier template) implements CustomPacketPayload {
		public static final Type<Request> TYPE = new Type<>(Eam.id("blueprint_template_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Request> STREAM_CODEC = StreamCodec.composite(
				Identifier.STREAM_CODEC, Request::template, Request::new);

		@Override
		public Type<Request> type() {
			return TYPE;
		}
	}

	/** The template's blocks, for the ghost. */
	public record Reply(BlueprintTemplate template) implements CustomPacketPayload {
		public static final Type<Reply> TYPE = new Type<>(Eam.id("blueprint_template"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Reply> STREAM_CODEC = StreamCodec.composite(
				BlueprintTemplate.STREAM_CODEC.cast(), Reply::template, Reply::new);

		@Override
		public Type<Reply> type() {
			return TYPE;
		}
	}
}
