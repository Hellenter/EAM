package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.vanilla.villager.RepairSettings;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Writes the default villager repair balance to {@code data/eam/villagers/repair.json} so datapacks see what to override. */
public class EamRepairSettingsProvider extends FabricCodecDataProvider<RepairSettings> {
	public EamRepairSettingsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, PackOutput.Target.DATA_PACK, "villagers", RepairSettings.CODEC);
	}

	@Override
	protected void configure(BiConsumer<Identifier, RepairSettings> provider, HolderLookup.Provider registries) {
		provider.accept(Eam.id("repair"), RepairSettings.DEFAULT);
	}

	@Override
	public String getName() {
		return "EAM villager repair settings";
	}
}
