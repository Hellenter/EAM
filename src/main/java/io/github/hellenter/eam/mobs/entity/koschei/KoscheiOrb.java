package io.github.hellenter.eam.mobs.entity.koschei;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.mobs.entity.draugr.Draugr;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
 * What Koschei throws from his staff in his second phase, drawn only as a trail of motes. The blue bolt flies straight and
 * fast at where the target stood and burns it with pure damage, unless a shield turned toward it takes it. A green soul
 * is thrown out in an arc over the arena and raises one of his draugr where it comes down.
 */
public class KoscheiOrb extends Projectile {
	public enum Kind { BOLT, SOUL }

	private static final EntityDataAccessor<Byte> DATA_KIND = SynchedEntityData.defineId(KoscheiOrb.class, EntityDataSerializers.BYTE);
	private static final DustParticleOptions BLUE = new DustParticleOptions(0x4C8CFF, 1.3F);
	private static final DustParticleOptions PALE_BLUE = new DustParticleOptions(0xBFE0FF, 0.8F);
	private static final DustParticleOptions GREEN = new DustParticleOptions(0x5CFF7A, 1.3F);
	private static final DustParticleOptions PALE_GREEN = new DustParticleOptions(0xC8FFD0, 0.8F);
	/** A soul falls this fast, blocks per tick per tick. */
	public static final double SOUL_GRAVITY = 0.04;
	private static final int LIFETIME = 80;

	public KoscheiOrb(EntityType<? extends KoscheiOrb> type, Level level) {
		super(type, level);
		noPhysics = true;
	}

	public KoscheiOrb(Level level, LivingEntity owner, Kind kind, Vec3 from, Vec3 velocity) {
		this(EamEntities.KOSCHEI_ORB, level);
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
		return entityData.get(DATA_KIND) == 1 ? Kind.SOUL : Kind.BOLT;
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
			if (kind() == Kind.SOUL && level() instanceof ServerLevel level) {
				land(level, position());
			}
			discard();
			return;
		}
		if (kind() == Kind.SOUL) {
			setDeltaMovement(getDeltaMovement().add(0.0, -SOUL_GRAVITY, 0.0));
		}
		HitResult hit = server ? ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity) : null;
		Vec3 movement = getDeltaMovement();
		setPos(position().add(movement));
		if (hit != null && isAlive() && hit.getType() != HitResult.Type.MISS) {
			hitTargetOrDeflectSelf(hit);
		}
		ProjectileUtil.rotateTowardsMovement(this, 0.5F);
		if (!server) {
			trail(movement);
		}
	}

	private void trail(Vec3 movement) {
		boolean soul = kind() == Kind.SOUL;
		for (int i = 0; i < 4; i++) {
			double t = i / 4.0;
			level().addParticle(soul ? GREEN : BLUE, getX() - movement.x * t, getY() - movement.y * t, getZ() - movement.z * t, 0.0, 0.0, 0.0);
		}
		level().addParticle(soul ? PALE_GREEN : PALE_BLUE, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
		if (soul && random.nextInt(2) == 0) {
			level().addParticle(ParticleTypes.SCULK_SOUL, getX(), getY(), getZ(), 0.0, 0.02, 0.0);
		} else if (!soul && random.nextInt(3) == 0) {
			level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), (random.nextDouble() - 0.5) * 0.1, 0.0,
					(random.nextDouble() - 0.5) * 0.1);
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		// A soul flies over everyone's heads to where it comes down; the bolt passes through his own.
		return kind() == Kind.BOLT && super.canHitEntity(entity) && !(entity instanceof Koschei) && !(entity instanceof KoscheiOrb)
				&& !(entity instanceof Draugr draugr && draugr.isThrall());
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		if (!(level() instanceof ServerLevel level) || !(hitResult.getEntity() instanceof LivingEntity target)) {
			return;
		}
		Vec3 v = getDeltaMovement();
		if (target.isBlocking() && FallenLordLogic.shieldCovers(target.getYHeadRot(), -v.x, -v.z)) {
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.HOSTILE, 1.0F, 1.3F);
			return;
		}
		target.hurtServer(level, damageSources().source(KoscheiModule.SORCERY_DAMAGE, this, getOwner()),
				KoscheiModule.settings().phaseTwo().bolt().damage());
	}

	@Override
	protected void onHitBlock(BlockHitResult hitResult) {
		super.onHitBlock(hitResult);
		if (level() instanceof ServerLevel level && kind() == Kind.SOUL) {
			land(level, hitResult.getLocation());
		}
	}

	/** The soul comes down: one of his draugr rises there. */
	private void land(ServerLevel level, Vec3 at) {
		if (getOwner() instanceof Koschei koschei && koschei.isAlive()) {
			koschei.raiseThrall(level, at);
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (level() instanceof ServerLevel level) {
			boolean soul = kind() == Kind.SOUL;
			level.sendParticles(soul ? GREEN : BLUE, getX(), getY(), getZ(), 16, 0.25, 0.25, 0.25, 0.0);
			level.sendParticles(soul ? ParticleTypes.SCULK_SOUL : ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 8, 0.2, 0.2, 0.2, 0.05);
			level.playSound(null, getX(), getY(), getZ(), soul ? SoundEvents.SOUL_ESCAPE.value() : SoundEvents.AMETHYST_BLOCK_BREAK,
					SoundSource.HOSTILE, soul ? 1.5F : 0.8F, soul ? 0.7F : 1.5F);
		}
		discard();
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
