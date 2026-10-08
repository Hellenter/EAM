package io.github.hellenter.eam.client;

import io.github.hellenter.eam.client.content.CampFireRenderer;
import io.github.hellenter.eam.client.crafting.CraftProgress;
import io.github.hellenter.eam.client.crafting.CraftingTableScreen;
import io.github.hellenter.eam.client.forging.HotIronTooltip;
import io.github.hellenter.eam.client.hud.NutritionPanel;
import io.github.hellenter.eam.content.EamBlockEntities;
import io.github.hellenter.eam.content.brewing.BrewingModule;
import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.client.hud.ThirstHud;
import io.github.hellenter.eam.client.survival.thirst.DrinkFromSourceHandler;
import io.github.hellenter.eam.world.crafting.CraftingTableMenu;
import io.github.hellenter.eam.world.crafting.CraftingTypes;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.CraftingMenu;

public class EamClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		NutritionPanel.register();
		// Both stay idle unless the server has thirst on: they need the synced bar or the server's drink channel.
		ThirstHud.register();
		DrinkFromSourceHandler.register();
		EntityRendererRegistry.register(EamEntities.PEBBLE, ThrownItemRenderer::new);
		BlockEntityRenderers.register(EamBlockEntities.CAMP_FIRE, CampFireRenderer::new);
		MenuScreens.<CraftingMenu, CraftingTableScreen>register(CraftingTypes.MENU, (menu, inventory, title) ->
				new CraftingTableScreen((CraftingTableMenu) menu, inventory, title));
		// The silver stand's menu has the vanilla layout: the vanilla brewing screen draws it.
		MenuScreens.<BrewingStandMenu, BrewingStandScreen>register(BrewingModule.MENU, BrewingStandScreen::new);
		CraftProgress.register();
		HotIronTooltip.register();
	}
}
