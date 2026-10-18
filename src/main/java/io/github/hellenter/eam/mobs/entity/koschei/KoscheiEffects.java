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
 * of force and sparks crackling at the sword's point before the bolt; in his second phase blue sparks at the staff's head
 * before its bolt and green souls drawn to his hands before he throws them. The glow of the blade and the eyes is the
 * renderer's.
 */
final class KoscheiEffects {
	private static final DustParticleOptions RED = new DustParticleOptions(0xE8231C, 0.9F);
	private static final DustParticleOptions BLUE = new DustParticleOptions(0x4C8CFF, 1.0F);
	private static final DustParticleOptions GREEN = new DustParticleOptions(0x5CFF7A, 1.0F);

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
			case SUMMON -> {
				if (t < KoscheiAction.SUMMON_RELEASE) {
					// Dark motes drawn in to his raised hand.
					Vec3 hand = koschei.raisedHand();
					for (int i = 0; i < 2; i++) {
						Vec3 from = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).scale(1.6);
						level.addParticle(EamParticles.KOSCHEI_WARP, hand.x + from.x, hand.y + from.y, hand.z + from.z, -from.x * 0.12,
								-from.y * 0.12, -from.z * 0.12);
					}
				}
			}
			case STAFF_BOLT -> {
				if (t < KoscheiAction.STAFF_BOLT_RELEASE) {
					// Blue sparks gathering at the staff's head.
					Vec3 head = koschei.position().add(Koschei.staffHeadOffset(koschei.yBodyRot, koschei.getBbHeight()));
					Vec3 from = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).scale(0.9);
					level.addParticle(BLUE, head.x + from.x, head.y + from.y, head.z + from.z, -from.x * 0.15, -from.y * 0.15, -from.z * 0.15);
					level.addParticle(ParticleTypes.ELECTRIC_SPARK, head.x, head.y, head.z, 0.0, 0.0, 0.0);
				}
			}
			case SOULS -> {
				if (t < KoscheiAction.SOULS_RELEASE) {
					// Green souls drawn in to his raised hands.
					Vec3 hand = koschei.raisedHand();
					Vec3 from = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).scale(1.8);
					level.addParticle(GREEN, hand.x + from.x, hand.y + from.y, hand.z + from.z, -from.x * 0.12, -from.y * 0.12, -from.z * 0.12);
					if (random.nextInt(3) == 0) {
						level.addParticle(ParticleTypes.SCULK_SOUL, hand.x, hand.y, hand.z, 0.0, 0.03, 0.0);
					}
				}
			}
			case SNOWSTORM -> {
				if (t < KoscheiAction.STORM_START) {
					// Frost gathering about his hands.
					Vec3 hands = koschei.position().add(Vec3.directionFromRotation(0.0F, koschei.yBodyRot).scale(0.9))
							.add(0.0, koschei.getBbHeight() * 0.6, 0.0);
					Vec3 from = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).scale(1.4);
					level.addParticle(ParticleTypes.SNOWFLAKE, hands.x + from.x, hands.y + from.y, hands.z + from.z, -from.x * 0.1,
							-from.y * 0.1, -from.z * 0.1);
				}
			}
			case ICE_RAVENS -> {
				if (t < KoscheiAction.ICE_RAVENS_RELEASE) {
					Vec3 hand = koschei.raisedHand();
					Vec3 from = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).scale(1.4);
					level.addParticle(ParticleTypes.SNOWFLAKE, hand.x + from.x, hand.y + from.y, hand.z + from.z, -from.x * 0.12,
							-from.y * 0.12, -from.z * 0.12);
				}
			}
			case BALL -> {
				if (t < KoscheiAction.BALL_RELEASE) {
					// Lightning crawling over the crystal at the top of the raised staff.
					Vec3 head = koschei.raisedStaffHead();
					level.addParticle(ParticleTypes.ELECTRIC_SPARK, head.x + (random.nextDouble() - 0.5) * 0.6, head.y + (random.nextDouble() - 0.5) * 0.6,
							head.z + (random.nextDouble() - 0.5) * 0.6, 0.0, 0.0, 0.0);
					level.addParticle(BLUE, head.x, head.y, head.z, 0.0, 0.0, 0.0);
				}
			}
			case TURTLE -> {
				if (t < KoscheiAction.TURTLE_RELEASE) {
					// Green souls seeping up from the ground before him.
					Vec3 ahead = koschei.position().add(Vec3.directionFromRotation(0.0F, koschei.yBodyRot).scale(2.5));
					level.addParticle(GREEN, ahead.x + (random.nextDouble() - 0.5) * 3.0, ahead.y + 0.1, ahead.z + (random.nextDouble() - 0.5) * 3.0,
							0.0, 0.06, 0.0);
				}
			}
			case ICE_COPIES -> {
				if (t < KoscheiAction.ICE_COPIES_RELEASE) {
					// Frost whirling about him.
					double a = random.nextDouble() * Math.PI * 2.0;
					level.addParticle(ParticleTypes.SNOWFLAKE, koschei.getX() + Math.cos(a) * 1.6, koschei.getY() + random.nextDouble() * koschei.getBbHeight(),
							koschei.getZ() + Math.sin(a) * 1.6, -Math.sin(a) * 0.15, 0.0, Math.cos(a) * 0.15);
				}
			}
			case DRAIN -> {
				// Green motes about the reaching hand.
				Vec3 hand = koschei.rightHand();
				level.addParticle(GREEN, hand.x + (random.nextDouble() - 0.5) * 0.4, hand.y + (random.nextDouble() - 0.5) * 0.4,
						hand.z + (random.nextDouble() - 0.5) * 0.4, 0.0, 0.0, 0.0);
			}
			case TRANSFORM -> {
				if (t < KoscheiAction.TRANSFORM_STAFF && random.nextInt(2) == 0) {
					level.addParticle(ParticleTypes.SOUL, koschei.getRandomX(1.2), koschei.getY() + random.nextDouble() * koschei.getBbHeight(),
							koschei.getRandomZ(1.2), 0.0, 0.03, 0.0);
				}
			}
			case TRAP, TELEPORT -> {
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
