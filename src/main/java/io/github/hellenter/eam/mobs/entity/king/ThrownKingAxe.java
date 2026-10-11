package io.github.hellenter.eam.mobs.entity.king;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic;

import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * The Pillager King's thrown axe. It spins through the air, knocks out a shield it hits and flies back to his hand.
 * Nobody can pick it up.
 */
public class ThrownKingAxe extends AbstractArrow {
	private static final EntityDataAccessor<Boolean> DATA_FLYING_BACK = SynchedEntityData.defineId(ThrownKingAxe.class,
			EntityDataSerializers.BOOLEAN);
	/** An axe that hit nothing starts back after this many ticks. */
	private static final int RETURN_AFTER = 14;
	private static final double RETURN_START_SPEED = 1.0;
	private static final double RETURN_ACCELERATION = 0.25;
	private static final double RETURN_MAX_SPEED = 3.0;

	private float damage = 5.0F;
	private float shieldSeconds = 5.0F;
	private boolean dealtDamage;
	private boolean playedReturnSound;
	private double returnSpeed = RETURN_START_SPEED;

	public ThrownKingAxe(EntityType<? extends ThrownKingAxe> type, Level level) {
		super(type, level);
		pickup = Pickup.DISALLOWED;
	}

	public ThrownKingAxe(Level level, LivingEntity owner, float damage, float shieldSeconds) {
		super(EamEntities.KING_AXE, owner, level, PillagerKing.axeStack(), null);
		this.damage = damage;
		this.shieldSeconds = shieldSeconds;
		pickup = Pickup.DISALLOWED;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_FLYING_BACK, false);
	}

	/** Whether it is on its way back to his hand. */
	public boolean isFlyingBack() {
		return entityData.get(DATA_FLYING_BACK);
	}

	/** Whether it is stuck in a block; it stops spinning there. */
	public boolean isStuck() {
		return isInGround();
	}

	@Override
	public void tick() {
		if (isInGround() && inGroundTime > 1) {
			dealtDamage = true;
		}
		if (!level().isClientSide() && (dealtDamage || tickCount > RETURN_AFTER)) {
			entityData.set(DATA_FLYING_BACK, true);
		}
		if (isFlyingBack() && !flyBack()) {
			return;
		}
		super.tick();
	}

	/** Steers towards the owner; false once the axe is caught or lost. */
	private boolean flyBack() {
		Entity owner = getOwner();
		boolean server = !level().isClientSide();
		if (!(owner instanceof PillagerKing king) || !king.isAlive()) {
			if (server) {
				discard();
				return false;
			}
			return true;
		}
		Vec3 toHand = king.position().add(0.0, king.getBbHeight() * 0.6, 0.0).subtract(position());
		if (server && toHand.length() < king.getBbWidth() + 1.0) {
			king.catchAxe();
			discard();
			return false;
		}
		setNoPhysics(true);
		setDeltaMovement(toHand.normalize().scale(Math.min(returnSpeed, toHand.length())));
		returnSpeed = Math.min(RETURN_MAX_SPEED, returnSpeed + RETURN_ACCELERATION);
		if (server && !playedReturnSound) {
			playSound(SoundEvents.TRIDENT_RETURN, 4.0F, 0.6F);
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
		return entity != getOwner() && super.canHitEntity(entity);
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		Entity target = hitResult.getEntity();
		Entity owner = getOwner();
		DamageSource source = damageSources().thrown(this, owner == null ? this : owner);
		dealtDamage = true;
		if (level() instanceof ServerLevel level && target instanceof LivingEntity living) {
			// A shield that takes the axe is knocked out of the guard, whatever vanilla thinks of thrown things.
			ItemStack shield = living.getItemBlockingWith();
			BlocksAttacks blocks = shield != null ? shield.get(DataComponents.BLOCKS_ATTACKS) : null;
			Vec3 motion = getDeltaMovement();
			boolean onShield = blocks != null && FallenLordLogic.shieldCovers(living.getYHeadRot(), -motion.x, -motion.z);
			if (living.hurtServer(level, source, damage)) {
				doKnockback(living, source);
			}
			if (onShield) {
				blocks.disable(level, living, shieldSeconds, shield);
				level.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.SHIELD_BREAK, getSoundSource(), 1.0F, 0.8F);
				if (owner instanceof PillagerKing king) {
					king.axeBrokeShield(living);
				}
			}
		}
		playSound(SoundEvents.TRIDENT_HIT, 1.0F, 0.7F);
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
		return PillagerKing.axeStack();
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
		output.putFloat("AxeDamage", damage);
		output.putFloat("ShieldSeconds", shieldSeconds);
		output.putBoolean("DealtDamage", dealtDamage);
		output.putBoolean("FlyingBack", isFlyingBack());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		damage = input.getFloatOr("AxeDamage", damage);
		shieldSeconds = input.getFloatOr("ShieldSeconds", shieldSeconds);
		dealtDamage = input.getBooleanOr("DealtDamage", false);
		entityData.set(DATA_FLYING_BACK, input.getBooleanOr("FlyingBack", false));
		pickup = Pickup.DISALLOWED;
	}
}
