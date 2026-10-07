package io.github.hellenter.eam.client;

import io.github.hellenter.eam.client.hud.NutritionPanel;

import net.fabricmc.api.ClientModInitializer;

public class EamClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Client-only registration (HUD, renderers, model layers) goes here.
		NutritionPanel.register();
	}
}
