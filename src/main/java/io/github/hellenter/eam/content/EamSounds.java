package io.github.hellenter.eam.content;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** Sound events added by EAM; their files and subtitles are in the client resources and datagen. */
public final class EamSounds {
	/** A bright metallic ring as the Fallen Lord's eyes flash: his unblockable dash is coming, step aside. */
	public static final SoundEvent FALLEN_LORD_DASH_WARNING = register("entity.fallen_lord.dash_warning");

	private EamSounds() {
	}

	public static void register() {
		// Loads the class, which registers the sound events above.
	}

	private static SoundEvent register(String name) {
		Identifier id = Eam.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}
}
