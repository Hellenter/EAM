package io.github.hellenter.eam.mobs.entity.warlock;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.mobs.entity.bat.VampireBat;
import io.github.hellenter.eam.mobs.entity.lost.Lost;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The undead warlock's magic missile: a red spark that leaves his hand at an angle and curves after its target. It
 * strikes through armor and shields, as magic does, but a wall stops it and a blow knocks it out of the air.
 */
public class MagicMissile extends Projectile {
	/** The entity it flies after; synced so clients curve it the same way between position updates. */
	private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(MagicMissile.class, EntityDataSerializers.INT);
	public static final int COLOR = 0xE8202A;
	private static final DustParticleOptions TRAIL = new DustParticleOptions(COLOR, 1.1F);
	private static final DustParticleOptions SPARK = new DustParticleOptions(0xFF8A70, 0.6F);

	private float damage = UndeadWarlockSettings.Missiles.DEFAULT.damage();
	private double speed = UndeadWarlockSettings.Missiles.DEFAULT.speed();
	private double turn = UndeadWarlockSettings.Missiles.DEFAULT.turn();
	private int lifetime = UndeadWarlockSettings.Missiles.DEFAULT.lifetime();

	public MagicMissile(EntityType<? extends MagicMissile> type, Level level) {
		super(type, level);
		noPhysics = true;
	}

	public MagicMissile(Level level, LivingEntity owner, Entity target, Vec3 from, Vec3 velocity, UndeadWarlockSettings.Missiles settings) {
		this(EamEntities.MAGIC_MISSILE, level);
		setOwner(owner);
		snapTo(from.x, from.y, from.z, owner.getYRot(), 0.0F);
		setDeltaMovement(velocity);
		entityData.set(DATA_TARGET, target.getId());
		damage = settings.damage();
		speed = settings.speed();
		turn = settings.turn();
		lifetime = settings.lifetime();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_TARGET, -1);
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
		if (server && tickCount > lifetime) {
			fizzle();
			return;
		}
		Entity target = level().getEntity(entityData.get(DATA_TARGET));
		if (target != null && target.isAlive() && !(target instanceof Player player && player.isSpectator())) {
			Vec3 to = target.getBoundingBox().getCenter().subtract(position());
			Vec3 v = getDeltaMovement();
			double[] next = UndeadWarlockLogic.steer(v.x, v.y, v.z, to.x, to.y, to.z, speed, turn);
			setDeltaMovement(next[0], next[1], next[2]);
		}
		HitResult hit = server ? ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity) : null;
		Vec3 movement = getDeltaMovement();
		setPos(position().add(movement));
		applyEffectsFromBlocks();
		if (hit != null && isAlive() && hit.getType() != HitResult.Type.MISS) {
			hitTargetOrDeflectSelf(hit);
		}
		ProjectileUtil.rotateTowardsMovement(this, 0.5F);
		if (!server) {
			// A thick red trail with a few bright sparks, so the missile reads as a streak of light.
			for (int i = 0; i < 3; i++) {
				double t = i / 3.0;
				level().addParticle(TRAIL, getX() - movement.x * t, getY() - movement.y * t + 0.1, getZ() - movement.z * t, 0.0, 0.0, 0.0);
			}
			if (random.nextInt(2) == 0) {
				level().addParticle(SPARK, getX(), getY() + 0.1, getZ(), 0.0, 0.0, 0.0);
			}
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		// The dead do not hurt their own.
		return super.canHitEntity(entity) && !(entity instanceof UndeadWarlock) && !(entity instanceof Lost) && !(entity instanceof VampireBat)
				&& !(entity instanceof MagicMissile);
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		Entity owner = getOwner();
		DamageSource source = damageSources().indirectMagic(this, owner);
		hitResult.getEntity().hurtOrSimulate(source, damage);
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		fizzle();
	}

	/** A puff of red sparks, and it is gone. */
	private void fizzle() {
		if (level() instanceof ServerLevel level) {
			level.sendParticles(TRAIL, getX(), getY() + 0.1, getZ(), 8, 0.15, 0.15, 0.15, 0.0);
			level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 0.1, getZ(), 4, 0.1, 0.1, 0.1, 0.1);
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 0.6F, 1.6F + random.nextFloat() * 0.3F);
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
	public boolean isPickable() {
		return true;
	}

	@Override
	public boolean hurtClient(DamageSource source) {
		return true;
	}

	/** A blow knocks it out of the air. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		fizzle();
		return true;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putFloat("Damage", damage);
		output.putDouble("Speed", speed);
		output.putDouble("Turn", turn);
		output.putInt("Lifetime", lifetime);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		damage = input.getFloatOr("Damage", damage);
		speed = input.getDoubleOr("Speed", speed);
		turn = input.getDoubleOr("Turn", turn);
		lifetime = input.getIntOr("Lifetime", lifetime);
	}
}
