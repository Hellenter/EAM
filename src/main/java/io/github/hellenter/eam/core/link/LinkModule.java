package io.github.hellenter.eam.core.link;

import com.mojang.brigadier.CommandDispatcher;

import io.github.hellenter.eam.Eam;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;

import java.util.function.Consumer;

/**
 * Running EAM as one server of a network: players without the mod get a clear message instead of a registry error,
 * and {@code /hub} sends players back to the hub server with the vanilla transfer packet.
 */
public final class LinkModule {
	/** Runs before Fabric's registry sync, which would otherwise disconnect vanilla clients with its own message. */
	private static final Identifier EARLY = Eam.id("handshake");

	private static LinkConfig config = LinkConfig.DEFAULT;

	private LinkModule() {
	}

	/** Payload types are registered on both sides so the client can answer. */
	public static void register() {
		PayloadTypeRegistry.clientboundConfiguration().register(HelloPayload.TYPE, HelloPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundConfiguration().register(HelloPayload.Reply.TYPE, HelloPayload.Reply.STREAM_CODEC);
		VanillaComponents.register();
		LinkState.setDedicatedServer(FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER);
		ServerLifecycleEvents.SERVER_STARTING.register(server -> LinkState.setLocalServer(true));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> LinkState.setLocalServer(false));
	}

	public static void init() {
		config = LinkConfig.load();

		ServerConfigurationConnectionEvents.BEFORE_CONFIGURE.addPhaseOrdering(EARLY, Event.DEFAULT_PHASE);
		ServerConfigurationConnectionEvents.BEFORE_CONFIGURE.register(EARLY, LinkModule::onConfigure);
		ServerConfigurationNetworking.registerGlobalReceiver(HelloPayload.Reply.TYPE, (reply, context) -> {
			ServerConfigurationPacketListenerImpl listener = context.packetListener();
			if (enforce(ServerConfigurationNetworking.getServer(listener))
					&& HandshakeLogic.afterReply(HandshakeLogic.PROTOCOL, reply.protocol()) != HandshakeLogic.Result.OK) {
				listener.disconnect(message(config.outdatedMessage(), reply.version()));
				return;
			}
			listener.completeTask(HelloTask.KEY);
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> registerCommand(dispatcher));
	}

	private static void onConfigure(ServerConfigurationPacketListenerImpl listener, MinecraftServer server) {
		boolean hasChannel = ServerConfigurationNetworking.canSend(listener, HelloPayload.TYPE);
		if (hasChannel) {
			// Always say hello, also in singleplayer and LAN: the client turns EAM rules on only after it.
			listener.addTask(new HelloTask());
		} else if (enforce(server) && HandshakeLogic.beforeHello(false) == HandshakeLogic.Result.MISSING) {
			listener.disconnect(message(config.missingModMessage(), "-"));
		}
	}

	/** Only a dedicated server turns players away; singleplayer and LAN stay open. */
	private static boolean enforce(MinecraftServer server) {
		return config.requireClientMod() && server.isDedicatedServer();
	}

	private static Component message(String template, String clientVersion) {
		return Component.literal(HandshakeLogic.format(template, config.downloadUrl(), version(), clientVersion));
	}

	static String version() {
		return FabricLoader.getInstance().getModContainer(Eam.MOD_ID)
				.map(mod -> mod.getMetadata().getVersion().getFriendlyString())
				.orElse("?");
	}

	private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		if (!config.hubEnabled()) {
			return;
		}
		dispatcher.register(Commands.literal("hub").executes(context -> {
			ServerPlayer player = context.getSource().getPlayerOrException();
			player.connection.send(new ClientboundTransferPacket(config.hubHost(), config.hubPort()));
			return 1;
		}));
		dispatcher.register(Commands.literal("lobby").redirect(dispatcher.getRoot().getChild("hub")));
	}

	/** Holds configuration until the client answers the hello. */
	private record HelloTask() implements ConfigurationTask {
		static final Type KEY = new Type(Eam.MOD_ID + ":hello");

		@Override
		public void start(Consumer<Packet<?>> sender) {
			sender.accept(ServerConfigurationNetworking.createClientboundPacket(new HelloPayload(HandshakeLogic.PROTOCOL, version())));
		}

		@Override
		public Type type() {
			return KEY;
		}
	}
}
