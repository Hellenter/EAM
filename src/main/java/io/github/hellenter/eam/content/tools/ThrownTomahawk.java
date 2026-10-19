package io.github.hellenter.eam.content.tools;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamItems;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * A thrown tomahawk: it spins end over end, hits once and drops or sticks where it lands. A player's tomahawk is his to
 * pick up again; one a duergar rider threw is a spare from his belt, nobody's to take, and soon lost in the dark.
 */
public class ThrownTomahawk extends AbstractArrow {
	/** A rider's tomahawk lies this long on the ground before it is gone. */
	private static final int MOB_TOMAHAWK_LIFE = 100;

	private float damage = TomahawkItem.THROWN_DAMAGE;
	private boolean dealtDamage;

	public ThrownTomahawk(EntityType<? extends ThrownTomahawk> type, Level level) {
		super(type, level);
	}

	/** Thrown by {@code owner}; a player gets it back, anyone else's stays where it falls. */
	public ThrownTomahawk(Level level, LivingEntity owner, ItemStack tomahawk, float damage) {
		super(EamEntities.TOMAHAWK, owner, level, tomahawk, null);
		this.damage = damage;
	}

	/** Whether it is stuck in a block or lying on one; it stops spinning there. */
	public boolean isStuck() {
		return isInGround();
	}

	@Override
	public void tick() {
		if (inGroundTime > 4) {
			dealtDamage = true;
		}
		if (!level().isClientSide() && pickup != Pickup.ALLOWED && inGroundTime > MOB_TOMAHAWK_LIFE) {
			discard();
			return;
		}
		super.tick();
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
		return entity != getOwner() && !(getOwner() != null && entity == getOwner().getVehicle()) && super.canHitEntity(entity);
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		Entity target = hitResult.getEntity();
		Entity owner = getOwner();
		DamageSource source = damageSources().thrown(this, owner == null ? this : owner);
		float amount = damage;
		if (level() instanceof ServerLevel level) {
			amount = EnchantmentHelper.modifyDamage(level, getWeaponItem(), target, source, amount);
		}
		dealtDamage = true;
		boolean hurt = target.hurtOrSimulate(source, amount);
		if (hurt && level() instanceof ServerLevel level) {
			EnchantmentHelper.doPostAttackEffectsWithItemSourceOnBreak(level, target, source, getWeaponItem(), weapon -> kill(level));
		}
		if (hurt && target instanceof LivingEntity living) {
			doKnockback(living, source);
			doPostHurtEffects(living);
		}
		if (target.projectileReceivesSideEffectsOnHit(hurt)) {
			playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.3F);
			setDeltaMovement(getDeltaMovement().multiply(-0.01, -0.1, -0.01));
		}
	}

	@Override
	public ItemStack getWeaponItem() {
		return getPickupItemStackOrigin();
	}

	@Override
	protected ItemStack getDefaultPickupItem() {
		return new ItemStack(EamItems.TOMAHAWK);
	}

	@Override
	protected SoundEvent getDefaultHitGroundSoundEvent() {
		return SoundEvents.TRIDENT_HIT_GROUND;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putFloat("TomahawkDamage", damage);
		output.putBoolean("DealtDamage", dealtDamage);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		damage = input.getFloatOr("TomahawkDamage", damage);
		dealtDamage = input.getBooleanOr("DealtDamage", false);
	}
}
