package io.github.hellenter.eam.mobs.entity.yaga;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;

/**
 * Baba Yaga's death: she bursts into flames and writhes in convulsions as she chars black, then crumbles into a heap of
 * ash, and her loot drops from the ashes. The renderer reads the same timeline to pose and tint her.
 */
public final class YagaDeath {
	/** She burns and writhes until this tick of her death. */
	public static final int CRUMBLE_START = 56;
	/** And is a heap of ash, gone, at this one. */
	public static final int DEATH_TICKS = 80;
	/** Her glowing eyes go out this far into it. */
	public static final int EYES_OUT = 30;
	/** Fully charred by this tick. */
	private static final float CHARRED_AT = 48.0F;
	private static final int CHARRED = 0x3A2E28;
	private static final int ASH = 0x8E8983;

	private YagaDeath() {
	}

	/** How hard she writhes, 0 to 1, {@code t} ticks into her death: fits from the start, dying down as she crumbles. */
	public static float convulsion(float t) {
		if (t < 0.0F || t >= DEATH_TICKS) {
			return 0.0F;
		}
		if (t < CRUMBLE_START) {
			return Math.min(1.0F, 0.6F + t / 20.0F);
		}
		return Math.max(0.0F, 1.0F - (t - CRUMBLE_START) / 10.0F);
	}

	/** How far she has crumbled into a heap, 0 to 1. */
	public static float crumble(float t) {
		return clamp((t - CRUMBLE_START) / (DEATH_TICKS - CRUMBLE_START - 4.0F));
	}

	/** The tint over her skin and clothes, RGB: white (none), charring black as she burns, then ash grey. */
	public static int tint(float t) {
		if (t < CRUMBLE_START) {
			return lerp(clamp(t / CHARRED_AT), 0xFFFFFF, CHARRED);
		}
		return lerp(crumble(t), CHARRED, ASH);
	}

	/** Whether the flames still burn on her. */
	public static boolean burning(float t) {
		return t >= 0.0F && t < CRUMBLE_START + 6;
	}

	/** Flames, smoke and screams, then ash: called on the server each tick of her death. */
	static void tick(BabaYaga yaga, ServerLevel level, int t) {
		double w = yaga.getBbWidth() * 0.4;
		double h = yaga.getBbHeight();
		if (t == 1) {
			// She catches fire all at once.
			level.sendParticles(ParticleTypes.FLAME, yaga.getX(), yaga.getY(0.5), yaga.getZ(), 30, w, h * 0.35, w, 0.06);
			yaga.playSound(SoundEvents.FIRECHARGE_USE, 1.5F, 0.7F);
			yaga.playSound(SoundEvents.BLAZE_SHOOT, 1.0F, 0.6F);
		}
		if (t < CRUMBLE_START) {
			level.sendParticles(ParticleTypes.FLAME, yaga.getX(), yaga.getY(0.45), yaga.getZ(), 3, w, h * 0.35, w, 0.01);
			if (t % 2 == 0) {
				level.sendParticles(ParticleTypes.LARGE_SMOKE, yaga.getX(), yaga.getY(0.9), yaga.getZ(), 1, w * 0.5, 0.1, w * 0.5, 0.02);
			}
			if (t % 5 == 0) {
				level.sendParticles(ParticleTypes.LAVA, yaga.getX(), yaga.getY(0.5), yaga.getZ(), 1, w, h * 0.25, w, 0.0);
			}
			if (t % 12 == 4) {
				yaga.playSound(SoundEvents.FIRE_AMBIENT, 1.5F, 0.8F);
			}
			if (t % 9 == 5 && t < CRUMBLE_START - 10) {
				// Shrieks, higher and higher.
				yaga.playSound(SoundEvents.WITCH_HURT, 1.6F, 0.7F + t / (float) CRUMBLE_START * 0.6F);
			}
		} else {
			level.sendParticles(ParticleTypes.WHITE_ASH, yaga.getX(), yaga.getY(0.4), yaga.getZ(), 14, w, h * 0.25, w, 0.02);
			level.sendParticles(ParticleTypes.ASH, yaga.getX(), yaga.getY(0.3), yaga.getZ(), 6, w, h * 0.2, w, 0.0);
			if (t % 3 == 0) {
				level.sendParticles(ParticleTypes.SMOKE, yaga.getX(), yaga.getY(0.4), yaga.getZ(), 3, w * 0.5, 0.1, w * 0.5, 0.01);
			}
			if (t % 5 == 0) {
				yaga.playSound(SoundEvents.SAND_BREAK, 0.9F, 0.6F);
			}
			if (t == CRUMBLE_START) {
				yaga.playSound(SoundEvents.FIRE_EXTINGUISH, 1.0F, 0.6F);
			}
		}
		if (t == DEATH_TICKS - 1) {
			level.sendParticles(ParticleTypes.WHITE_ASH, yaga.getX(), yaga.getY(0.1), yaga.getZ(), 50, w * 1.4, 0.1, w * 1.4, 0.03);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, yaga.getX(), yaga.getY(0.2), yaga.getZ(), 8, w, 0.1, w, 0.01);
		}
	}

	private static float clamp(float x) {
		return Math.max(0.0F, Math.min(1.0F, x));
	}

	private static int lerp(float f, int from, int to) {
		int r = Math.round((from >> 16 & 0xFF) + f * ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)));
		int g = Math.round((from >> 8 & 0xFF) + f * ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)));
		int b = Math.round((from & 0xFF) + f * ((to & 0xFF) - (from & 0xFF)));
		return r << 16 | g << 8 | b;
	}
}
