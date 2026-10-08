package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.world.crafting.CraftTimeRules;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Writes the default craft times to {@code data/eam/crafting/times.json} so datapacks see what to override. */
public class EamCraftTimesProvider extends FabricCodecDataProvider<CraftTimeRules> {
	public EamCraftTimesProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, PackOutput.Target.DATA_PACK, "crafting", CraftTimeRules.CODEC);
	}

	@Override
	protected void configure(BiConsumer<Identifier, CraftTimeRules> provider, HolderLookup.Provider registries) {
		provider.accept(Eam.id("times"), CraftTimeRules.DEFAULT);
	}

	@Override
	public String getName() {
		return "EAM craft times";
	}
}
