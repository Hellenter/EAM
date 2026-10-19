package io.github.hellenter.eam.content.longbow;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A longbow: drawn twice as long as a bow, its arrows fly twice as fast and hurt two and a half times as much
 * ({@link LongbowLogic}). Otherwise a bow: the same arrows, enchantments and pose.
 */
public class LongbowItem extends BowItem {
	public LongbowItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean releaseUsing(ItemStack itemStack, Level level, LivingEntity entity, int remainingTime) {
		if (!(entity instanceof Player player)) {
			return false;
		}
		ItemStack projectile = player.getProjectile(itemStack);
		if (projectile.isEmpty()) {
			return false;
		}
		float power = LongbowLogic.powerForTime(getUseDuration(itemStack, entity) - remainingTime);
		if (power < 0.1F) {
			return false;
		}
		List<ItemStack> drawn = draw(itemStack, projectile, player);
		if (level instanceof ServerLevel serverLevel && !drawn.isEmpty()) {
			shoot(serverLevel, player, player.getUsedItemHand(), itemStack, drawn, LongbowLogic.speed(power), 1.0F, power == 1.0F, null);
		}
		// Lower and heavier than a bow's twang.
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F,
				1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.25F);
		player.awardStat(Stats.ITEM_USED.get(this));
		return true;
	}

	@Override
	protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack projectile, boolean isCrit) {
		Projectile arrow = super.createProjectile(level, shooter, weapon, projectile, isCrit);
		if (arrow instanceof AbstractArrow abstractArrow) {
			abstractArrow.setBaseDamage(LongbowLogic.baseDamage(LongbowLogic.ARROW_BASE_DAMAGE, LongbowLogic.DAMAGE_MULTIPLIER,
					LongbowLogic.SPEED_MULTIPLIER));
		}
		return arrow;
	}

	@Override
	public int getDefaultProjectileRange() {
		return 30;
	}
}
