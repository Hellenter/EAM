package io.github.hellenter.eam.client.link;

import io.github.hellenter.eam.core.link.HandshakeLogic;
import io.github.hellenter.eam.core.link.HelloPayload;
import io.github.hellenter.eam.core.link.LinkState;
import io.github.hellenter.eam.core.link.VanillaComponents;

import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.Item;

/**
 * Answers the server's EAM hello and remembers whether this server runs EAM. Every new connection (also a transfer
 * from the hub) starts as vanilla until the hello arrives.
 */
public final class HandshakeClient {
	private HandshakeClient() {
	}

	public static void init() {
		ClientConfigurationConnectionEvents.INIT.register((listener, client) -> LinkState.setRemoteHasEam(false));
		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> LinkState.setRemoteHasEam(false));
		ClientConfigurationNetworking.registerGlobalReceiver(HelloPayload.TYPE, (hello, context) -> {
			LinkState.setRemoteHasEam(true);
			String version = FabricLoader.getInstance().getModContainer("eam")
					.map(mod -> mod.getMetadata().getVersion().getFriendlyString())
					.orElse("?");
			context.responseSender().sendPacket(new HelloPayload.Reply(HandshakeLogic.PROTOCOL, version));
		});
	}

	/** Item components the client should use: EAM's own on an EAM server, the vanilla ones elsewhere. */
	public static DataComponentMap components(Holder.Reference<?> holder, DataComponentMap current) {
		if (LinkState.active() || !(holder.value() instanceof Item item)) {
			return current;
		}
		DataComponentMap vanilla = VanillaComponents.of(item);
		return vanilla != null ? vanilla : current;
	}
}
