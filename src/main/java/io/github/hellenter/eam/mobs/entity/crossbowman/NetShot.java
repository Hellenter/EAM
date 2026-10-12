package io.github.hellenter.eam.mobs.entity.crossbowman;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamItems;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** The elite crossbowman's net: it leaves a sticky net under whoever it hits, and tears up on anything else. */
public class NetShot extends ThrowableItemProjectile {
	private static final byte TEAR_EVENT = 3;

	public NetShot(EntityType<? extends NetShot> type, Level level) {
		super(type, level);
	}

	NetShot(Level level, LivingEntity owner) {
		super(EamEntities.NET_SHOT, owner, level, EamItems.NET.getDefaultInstance());
	}

	@Override
	protected Item getDefaultItem() {
		return EamItems.NET;
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		if (hit.getEntity() instanceof LivingEntity living && level() instanceof ServerLevel level) {
			BlockPos feet = living.blockPosition();
			BlockPos pos = NetBlock.canPlaceAt(level, feet) ? feet : feet.above();
			if (NetBlock.place(level, pos)) {
				level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.HOSTILE, 1.0F, 0.8F);
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
