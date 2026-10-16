package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamSounds;

import net.fabricmc.fabric.api.client.datagen.v1.builder.SoundTypeBuilder;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricSoundsProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

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
		exporter.add(EamSounds.FALLEN_LORD_PARRY, SoundTypeBuilder.of(EamSounds.FALLEN_LORD_PARRY)
				.subtitle("subtitles.eam.entity.fallen_lord.parry")
				.sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Eam.id("entity/fallen_lord/parry")).attenuationDistance(24)));
		exporter.add(EamSounds.FALLEN_LORD_PARRY_WINDOW, SoundTypeBuilder.of(EamSounds.FALLEN_LORD_PARRY_WINDOW)
				.subtitle("subtitles.eam.entity.fallen_lord.parry_window")
				.sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Eam.id("entity/fallen_lord/parry_window")).attenuationDistance(24)));
		exporter.add(EamSounds.KOSCHEI_ORANGE_CUE, SoundTypeBuilder.of(EamSounds.KOSCHEI_ORANGE_CUE)
				.subtitle("subtitles.eam.entity.koschei.orange_cue")
				.sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Eam.id("entity/koschei/orange_cue")).attenuationDistance(24)));
		exporter.add(EamSounds.KOSCHEI_RAVENS, SoundTypeBuilder.of(EamSounds.KOSCHEI_RAVENS)
				.subtitle("subtitles.eam.entity.koschei.ravens")
				.sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Eam.id("entity/koschei/ravens")).attenuationDistance(24)));
		// The wight: vanilla husk and cat files pitched down into a dry, rasping voice.
		exporter.add(EamSounds.WIGHT_AMBIENT, wight(EamSounds.WIGHT_AMBIENT, "ambient", "mob/husk/idle", 3, 0.5F, 0.5F));
		exporter.add(EamSounds.WIGHT_ATTACK, wight(EamSounds.WIGHT_ATTACK, "attack", "mob/cat/hiss", 3, 0.55F, 1.0F));
		exporter.add(EamSounds.WIGHT_GROAN, wight(EamSounds.WIGHT_GROAN, "groan", "mob/husk/idle", 3, 0.6F, 1.0F));
		exporter.add(EamSounds.WIGHT_HURT, wight(EamSounds.WIGHT_HURT, "hurt", "mob/husk/hurt", 2, 0.6F, 1.0F));
		exporter.add(EamSounds.WIGHT_DEATH, wight(EamSounds.WIGHT_DEATH, "death", "mob/husk/death", 2, 0.5F, 1.0F));
		// A vampire bat flock in the distance: vanilla bat take-off and squeaks pitched down into a harsh screech.
		exporter.add(EamSounds.VAMPIRE_BAT_SWARM, SoundTypeBuilder.of(EamSounds.VAMPIRE_BAT_SWARM)
				.subtitle("subtitles.eam.entity.vampire_bat.swarm")
				.sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Identifier.withDefaultNamespace("mob/bat/takeoff")).pitch(0.7F))
				.sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Identifier.withDefaultNamespace("mob/bat/idle1")).pitch(0.6F))
				.sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Identifier.withDefaultNamespace("mob/bat/idle2")).pitch(0.6F))
				.sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Identifier.withDefaultNamespace("mob/bat/idle3")).pitch(0.6F))
				.sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Identifier.withDefaultNamespace("mob/bat/idle4")).pitch(0.6F)));
	}

	private static SoundTypeBuilder wight(SoundEvent event, String name, String file, int files, float pitch, float volume) {
		SoundTypeBuilder builder = SoundTypeBuilder.of(event).subtitle("subtitles.eam.entity.wight." + name);
		for (int i = 1; i <= files; i++) {
			builder.sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Identifier.withDefaultNamespace(file + i)).pitch(pitch).volume(volume));
		}
		return builder;
	}

	@Override
	public String getName() {
		return "Sounds";
	}
}
