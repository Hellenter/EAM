package io.github.hellenter.eam.content.slingshot;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.ground.ThrownPebble;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/** Drawn like a bow; shoots a pebble from the inventory. Damage depends on draw power and flight distance. */
public class SlingshotItem extends ProjectileWeaponItem {
	private static final Predicate<ItemStack> PEBBLE_ONLY = itemStack -> itemStack.is(EamItems.PEBBLE);

	public SlingshotItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack itemStack = player.getItemInHand(hand);
		if (!player.hasInfiniteMaterials() && player.getProjectile(itemStack).isEmpty()) {
			return InteractionResult.FAIL;
		}
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public boolean releaseUsing(ItemStack itemStack, Level level, LivingEntity entity, int remainingTime) {
		if (!(entity instanceof Player player)) {
			return false;
		}
		ItemStack pebble = player.getProjectile(itemStack);
		if (pebble.isEmpty()) {
			return false;
		}
		float power = SlingshotLogic.powerForTime(getUseDuration(itemStack, entity) - remainingTime);
		if (power < SlingshotLogic.MIN_POWER) {
			return false;
		}
		List<ItemStack> drawn = draw(itemStack, pebble, player);
		if (level instanceof ServerLevel serverLevel && !drawn.isEmpty()) {
			// The speed carries the draw power to shootProjectile, which hands it to the pebble.
			shoot(serverLevel, player, player.getUsedItemHand(), itemStack, drawn, power * SlingshotLogic.FULL_SPEED, 1.0F, power == 1.0F, null);
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS,
				1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
		player.awardStat(Stats.ITEM_USED.get(this));
		return true;
	}

	@Override
	protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack projectile, boolean isCrit) {
		// In creative with no pebbles the player hands us an arrow; it is still a pebble that flies.
		ItemStack pebble = projectile.is(EamItems.PEBBLE) ? projectile : new ItemStack(EamItems.PEBBLE);
		return new ThrownPebble(level, shooter, pebble);
	}

	@Override
	protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float power, float uncertainty, float angle,
			@Nullable LivingEntity target) {
		projectile.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot() + angle, 0.0F, power, uncertainty);
		if (projectile instanceof ThrownPebble pebble) {
			pebble.fromSlingshot(power / SlingshotLogic.FULL_SPEED);
		}
	}

	@Override
	public int getUseDuration(ItemStack itemStack, LivingEntity user) {
		return 72000;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack itemStack) {
		return ItemUseAnimation.BOW;
	}

	@Override
	public Predicate<ItemStack> getAllSupportedProjectiles() {
		return PEBBLE_ONLY;
	}

	@Override
	public int getDefaultProjectileRange() {
		return 15;
	}
}
