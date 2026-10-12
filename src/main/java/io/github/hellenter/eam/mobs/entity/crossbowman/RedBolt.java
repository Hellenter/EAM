package io.github.hellenter.eam.mobs.entity.crossbowman;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * The elite crossbowman's charged shot: a glowing red bolt. It goes through a raised shield, which still takes part of
 * the damage; it cannot be picked up.
 */
public class RedBolt extends AbstractArrow {
	private static final DustParticleOptions TRAIL = new DustParticleOptions(0xFF2A10, 0.9F);

	/** Damage of a hit, whatever the speed by then. */
	private float damage = EliteCrossbowmanSettings.DEFAULT.normal().damage() * EliteCrossbowmanSettings.DEFAULT.charged().damageMultiplier();
	/** Share of the damage that goes through a shield. */
	private float shieldMultiplier = EliteCrossbowmanSettings.DEFAULT.charged().shieldMultiplier();

	public RedBolt(EntityType<? extends RedBolt> type, Level level) {
		super(type, level);
	}

	RedBolt(Level level, LivingEntity owner, @Nullable ItemStack weapon, float damage, float shieldMultiplier) {
		super(EamEntities.RED_BOLT, owner, level, new ItemStack(Items.ARROW), weapon);
		this.damage = damage;
		this.shieldMultiplier = shieldMultiplier;
		pickup = Pickup.DISALLOWED;
		setSoundEvent(SoundEvents.CROSSBOW_HIT);
	}

	@Override
	protected ItemStack getDefaultPickupItem() {
		return new ItemStack(Items.ARROW);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide() && !isInGround()) {
			Vec3 motion = getDeltaMovement();
			for (int i = 0; i < 3; i++) {
				double f = i / 3.0;
				level().addParticle(TRAIL, getX() - motion.x * f, getY() - motion.y * f, getZ() - motion.z * f, 0.0, 0.0, 0.0);
			}
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		Entity entity = hit.getEntity();
		Entity owner = getOwner();
		DamageSource source = damageSources().source(EliteCrossbowmanModule.RED_BOLT_DAMAGE, this, owner != null ? owner : this);
		Vec3 motion = getDeltaMovement();
		boolean shielded = entity instanceof LivingEntity living && living.isBlocking()
				&& FallenLordLogic.shieldCovers(living.getYHeadRot(), -motion.x, -motion.z);
		float dealt = EliteCrossbowmanLogic.redBoltDamage(damage, shielded, shieldMultiplier);
		if (shielded) {
			playSound(SoundEvents.SHIELD_BLOCK.value(), 1.0F, 0.6F);
		}
		if (owner instanceof LivingEntity livingOwner) {
			livingOwner.setLastHurtMob(entity);
		}
		if (entity.hurtOrSimulate(source, dealt) && entity instanceof LivingEntity living) {
			doKnockback(living, source);
			doPostHurtEffects(living);
		}
		playSound(SoundEvents.CROSSBOW_HIT, 1.0F, 0.8F);
		discard();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putFloat("red_damage", damage);
		output.putFloat("shield_multiplier", shieldMultiplier);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		damage = input.getFloatOr("red_damage", damage);
		shieldMultiplier = input.getFloatOr("shield_multiplier", shieldMultiplier);
	}
}
