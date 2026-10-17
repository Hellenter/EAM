package io.github.hellenter.eam.client.world.underworld;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import org.jspecify.annotations.Nullable;

/**
 * The air of the underworld biomes: motes the biome spawns around the player, standing still at first, then drifting off
 * on their own. They fade in and out instead of popping, and give off a little light of their own so a cave lit by
 * nothing still shows them faintly.
 */
public class UnderworldAmbientParticle extends SingleQuadParticle {
	/**
	 * How each kind moves. Gravity is in vanilla particle units (0.04 blocks/tick² each); a negative one rises.
	 *
	 * @param rise     starting upward speed, blocks per tick, picked between zero and this
	 * @param drift    starting sideways speed, blocks per tick, picked between minus and plus this
	 * @param animated frames play out over the particle's life; otherwise one is picked at random
	 * @param sway     drifts from side to side, like a flake settling
	 * @param glow     block light it carries itself, 0..15
	 * @param alpha    how opaque it is at its fullest
	 */
	public enum Style {
		/** Pale spores floating up between the giant mushrooms. */
		SPORE(140, 220, 0.05F, 0.09F, -0.004F, 0.99F, 0.006F, 0.004F, 0.0F, false, true, 9, 0.85F, false),
		/** Ash flakes settling on the burnt forest, turning as they fall. */
		ASH(120, 200, 0.06F, 0.1F, 0.01F, 0.98F, 0.0F, 0.004F, 0.06F, false, true, 3, 0.9F, true),
		/** Snow sifting down from the ice overhead. */
		SNOW(140, 220, 0.06F, 0.1F, 0.0075F, 0.98F, 0.0F, 0.003F, 0.03F, false, true, 5, 0.95F, true),
		/** Embers lifting off the lava, cooling from yellow to dull red. */
		EMBER(30, 60, 0.04F, 0.07F, -0.012F, 0.96F, 0.03F, 0.008F, 0.0F, true, false, 15, 1.0F, false),
		/** Stone dust hanging in the wastes, barely moving. */
		DUST(100, 160, 0.03F, 0.05F, 0.0005F, 0.99F, 0.0F, 0.002F, 0.0F, false, false, 3, 0.6F, false);

		final int minLife;
		final int maxLife;
		final float minSize;
		final float maxSize;
		final float gravity;
		final float friction;
		final float rise;
		final float drift;
		final float spin;
		final boolean animated;
		final boolean sway;
		final int glow;
		final float alpha;
		final boolean physics;

		Style(int minLife, int maxLife, float minSize, float maxSize, float gravity, float friction, float rise, float drift, float spin,
				boolean animated, boolean sway, int glow, float alpha, boolean physics) {
			this.minLife = minLife;
			this.maxLife = maxLife;
			this.minSize = minSize;
			this.maxSize = maxSize;
			this.gravity = gravity;
			this.friction = friction;
			this.rise = rise;
			this.drift = drift;
			this.spin = spin;
			this.animated = animated;
			this.sway = sway;
			this.glow = glow;
			this.alpha = alpha;
			this.physics = physics;
		}
	}

	private final Style style;
	private final SpriteSet sprites;
	private final float spinSpeed;
	private final float swayPhase;
	private final float flicker;

	private UnderworldAmbientParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites, Style style) {
		super(level, x, y, z, sprites.first());
		this.style = style;
		this.sprites = sprites;
		this.lifetime = style.minLife + random.nextInt(style.maxLife - style.minLife + 1);
		this.gravity = style.gravity;
		this.friction = style.friction;
		this.hasPhysics = style.physics;
		this.xd = (random.nextFloat() * 2.0F - 1.0F) * style.drift;
		this.yd = random.nextFloat() * style.rise;
		this.zd = (random.nextFloat() * 2.0F - 1.0F) * style.drift;
		this.quadSize = Mth.lerp(random.nextFloat(), style.minSize, style.maxSize);
		this.spinSpeed = (random.nextFloat() * 2.0F - 1.0F) * style.spin;
		this.roll = style.spin > 0.0F ? random.nextFloat() * Mth.TWO_PI : 0.0F;
		this.oRoll = roll;
		this.swayPhase = random.nextFloat() * Mth.TWO_PI;
		this.flicker = 0.6F + random.nextFloat() * 0.8F;
		if (style.animated) {
			setSpriteFromAge(sprites);
		} else {
			setSprite(sprites.get(random));
		}
	}

	@Override
	public void tick() {
		oRoll = roll;
		roll += spinSpeed;
		super.tick();
		if (removed) {
			return;
		}
		if (style.sway) {
			float swing = Mth.sin(age * 0.08F + swayPhase) * 0.0015F;
			xd += swing * Mth.cos(swayPhase);
			zd += swing * Mth.sin(swayPhase);
		}
		if (style.animated) {
			setSpriteFromAge(sprites);
		}
		if (onGround && style.physics) {
			// Settled flakes melt or crumble away instead of piling up.
			lifetime = Math.min(lifetime, age + 10);
		}
	}

	@Override
	public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
		float time = age + partialTick;
		float in = Mth.clamp(time / (lifetime * 0.15F), 0.0F, 1.0F);
		float out = Mth.clamp((lifetime - time) / (lifetime * 0.35F), 0.0F, 1.0F);
		float fade = style == Style.EMBER ? 0.75F + 0.25F * Mth.sin(time * flicker) : 1.0F;
		alpha = style.alpha * Math.min(in, out) * fade;
		super.extract(state, camera, partialTick);
	}

	@Override
	public int getLightCoords(float partialTick) {
		return LightCoordsUtil.lightCoordsWithEmission(super.getLightCoords(partialTick), style.glow);
	}

	@Override
	protected SingleQuadParticle.Layer getLayer() {
		return SingleQuadParticle.Layer.TRANSLUCENT;
	}

	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;
		private final Style style;

		public Provider(SpriteSet sprites, Style style) {
			this.sprites = sprites;
			this.style = style;
		}

		@Override
		public @Nullable Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux,
				double zAux, RandomSource random) {
			return new UnderworldAmbientParticle(level, x, y, z, sprites, style);
		}
	}
}
