package io.github.hellenter.eam.client;

import io.github.hellenter.eam.client.brewing.SilverBrewingStandScreen;
import io.github.hellenter.eam.client.bucket.BucketMeltTooltip;
import io.github.hellenter.eam.client.content.CampFireRenderer;
import io.github.hellenter.eam.client.content.talisman.TalismanActivation;
import io.github.hellenter.eam.client.crafting.CraftProgress;
import io.github.hellenter.eam.client.crafting.CraftingTableScreen;
import io.github.hellenter.eam.client.enchanting.EnchantingClient;
import io.github.hellenter.eam.client.forging.AnvilProgress;
import io.github.hellenter.eam.client.forging.HotIronTooltip;
import io.github.hellenter.eam.client.hud.NutritionPanel;
import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordEyesModel;
import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordGearModel;
import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordModel;
import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordRenderer;
import io.github.hellenter.eam.client.mobs.fallenlord.HeavyWaveParticle;
import io.github.hellenter.eam.client.mobs.knight.SkeletonKnightRenderer;
import io.github.hellenter.eam.client.mobs.lurker.SwampLurkerModel;
import io.github.hellenter.eam.client.mobs.lurker.SwampLurkerRenderer;
import io.github.hellenter.eam.client.mobs.wight.WightModel;
import io.github.hellenter.eam.client.mobs.wight.WightRenderer;
import io.github.hellenter.eam.client.mobs.fallenlord.LordShieldRenderer;
import io.github.hellenter.eam.client.mobs.fallenlord.LordSpearRenderer;
import io.github.hellenter.eam.content.EamBlockEntities;
import io.github.hellenter.eam.content.brewing.BrewingModule;
import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamParticles;
import io.github.hellenter.eam.client.hud.ThirstHud;
import io.github.hellenter.eam.client.survival.thirst.DrinkFromSourceHandler;
import io.github.hellenter.eam.client.world.bloodmoon.BloodMoonSky;
import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.world.crafting.CraftingTableMenu;
import io.github.hellenter.eam.world.crafting.CraftingTypes;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.CraftingMenu;

import java.util.List;

public class EamClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		NutritionPanel.register();
		// The dense shrub's leaves take the biome's foliage colour, like the trees around it.
		BlockColorRegistry.register(List.of(BlockTintSources.foliage()), EamBlocks.DENSE_SHRUB, EamBlocks.BIG_SHRUB_LEAVES);
		// Both stay idle unless the server has thirst on: they need the synced bar or the server's drink channel.
		ThirstHud.register();
		DrinkFromSourceHandler.register();
		EntityRendererRegistry.register(EamEntities.PEBBLE, ThrownItemRenderer::new);
		ModelLayerRegistry.registerModelLayer(FallenLordModel.LAYER, FallenLordModel::createLayer);
		ParticleProviderRegistry.getInstance().register(EamParticles.HEAVY_WAVE, HeavyWaveParticle.Provider::new);
		ModelLayerRegistry.registerModelLayer(FallenLordGearModel.LAYER, FallenLordGearModel::createLayer);
		ModelLayerRegistry.registerModelLayer(FallenLordEyesModel.LAYER, FallenLordEyesModel::createLayer);
		EntityRendererRegistry.register(EamEntities.FALLEN_LORD, FallenLordRenderer::new);
		EntityRendererRegistry.register(EamEntities.LORD_SPEAR, LordSpearRenderer::new);
		EntityRendererRegistry.register(EamEntities.LORD_SHIELD, LordShieldRenderer::new);
		EntityRendererRegistry.register(EamEntities.SKELETON_KNIGHT, SkeletonKnightRenderer::new);
		ModelLayerRegistry.registerModelLayer(SwampLurkerModel.LAYER, SwampLurkerModel::createLayer);
		EntityRendererRegistry.register(EamEntities.SWAMP_LURKER, SwampLurkerRenderer::new);
		ModelLayerRegistry.registerModelLayer(WightModel.LAYER, WightModel::createLayer);
		EntityRendererRegistry.register(EamEntities.WIGHT, WightRenderer::new);
		BlockEntityRenderers.register(EamBlockEntities.CAMP_FIRE, CampFireRenderer::new);
		MenuScreens.<CraftingMenu, CraftingTableScreen>register(CraftingTypes.MENU, (menu, inventory, title) ->
				new CraftingTableScreen((CraftingTableMenu) menu, inventory, title));
		// The silver stand's menu has the vanilla layout: the vanilla brewing screen draws it, plus a recipe book.
		MenuScreens.<BrewingStandMenu, SilverBrewingStandScreen>register(BrewingModule.MENU, SilverBrewingStandScreen::new);
		CraftProgress.register();
		HotIronTooltip.register();
		BucketMeltTooltip.register();
		AnvilProgress.register();
		EnchantingClient.register();
		BloodMoonSky.register();
		TalismanActivation.register();
	}
}
