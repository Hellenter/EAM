package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.ancient.AncientMetalGear;
import io.github.hellenter.eam.content.chainmail.ChainmailGear;
import io.github.hellenter.eam.content.adamantium.AdamantiumGear;
import io.github.hellenter.eam.content.mithril.MithrilGear;
import io.github.hellenter.eam.content.rusty.RustyGear;
import io.github.hellenter.eam.content.silver.SilverGear;
import io.github.hellenter.eam.mobs.entity.lost.LostGear;
import io.github.hellenter.eam.mobs.vanilla.evolution.WornLooks;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;

import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAsset;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** How EAM armor looks when worn: {@code assets/eam/equipment}, pointing at {@code textures/entity/equipment}. */
public class EamEquipmentAssetProvider implements DataProvider {
	private final PackOutput.PathProvider pathProvider;

	public EamEquipmentAssetProvider(FabricPackOutput output) {
		this.pathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "equipment");
	}

	@Override
	public CompletableFuture<?> run(CachedOutput cache) {
		return DataProvider.saveAll(cache, EquipmentClientInfo.CODEC, pathProvider::json, worn(Map.of(
				SilverGear.ASSET, EquipmentClientInfo.builder().addHumanoidLayers(Eam.id("silver")).build(),
				MithrilGear.ASSET, EquipmentClientInfo.builder().addHumanoidLayers(Eam.id("mithril")).build(),
				AdamantiumGear.ASSET, EquipmentClientInfo.builder().addHumanoidLayers(Eam.id("adamantium")).build(),
				RustyGear.IRON_ASSET, EquipmentClientInfo.builder().addHumanoidLayers(Eam.id("rusty_iron")).build(),
				RustyGear.CHAINMAIL_ASSET, EquipmentClientInfo.builder().addHumanoidLayers(Eam.id("rusty_chainmail")).build(),
				AncientMetalGear.ASSET, EquipmentClientInfo.builder().addHumanoidLayers(Eam.id("ancient_metal")).build(),
				AncientMetalGear.CHAINMAIL_ASSET, EquipmentClientInfo.builder().addHumanoidLayers(Eam.id("ancient_metal_chainmail")).build(),
				ChainmailGear.COPPER_ASSET, EquipmentClientInfo.builder().addHumanoidLayers(Eam.id("copper_chainmail")).build(),
				ChainmailGear.SILVER_ASSET, EquipmentClientInfo.builder().addHumanoidLayers(Eam.id("silver_chainmail")).build(),
				ChainmailGear.MITHRIL_ASSET, EquipmentClientInfo.builder().addHumanoidLayers(Eam.id("mithril_chainmail")).build())));
	}

	/**
	 * Adds the worn looks: the Lost's worn ancient metal (each its own helmet, chestplate and boots texture, all sharing one
	 * pair of worn leggings) and the armor of evolved monsters.
	 */
	private static Map<ResourceKey<EquipmentAsset>, EquipmentClientInfo> worn(Map<ResourceKey<EquipmentAsset>, EquipmentClientInfo> assets) {
		Map<ResourceKey<EquipmentAsset>, EquipmentClientInfo> all = new HashMap<>(assets);
		for (ResourceKey<EquipmentAsset> asset : LostGear.WORN) {
			all.put(asset, EquipmentClientInfo.builder()
					.addLayers(EquipmentClientInfo.LayerType.HUMANOID, new EquipmentClientInfo.Layer(asset.identifier()))
					.addLayers(EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS, new EquipmentClientInfo.Layer(Eam.id("worn_ancient_metal")))
					.build());
		}
		// Evolved monsters' scuffed, torn and holed armor, with its own baby layer for baby zombies.
		for (ResourceKey<EquipmentAsset> asset : WornLooks.ARMOR.values()) {
			all.put(asset, EquipmentClientInfo.builder().addHumanoidLayers(asset.identifier()).build());
		}
		return all;
	}

	@Override
	public String getName() {
		return "EAM equipment assets";
	}
}
