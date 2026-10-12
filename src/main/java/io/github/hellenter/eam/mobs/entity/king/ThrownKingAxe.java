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
 * An axe thrown by the Pillager King or one of his guards ({@link AxeThrower}). It spins through the air, knocks out
 * a shield it hits (if it is meant to) and flies back to the thrower's hand. Thrown as a boomerang it curves past its
 * target, round behind it and into its back. Nobody can pick it up.
 */
public class ThrownKingAxe extends AbstractArrow {
	private static final EntityDataAccessor<Boolean> DATA_FLYING_BACK = SynchedEntityData.defineId(ThrownKingAxe.class,
			EntityDataSerializers.BOOLEAN);
	/** The boomerang's target, or -1 for a straight throw. */
	private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(ThrownKingAxe.class, EntityDataSerializers.INT);
	/** Which way round the boomerang goes, the leg of its path it is on, and the direction it was thrown in. */
	private static final EntityDataAccessor<Boolean> DATA_CURVE_LEFT = SynchedEntityData.defineId(ThrownKingAxe.class,
			EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Byte> DATA_LEG = SynchedEntityData.defineId(ThrownKingAxe.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Float> DATA_THROW_YAW = SynchedEntityData.defineId(ThrownKingAxe.class,
			EntityDataSerializers.FLOAT);
	/** Boomerang legs: past the target's side, round behind it, into its back. */
	private static final byte LEG_SIDE = 0;
	private static final byte LEG_BEHIND = 1;
	private static final byte LEG_STRIKE = 2;
	/** How far to the side and behind the target the boomerang swings, and how close it must come to each point. */
	private static final double SWING_OUT = 2.8;
	private static final double SWING_BEHIND = 3.0;
	private static final double WAYPOINT_REACH = 1.3;
	/** How quickly the boomerang turns: the share of its new heading taken each tick. */
	private static final double TURN = 0.3;
	/** A boomerang that has not struck by then flies back. */
	private static final int BOOMERANG_TIMEOUT = 70;
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
	private double boomerangSpeed = 1.0;

	public ThrownKingAxe(EntityType<? extends ThrownKingAxe> type, Level level) {
		super(type, level);
		pickup = Pickup.DISALLOWED;
	}

	public ThrownKingAxe(Level level, LivingEntity owner, float damage, float shieldSeconds) {
		this(EamEntities.KING_AXE, level, owner, PillagerKing.axeStack(), damage, shieldSeconds);
	}

	/** An axe of type {@code type} that looks like {@code axe}; with no {@code shieldSeconds} it leaves shields be. */
	public ThrownKingAxe(EntityType<? extends ThrownKingAxe> type, Level level, LivingEntity owner, ItemStack axe, float damage,
			float shieldSeconds) {
		super(type, owner, level, axe, null);
		this.damage = damage;
		this.shieldSeconds = shieldSeconds;
		pickup = Pickup.DISALLOWED;
	}

	/** An axe thrown round {@code target}, curving left or right of it, to strike it in the back. */
	public static ThrownKingAxe boomerang(Level level, LivingEntity owner, LivingEntity target, float damage, double speed, boolean left) {
		ThrownKingAxe axe = new ThrownKingAxe(level, owner, damage, 0.0F);
		axe.boomerangSpeed = speed;
		axe.entityData.set(DATA_TARGET, target.getId());
		axe.entityData.set(DATA_CURVE_LEFT, left);
		axe.entityData.set(DATA_THROW_YAW, (float) Math.atan2(target.getZ() - owner.getZ(), target.getX() - owner.getX()));
		axe.setNoPhysics(true);
		// Off to the side it will curve round, a little up.
		Vec3 ahead = new Vec3(target.getX() - owner.getX(), 0.0, target.getZ() - owner.getZ()).normalize();
		Vec3 side = new Vec3(-ahead.z, 0.0, ahead.x).scale(left ? 1.0 : -1.0);
		axe.setDeltaMovement(ahead.add(side.scale(0.6)).normalize().scale(speed).add(0.0, 0.1, 0.0));
		return axe;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_FLYING_BACK, false);
		builder.define(DATA_TARGET, -1);
		builder.define(DATA_CURVE_LEFT, false);
		builder.define(DATA_LEG, LEG_SIDE);
		builder.define(DATA_THROW_YAW, 0.0F);
	}

	/** Whether it was thrown as a boomerang. */
	public boolean isBoomerang() {
		return entityData.get(DATA_TARGET) >= 0;
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
		if (isBoomerang() && !isFlyingBack()) {
			if (!curve()) {
				return;
			}
		} else if (!level().isClientSide() && (dealtDamage || tickCount > RETURN_AFTER)) {
			entityData.set(DATA_FLYING_BACK, true);
		}
		if (isFlyingBack() && !flyBack()) {
			return;
		}
		super.tick();
	}

	/**
	 * Steers the boomerang along its legs: to a point beside the target, round to a point behind it, then into its
	 * back. It flies through blocks. False once it is gone.
	 */
	private boolean curve() {
		boolean server = !level().isClientSide();
		Entity entity = level().getEntity(entityData.get(DATA_TARGET));
		if (!(entity instanceof LivingEntity target) || !target.isAlive() || tickCount > BOOMERANG_TIMEOUT) {
			if (server) {
				entityData.set(DATA_FLYING_BACK, true);
			}
			return true;
		}
		float yaw = entityData.get(DATA_THROW_YAW);
		Vec3 ahead = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
		Vec3 side = new Vec3(-ahead.z, 0.0, ahead.x).scale(entityData.get(DATA_CURVE_LEFT) ? 1.0 : -1.0);
		Vec3 chest = target.position().add(0.0, target.getBbHeight() * 0.6, 0.0);
		byte leg = entityData.get(DATA_LEG);
		Vec3 aim = switch (leg) {
			case LEG_SIDE -> chest.add(side.scale(SWING_OUT)).add(ahead.scale(0.5));
			case LEG_BEHIND -> chest.add(ahead.scale(SWING_BEHIND));
			default -> chest;
		};
		Vec3 to = aim.subtract(position());
		if (server) {
			boolean reached = target.getBoundingBox().inflate(0.4).contains(position()) || to.length() < target.getBbWidth() * 0.5 + 0.8;
			if (leg == LEG_STRIKE && reached) {
				strikeBack(target);
				entityData.set(DATA_FLYING_BACK, true);
				return true;
			}
			// Past the point, or close to it and heading away: on to the next leg.
			boolean passed = to.length() < WAYPOINT_REACH * 2.5 && to.dot(getDeltaMovement()) < 0.0;
			if (leg != LEG_STRIKE && (to.length() < WAYPOINT_REACH || passed)) {
				entityData.set(DATA_LEG, (byte) (leg + 1));
			}
		}
		setNoPhysics(true);
		// The client does not know the speed it was thrown at: it keeps the speed the server sent.
		double speed = server ? boomerangSpeed : Math.max(0.5, getDeltaMovement().length());
		// Round the target it swings wide; coming in at its back it flies straight at it.
		double turn = leg == LEG_STRIKE ? 1.0 : TURN;
		Vec3 heading = getDeltaMovement().scale(1.0 - turn).add(to.normalize().scale(speed * turn));
		setDeltaMovement(heading.normalize().scale(speed));
		return true;
	}

	/** The boomerang strikes the target from behind: no shield covers it there. */
	private void strikeBack(LivingEntity target) {
		dealtDamage = true;
		if (level() instanceof ServerLevel level) {
			Entity owner = getOwner();
			DamageSource source = damageSources().thrown(this, owner == null ? this : owner);
			target.damageCooldownTime = 0;
			if (target.hurtServer(level, source, damageTo(target))) {
				doKnockback(target, source);
			}
			playSound(SoundEvents.TRIDENT_HIT, 1.0F, 0.7F);
		}
	}

	/** Steers towards the owner; false once the axe is caught or lost. */
	private boolean flyBack() {
		Entity owner = getOwner();
		boolean server = !level().isClientSide();
		if (!(owner instanceof LivingEntity thrower) || !(owner instanceof AxeThrower hand) || !thrower.isAlive()) {
			if (server) {
				discard();
				return false;
			}
			return true;
		}
		Vec3 toHand = thrower.position().add(0.0, thrower.getBbHeight() * 0.6, 0.0).subtract(position());
		if (server && toHand.length() < thrower.getBbWidth() + 1.0) {
			hand.catchAxe();
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
			if (living.hurtServer(level, source, damageTo(living))) {
				doKnockback(living, source);
			}
			if (onShield && shieldSeconds > 0.0F) {
				blocks.disable(level, living, shieldSeconds, shield);
				level.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.SHIELD_BREAK, getSoundSource(), 1.0F, 0.8F);
				if (owner instanceof AxeThrower thrower) {
					thrower.axeBrokeShield(living);
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
		// A boomerang loaded again just flies back: its target is not kept.
		output.putBoolean("FlyingBack", isFlyingBack() || isBoomerang());
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

	/** As much as the thrower says: the king's axe hits anything but a player many times over, even with him unloaded. */
	private float damageTo(Entity target) {
		if (getOwner() instanceof AxeThrower thrower) {
			return thrower.axeDamageTo(damage, target);
		}
		return getType() == EamEntities.KING_AXE
				? PillagerKingLogic.damageTo(damage, target instanceof Player, PillagerKingModule.settings().mobDamageMultiplier())
				: damage;
	}
}
