package io.github.hellenter.eam.content.slingshot;

import io.github.hellenter.eam.content.EamItems;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * A slingshot with a metal fork: pebbles fly faster and hit harder the better the metal, and a splash or lingering
 * potion held in the other hand is shot instead of a pebble. Potions in the bag are never taken, so pebbles don't
 * waste them.
 */
public class PowerSlingshotItem extends SlingshotItem {
	private static final Predicate<ItemStack> PEBBLE_OR_POTION = itemStack -> itemStack.is(EamItems.PEBBLE) || isPotion(itemStack);

	private final SlingshotLogic.PowerTier tier;

	public PowerSlingshotItem(SlingshotLogic.PowerTier tier, Properties properties) {
		super(properties);
		this.tier = tier;
	}

	private static boolean isPotion(ItemStack itemStack) {
		return itemStack.is(Items.SPLASH_POTION) || itemStack.is(Items.LINGERING_POTION);
	}

	@Override
	public Predicate<ItemStack> getSupportedHeldProjectiles() {
		return PEBBLE_OR_POTION;
	}

	@Override
	protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack projectile, boolean isCrit) {
		if (projectile.is(Items.SPLASH_POTION)) {
			return new ThrownSplashPotion(level, shooter, projectile);
		}
		if (projectile.is(Items.LINGERING_POTION)) {
			return new ThrownLingeringPotion(level, shooter, projectile);
		}
		return super.createProjectile(level, shooter, weapon, projectile, isCrit);
	}

	@Override
	protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float power, float uncertainty, float angle,
			@Nullable LivingEntity target) {
		if (projectile instanceof AbstractThrownPotion) {
			float speed = power / fullSpeed() * tier.potionSpeed();
			projectile.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot() + angle, 0.0F, speed, uncertainty);
			return;
		}
		super.shootProjectile(shooter, projectile, index, power, uncertainty, angle, target);
	}

	@Override
	protected float fullSpeed() {
		return tier.pebbleSpeed();
	}

	@Override
	protected float damageScale() {
		return tier.damageScale();
	}

	@Override
	protected SoundEvent shotSound(ItemStack ammo) {
		return isPotion(ammo) ? SoundEvents.SPLASH_POTION_THROW : super.shotSound(ammo);
	}

	@Override
	public int getDefaultProjectileRange() {
		return 20;
	}
}
