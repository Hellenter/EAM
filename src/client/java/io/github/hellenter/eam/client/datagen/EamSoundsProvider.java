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
	/** Scales every underworld ambience volume below; Maxim found the first mix too loud (2026-10-17: 70% quieter). */
	private static final float AMBIENCE_VOLUME = 0.3F;

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
		exporter.add(EamSounds.KOSCHEI_RAVEN_CAW, SoundTypeBuilder.of(EamSounds.KOSCHEI_RAVEN_CAW)
				.subtitle("subtitles.eam.entity.koschei_raven.caw")
				.sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Eam.id("entity/koschei/caw1")).attenuationDistance(20))
				.sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Eam.id("entity/koschei/caw2")).attenuationDistance(20))
				.sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Eam.id("entity/koschei/caw3")).attenuationDistance(20)));
		// The wight: vanilla husk and cat files pitched down into a dry, rasping voice.
		exporter.add(EamSounds.WIGHT_AMBIENT, wight(EamSounds.WIGHT_AMBIENT, "ambient", "mob/husk/idle", 3, 0.5F, 0.5F));
		exporter.add(EamSounds.WIGHT_ATTACK, wight(EamSounds.WIGHT_ATTACK, "attack", "mob/cat/hiss", 3, 0.55F, 1.0F));
		exporter.add(EamSounds.WIGHT_GROAN, wight(EamSounds.WIGHT_GROAN, "groan", "mob/husk/idle", 3, 0.6F, 1.0F));
		exporter.add(EamSounds.WIGHT_HURT, wight(EamSounds.WIGHT_HURT, "hurt", "mob/husk/hurt", 2, 0.6F, 1.0F));
		exporter.add(EamSounds.WIGHT_DEATH, wight(EamSounds.WIGHT_DEATH, "death", "mob/husk/death", 2, 0.5F, 1.0F));
		// The leshy: vanilla creaking, creaking heart and ravager files pitched down into a voice of old wood; crows are the
		// raven caws Koschei's ravens use.
		exporter.add(EamSounds.LESHY_AMBIENT, leshy(EamSounds.LESHY_AMBIENT, "ambient", 0.55F, 1.0F, "mob/creaking/creaking_idle1",
				"mob/creaking/creaking_idle2", "mob/creaking/creaking_idle3", "mob/creaking/creaking_idle4", "mob/creaking/creaking_idle5",
				"mob/creaking/creaking_idle6"));
		exporter.add(EamSounds.LESHY_ATTACK, leshy(EamSounds.LESHY_ATTACK, "attack", 0.7F, 1.0F, "mob/creaking/creaking_attack1",
				"mob/creaking/creaking_attack2", "mob/creaking/creaking_attack3", "mob/creaking/creaking_attack4"));
		exporter.add(EamSounds.LESHY_ROAR, leshy(EamSounds.LESHY_ROAR, "roar", 0.6F, 1.0F, "mob/ravager/roar1", "mob/ravager/roar2",
				"mob/ravager/roar3", "mob/ravager/roar4"));
		exporter.add(EamSounds.LESHY_ROOTS, leshy(EamSounds.LESHY_ROOTS, "roots", 0.7F, 1.0F, "block/rooted_dirt/break1", "block/rooted_dirt/break2",
				"block/mangrove_roots/break1", "block/mangrove_roots/break2", "block/roots/break1", "block/roots/break2"));
		exporter.add(EamSounds.LESHY_SHIELD, leshy(EamSounds.LESHY_SHIELD, "shield", 0.6F, 1.0F, "dig/wood1", "dig/wood2", "dig/wood3",
				"dig/wood4"));
		exporter.add(EamSounds.LESHY_HURT, leshy(EamSounds.LESHY_HURT, "hurt", 0.7F, 1.0F, "block/creaking_heart/hurt/trail1",
				"block/creaking_heart/hurt/trail2", "block/creaking_heart/hurt/trail3", "block/creaking_heart/hurt/trail4"));
		exporter.add(EamSounds.LESHY_DEATH, leshy(EamSounds.LESHY_DEATH, "death", 0.55F, 1.0F, "mob/creaking/creaking_death",
				"mob/ravager/death1"));
		exporter.add(EamSounds.LESHY_CROWS, SoundTypeBuilder.of(EamSounds.LESHY_CROWS)
				.subtitle("subtitles.eam.entity.leshy.crows")
				.sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Eam.id("entity/koschei/caw1")).pitch(1.1F).attenuationDistance(24))
				.sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Eam.id("entity/koschei/caw2")).pitch(1.1F).attenuationDistance(24))
				.sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Eam.id("entity/koschei/caw3")).pitch(1.1F).attenuationDistance(24))
				.sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Eam.id("entity/koschei/ravens")).attenuationDistance(24)));
		// A vampire bat flock in the distance: vanilla bat take-off and squeaks pitched down into a harsh screech.
		exporter.add(EamSounds.VAMPIRE_BAT_SWARM, SoundTypeBuilder.of(EamSounds.VAMPIRE_BAT_SWARM)
				.subtitle("subtitles.eam.entity.vampire_bat.swarm")
				.sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Identifier.withDefaultNamespace("mob/bat/takeoff")).pitch(0.7F))
				.sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Identifier.withDefaultNamespace("mob/bat/idle1")).pitch(0.6F))
				.sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Identifier.withDefaultNamespace("mob/bat/idle2")).pitch(0.6F))
				.sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Identifier.withDefaultNamespace("mob/bat/idle3")).pitch(0.6F))
				.sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Identifier.withDefaultNamespace("mob/bat/idle4")).pitch(0.6F)));
		underworldAmbience(exporter);
	}

	/**
	 * Underworld biome ambience. The loops are our own synthesized files (ambient/underworld/*); the additions mix
	 * vanilla files, pitched and quietened so they read as the biome's own: trickling pebbles in the wastes, spore puffs
	 * in the mushroom forest, crackling embers in the burnt one, drips over the lakes, creaking wood and insects in the
	 * forest, ringing ice in the cold caves, deep rumbles in the depths.
	 */
	private static void underworldAmbience(SoundExporter exporter) {
		exporter.add(EamSounds.UNDERWORLD_STONE_WASTES_LOOP, loop(EamSounds.UNDERWORLD_STONE_WASTES_LOOP, "stone_wastes", 0.3F, 1.0F));
		exporter.add(EamSounds.UNDERWORLD_STONE_WASTES_ADDITIONS, SoundTypeBuilder.of(EamSounds.UNDERWORLD_STONE_WASTES_ADDITIONS)
				.sound(vanilla("step/gravel1", 0.6F, 0.25F, 2)).sound(vanilla("step/gravel2", 0.6F, 0.25F, 2))
				.sound(vanilla("step/gravel3", 0.55F, 0.25F, 2)).sound(vanilla("step/gravel4", 0.55F, 0.25F, 2))
				.sound(vanilla("block/dripstone/break1", 0.5F, 0.15F, 1)).sound(vanilla("block/dripstone/break3", 0.5F, 0.15F, 1))
				.sound(vanilla("block/pointed_dripstone/drip_water1", 0.9F, 0.3F, 1))
				.sound(vanilla("block/pointed_dripstone/drip_water4", 0.85F, 0.3F, 1)));

		exporter.add(EamSounds.UNDERWORLD_MUSHROOM_FOREST_LOOP, loop(EamSounds.UNDERWORLD_MUSHROOM_FOREST_LOOP, "mushroom_forest", 0.4F, 1.0F));
		exporter.add(EamSounds.UNDERWORLD_MUSHROOM_FOREST_ADDITIONS, SoundTypeBuilder.of(EamSounds.UNDERWORLD_MUSHROOM_FOREST_ADDITIONS)
				.sound(vanilla("ambient/nether/crimson_forest/particles1", 0.7F, 0.35F, 4))
				.sound(vanilla("ambient/nether/crimson_forest/particles2", 0.7F, 0.35F, 4))
				.sound(vanilla("ambient/nether/crimson_forest/particles3", 0.65F, 0.35F, 4))
				.sound(vanilla("ambient/nether/crimson_forest/shroom1", 0.6F, 0.25F, 2))
				.sound(vanilla("ambient/nether/crimson_forest/shroom2", 0.6F, 0.25F, 2))
				.sound(vanilla("ambient/nether/crimson_forest/shroom3", 0.55F, 0.25F, 2))
				.sound(vanilla("ambient/nether/warped_forest/addition1", 0.6F, 0.08F, 1)));

		exporter.add(EamSounds.UNDERWORLD_BURNT_FOREST_LOOP, loop(EamSounds.UNDERWORLD_BURNT_FOREST_LOOP, "burnt_mushroom_forest", 0.55F, 1.0F));
		exporter.add(EamSounds.UNDERWORLD_BURNT_FOREST_ADDITIONS, SoundTypeBuilder.of(EamSounds.UNDERWORLD_BURNT_FOREST_ADDITIONS)
				.sound(vanilla("ambient/nether/basalt_deltas/click1", 0.8F, 0.25F, 3))
				.sound(vanilla("ambient/nether/basalt_deltas/click2", 0.8F, 0.25F, 3))
				.sound(vanilla("ambient/nether/basalt_deltas/click4", 0.75F, 0.3F, 3))
				.sound(vanilla("ambient/nether/soulsand_valley/sand1", 0.8F, 0.25F, 3))
				.sound(vanilla("ambient/nether/soulsand_valley/sand2", 0.75F, 0.25F, 3))
				.sound(vanilla("random/fizz", 0.6F, 0.05F, 1)));

		exporter.add(EamSounds.UNDERWORLD_LAVA_CAVES_LOOP, SoundTypeBuilder.of(EamSounds.UNDERWORLD_LAVA_CAVES_LOOP)
				.sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Identifier.withDefaultNamespace("ambient/nether/basalt_deltas/ambience"))
						.pitch(0.8F).volume(0.7F * AMBIENCE_VOLUME).stream(true)));
		exporter.add(EamSounds.UNDERWORLD_LAVA_CAVES_ADDITIONS, SoundTypeBuilder.of(EamSounds.UNDERWORLD_LAVA_CAVES_ADDITIONS)
				.sound(vanilla("liquid/lavapop", 0.8F, 0.5F, 4))
				.sound(vanilla("ambient/nether/basalt_deltas/basaltground1", 0.7F, 0.4F, 2))
				.sound(vanilla("ambient/nether/basalt_deltas/basaltground2", 0.7F, 0.4F, 2))
				.sound(vanilla("block/pointed_dripstone/drip_lava1", 0.8F, 0.4F, 1))
				.sound(vanilla("block/pointed_dripstone/drip_lava3", 0.8F, 0.4F, 1)));

		SoundTypeBuilder drips = SoundTypeBuilder.of(EamSounds.UNDERWORLD_LAKE_CAVES_ADDITIONS);
		for (int i = 1; i <= 6; i++) {
			drips.sound(vanilla("block/pointed_dripstone/drip_water" + i, 0.9F, 0.45F, 2));
			drips.sound(vanilla("block/pointed_dripstone/drip_water_cauldron" + i, 0.8F, 0.4F, 2));
		}
		exporter.add(EamSounds.UNDERWORLD_LAKE_CAVES_ADDITIONS, drips);
		exporter.add(EamSounds.UNDERWORLD_RIVER_CAVES_LOOP, loop(EamSounds.UNDERWORLD_RIVER_CAVES_LOOP, "river_caves", 0.3F, 1.0F));

		exporter.add(EamSounds.UNDERWORLD_FROZEN_CAVES_LOOP, loop(EamSounds.UNDERWORLD_FROZEN_CAVES_LOOP, "wind", 0.22F, 0.9F));
		exporter.add(EamSounds.UNDERWORLD_ICE_CAVES_LOOP, loop(EamSounds.UNDERWORLD_ICE_CAVES_LOOP, "wind", 0.35F, 1.0F));
		SoundTypeBuilder ice = SoundTypeBuilder.of(EamSounds.UNDERWORLD_ICE_ADDITIONS);
		for (int i = 1; i <= 4; i++) {
			ice.sound(vanilla("block/amethyst/resonate" + i, 0.5F, 0.15F, 2));
			ice.sound(vanilla("block/powder_snow/break" + i, 0.7F, 0.3F, 2));
		}
		ice.sound(vanilla("random/glass1", 0.5F, 0.05F, 1)).sound(vanilla("random/glass2", 0.5F, 0.05F, 1));
		exporter.add(EamSounds.UNDERWORLD_ICE_ADDITIONS, ice);

		exporter.add(EamSounds.UNDERWORLD_DEPTHS_LOOP, loop(EamSounds.UNDERWORLD_DEPTHS_LOOP, "depths", 0.2F, 1.0F));
		exporter.add(EamSounds.UNDERWORLD_DEPTHS_ADDITIONS, SoundTypeBuilder.of(EamSounds.UNDERWORLD_DEPTHS_ADDITIONS)
				.sound(vanilla("ambient/nether/basalt_deltas/basaltground1", 0.5F, 0.5F, 3))
				.sound(vanilla("ambient/nether/basalt_deltas/basaltground2", 0.5F, 0.5F, 3))
				.sound(vanilla("ambient/nether/basalt_deltas/basaltground3", 0.5F, 0.5F, 3))
				.sound(vanilla("ambient/nether/nether_wastes/addition1", 0.6F, 0.3F, 1))
				.sound(vanilla("ambient/nether/nether_wastes/addition4", 0.6F, 0.3F, 1))
				.sound(vanilla("liquid/lavapop", 0.6F, 0.4F, 2)));
	}

	private static SoundTypeBuilder loop(SoundEvent event, String file, float volume, float pitch) {
		return SoundTypeBuilder.of(event).sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Eam.id("ambient/underworld/" + file))
				.volume(volume * AMBIENCE_VOLUME).pitch(pitch).stream(true));
	}

	private static SoundTypeBuilder.RegistrationBuilder vanilla(String file, float pitch, float volume, int weight) {
		return SoundTypeBuilder.RegistrationBuilder.ofFile(Identifier.withDefaultNamespace(file)).pitch(pitch).volume(volume * AMBIENCE_VOLUME).weight(weight);
	}

	private static SoundTypeBuilder wight(SoundEvent event, String name, String file, int files, float pitch, float volume) {
		SoundTypeBuilder builder = SoundTypeBuilder.of(event).subtitle("subtitles.eam.entity.wight." + name);
		for (int i = 1; i <= files; i++) {
			builder.sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Identifier.withDefaultNamespace(file + i)).pitch(pitch).volume(volume));
		}
		return builder;
	}

	private static SoundTypeBuilder leshy(SoundEvent event, String name, float pitch, float volume, String... files) {
		SoundTypeBuilder builder = SoundTypeBuilder.of(event).subtitle("subtitles.eam.entity.leshy." + name);
		for (String file : files) {
			builder.sound(SoundTypeBuilder.RegistrationBuilder.ofFile(Identifier.withDefaultNamespace(file)).pitch(pitch).volume(volume));
		}
		return builder;
	}

	@Override
	public String getName() {
		return "Sounds";
	}
}
