package io.github.hellenter.eam.mobs.entity.lost;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamParticles;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * A longbowman's arrow: it flies through the Lost, so he can shoot past his squad. The arrows of his volley each land
 * and push the target back. A black marking arrow marks whoever it hits; his red power arrow goes through a raised
 * shield, which takes most of the hit and is knocked out for a while.
 */
public class LostArrow extends AbstractArrow {
	/** What kind of arrow it is; the client draws and trails it by this. */
	public enum Kind {
		PLAIN, MARK, RED;

		private static Kind of(int id) {
			return id >= 0 && id < values().length ? values()[id] : PLAIN;
		}
	}

	private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(LostArrow.class, EntityDataSerializers.INT);

	/** How hard it pushes back, as Punch levels; 0 leaves it to the bow's enchantments. */
	private float knockback;
	/** A volley arrow: it lands even right after another hit, which would otherwise leave the target unhurt for half a second. */
	private boolean volley;
	/** Ticks of the mark a marking arrow leaves. */
	private int markTicks;
	/** A red arrow's damage, whatever its speed by then. */
	private float redDamage;
	private float shieldMultiplier;
	private float shieldSeconds;

	public LostArrow(EntityType<? extends LostArrow> type, Level level) {
		super(type, level);
	}

	LostArrow(Level level, LivingEntity owner, @Nullable ItemStack weapon) {
		super(EamEntities.LOST_ARROW, owner, level, new ItemStack(Items.ARROW), weapon);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_KIND, Kind.PLAIN.ordinal());
	}

	public Kind kind() {
		return Kind.of(entityData.get(DATA_KIND));
	}

	/** One arrow of a quick volley, pushing back this hard. */
	void setVolley(float knockback) {
		this.knockback = knockback;
		this.volley = true;
	}

	/** A black arrow that marks whoever it hits for this many ticks. */
	void setMarking(int ticks) {
		markTicks = ticks;
		entityData.set(DATA_KIND, Kind.MARK.ordinal());
	}

	/** A red power arrow: this much damage, of which a raised shield lets {@code shieldMultiplier} through and goes down. */
	void setRed(float damage, float shieldMultiplier, float shieldSeconds) {
		this.redDamage = damage;
		this.shieldMultiplier = shieldMultiplier;
		this.shieldSeconds = shieldSeconds;
		pickup = Pickup.DISALLOWED;
		entityData.set(DATA_KIND, Kind.RED.ordinal());
	}

	@Override
	protected ItemStack getDefaultPickupItem() {
		return new ItemStack(Items.ARROW);
	}

	@Override
	public void tick() {
		super.tick();
		Kind kind = kind();
		if (kind != Kind.PLAIN && level().isClientSide() && !isInGround()) {
			Vec3 motion = getDeltaMovement();
			int steps = kind == Kind.RED ? 4 : 2;
			for (int i = 0; i < steps; i++) {
				double f = (double) i / steps;
				level().addParticle(kind == Kind.RED ? EamParticles.RED_ARROW_TRAIL : EamParticles.MARK_TRAIL, getX() - motion.x * f, getY() - motion.y * f,
						getZ() - motion.z * f, 0.0, 0.0, 0.0);
			}
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		if (kind() == Kind.RED) {
			hitRed(hit.getEntity());
			return;
		}
		if (volley && hit.getEntity() instanceof LivingEntity living) {
			living.damageCooldownTime = 0;
		}
		super.onHitEntity(hit);
	}

	/** As the elite crossbowman's red bolt, and the shield it went through is knocked out. */
	private void hitRed(Entity entity) {
		Entity owner = getOwner();
		DamageSource source = damageSources().source(LostModule.RED_ARROW_DAMAGE, this, owner != null ? owner : this);
		Vec3 motion = getDeltaMovement();
		ItemStack shield = entity instanceof LivingEntity living && living.isBlocking()
				&& FallenLordLogic.shieldCovers(living.getYHeadRot(), -motion.x, -motion.z) ? living.getItemBlockingWith() : null;
		float dealt = shield != null ? redDamage * shieldMultiplier : redDamage;
		if (shield != null && level() instanceof ServerLevel level && entity instanceof LivingEntity living) {
			BlocksAttacks blocks = shield.get(DataComponents.BLOCKS_ATTACKS);
			if (blocks != null && shieldSeconds > 0.0F) {
				blocks.disable(level, living, shieldSeconds, shield);
			}
			playSound(SoundEvents.SHIELD_BREAK.value(), 1.0F, 0.8F);
		}
		if (owner instanceof LivingEntity livingOwner) {
			livingOwner.setLastHurtMob(entity);
		}
		if (level() instanceof ServerLevel level) {
			level.sendParticles(EamParticles.RED_ARROW_TRAIL, getX(), getY(), getZ(), 12, 0.15, 0.15, 0.15, 0.12);
		}
		if (entity.hurtOrSimulate(source, dealt) && entity instanceof LivingEntity living) {
			doKnockback(living, source);
			doPostHurtEffects(living);
		}
		playSound(SoundEvents.ARROW_HIT, 1.0F, 0.7F);
		discard();
	}

	@Override
	protected void doPostHurtEffects(LivingEntity target) {
		super.doPostHurtEffects(target);
		if (kind() == Kind.MARK && markTicks > 0) {
			target.addEffect(new MobEffectInstance(LostMark.MARKED, markTicks), getEffectSource());
			// A puff of soot where the mark lands.
			if (level() instanceof ServerLevel level) {
				level.sendParticles(EamParticles.MARK_TRAIL, target.getX(), target.getY(0.6), target.getZ(), 10, 0.25, 0.3, 0.25, 0.02);
			}
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return !LostCommander.isKin(entity) && super.canHitEntity(entity);
	}

	/** As vanilla pushes for Punch, with this arrow's own strength. */
	@Override
	protected void doKnockback(LivingEntity mob, DamageSource damageSource) {
		if (knockback <= 0.0F) {
			super.doKnockback(mob, damageSource);
			return;
		}
		double resistance = Math.max(0.0, 1.0 - mob.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
		Vec3 push = getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize().scale(knockback * 0.6 * resistance);
		if (push.lengthSqr() > 0.0) {
			mob.push(push.x, 0.1, push.z);
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("lost_kind", kind().ordinal());
		output.putInt("mark_ticks", markTicks);
		output.putFloat("red_damage", redDamage);
		output.putFloat("shield_multiplier", shieldMultiplier);
		output.putFloat("shield_seconds", shieldSeconds);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(DATA_KIND, Kind.of(input.getIntOr("lost_kind", 0)).ordinal());
		markTicks = input.getIntOr("mark_ticks", 0);
		redDamage = input.getFloatOr("red_damage", 0.0F);
		shieldMultiplier = input.getFloatOr("shield_multiplier", 0.0F);
		shieldSeconds = input.getFloatOr("shield_seconds", 0.0F);
	}
}
