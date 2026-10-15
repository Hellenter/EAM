package io.github.hellenter.eam.mobs.entity.koschei;

import io.github.hellenter.eam.content.EamParticles;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The particles that go with Koschei's cues, spawned on the client from his synced action: orange sparks streaming off the
 * blade before the orange blow, red motes before the red ones, blue motes gathering in his right palm before the burst
 * of force and sparks crackling at the sword's point before the bolt. The glow of the blade and the eyes is the
 * renderer's.
 */
final class KoscheiEffects {
	private static final DustParticleOptions RED = new DustParticleOptions(0xE8231C, 0.9F);

	private KoscheiEffects() {
	}

	static void tick(Koschei koschei) {
		Level level = koschei.level();
		RandomSource random = koschei.getRandom();
		float t = koschei.actionTime(0.0F);
		switch (koschei.action()) {
			case ORANGE -> {
				if (t < KoscheiAction.ORANGE_HIT) {
					for (int i = 0; i < 3; i++) {
						Vec3 at = blade(koschei, random);
						level.addParticle(EamParticles.KOSCHEI_SPARK, at.x, at.y, at.z, (random.nextDouble() - 0.5) * 0.04, 0.03,
								(random.nextDouble() - 0.5) * 0.04);
					}
				}
			}
			case RED, OVERHEAD -> {
				int hit = koschei.action() == KoscheiAction.RED ? KoscheiAction.RED_HIT : KoscheiAction.OVERHEAD_HIT;
				if (t < hit && random.nextInt(2) == 0) {
					Vec3 at = blade(koschei, random);
					level.addParticle(RED, at.x, at.y + (koschei.action() == KoscheiAction.OVERHEAD ? 0.8 : 0.0), at.z, 0.0, 0.02, 0.0);
				}
			}
			case FORCE -> {
				if (t < KoscheiAction.FORCE_RELEASE) {
					Vec3 hand = koschei.rightHand();
					for (int i = 0; i < 2; i++) {
						Vec3 from = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).scale(1.4);
						level.addParticle(EamParticles.KOSCHEI_FORCE, hand.x + from.x, hand.y + from.y, hand.z + from.z, -from.x * 0.15,
								-from.y * 0.15, -from.z * 0.15);
					}
				}
			}
			case BOLT -> {
				if (t < KoscheiAction.BOLT_RELEASE) {
					Vec3 tip = koschei.position().add(Koschei.swordTipOffset(koschei.yBodyRot, koschei.getBbHeight()));
					level.addParticle(ParticleTypes.ELECTRIC_SPARK, tip.x + (random.nextDouble() - 0.5) * 0.4, tip.y + (random.nextDouble() - 0.5) * 0.4,
							tip.z + (random.nextDouble() - 0.5) * 0.4, 0.0, 0.0, 0.0);
				}
			}
			case TELEPORT -> {
				if (random.nextInt(2) == 0) {
					level.addParticle(EamParticles.KOSCHEI_WARP, koschei.getRandomX(0.6), koschei.getY() + random.nextDouble() * 0.3,
							koschei.getRandomZ(0.6), 0.0, 0.02, 0.0);
				}
			}
			default -> {
			}
		}
	}

	/** A point near his sword as it is drawn back for a blow, on his left. */
	private static Vec3 blade(Koschei koschei, RandomSource random) {
		Vec3 forward = Vec3.directionFromRotation(0.0F, koschei.yBodyRot);
		Vec3 left = Vec3.directionFromRotation(0.0F, koschei.yBodyRot - 90.0F);
		double along = random.nextDouble();
		return koschei.position().add(left.scale(0.6 + along * 0.6)).add(forward.scale(0.2 - along * 0.4))
				.add(0.0, koschei.getBbHeight() * (0.5 + along * 0.3), 0.0);
	}
}
