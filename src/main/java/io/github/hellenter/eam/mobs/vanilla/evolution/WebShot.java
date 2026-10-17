package io.github.hellenter.eam.mobs.vanilla.evolution;

import io.github.hellenter.eam.content.EamEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** A ball of web an evolved spider spits: it wraps whoever it hits in a web, and tears up on anything else. */
public class WebShot extends ThrowableItemProjectile {
	private static final byte TEAR_EVENT = 3;

	public WebShot(EntityType<? extends WebShot> type, Level level) {
		super(type, level);
	}

	WebShot(Level level, LivingEntity owner) {
		super(EamEntities.WEB_SHOT, owner, level, Items.COBWEB.getDefaultInstance());
	}

	@Override
	protected Item getDefaultItem() {
		return Items.COBWEB;
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		if (hit.getEntity() instanceof LivingEntity living && level() instanceof ServerLevel level) {
			BlockPos feet = living.blockPosition();
			boolean placed = SpiderWebBlock.place(level, feet);
			placed |= SpiderWebBlock.place(level, feet.above());
			if (placed) {
				level.playSound(null, feet, SoundEvents.WOOL_PLACE, SoundSource.HOSTILE, 1.0F, 1.2F);
			}
		}
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (!level().isClientSide()) {
			level().broadcastEntityEvent(this, TEAR_EVENT);
			discard();
		}
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == TEAR_EVENT) {
			ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(getItem()));
			for (int i = 0; i < 8; i++) {
				level().addParticle(particle, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
			}
		} else {
			super.handleEntityEvent(id);
		}
	}
}
