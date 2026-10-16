package io.github.hellenter.eam.client.mobs.koschei;

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

import org.jspecify.annotations.Nullable;

/**
 * One of the ravens that scatter from where Koschei vanishes: it flies off the way it was thrown, climbing, beating its
 * wings, and fades into nothing before it gets far.
 */
public class KoscheiRavenParticle extends SingleQuadParticle {
	/** The wing beat, frame by frame: up, half up, level, down and back. */
	private static final int[] BEAT = {0, 1, 2, 3, 2, 1};

	private final SpriteSet sprites;
	private final int beatOffset;

	private KoscheiRavenParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
		super(level, x, y, z, sprites.first());
		this.sprites = sprites;
		this.xd = xd;
		this.yd = yd;
		this.zd = zd;
		this.lifetime = 24 + random.nextInt(14);
		this.gravity = -0.015F;
		this.friction = 0.95F;
		this.hasPhysics = false;
		this.quadSize = 0.22F + random.nextFloat() * 0.1F;
		this.beatOffset = random.nextInt(BEAT.length);
		setBeat();
	}

	@Override
	public void tick() {
		super.tick();
		if (!removed) {
			setBeat();
		}
	}

	private void setBeat() {
		setSprite(sprites.get(BEAT[(age + beatOffset) % BEAT.length], 3));
	}

	@Override
	public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
		// Fades out over the last part of its flight.
		alpha = Mth.clamp((lifetime - age - partialTick) / (lifetime * 0.4F), 0.0F, 1.0F);
		super.extract(state, camera, partialTick);
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
		public @Nullable Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux,
				double zAux, RandomSource random) {
			return new KoscheiRavenParticle(level, x, y, z, xAux, yAux, zAux, sprites);
		}
	}
}
