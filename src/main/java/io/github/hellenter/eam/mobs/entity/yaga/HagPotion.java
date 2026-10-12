package io.github.hellenter.eam.mobs.entity.yaga;

import io.github.hellenter.eam.content.EamEntities;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.Optional;

/**
 * Baba Yaga's poison flask: it bursts wider than a splash potion and leaves whoever it reaches poisoned, sick and weak.
 * Her own creatures are spared.
 */
public class HagPotion extends ThrowableItemProjectile {
	/** The murky green of her brew. */
	public static final int COLOR = 0x4E7A1E;

	public HagPotion(EntityType<? extends HagPotion> type, Level level) {
		super(type, level);
	}

	public HagPotion(Level level, LivingEntity owner) {
		super(EamEntities.HAG_POTION, owner, level, flask());
	}

	/** A splash potion bottle with her brew's colour, for the look of the flask in flight. */
	private static ItemStack flask() {
		ItemStack stack = new ItemStack(Items.SPLASH_POTION);
		stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.of(COLOR), List.of(), Optional.empty()));
		return stack;
	}

	@Override
	protected Item getDefaultItem() {
		return Items.SPLASH_POTION;
	}

	@Override
	protected double getDefaultGravity() {
		return 0.05;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && !(entity instanceof BabaYaga) && !BabaYagaModule.isMinion(entity);
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		BabaYagaSettings.Potion potion = BabaYagaModule.settings().potion();
		double radius = potion.radius();
		AABB box = getBoundingBox().inflate(radius, radius * 0.5, radius);
		Entity owner = getOwner();
		for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e.isAlive() && e.isAffectedByPotions() && !(e instanceof BabaYaga) && !BabaYagaModule.isMinion(e))) {
			double distance = Math.sqrt(distanceToSqr(living));
			if (distance > radius) {
				continue;
			}
			if (potion.poisonTicks() > 0) {
				living.addEffect(new MobEffectInstance(MobEffects.POISON, potion.poisonTicks(), 0), owner);
			}
			if (potion.nauseaTicks() > 0) {
				living.addEffect(new MobEffectInstance(MobEffects.NAUSEA, potion.nauseaTicks(), potion.nauseaLevel()), owner);
			}
			if (potion.weaknessTicks() > 0) {
				living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, potion.weaknessTicks(), potion.weaknessLevel()), owner);
			}
		}
		level.levelEvent(LevelEvent.PARTICLES_SPELL_POTION_SPLASH, blockPosition(), COLOR);
		// The burst is wider than a potion's: a ring of green mist shows how far it reached.
		DustParticleOptions mist = new DustParticleOptions(COLOR, 1.5F);
		for (int i = 0; i < 32; i++) {
			double angle = i * Math.PI * 2.0 / 32.0;
			level.sendParticles(mist, getX() + Math.cos(angle) * radius, getY() + 0.3, getZ() + Math.sin(angle) * radius, 1, 0.1, 0.2, 0.1, 0.0);
		}
		level.levelEvent(LevelEvent.SOUND_SPELL_POTION_SPLASH, blockPosition(), 0);
		discard();
	}
}
