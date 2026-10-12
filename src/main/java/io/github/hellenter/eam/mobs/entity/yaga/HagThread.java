package io.github.hellenter.eam.mobs.entity.yaga;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamEntities;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

/**
 * One of the purple threads Baba Yaga's bolt pulls out of the ground: it holds the player it grew round by the legs, so
 * they can neither walk nor jump while any thread holding them is left. A thread breaks from one weak blow. They wither
 * by themselves after a while and are never saved.
 */
public class HagThread extends Mob {
	private static final EntityDataAccessor<Integer> DATA_HELD = SynchedEntityData.defineId(HagThread.class, EntityDataSerializers.INT);
	private static final Identifier ROOT_SPEED = Eam.id("baba_yaga_threads_speed");
	private static final Identifier ROOT_JUMP = Eam.id("baba_yaga_threads_jump");
	/** A thread whose player got further than this (pushed, or teleported) lets go. */
	private static final double MAX_STRETCH = 3.0;
	private static final DustParticleOptions SHRED = new DustParticleOptions(YagaGlow.THREADS.color, 1.0F);

	/** Server: what it holds, kept so it can be let go even after it changed dimension. */
	private @Nullable LivingEntity heldEntity;
	private int maxTicks = BabaYagaSettings.Threads.DEFAULT.maxTicks();

	public HagThread(EntityType<? extends HagThread> type, Level level) {
		super(type, level);
		setNoGravity(true);
		setNoAi(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, BabaYagaSettings.Threads.DEFAULT.health())
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	/** Ties {@code target} down with threads round its feet, and holds it at once. */
	public static void tie(ServerLevel level, @Nullable Entity owner, LivingEntity target, BabaYagaSettings.Threads settings) {
		for (int i = 0; i < settings.count(); i++) {
			double angle = Math.PI * 2.0 * i / settings.count() + target.getYRot() * Math.PI / 180.0;
			double reach = target.getBbWidth() * 0.5 + 0.55;
			HagThread thread = new HagThread(EamEntities.HAG_THREAD, level);
			thread.snapTo(target.getX() + Math.cos(angle) * reach, target.getY(), target.getZ() + Math.sin(angle) * reach, 0.0F, 0.0F);
			thread.entityData.set(DATA_HELD, target.getId());
			thread.heldEntity = target;
			thread.maxTicks = settings.maxTicks();
			AttributeInstance health = thread.getAttribute(Attributes.MAX_HEALTH);
			if (health != null) {
				health.setBaseValue(settings.health());
			}
			thread.setHealth(settings.health());
			level.addFreshEntity(thread);
			level.sendParticles(SHRED, thread.getX(), thread.getY() + 0.2, thread.getZ(), 8, 0.1, 0.2, 0.1, 0.0);
		}
		hold(target);
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SCULK_VEIN_PLACE, SoundSource.HOSTILE, 1.5F, 0.6F);
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.COBWEB_PLACE, SoundSource.HOSTILE, 1.0F, 0.6F);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_HELD, -1);
	}

	/** The entity this thread holds, if it is still there. */
	public @Nullable LivingEntity held() {
		if (heldEntity != null) {
			return heldEntity;
		}
		return level().getEntity(entityData.get(DATA_HELD)) instanceof LivingEntity living ? living : null;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		LivingEntity held = held();
		if (held == null || !held.isAlive() || held.isRemoved() || held.level() != level || held.distanceTo(this) > MAX_STRETCH
				|| tickCount > maxTicks) {
			snap(level);
			return;
		}
		hold(held);
	}

	/** Stops the held entity in its tracks: no walking, no jumping. */
	private static void hold(LivingEntity target) {
		modifier(target, Attributes.MOVEMENT_SPEED, ROOT_SPEED, true);
		modifier(target, Attributes.JUMP_STRENGTH, ROOT_JUMP, true);
	}

	/** Lets the held entity go once the last thread on it is gone. */
	private static void release(LivingEntity target) {
		modifier(target, Attributes.MOVEMENT_SPEED, ROOT_SPEED, false);
		modifier(target, Attributes.JUMP_STRENGTH, ROOT_JUMP, false);
	}

	private static void modifier(LivingEntity target, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
			Identifier id, boolean on) {
		AttributeInstance instance = target.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		if (on) {
			if (!instance.hasModifier(id)) {
				instance.addTransientModifier(new AttributeModifier(id, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			}
		} else if (instance.hasModifier(id)) {
			instance.removeModifier(id);
		}
	}

	/** Whether {@code target} is held by any thread right now. */
	public static boolean isTied(LivingEntity target) {
		AttributeInstance instance = target.getAttribute(Attributes.MOVEMENT_SPEED);
		return instance != null && instance.hasModifier(ROOT_SPEED);
	}

	/** Gone, by a blow or by itself: a puff of purple, and the player is free if no other thread holds them. */
	private void snap(ServerLevel level) {
		level.sendParticles(SHRED, getX(), getY() + 0.5, getZ(), 10, 0.1, 0.4, 0.1, 0.0);
		discard();
	}

	@Override
	public void remove(Entity.RemovalReason reason) {
		LivingEntity held = held();
		super.remove(reason);
		if (held != null && level() instanceof ServerLevel level && level.getEntitiesOfClass(HagThread.class, held.getBoundingBox().inflate(MAX_STRETCH + 1.0),
				thread -> thread != this && thread.isAlive() && thread.held() == held).isEmpty()) {
			release(held);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt && !isAlive()) {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.WOOL_BREAK, SoundSource.HOSTILE, 1.0F, 0.7F);
			snap(level);
		}
		return hurt;
	}

	// ---- A plain prop: no AI, no pushing, no drops ----

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
	}

	@Override
	public void knockback(double power, double xd, double zd, DamageSource source, float damage) {
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	protected boolean shouldDropLoot(ServerLevel level) {
		return false;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.WOOL_HIT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return null;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return null;
	}
}
