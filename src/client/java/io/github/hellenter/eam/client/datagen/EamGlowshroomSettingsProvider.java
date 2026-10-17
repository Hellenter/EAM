package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.underworld.GlowshroomSettings;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Writes how fast glowshroom tips ripen to {@code data/eam/glowshroom/settings.json}. */
public class EamGlowshroomSettingsProvider extends FabricCodecDataProvider<GlowshroomSettings> {
	public EamGlowshroomSettingsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, PackOutput.Target.DATA_PACK, "glowshroom", GlowshroomSettings.CODEC);
	}

	@Override
	protected void configure(BiConsumer<Identifier, GlowshroomSettings> provider, HolderLookup.Provider registries) {
		provider.accept(Eam.id("settings"), GlowshroomSettings.DEFAULT);
	}

	@Override
	public String getName() {
		return "EAM glowshroom settings";
	}
}
