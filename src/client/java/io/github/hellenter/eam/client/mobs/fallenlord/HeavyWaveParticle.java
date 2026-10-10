package io.github.hellenter.eam.client.mobs.fallenlord;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

/**
 * One wave of the Fallen Lord's heavy strike: a red pixel arc lying flat on the ground that rolls out from him to the
 * end of the strike's reach, then fades. The quad is centred on him and grows; the arc is drawn near the texture's
 * edge, so it sits at the quad's half-size from him.
 */
public class HeavyWaveParticle extends SingleQuadParticle {
	/** Ticks the wave lingers at the edge of the reach while it fades. */
	private static final int FADE_TICKS = 3;
	/** The arc's distance from the texture centre, as a share of half the texture. */
	private static final float ARC_RADIUS = 15.0F / 16.0F;

	private final float reach;
	private final int travelTicks;
	private final Quaternionf rotation;

	private HeavyWaveParticle(ClientLevel level, double x, double y, double z, double dirX, double dirZ, float reach, int travelTicks,
			SpriteSet sprites) {
		super(level, x, y, z, sprites.first());
		this.reach = reach;
		this.travelTicks = Math.max(1, travelTicks);
		this.lifetime = this.travelTicks + FADE_TICKS;
		this.gravity = 0.0F;
		this.hasPhysics = false;
		// Flat on the ground, facing up, with the texture's top towards where he faces.
		this.rotation = new Quaternionf().rotationY((float) Math.atan2(-dirX, -dirZ)).rotateX((float) (-Math.PI / 2.0));
	}

	@Override
	public void tick() {
		xo = x;
		yo = y;
		zo = z;
		if (age++ >= lifetime) {
			remove();
		}
	}

	@Override
	public float getQuadSize(float partialTick) {
		float progress = Math.min(1.0F, (age + partialTick) / travelTicks);
		return reach * progress / ARC_RADIUS;
	}

	@Override
	public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
		float time = age + partialTick;
		alpha = time <= travelTicks ? 1.0F : 1.0F - Mth.clamp((time - travelTicks) / FADE_TICKS, 0.0F, 1.0F);
		extractRotatedQuad(state, camera, rotation, partialTick);
	}

	@Override
	public int getLightCoords(float partialTick) {
		// The arc glows like the blade's red edge.
		return 15728880;
	}

	@Override
	protected SingleQuadParticle.Layer getLayer() {
		return SingleQuadParticle.Layer.TRANSLUCENT;
	}

	/** Reads the wave's data from the particle "velocity": where he faces times the reach, and the travel ticks. */
	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public @Nullable Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux,
				double yAux, double zAux, RandomSource random) {
			double reach = Math.sqrt(xAux * xAux + zAux * zAux);
			if (reach < 1.0E-3) {
				return null;
			}
			return new HeavyWaveParticle(level, x, y, z, xAux / reach, zAux / reach, (float) reach, (int) Math.round(yAux), sprites);
		}
	}
}
