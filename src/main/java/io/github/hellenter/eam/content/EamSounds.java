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
	/** A sharp clang over a heavy thump: a player parried the Fallen Lord. */
	public static final SoundEvent FALLEN_LORD_PARRY = register("entity.fallen_lord.parry");
	/** A bright double ping, like a flicked coin: the Fallen Lord can be parried now. */
	public static final SoundEvent FALLEN_LORD_PARRY_WINDOW = register("entity.fallen_lord.parry_window");

	/** The wight's rare rasping breath while it stalks unseen. */
	public static final SoundEvent WIGHT_AMBIENT = register("entity.wight.ambient");
	/** The hiss of its blow. */
	public static final SoundEvent WIGHT_ATTACK = register("entity.wight.attack");
	/** Hoarse groans while it burns. */
	public static final SoundEvent WIGHT_GROAN = register("entity.wight.groan");
	public static final SoundEvent WIGHT_HURT = register("entity.wight.hurt");
	public static final SoundEvent WIGHT_DEATH = register("entity.wight.death");

	/** The screech of a vampire bat flock taking wing in the distance: get ready. */
	public static final SoundEvent VAMPIRE_BAT_SWARM = register("entity.vampire_bat.swarm");

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
