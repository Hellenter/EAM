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

	private EamParticles() {
	}

	public static void register() {
		// Loads the class, which registers the particle types above.
	}

	private static SimpleParticleType register(String name) {
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, Eam.id(name), FabricParticleTypes.simple(true));
	}
}
