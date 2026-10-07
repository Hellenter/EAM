package io.github.hellenter.eam.content.ground;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamItems;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** A thrown pebble: flies like a snowball, hurts for half a heart and falls to the ground as an item. */
public class ThrownPebble extends ThrowableItemProjectile {
	/** Half a heart. */
	public static final float DAMAGE = 1.0F;

	private static final byte HIT_EVENT = 3;

	public ThrownPebble(EntityType<? extends ThrownPebble> type, Level level) {
		super(type, level);
	}

	public ThrownPebble(Level level, LivingEntity owner, ItemStack itemStack) {
		super(EamEntities.PEBBLE, owner, level, itemStack);
	}

	@Override
	protected Item getDefaultItem() {
		return EamItems.PEBBLE;
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == HIT_EVENT) {
			ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(getItem()));
			for (int i = 0; i < 8; i++) {
				level().addParticle(particle, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
			}
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		if (level() instanceof ServerLevel serverLevel) {
			hitResult.getEntity().hurtServer(serverLevel, damageSources().thrown(this, getOwner()), DAMAGE);
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (level() instanceof ServerLevel serverLevel) {
			serverLevel.broadcastEntityEvent(this, HIT_EVENT);
			if (!(getOwner() instanceof Player player && player.hasInfiniteMaterials())) {
				spawnAtLocation(serverLevel, getItem());
			}
			discard();
		}
	}
}
