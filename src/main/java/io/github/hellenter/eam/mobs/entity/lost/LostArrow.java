package io.github.hellenter.eam.mobs.entity.lost;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * A longbowman's arrow: it flies through the Lost, so he can shoot past his squad. The arrows of his volley each land
 * and push the target back. Saved, it is a plain arrow again; by then it has landed.
 */
final class LostArrow extends Arrow {
	/** How hard it pushes back, as Punch levels; 0 leaves it to the bow's enchantments. */
	private float knockback;
	/** A volley arrow: it lands even right after another hit, which would otherwise leave the target unhurt for half a second. */
	private boolean volley;

	LostArrow(Level level, LivingEntity owner, ItemStack pickup, @Nullable ItemStack weapon) {
		super(level, owner, pickup, weapon);
	}

	/** One arrow of a quick volley, pushing back this hard. */
	void setVolley(float knockback) {
		this.knockback = knockback;
		this.volley = true;
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		if (volley && hit.getEntity() instanceof LivingEntity living) {
			living.damageCooldownTime = 0;
		}
		super.onHitEntity(hit);
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
}
