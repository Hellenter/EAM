package io.github.hellenter.eam.mobs.entity.fallenlord;

import io.github.hellenter.eam.content.EamEntities;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * The Fallen Lord's thrown spear, or his sword thrown in phase two. Nobody can pick it up. In phase one it flies back to his hand like a loyal trident;
 * the spear thrown when phase two starts stays where it lands and vanishes after a while.
 */
public class ThrownLordSpear extends AbstractArrow {
	/** Flying back to the owner; synced so clients steer it the same way between position updates. */
	private static final EntityDataAccessor<Boolean> DATA_FLYING_BACK = SynchedEntityData.defineId(ThrownLordSpear.class,
			EntityDataSerializers.BOOLEAN);
	/** His sword, thrown in phase two at a player out of reach, instead of the spear; clients draw it. */
	private static final EntityDataAccessor<Boolean> DATA_SWORD = SynchedEntityData.defineId(ThrownLordSpear.class,
			EntityDataSerializers.BOOLEAN);
	/** A spear that hit nothing starts back after this many ticks, before it gets far. */
	private static final int RETURN_AFTER = 8;
	/** A spear left lying vanishes after this many ticks in the ground. */
	private static final int LIE_TICKS = 200;
	/** On the way back it flies straight at his hand, this fast at first, faster every tick up to the most. */
	private static final double RETURN_START_SPEED = 1.2;
	private static final double RETURN_ACCELERATION = 0.25;
	private static final double RETURN_MAX_SPEED = 3.5;

	private float damage = 7.0F;
	private boolean returning = true;
	private boolean dealtDamage;
	private boolean playedReturnSound;
	private double returnSpeed = RETURN_START_SPEED;

	public ThrownLordSpear(EntityType<? extends ThrownLordSpear> type, Level level) {
		super(type, level);
		pickup = Pickup.DISALLOWED;
	}

	public ThrownLordSpear(Level level, LivingEntity owner, float damage, boolean returning, boolean sword) {
		super(EamEntities.LORD_SPEAR, owner, level, FallenLord.spearStack(), null);
		this.damage = damage;
		this.returning = returning;
		pickup = Pickup.DISALLOWED;
		entityData.set(DATA_SWORD, sword);
	}

	/** Whether this is his sword rather than his spear. */
	public boolean isSword() {
		return entityData.get(DATA_SWORD);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_FLYING_BACK, false);
		builder.define(DATA_SWORD, false);
	}

	@Override
	public void tick() {
		if (isInGround() && inGroundTime > 1) {
			dealtDamage = true;
		}
		if (!level().isClientSide()) {
			if (returning && (dealtDamage || tickCount > RETURN_AFTER)) {
				entityData.set(DATA_FLYING_BACK, true);
			} else if (!returning && inGroundTime > LIE_TICKS) {
				discard();
				return;
			}
		}
		if (entityData.get(DATA_FLYING_BACK) && !flyBack()) {
			return;
		}
		super.tick();
	}

	/** Steers towards the owner; false once the spear is caught or lost. */
	private boolean flyBack() {
		Entity owner = getOwner();
		boolean server = !level().isClientSide();
		if (!(owner instanceof FallenLord lord) || !lord.isAlive()) {
			if (server) {
				discard();
				return false;
			}
			return true;
		}
		Vec3 toHand = lord.getEyePosition().subtract(position());
		if (server && toHand.length() < lord.getBbWidth() + 1.0) {
			lord.catchSpear(this);
			discard();
			return false;
		}
		setNoPhysics(true);
		// Straight back without the trident's slow turn, so a miss does not leave him unarmed for long.
		setDeltaMovement(toHand.normalize().scale(Math.min(returnSpeed, toHand.length())));
		returnSpeed = Math.min(RETURN_MAX_SPEED, returnSpeed + RETURN_ACCELERATION);
		if (server && !playedReturnSound) {
			playSound(SoundEvents.TRIDENT_RETURN, 10.0F, 0.8F);
			playedReturnSound = true;
		}
		return true;
	}

	@Override
	protected @Nullable EntityHitResult findHitEntity(Vec3 from, Vec3 to) {
		return dealtDamage ? null : super.findHitEntity(from, to);
	}

	@Override
	protected Collection<EntityHitResult> findHitEntities(Vec3 from, Vec3 to) {
		EntityHitResult hit = findHitEntity(from, to);
		return hit != null ? List.of(hit) : List.of();
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		// He never spears himself on the way back.
		return entity != getOwner() && super.canHitEntity(entity);
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		Entity target = hitResult.getEntity();
		Entity owner = getOwner();
		DamageSource source = damageSources().trident(this, owner == null ? this : owner);
		dealtDamage = true;
		if (level() instanceof ServerLevel && target.hurtOrSimulate(source, damage) && target instanceof LivingEntity living) {
			doKnockback(living, source);
		}
		playSound(SoundEvents.TRIDENT_HIT, 1.0F, 0.8F);
		setDeltaMovement(getDeltaMovement().multiply(-0.01, -0.1, -0.01));
	}

	@Override
	public void playerTouch(Player player) {
		// Never picked up.
	}

	@Override
	protected boolean tryPickup(Player player) {
		return false;
	}

	@Override
	protected ItemStack getDefaultPickupItem() {
		return FallenLord.spearStack();
	}

	@Override
	protected SoundEvent getDefaultHitGroundSoundEvent() {
		return SoundEvents.TRIDENT_HIT_GROUND;
	}

	@Override
	public boolean shouldRender(double camX, double camY, double camZ) {
		return true;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putFloat("SpearDamage", damage);
		output.putBoolean("Returning", returning);
		output.putBoolean("DealtDamage", dealtDamage);
		output.putBoolean("FlyingBack", entityData.get(DATA_FLYING_BACK));
		output.putBoolean("Sword", isSword());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		damage = input.getFloatOr("SpearDamage", damage);
		returning = input.getBooleanOr("Returning", true);
		entityData.set(DATA_FLYING_BACK, input.getBooleanOr("FlyingBack", false));
		dealtDamage = input.getBooleanOr("DealtDamage", false);
		entityData.set(DATA_SWORD, input.getBooleanOr("Sword", false));
		pickup = Pickup.DISALLOWED;
	}
}
