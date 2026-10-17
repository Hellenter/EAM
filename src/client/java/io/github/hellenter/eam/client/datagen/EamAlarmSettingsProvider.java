package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.vanilla.villager.AlarmSettings;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Writes the default village alarm balance to {@code data/eam/villagers/alarm.json} so datapacks see what to override. */
public class EamAlarmSettingsProvider extends FabricCodecDataProvider<AlarmSettings> {
	public EamAlarmSettingsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, PackOutput.Target.DATA_PACK, "villagers", AlarmSettings.CODEC);
	}

	@Override
	protected void configure(BiConsumer<Identifier, AlarmSettings> provider, HolderLookup.Provider registries) {
		provider.accept(Eam.id("alarm"), AlarmSettings.DEFAULT);
	}

	@Override
	public String getName() {
		return "EAM village alarm settings";
	}
}
