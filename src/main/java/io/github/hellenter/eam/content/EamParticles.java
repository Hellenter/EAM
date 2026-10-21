package io.github.hellenter.eam.content;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;

import net.minecraft.core.Registry;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

/** Particle types added by EAM; their textures and client particles are in the client source set. */
public final class EamParticles {
	/**
	 * The Fallen Lord's heavy strike: a red arc that rolls flat along the ground out to the end of its reach. Sent with
	 * count 0 so the "velocity" carries its data: x and z are where he faces times the reach, y is the ticks it takes.
	 */
	public static final SimpleParticleType HEAVY_WAVE = register("heavy_wave");
	/**
	 * The icons circling over the head of a creature Baba Yaga strengthened: yellow swords for strength, red hearts for
	 * absorption, pale blue bolts for speed. Sent with count 0 so the "velocity" is the icon's motion along the ring.
	 */
	public static final SimpleParticleType YAGA_STRENGTH = register("yaga_strength");
	public static final SimpleParticleType YAGA_ABSORPTION = register("yaga_absorption");
	public static final SimpleParticleType YAGA_SPEED = register("yaga_speed");
	/** Baba Yaga's attacks, one look each. Black crow feathers trailing her dive. */
	public static final SimpleParticleType YAGA_FEATHER = register("yaga_feather");
	/** Green grave wisps rising as she raises her creatures. */
	public static final SimpleParticleType YAGA_WISP = register("yaga_wisp");
	/** Skull-faced green fumes off her burst flask. */
	public static final SimpleParticleType YAGA_TOXIC = register("yaga_toxic");
	/** Runes along a strengthening spell, in the spell's colour. */
	public static final ParticleType<ColorParticleOption> YAGA_RUNE = Registry.register(BuiltInRegistries.PARTICLE_TYPE, Eam.id("yaga_rune"),
			FabricParticleTypes.complex(true, ColorParticleOption::codec, ColorParticleOption::streamCodec));
	/** An eye opening over a player the blinding glyph curses. */
	public static final SimpleParticleType YAGA_EYE = register("yaga_eye");
	/** Blood: drawn off by the draining glyph, spurting from her meal. */
	public static final SimpleParticleType YAGA_BLOOD = register("yaga_blood");
	/** Acid globs dripping off the acid bolt and splashing where it lands. */
	public static final SimpleParticleType YAGA_ACID = register("yaga_acid");
	/** Purple thread curls off the threads bolt and round whoever it ties. */
	public static final SimpleParticleType YAGA_THREAD = register("yaga_thread");
	/** Embers of the fire beam. */
	public static final SimpleParticleType YAGA_EMBER = register("yaga_ember");
	/** Claw rakes torn across the air, one per rake. */
	public static final SimpleParticleType YAGA_CLAW = register("yaga_claw");

	/** Yellow stars circling round the head of a stunned boss; sent with count 0 so the "velocity" is the star's motion along the ring. */
	public static final SimpleParticleType DIZZY_STAR = register("dizzy_star");

	/** Orange embers streaming off Koschei's blade before his orange blow. */
	public static final SimpleParticleType KOSCHEI_SPARK = register("koschei_spark");
	/** Purple and blue motes left hanging where Koschei vanished, and gathering where he appears. */
	public static final SimpleParticleType KOSCHEI_WARP = register("koschei_warp");
	/** The ravens that scatter, beating their wings, from where Koschei vanishes; sent with count 0 so the "velocity" is their flight. */
	public static final SimpleParticleType KOSCHEI_RAVEN = register("koschei_raven");
	/** Blue motes of Koschei's burst of force, rushing out from his palm. */
	public static final SimpleParticleType KOSCHEI_FORCE = register("koschei_force");
	/** Small spinning snowflakes of Koschei's blizzard; sent with count 0 so the "velocity" is their flight. */
	public static final SimpleParticleType KOSCHEI_SNOWFLAKE = register("koschei_snowflake");
	/** Panes of the ice field round Koschei, that show for a moment where it turns an arrow. */
	public static final SimpleParticleType KOSCHEI_ICE_FIELD = register("koschei_ice_field");
	/** Shards of ice and glass flung out when the crystal Koschei waits sealed in at his castle bursts. */
	public static final SimpleParticleType KOSCHEI_CRYSTAL_SHARD = register("koschei_crystal_shard");

	/**
	 * The Lost longbowman's power shot: red glints drawn into his bow while it is warned (sent with count 0 so the
	 * "velocity" is their flight to the bow), and the red starburst at the bow when the arrow flies.
	 */
	public static final SimpleParticleType LONGBOW_CHARGE = register("longbow_charge");
	public static final SimpleParticleType LONGBOW_FLARE = register("longbow_flare");
	/** Hot motes behind his red power arrow. */
	public static final SimpleParticleType RED_ARROW_TRAIL = register("red_arrow_trail");
	/** Black soot with a dried-blood rim behind his marking arrow, and puffing where it marks someone. */
	public static final SimpleParticleType MARK_TRAIL = register("mark_trail");

	/**
	 * Underworld biome ambience, spawned by the biomes themselves around the player: pale spores drifting up in the
	 * mushroom forest, ash flakes settling in the burnt one, snow sifting down in the frozen and ice caves, embers rising
	 * over lava and stone dust hanging in the wastes.
	 */
	public static final SimpleParticleType UNDERWORLD_SPORE = registerAmbient("underworld_spore");
	public static final SimpleParticleType UNDERWORLD_ASH = registerAmbient("underworld_ash");
	public static final SimpleParticleType UNDERWORLD_SNOW = registerAmbient("underworld_snow");
	public static final SimpleParticleType UNDERWORLD_EMBER = registerAmbient("underworld_ember");
	public static final SimpleParticleType UNDERWORLD_DUST = registerAmbient("underworld_dust");

	private EamParticles() {
	}

	public static void register() {
		// Loads the class, which registers the particle types above.
	}

	private static SimpleParticleType register(String name) {
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, Eam.id(name), FabricParticleTypes.simple(true));
	}

	/** Scenery, not a cue: thinned out by the particle setting like vanilla biome particles. */
	private static SimpleParticleType registerAmbient(String name) {
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, Eam.id(name), FabricParticleTypes.simple(false));
	}
}
