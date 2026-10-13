package io.github.hellenter.eam.mobs.entity.yaga;

import io.github.hellenter.eam.content.EamParticles;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The look of Baba Yaga's attacks, each its own: crow feathers trail her dive, grave wisps rise with her creatures,
 * skull-faced fumes boil off her flask, runes fly with her spells, an eye opens over whoever her blinding glyph
 * curses and blood is drawn off whoever the draining one does, acid drips off her acid bolt, thread curls off the
 * threads bolt, embers gather to her fire beam and claw rakes tear the air. Only looks and sounds: nothing here changes
 * what an attack does or when.
 * <p>
 * Sent with count 0 a particle goes exactly where it is sent at exactly the velocity given.
 */
final class YagaFx {
	private static final BlockParticleOption BONE_CHIPS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState());

	private YagaFx() {
	}

	/** Feathers torn off her as she dives. */
	static void diveTrail(ServerLevel level, Entity yaga) {
		level.sendParticles(EamParticles.YAGA_FEATHER, yaga.getX(), yaga.getY(0.6), yaga.getZ(), 2, 0.5, 0.5, 0.5, 0.02);
	}

	/** A burst of feathers as the dive strikes. */
	static void diveStrike(ServerLevel level, Entity yaga) {
		level.sendParticles(EamParticles.YAGA_FEATHER, yaga.getX(), yaga.getY(0.5), yaga.getZ(), 16, 0.4, 0.4, 0.4, 0.18);
		level.playSound(null, yaga.getX(), yaga.getY(), yaga.getZ(), SoundEvents.PARROT_FLY, SoundSource.HOSTILE, 1.5F, 0.6F);
	}

	/** Grave wisps gathering round the staff as she calls her creatures. */
	static void summonCharge(ServerLevel level, Vec3 tip) {
		level.sendParticles(EamParticles.YAGA_WISP, tip.x, tip.y, tip.z, 2, 0.25, 0.25, 0.25, 0.02);
	}

	/** Bone chips thrown up and wisps rising from the ground a creature climbs out of. */
	static void graveRise(ServerLevel level, Vec3 at) {
		level.sendParticles(BONE_CHIPS, at.x, at.y + 0.2, at.z, 10, 0.35, 0.1, 0.35, 0.1);
		level.sendParticles(EamParticles.YAGA_WISP, at.x, at.y + 0.3, at.z, 8, 0.4, 0.2, 0.4, 0.03);
	}

	/** Skull-faced fumes boiling up all over where her flask burst. */
	static void toxicBurst(ServerLevel level, Vec3 at, double radius, RandomSource random) {
		for (int i = 0; i < 14; i++) {
			double angle = random.nextDouble() * Mth.TWO_PI;
			double d = Math.sqrt(random.nextDouble()) * radius;
			level.sendParticles(EamParticles.YAGA_TOXIC, at.x + Math.cos(angle) * d, at.y + 0.3, at.z + Math.sin(angle) * d, 0,
					0.0, 0.03 + random.nextDouble() * 0.03, 0.0, 1.0);
		}
		level.playSound(null, at.x, at.y, at.z, SoundEvents.PUFFER_FISH_BLOW_UP, SoundSource.HOSTILE, 1.5F, 0.5F);
	}

	/** Runes in the spell's colour swirling at the staff as she casts, and along the spell's arc. */
	static void runes(ServerLevel level, Vec3 at, int color, int count, double spread) {
		level.sendParticles(ColorParticleOption.create(EamParticles.YAGA_RUNE, color | 0xFF000000), at.x, at.y, at.z, count, spread, spread,
				spread, 0.01);
	}

	/** A ring of runes rising round the creatures the spell lands on. */
	static void runeRing(ServerLevel level, Vec3 at, int color) {
		ColorParticleOption rune = ColorParticleOption.create(EamParticles.YAGA_RUNE, color | 0xFF000000);
		for (int i = 0; i < 16; i++) {
			double angle = i * Mth.TWO_PI / 16.0;
			level.sendParticles(rune, at.x + Math.cos(angle) * 1.3, at.y - 0.5, at.z + Math.sin(angle) * 1.3, 0, 0.0, 0.09, 0.0, 1.0);
		}
	}

	/** Over a player a glyph curses: an eye opening over the blinded, blood drawn off towards the glyph by the drained. */
	static void curse(ServerLevel level, Entity glyph, LivingEntity player, BabaYagaLogic.Glyph kind) {
		if (kind == BabaYagaLogic.Glyph.BLIND) {
			level.sendParticles(EamParticles.YAGA_EYE, player.getX(), player.getY() + player.getBbHeight() + 0.7, player.getZ(), 0, 0.0, 0.0, 0.0,
					1.0);
			return;
		}
		Vec3 from = player.position().add(0.0, player.getBbHeight() * 0.6, 0.0);
		Vec3 to = glyph.position().subtract(from).scale(0.06);
		for (int i = 0; i < 5; i++) {
			level.sendParticles(EamParticles.YAGA_BLOOD, from.x, from.y, from.z, 0, to.x + (level.getRandom().nextDouble() - 0.5) * 0.1, 0.12,
					to.z + (level.getRandom().nextDouble() - 0.5) * 0.1, 1.0);
		}
	}

	/** Acid dripping off the skull as she readies the acid bolt. */
	static void acidCharge(ServerLevel level, Vec3 tip) {
		level.sendParticles(EamParticles.YAGA_ACID, tip.x, tip.y - 0.1, tip.z, 1, 0.1, 0.05, 0.1, 0.0);
	}

	/** Acid splashing where the bolt lands. */
	static void acidSplash(ServerLevel level, Vec3 at) {
		level.sendParticles(EamParticles.YAGA_ACID, at.x, at.y + 0.1, at.z, 16, 0.2, 0.1, 0.2, 0.25);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.SLIME_SQUISH_SMALL, SoundSource.HOSTILE, 1.2F, 0.6F);
	}

	/** Thread curling round the skull as she readies the threads bolt. */
	static void threadCharge(ServerLevel level, Vec3 tip) {
		level.sendParticles(EamParticles.YAGA_THREAD, tip.x, tip.y, tip.z, 1, 0.2, 0.2, 0.2, 0.01);
	}

	/** Thread winding up round whoever the bolt ties, and where it breaks on a shield. */
	static void threadWind(ServerLevel level, Entity target) {
		for (int i = 0; i < 18; i++) {
			double angle = i * Mth.TWO_PI / 9.0;
			double r = target.getBbWidth() * 0.5 + 0.4;
			double y = target.getY() + i * 0.08;
			level.sendParticles(EamParticles.YAGA_THREAD, target.getX() + Math.cos(angle) * r, y, target.getZ() + Math.sin(angle) * r, 0,
					-Math.sin(angle) * 0.08, 0.03, Math.cos(angle) * 0.08, 1.0);
		}
	}

	/** Embers drawn in to the skull from all round while she aims the fire beam. */
	static void emberCharge(ServerLevel level, Vec3 tip, RandomSource random) {
		for (int i = 0; i < 3; i++) {
			Vec3 off = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).normalize().scale(1.3);
			Vec3 at = tip.add(off);
			Vec3 in = off.scale(-0.075);
			level.sendParticles(EamParticles.YAGA_EMBER, at.x, at.y, at.z, 0, in.x, in.y, in.z, 1.0);
		}
	}

	/** Embers shed along the burning beam. */
	static void emberAlong(ServerLevel level, Vec3 at) {
		level.sendParticles(EamParticles.YAGA_EMBER, at.x, at.y, at.z, 1, 0.08, 0.08, 0.08, 0.04);
	}

	/** Embers sprayed where the beam strikes. */
	static void emberHit(ServerLevel level, Vec3 at) {
		level.sendParticles(EamParticles.YAGA_EMBER, at.x, at.y, at.z, 4, 0.15, 0.15, 0.15, 0.12);
	}

	/** One rake of her claws torn across the air in front of her, and blood if it caught the target. */
	static void clawRake(ServerLevel level, Entity yaga, LivingEntity target, boolean hit) {
		Vec3 mid = yaga.position().lerp(target.position(), 0.65).add(0.0, target.getBbHeight() * 0.6, 0.0);
		level.sendParticles(EamParticles.YAGA_CLAW, mid.x, mid.y, mid.z, 0, 0.0, 0.0, 0.0, 1.0);
		if (hit) {
			level.sendParticles(EamParticles.YAGA_BLOOD, target.getX(), target.getY(0.6), target.getZ(), 5, 0.15, 0.2, 0.15, 0.15);
		}
	}

	/** She vanishes into, and comes out of, a cloud of black ink. */
	static void blink(ServerLevel level, Entity yaga) {
		level.sendParticles(ParticleTypes.SQUID_INK, yaga.getX(), yaga.getY(0.5), yaga.getZ(), 14, 0.25, 0.5, 0.25, 0.05);
	}

	/** Blood spurting from the creature she eats. */
	static void feed(ServerLevel level, Entity meal) {
		level.sendParticles(EamParticles.YAGA_BLOOD, meal.getX(), meal.getY(0.6), meal.getZ(), 6, 0.2, 0.25, 0.2, 0.18);
	}
}
