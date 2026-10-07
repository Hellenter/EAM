package io.github.hellenter.eam.client;

import io.github.hellenter.eam.client.content.CampFireRenderer;
import io.github.hellenter.eam.client.hud.NutritionPanel;
import io.github.hellenter.eam.content.EamBlockEntities;
import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.client.hud.ThirstHud;
import io.github.hellenter.eam.client.survival.thirst.DrinkFromSourceHandler;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class EamClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		NutritionPanel.register();
		// Both stay idle unless the server has thirst on: they need the synced bar or the server's drink channel.
		ThirstHud.register();
		DrinkFromSourceHandler.register();
		EntityRendererRegistry.register(EamEntities.PEBBLE, ThrownItemRenderer::new);
		BlockEntityRenderers.register(EamBlockEntities.CAMP_FIRE, CampFireRenderer::new);
	}
}
