package io.github.hellenter.eam.content.underworld;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.mobs.entity.shambler.ShamblerSpores;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** A thrown spore sac: flies like a snowball and bursts into a mushroom shambler's spore cloud where it lands. */
public class ThrownSporeSac extends ThrowableItemProjectile {
	public ThrownSporeSac(EntityType<? extends ThrownSporeSac> type, Level level) {
		super(type, level);
	}

	public ThrownSporeSac(Level level, LivingEntity owner, ItemStack itemStack) {
		super(EamEntities.SPORE_SAC, owner, level, itemStack);
	}

	@Override
	protected Item getDefaultItem() {
		return EamItems.SPORE_SAC;
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (level() instanceof ServerLevel serverLevel) {
			ShamblerSpores.spores(serverLevel, hitResult.getLocation(), getOwner());
			discard();
		}
	}
}
