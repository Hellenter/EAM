package io.github.hellenter.eam.client.link;

import io.github.hellenter.eam.core.link.HandshakeLogic;
import io.github.hellenter.eam.core.link.HelloPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.loader.api.FabricLoader;

/** Answers the server's EAM hello with this client's protocol and version. */
public final class HandshakeClient {
	private HandshakeClient() {
	}

	public static void init() {
		ClientConfigurationNetworking.registerGlobalReceiver(HelloPayload.TYPE, (hello, context) -> {
			String version = FabricLoader.getInstance().getModContainer("eam")
					.map(mod -> mod.getMetadata().getVersion().getFriendlyString())
					.orElse("?");
			context.responseSender().sendPacket(new HelloPayload.Reply(HandshakeLogic.PROTOCOL, version));
		});
	}
}
