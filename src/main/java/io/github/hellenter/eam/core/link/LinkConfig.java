package io.github.hellenter.eam.core.link;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.hellenter.eam.Eam;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Server settings for running EAM behind a hub server ({@code config/eam-server.json}).
 *
 * @param requireClientMod disconnect players whose client has no EAM or an incompatible one
 * @param downloadUrl where players get the mod; shown in the disconnect message
 * @param missingModMessage disconnect text for a client without EAM; {@code {url}} and {@code {version}} are replaced
 * @param outdatedMessage disconnect text for an incompatible EAM; also takes {@code {client}}
 * @param hubHost address {@code /hub} sends players to; empty turns the command off
 * @param hubPort port of the hub server
 */
public record LinkConfig(boolean requireClientMod, String downloadUrl, String missingModMessage, String outdatedMessage,
		String hubHost, int hubPort) {
	public static final LinkConfig DEFAULT = new LinkConfig(true, "",
			"На этом сервере нужен мод EAM {version} (Fabric 26.3).\nСкачать: {url}",
			"У тебя EAM {client}, а серверу нужен EAM {version}.\nОбнови мод: {url}",
			"", 25565);

	public static final Codec<LinkConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.optionalFieldOf("require_client_mod", DEFAULT.requireClientMod).forGetter(LinkConfig::requireClientMod),
			Codec.STRING.optionalFieldOf("download_url", DEFAULT.downloadUrl).forGetter(LinkConfig::downloadUrl),
			Codec.STRING.optionalFieldOf("missing_mod_message", DEFAULT.missingModMessage).forGetter(LinkConfig::missingModMessage),
			Codec.STRING.optionalFieldOf("outdated_message", DEFAULT.outdatedMessage).forGetter(LinkConfig::outdatedMessage),
			Codec.STRING.optionalFieldOf("hub_host", DEFAULT.hubHost).forGetter(LinkConfig::hubHost),
			Codec.INT.optionalFieldOf("hub_port", DEFAULT.hubPort).forGetter(LinkConfig::hubPort)
	).apply(instance, LinkConfig::new));

	public boolean hubEnabled() {
		return !hubHost.isBlank();
	}

	public static LinkConfig load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(Eam.MOD_ID + "-server.json");
		LinkConfig config = DEFAULT;
		if (Files.exists(path)) {
			try {
				config = CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(Files.readString(path)))
						.resultOrPartial(error -> Eam.LOGGER.error("Invalid {}: {}", path, error))
						.orElse(DEFAULT);
			} catch (Exception e) {
				Eam.LOGGER.error("Failed to read {}, using defaults", path, e);
			}
		}
		// Only a dedicated server writes the file: clients never use it.
		if (FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.SERVER) {
			LinkConfig written = config;
			CODEC.encodeStart(JsonOps.INSTANCE, written).result().ifPresent(json -> {
				try {
					Files.writeString(path, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(json));
				} catch (IOException e) {
					Eam.LOGGER.error("Failed to write {}", path, e);
				}
			});
		}
		return config;
	}
}
