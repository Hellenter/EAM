package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.mithril.MithrilGear;
import io.github.hellenter.eam.content.rusty.RustyGear;
import io.github.hellenter.eam.content.silver.SilverGear;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;

import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

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
		return DataProvider.saveAll(cache, EquipmentClientInfo.CODEC, pathProvider::json, Map.of(
				SilverGear.ASSET, EquipmentClientInfo.builder().addHumanoidLayers(Eam.id("silver")).build(),
				MithrilGear.ASSET, EquipmentClientInfo.builder().addHumanoidLayers(Eam.id("mithril")).build(),
				RustyGear.IRON_ASSET, EquipmentClientInfo.builder().addHumanoidLayers(Eam.id("rusty_iron")).build(),
				RustyGear.CHAINMAIL_ASSET, EquipmentClientInfo.builder().addHumanoidLayers(Eam.id("rusty_chainmail")).build()));
	}

	@Override
	public String getName() {
		return "EAM equipment assets";
	}
}
