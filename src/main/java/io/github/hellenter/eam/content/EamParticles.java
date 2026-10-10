package io.github.hellenter.eam.content;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;

import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

/** Particle types added by EAM; their textures and client particles are in the client source set. */
public final class EamParticles {
	/**
	 * The Fallen Lord's heavy strike: a red arc that rolls flat along the ground out to the end of its reach. Sent with
	 * count 0 so the "velocity" carries its data: x and z are where he faces times the reach, y is the ticks it takes.
	 */
	public static final SimpleParticleType HEAVY_WAVE = register("heavy_wave");

	private EamParticles() {
	}

	public static void register() {
		// Loads the class, which registers the particle types above.
	}

	private static SimpleParticleType register(String name) {
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, Eam.id(name), FabricParticleTypes.simple(true));
	}
}
