package io.github.hellenter.eam.mobs.entity.boss;

import io.github.hellenter.eam.content.EamParticles;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * Yellow stars circling round the head of a stunned boss. Each star lives a few ticks sliding along the ring, and the
 * next one is sent where it leaves off, so together they go round; called every tick while the boss is dazed.
 */
public final class DizzyStars {
	private static final int STARS = 4;
	/** How far round the ring the stars go each tick, in radians. */
	private static final double TURN = 0.22;
	/** A new set of stars every this many ticks; each one lives a little longer than that. */
	private static final int EVERY = 5;

	private DizzyStars() {
	}

	public static void show(ServerLevel level, LivingEntity entity) {
		if (entity.tickCount % EVERY != 0) {
			return;
		}
		double spin = entity.tickCount * TURN;
		double y = entity.getEyeY() + 0.45;
		double radius = Math.max(0.35, entity.getBbWidth() * 0.5);
		// They keep up with the head as the boss moves.
		double mx = entity.getX() - entity.xo;
		double my = entity.getY() - entity.yo;
		double mz = entity.getZ() - entity.zo;
		for (int i = 0; i < STARS; i++) {
			double angle = spin + i * Math.PI * 2.0 / STARS;
			double x = entity.getX() + Math.cos(angle) * radius;
			double z = entity.getZ() + Math.sin(angle) * radius;
			// Count 0: the "velocity" is the star's step along the ring each tick.
			level.sendParticles(EamParticles.DIZZY_STAR, x, y, z, 0, mx - Math.sin(angle) * radius * TURN, my,
					mz + Math.cos(angle) * radius * TURN, 1.0);
		}
	}
}
