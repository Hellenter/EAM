package io.github.hellenter.eam.mobs.entity.yaga;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamParticles;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A bolt from Baba Yaga's staff, flying straight and fast at where the player stood, so a quick step aside dodges it.
 * The acid bolt goes through a shield, burns through armor and leaves a burning puddle; the purple bolt ties the player
 * it hits to the ground with threads (a shield turned towards it stops it). Only a trail of sparks is drawn.
 */
public class HagBolt extends Projectile {
	public enum Kind { ACID, THREADS }

	private static final EntityDataAccessor<Byte> DATA_KIND = SynchedEntityData.defineId(HagBolt.class, EntityDataSerializers.BYTE);
	private static final int LIFETIME = 60;

	public HagBolt(EntityType<? extends HagBolt> type, Level level) {
		super(type, level);
		noPhysics = true;
	}

	public HagBolt(Level level, LivingEntity owner, Kind kind, Vec3 from, Vec3 velocity) {
		this(EamEntities.HAG_BOLT, level);
		setOwner(owner);
		entityData.set(DATA_KIND, (byte) kind.ordinal());
		snapTo(from.x, from.y, from.z, owner.getYRot(), 0.0F);
		setDeltaMovement(velocity);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_KIND, (byte) 0);
	}

	public Kind kind() {
		return entityData.get(DATA_KIND) == 1 ? Kind.THREADS : Kind.ACID;
	}

	public int color() {
		return kind() == Kind.ACID ? YagaGlow.ACID.color : YagaGlow.THREADS.color;
	}

	@Override
	public SoundSource getSoundSource() {
		return SoundSource.HOSTILE;
	}

	@Override
	public void checkDespawn() {
		if (level().getDifficulty() == Difficulty.PEACEFUL) {
			discard();
		}
	}

	@Override
	public void tick() {
		super.tick();
		boolean server = !level().isClientSide();
		if (server && tickCount > LIFETIME) {
			discard();
			return;
		}
		HitResult hit = server ? ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity) : null;
		Vec3 movement = getDeltaMovement();
		setPos(position().add(movement));
		if (hit != null && isAlive() && hit.getType() != HitResult.Type.MISS) {
			hitTargetOrDeflectSelf(hit);
		}
		ProjectileUtil.rotateTowardsMovement(this, 0.5F);
		if (!server) {
			DustParticleOptions trail = new DustParticleOptions(color(), 1.1F);
			for (int i = 0; i < 4; i++) {
				double t = i / 4.0;
				level().addParticle(trail, getX() - movement.x * t, getY() - movement.y * t, getZ() - movement.z * t, 0.0, 0.0, 0.0);
			}
			// Acid drips off the acid bolt; thread curls trail off the threads bolt.
			if (kind() == Kind.ACID && random.nextInt(2) == 0) {
				level().addParticle(EamParticles.YAGA_ACID, getX(), getY(), getZ(), movement.x * 0.1, 0.0, movement.z * 0.1);
			} else if (kind() == Kind.THREADS) {
				level().addParticle(EamParticles.YAGA_THREAD, getX(), getY(), getZ(), (random.nextDouble() - 0.5) * 0.05, 0.0,
						(random.nextDouble() - 0.5) * 0.05);
			}
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && !(entity instanceof BabaYaga) && !(entity instanceof HagThread) && !BabaYagaModule.isMinion(entity);
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		if (!(level() instanceof ServerLevel level) || !(hitResult.getEntity() instanceof LivingEntity target)) {
			return;
		}
		BabaYagaSettings settings = BabaYagaModule.settings();
		if (kind() == Kind.ACID) {
			DamageSource source = damageSources().source(BabaYagaModule.CURSE_DAMAGE, this, getOwner());
			target.hurtServer(level, source, settings.acid().damage());
			puddle(level, target.position());
			return;
		}
		Vec3 v = getDeltaMovement();
		if (target.isBlocking() && FallenLordLogic.shieldCovers(target.getYHeadRot(), -v.x, -v.z)) {
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.HOSTILE, 1.0F, 1.2F);
			return;
		}
		if (target instanceof Player || target.isAlive()) {
			HagThread.tie(level, getOwner(), target, settings.threads());
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult hitResult) {
		super.onHitBlock(hitResult);
		if (level() instanceof ServerLevel level && kind() == Kind.ACID) {
			puddle(level, hitResult.getLocation());
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (level() instanceof ServerLevel level) {
			level.sendParticles(new DustParticleOptions(color(), 1.4F), getX(), getY(), getZ(), 14, 0.25, 0.25, 0.25, 0.0);
			if (kind() == Kind.ACID) {
				YagaFx.acidSplash(level, position());
			} else {
				level.sendParticles(EamParticles.YAGA_THREAD, getX(), getY(), getZ(), 8, 0.2, 0.2, 0.2, 0.08);
			}
			level.playSound(null, getX(), getY(), getZ(), kind() == Kind.ACID ? SoundEvents.LAVA_EXTINGUISH : SoundEvents.AMETHYST_BLOCK_BREAK,
					SoundSource.HOSTILE, 0.8F, 1.4F);
		}
		discard();
	}

	/** A burning puddle on the ground at {@code at}. */
	private void puddle(ServerLevel level, Vec3 at) {
		BabaYagaSettings.Acid acid = BabaYagaModule.settings().acid();
		HagGlyph pool = HagGlyph.create(level, getOwner(), BabaYagaLogic.Glyph.ACID, acid.poolRadius(), acid.poolTicks());
		pool.setPos(at.x, Math.floor(at.y + 0.01), at.z);
		level.addFreshEntity(pool);
	}

	@Override
	public boolean isOnFire() {
		return false;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 16384.0;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putByte("Kind", entityData.get(DATA_KIND));
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(DATA_KIND, input.getByteOr("Kind", (byte) 0));
	}
}
