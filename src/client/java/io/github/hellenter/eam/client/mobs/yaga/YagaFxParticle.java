package io.github.hellenter.eam.client.mobs.yaga;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import org.jspecify.annotations.Nullable;

/**
 * The particles of Baba Yaga's attacks, one look per attack. They share one body and differ by {@link Style}: how long
 * they live, how big they are, how they fall, drift, spin and fade, whether they glow in the dark, and whether their
 * frames play out over their life or one is picked at random.
 */
public class YagaFxParticle extends SingleQuadParticle {
	/**
	 * How each kind behaves. Gravity is in vanilla particle units (0.04 blocks/tick² each); a negative one rises.
	 *
	 * @param animated frames play out over the particle's life; otherwise one is picked at random
	 * @param sway     drifts from side to side as it falls, like a feather
	 * @param grow     how much bigger it is at the end of its life than at the start
	 */
	public enum Style {
		FEATHER(40, 70, 0.14F, 0.2F, 0.05F, 0.96F, 0.12F, false, false, true, 0.0F, true),
		WISP(18, 30, 0.14F, 0.22F, -0.04F, 0.93F, 0.0F, true, true, false, 0.3F, false),
		TOXIC(30, 46, 0.3F, 0.45F, -0.012F, 0.9F, 0.02F, true, false, false, 0.6F, false),
		RUNE(14, 20, 0.09F, 0.13F, 0.0F, 0.9F, 0.05F, false, true, false, -0.3F, false),
		EYE(36, 36, 0.32F, 0.32F, 0.0F, 0.0F, 0.0F, true, true, false, 0.0F, false),
		BLOOD(18, 30, 0.05F, 0.08F, 0.9F, 0.98F, 0.0F, false, false, false, 0.0F, true),
		ACID(16, 26, 0.06F, 0.1F, 0.8F, 0.98F, 0.0F, false, true, false, 0.0F, true),
		THREAD(18, 28, 0.14F, 0.22F, -0.005F, 0.88F, 0.25F, false, true, false, 0.2F, false),
		EMBER(10, 22, 0.05F, 0.09F, -0.03F, 0.92F, 0.0F, true, true, false, -0.4F, false),
		CLAW(7, 7, 0.55F, 0.65F, 0.0F, 0.0F, 0.0F, true, true, false, 0.15F, false),
		// Koschei's cues share this body: embers off his orange blade, the motes he leaves when he vanishes, his burst of force.
		SPARK(8, 16, 0.05F, 0.08F, -0.02F, 0.9F, 0.0F, true, true, false, -0.5F, false),
		WARP(30, 50, 0.08F, 0.14F, -0.003F, 0.92F, 0.1F, true, true, false, -0.3F, false),
		FORCE(8, 14, 0.12F, 0.2F, 0.0F, 0.84F, 0.2F, true, true, false, 0.8F, false),
		// His second phase: the blizzard's small flakes, blown out fast and spinning, and the panes of his ice field.
		FLAKE(24, 40, 0.035F, 0.07F, 0.01F, 0.95F, 0.25F, false, true, true, 0.0F, true),
		ICE_FIELD(7, 10, 0.2F, 0.26F, 0.0F, 0.0F, 0.0F, false, true, false, 0.25F, false),
		// The Lost longbowman's shots: glints flying into his bow, the flash when the red arrow flies, the red arrow's
		// hot motes and the marking arrow's soot.
		CHARGE(10, 12, 0.07F, 0.1F, 0.0F, 0.86F, 0.0F, true, true, false, -0.4F, false),
		FLARE(6, 6, 0.45F, 0.55F, 0.0F, 0.0F, 0.0F, true, true, false, 0.5F, false),
		HOT_TRAIL(6, 10, 0.06F, 0.09F, 0.0F, 0.8F, 0.0F, true, true, false, -0.5F, false),
		SOOT(16, 24, 0.09F, 0.14F, -0.01F, 0.9F, 0.08F, true, true, false, 0.6F, false);

		final int minLife;
		final int maxLife;
		final float minSize;
		final float maxSize;
		final float gravity;
		final float friction;
		final float spin;
		final boolean animated;
		final boolean lit;
		final boolean sway;
		final float grow;
		final boolean physics;

		Style(int minLife, int maxLife, float minSize, float maxSize, float gravity, float friction, float spin, boolean animated, boolean lit,
				boolean sway, float grow, boolean physics) {
			this.minLife = minLife;
			this.maxLife = maxLife;
			this.minSize = minSize;
			this.maxSize = maxSize;
			this.gravity = gravity;
			this.friction = friction;
			this.spin = spin;
			this.animated = animated;
			this.lit = lit;
			this.sway = sway;
			this.grow = grow;
			this.physics = physics;
		}
	}

	private final Style style;
	private final SpriteSet sprites;
	private final float baseSize;
	private final float spinSpeed;
	private final float swayPhase;

	private YagaFxParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites, Style style) {
		super(level, x, y, z, sprites.first());
		this.style = style;
		this.sprites = sprites;
		this.xd = xd;
		this.yd = yd;
		this.zd = zd;
		this.lifetime = style.minLife + (style.maxLife > style.minLife ? random.nextInt(style.maxLife - style.minLife + 1) : 0);
		this.gravity = style.gravity;
		this.friction = style.friction;
		this.hasPhysics = style.physics;
		this.baseSize = Mth.lerp(random.nextFloat(), style.minSize, style.maxSize);
		this.quadSize = baseSize;
		this.spinSpeed = (random.nextFloat() * 2.0F - 1.0F) * style.spin;
		this.swayPhase = random.nextFloat() * Mth.TWO_PI;
		this.roll = style == Style.CLAW ? (random.nextFloat() - 0.5F) * 1.6F : random.nextFloat() * Mth.TWO_PI * (style.spin > 0.0F ? 1.0F : 0.0F);
		this.oRoll = roll;
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
			// Rocking from side to side as it drifts down.
			float swing = Mth.sin(age * 0.25F + swayPhase) * 0.03F;
			xd += swing * Mth.cos(swayPhase);
			zd += swing * Mth.sin(swayPhase);
		}
		if (style.animated) {
			setSpriteFromAge(sprites);
		}
		quadSize = baseSize * (1.0F + style.grow * age / (float) lifetime);
	}

	@Override
	public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
		// Fades out over the last part of its life; the claw rakes, the eye and the flare hold until their frames end.
		float left = (lifetime - age - partialTick) / Math.max(1.0F, lifetime * 0.35F);
		alpha = style == Style.CLAW || style == Style.EYE || style == Style.FLARE ? 1.0F : Mth.clamp(left, 0.0F, 1.0F);
		super.extract(state, camera, partialTick);
	}

	@Override
	public int getLightCoords(float partialTick) {
		return style.lit ? 15728880 : super.getLightCoords(partialTick);
	}

	@Override
	protected SingleQuadParticle.Layer getLayer() {
		return SingleQuadParticle.Layer.TRANSLUCENT;
	}

	public static class Provider<T extends ParticleOptions> implements ParticleProvider<T> {
		private final SpriteSet sprites;
		private final Style style;

		public Provider(SpriteSet sprites, Style style) {
			this.sprites = sprites;
			this.style = style;
		}

		@Override
		public @Nullable Particle createParticle(T options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux,
				RandomSource random) {
			YagaFxParticle particle = new YagaFxParticle(level, x, y, z, xAux, yAux, zAux, sprites, style);
			if (options instanceof ColorParticleOption color) {
				particle.setColor(color.getRed(), color.getGreen(), color.getBlue());
			}
			return particle;
		}
	}
}
