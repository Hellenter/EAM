package io.github.hellenter.eam.client;

import io.github.hellenter.eam.client.hud.ThirstHud;
import io.github.hellenter.eam.client.survival.thirst.DrinkFromSourceHandler;

import net.fabricmc.api.ClientModInitializer;

public class EamClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Both stay idle unless the server has thirst on: they need the synced bar or the server's drink channel.
		ThirstHud.register();
		DrinkFromSourceHandler.register();
	}
}
