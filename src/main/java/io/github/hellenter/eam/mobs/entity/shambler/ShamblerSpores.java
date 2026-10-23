package io.github.hellenter.eam.mobs.entity.shambler;

import io.github.hellenter.eam.content.EamParticles;
import io.github.hellenter.eam.content.EamSounds;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The puffs a shambler gives off when it is hit: a cloud of green-brown spores that turns the stomach of everything
 * round it, or a smoldering one's spray of sparks that sets it alight. Other shamblers do not mind either. A thrown
 * spore sac bursts into the same cloud.
 */
public final class ShamblerSpores {
	private ShamblerSpores() {
	}

	/** A spore cloud at {@code at}: everything alive within the spore radius is sick for a while. */
	public static void spores(ServerLevel level, Vec3 at, @Nullable Entity cause) {
		ShamblerSettings settings = ShamblerModule.settings();
		double radius = settings.sporeRadius();
		level.sendParticles(EamParticles.SHAMBLER_SPORE, at.x, at.y, at.z, 40, radius * 0.35, 0.4, radius * 0.35, 0.02);
		level.playSound(null, at.x, at.y, at.z, EamSounds.SHAMBLER_SPORES, SoundSource.HOSTILE, 1.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
		for (LivingEntity living : around(level, at, radius)) {
			living.addEffect(new MobEffectInstance(MobEffects.NAUSEA, settings.nauseaTicks()), cause);
		}
	}

	/** A spray of sparks at {@code at}: everything within the spore radius that fire can hurt catches fire for a moment. */
	public static void sparks(ServerLevel level, Vec3 at) {
		ShamblerSettings settings = ShamblerModule.settings();
		double radius = settings.sporeRadius();
		level.sendParticles(EamParticles.SHAMBLER_SPARK, at.x, at.y, at.z, 30, radius * 0.3, 0.4, radius * 0.3, 0.08);
		level.playSound(null, at.x, at.y, at.z, EamSounds.SHAMBLER_SPARKS, SoundSource.HOSTILE, 1.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
		for (LivingEntity living : around(level, at, radius)) {
			if (!living.fireImmune()) {
				living.igniteForTicks(settings.sparkFireTicks());
			}
		}
	}

	private static Iterable<LivingEntity> around(ServerLevel level, Vec3 at, double radius) {
		return level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius),
				living -> living.isAlive() && !(living instanceof MushroomShambler) && !living.isSpectator()
						&& living.distanceToSqr(at) <= radius * radius);
	}
}
