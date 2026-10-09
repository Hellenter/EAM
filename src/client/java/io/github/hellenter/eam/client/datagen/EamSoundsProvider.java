package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamSounds;

import net.fabricmc.fabric.api.client.datagen.v1.builder.SoundTypeBuilder;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricSoundsProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;

import net.minecraft.core.HolderLookup;

import java.util.concurrent.CompletableFuture;

/** {@code sounds.json}: our sound events, their files and subtitles. */
public class EamSoundsProvider extends FabricSoundsProvider {
	public EamSoundsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected void configure(HolderLookup.Provider registries, SoundExporter exporter) {
		exporter.add(EamSounds.FALLEN_LORD_DASH_WARNING, SoundTypeBuilder.of(EamSounds.FALLEN_LORD_DASH_WARNING)
				.subtitle("subtitles.eam.entity.fallen_lord.dash_warning")
				.sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Eam.id("entity/fallen_lord/dash_warning")).attenuationDistance(24)));
	}

	@Override
	public String getName() {
		return "Sounds";
	}
}
