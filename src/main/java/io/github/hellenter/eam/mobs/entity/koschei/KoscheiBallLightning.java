package io.github.hellenter.eam.mobs.entity.koschei;

import io.github.hellenter.eam.mobs.entity.draugr.Draugr;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * Koschei's ball lightning: a crackling ball of blue light that drifts after his target, darting off aside now and then,
 * and burns whoever is in its field a little. At first nothing touches it; after a while it starts to flicker, and then
 * one blow or arrow bursts it, burning everyone close by. It fades out on its own when its time is up or he is gone. Drawn
 * only with particles. Never saved.
 */
public class KoscheiBallLightning extends Monster {
	private static final EntityDataAccessor<Boolean> DATA_UNSTABLE = SynchedEntityData.defineId(KoscheiBallLightning.class,
			EntityDataSerializers.BOOLEAN);
	private static final DustParticleOptions BLUE = new DustParticleOptions(0x3F7CFF, 1.0F);
	private static final DustParticleOptions PALE = new DustParticleOptions(0xD8ECFF, 1.0F);
	private static final DustParticleOptions VIOLET = new DustParticleOptions(0xB070FF, 1.0F);
	/** The field burns in pulses this many ticks apart. */
	private static final int PULSE = 10;
	/** It eases toward the speed it wants by this share each tick. */
	private static final double EASE = 0.15;
	/** It floats this high above the target's feet. */
	private static final double HOVER = 1.1;
	private static final int DART_TICKS = 8;

	private @Nullable Koschei owner;
	private PhaseTwoSettings.Ball settings = PhaseTwoSettings.Ball.DEFAULT;
	private int dartTicks;
	private Vec3 dartTo = Vec3.ZERO;
	private boolean burst;

	public KoscheiBallLightning(EntityType<? extends KoscheiBallLightning> type, Level level) {
		super(type, level);
		xpReward = 0;
		setNoGravity(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 1.0)
				.add(Attributes.FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_UNSTABLE, false);
	}

	/** Sets it on {@code target} for {@code owner}. */
	void summon(Koschei owner, LivingEntity target, PhaseTwoSettings.Ball settings) {
		this.owner = owner;
		this.settings = settings;
		setTarget(target);
	}

	/** Whether it flickers: a hit bursts it now. */
	public boolean unstable() {
		return entityData.get(DATA_UNSTABLE);
	}

	@Override
	public void tick() {
		noPhysics = true;
		super.tick();
		noPhysics = false;
		setNoGravity(true);
		if (level().isClientSide()) {
			sparkle();
		}
	}

	private void sparkle() {
		boolean unstable = unstable();
		boolean flicker = unstable && (tickCount / 2) % 2 == 0;
		Vec3 centre = position().add(0.0, getBbHeight() * 0.5, 0.0);
		for (int i = 0; i < 2; i++) {
			Vec3 at = centre.add((random.nextDouble() - 0.5) * 0.35, (random.nextDouble() - 0.5) * 0.35, (random.nextDouble() - 0.5) * 0.35);
			level().addParticle(flicker ? VIOLET : BLUE, at.x, at.y, at.z, 0.0, 0.0, 0.0);
		}
		level().addParticle(flicker ? BLUE : PALE, centre.x, centre.y, centre.z, 0.0, 0.0, 0.0);
		level().addParticle(ParticleTypes.ELECTRIC_SPARK, centre.x, centre.y, centre.z, (random.nextDouble() - 0.5) * 0.4,
				(random.nextDouble() - 0.5) * 0.4, (random.nextDouble() - 0.5) * 0.4);
		// The rim of its field, crackling here and there.
		if (random.nextInt(2) == 0) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double radius = settings.radius();
			level().addParticle(ParticleTypes.ELECTRIC_SPARK, centre.x + Math.cos(angle) * radius, centre.y - HOVER + 0.2 + random.nextDouble() * 1.6,
					centre.z + Math.sin(angle) * radius, 0.0, 0.0, 0.0);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		LivingEntity target = getTarget();
		if (owner == null || !owner.isAlive() || tickCount > settings.lifetime() || target == null || !target.isAlive()
				|| target instanceof Player player && (player.isCreative() || player.isSpectator())) {
			fade(level);
			return;
		}
		if (!unstable() && tickCount >= settings.unstableAfter()) {
			entityData.set(DATA_UNSTABLE, true);
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.2F, 1.8F);
		}
		Vec3 aim;
		if (dartTicks > 0) {
			dartTicks--;
			aim = dartTo;
		} else {
			aim = target.position().add(0.0, HOVER - getBbHeight() * 0.5, 0.0);
			if (random.nextFloat() < settings.dartChance()) {
				// Off a couple of blocks aside, then after the target again.
				double angle = random.nextDouble() * Math.PI * 2.0;
				dartTo = position().add(Math.cos(angle) * settings.dartDistance(), 0.0, Math.sin(angle) * settings.dartDistance());
				dartTicks = DART_TICKS;
				playSound(SoundEvents.TRIDENT_THUNDER.value(), 0.2F, 2.0F);
			}
		}
		double want = dartTicks > 0 ? settings.dartDistance() / DART_TICKS : settings.speed();
		Vec3 to = aim.subtract(position());
		double length = to.length();
		Vec3 desired = length < 1.0E-4 ? Vec3.ZERO : to.scale(Math.min(want, length) / length);
		double ease = dartTicks > 0 ? 0.5 : EASE;
		setDeltaMovement(getDeltaMovement().add(desired.subtract(getDeltaMovement()).scale(ease)));
		if (tickCount % PULSE == 0) {
			burn(level, settings.radius(), settings.damagePerSecond() * PULSE / 20.0F);
		}
		if (tickCount % 15 == 0) {
			playSound(SoundEvents.BEE_LOOP_AGGRESSIVE, 0.4F, 1.8F);
		}
	}

	/** Pure damage to everyone not on his side within {@code radius} of it. */
	private void burn(ServerLevel level, double radius, float damage) {
		Vec3 centre = position().add(0.0, getBbHeight() * 0.5, 0.0);
		DamageSource source = damageSources().source(KoscheiModule.SORCERY_DAMAGE, this, owner);
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius + 1.0),
				e -> e != this && e.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(e) && !spared(e))) {
			Vec3 closest = new Vec3(Mth.clamp(centre.x, victim.getBoundingBox().minX, victim.getBoundingBox().maxX),
					Mth.clamp(centre.y, victim.getBoundingBox().minY, victim.getBoundingBox().maxY),
					Mth.clamp(centre.z, victim.getBoundingBox().minZ, victim.getBoundingBox().maxZ));
			if (closest.distanceTo(centre) > radius) {
				continue;
			}
			victim.hurtServer(level, source, damage);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, victim.getX(), victim.getY(0.5), victim.getZ(), 5, 0.2, 0.4, 0.2, 0.1);
		}
	}

	/** Koschei, his draugr and his other sorcery. */
	private boolean spared(Entity entity) {
		return entity instanceof Koschei || entity instanceof KoscheiBallLightning || entity instanceof KoscheiIceRaven
				|| owner != null && entity instanceof Draugr draugr && draugr.servedBy(owner);
	}

	/** Until it flickers nothing touches it; then a blow or an arrow bursts it. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, damage);
		}
		if (!unstable() || source.getEntity() != null && spared(source.getEntity())) {
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(0.5), getZ(), 6, 0.2, 0.2, 0.2, 0.2);
			return false;
		}
		return super.hurtServer(level, source, damage);
	}

	/** Struck down: it bursts. */
	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level && !burst) {
			burst = true;
			Vec3 centre = position().add(0.0, getBbHeight() * 0.5, 0.0);
			level.sendParticles(ParticleTypes.EXPLOSION, centre.x, centre.y, centre.z, 1, 0.0, 0.0, 0.0, 0.0);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, centre.x, centre.y, centre.z, 60, 0.6, 0.6, 0.6, 0.6);
			level.sendParticles(BLUE, centre.x, centre.y, centre.z, 40, 1.0, 1.0, 1.0, 0.0);
			level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 2.0F, 1.2F);
			level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8F, 1.6F);
			burn(level, settings.burstRadius(), settings.burstDamage());
		}
	}

	/** Its time is up or he is gone: it fizzles out without harm. */
	private void fade(ServerLevel level) {
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(0.5), getZ(), 16, 0.3, 0.3, 0.3, 0.1);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 0.8F, 1.5F);
		discard();
	}

	@Override
	protected void tickDeath() {
		remove(RemovalReason.KILLED);
	}

	@Override
	public void travel(Vec3 input) {
		travelFlying(input, 0.0F);
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
	}

	@Override
	protected boolean shouldDropLoot(ServerLevel level) {
		return false;
	}

	@Override
	public boolean isOnFire() {
		return false;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return null;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.AMETHYST_BLOCK_HIT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return null;
	}
}
