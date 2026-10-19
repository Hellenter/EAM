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

	/** A rising, ringing hum with a crackle of embers as Koschei's blade glows orange: only a raised shield stops this blow. */
	public static final SoundEvent KOSCHEI_ORANGE_CUE = register("entity.koschei.orange_cue");
	/** A flock of ravens bursting up in a flurry of wing beats where Koschei vanishes. */
	public static final SoundEvent KOSCHEI_RAVENS = register("entity.koschei.ravens");
	/** The hoarse, hollow caw of one of the phantom ravens Koschei calls up. */
	public static final SoundEvent KOSCHEI_RAVEN_CAW = register("entity.koschei_raven.caw");

	/** The wight's rare rasping breath while it stalks unseen. */
	public static final SoundEvent WIGHT_AMBIENT = register("entity.wight.ambient");
	/** The hiss of its blow. */
	public static final SoundEvent WIGHT_ATTACK = register("entity.wight.attack");
	/** Hoarse groans while it burns. */
	public static final SoundEvent WIGHT_GROAN = register("entity.wight.groan");
	public static final SoundEvent WIGHT_HURT = register("entity.wight.hurt");
	public static final SoundEvent WIGHT_DEATH = register("entity.wight.death");

	/** The leshy's slow creaking groan, like a tree in the wind. */
	public static final SoundEvent LESHY_AMBIENT = register("entity.leshy.ambient");
	/** The rake of its twig claws. */
	public static final SoundEvent LESHY_ATTACK = register("entity.leshy.attack");
	/** Its bellow as it turns on a player, and as it calls the wolves. */
	public static final SoundEvent LESHY_ROAR = register("entity.leshy.roar");
	/** Roots tearing up out of the ground. */
	public static final SoundEvent LESHY_ROOTS = register("entity.leshy.roots");
	/** A flock of crows: its flight, and the cawing a player it haunts hears overhead. */
	public static final SoundEvent LESHY_CROWS = register("entity.leshy.crows");
	/** A dull knock of wood: a blow the tree beside it took. */
	public static final SoundEvent LESHY_SHIELD = register("entity.leshy.shield");
	public static final SoundEvent LESHY_HURT = register("entity.leshy.hurt");
	public static final SoundEvent LESHY_DEATH = register("entity.leshy.death");

	/** The screech of a vampire bat flock taking wing in the distance: get ready. */
	public static final SoundEvent VAMPIRE_BAT_SWARM = register("entity.vampire_bat.swarm");

	/**
	 * Underworld biome ambience: a loop that plays while the player is in the biome and rare additions over it. Loops are
	 * our own synthesized files; additions are vanilla files pitched and mixed into a voice of each biome.
	 */
	public static final SoundEvent UNDERWORLD_STONE_WASTES_LOOP = register("ambient.underworld.stone_wastes.loop");
	public static final SoundEvent UNDERWORLD_STONE_WASTES_ADDITIONS = register("ambient.underworld.stone_wastes.additions");
	public static final SoundEvent UNDERWORLD_MUSHROOM_FOREST_LOOP = register("ambient.underworld.mushroom_forest.loop");
	public static final SoundEvent UNDERWORLD_MUSHROOM_FOREST_ADDITIONS = register("ambient.underworld.mushroom_forest.additions");
	public static final SoundEvent UNDERWORLD_BURNT_FOREST_LOOP = register("ambient.underworld.burnt_mushroom_forest.loop");
	public static final SoundEvent UNDERWORLD_BURNT_FOREST_ADDITIONS = register("ambient.underworld.burnt_mushroom_forest.additions");
	public static final SoundEvent UNDERWORLD_LAVA_CAVES_LOOP = register("ambient.underworld.lava_caves.loop");
	public static final SoundEvent UNDERWORLD_LAVA_CAVES_ADDITIONS = register("ambient.underworld.lava_caves.additions");
	public static final SoundEvent UNDERWORLD_LAKE_CAVES_ADDITIONS = register("ambient.underworld.lake_caves.additions");
	public static final SoundEvent UNDERWORLD_RIVER_CAVES_LOOP = register("ambient.underworld.river_caves.loop");
	public static final SoundEvent UNDERWORLD_FROZEN_CAVES_LOOP = register("ambient.underworld.frozen_caves.loop");
	public static final SoundEvent UNDERWORLD_ICE_CAVES_LOOP = register("ambient.underworld.ice_caves.loop");
	public static final SoundEvent UNDERWORLD_ICE_ADDITIONS = register("ambient.underworld.ice_caves.additions");
	public static final SoundEvent UNDERWORLD_DEPTHS_LOOP = register("ambient.underworld.depths.loop");
	public static final SoundEvent UNDERWORLD_DEPTHS_ADDITIONS = register("ambient.underworld.depths.additions");

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
