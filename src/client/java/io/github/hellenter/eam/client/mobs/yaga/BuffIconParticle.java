package io.github.hellenter.eam.client.mobs.yaga;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

import org.jspecify.annotations.Nullable;

/**
 * One icon circling over the head of a creature Baba Yaga strengthened: swords, a heart or a bolt, glowing and facing
 * the camera. It lives a few ticks, sliding along the ring at the "velocity" it was sent with; the server sends the next
 * one where it leaves off, so together they go round.
 */
public class BuffIconParticle extends SingleQuadParticle {
	private static final int LIFETIME = 6;

	private BuffIconParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
		super(level, x, y, z, sprites.first());
		this.xd = xd;
		this.yd = yd;
		this.zd = zd;
		this.lifetime = LIFETIME;
		this.gravity = 0.0F;
		this.hasPhysics = false;
		this.quadSize = 0.13F;
	}

	@Override
	public void tick() {
		xo = x;
		yo = y;
		zo = z;
		if (age++ >= lifetime) {
			remove();
			return;
		}
		x += xd;
		y += yd;
		z += zd;
	}

	@Override
	public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
		// Fades in and out at the ends so the hand-over to the next icon doesn't blink.
		float time = age + partialTick;
		alpha = Math.min(1.0F, Math.min(time + 1.0F, lifetime - time + 1.0F));
		super.extract(state, camera, partialTick);
	}

	@Override
	public int getLightCoords(float partialTick) {
		return 15728880;
	}

	@Override
	protected SingleQuadParticle.Layer getLayer() {
		return SingleQuadParticle.Layer.TRANSLUCENT;
	}

	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public @Nullable Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux,
				double yAux, double zAux, RandomSource random) {
			return new BuffIconParticle(level, x, y, z, xAux, yAux, zAux, sprites);
		}
	}
}
