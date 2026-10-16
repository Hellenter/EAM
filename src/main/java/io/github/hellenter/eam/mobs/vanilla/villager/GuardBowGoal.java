package io.github.hellenter.eam.mobs.vanilla.villager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumSet;

/**
 * A guard with a bow or crossbow keeps his distance and shoots, like a skeleton: he walks closer while his target is
 * out of range or out of sight, draws for a moment and lets go. His arrows cannot be picked up.
 */
final class GuardBowGoal extends Goal {
	private final Villager villager;
	private int rest;

	GuardBowGoal(Villager villager) {
		this.villager = villager;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = villager.getTarget();
		return target != null && target.isAlive() && VillagerGuardModule.isGuard(villager) && VillagerGuardModule.hasBow(villager);
	}

	@Override
	public void stop() {
		villager.stopUsingItem();
		villager.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity target = villager.getTarget();
		if (target == null) return;
		GuardSettings settings = VillagerGuardModule.settings();
		villager.getLookControl().setLookAt(target, 30.0F, 30.0F);
		double range = settings.bowRange();
		boolean sees = villager.getSensing().hasLineOfSight(target);
		boolean inRange = villager.distanceToSqr(target) <= range * range;
		if (sees && inRange) {
			villager.getNavigation().stop();
		} else {
			villager.getNavigation().moveTo(target, settings.meleeSpeed());
		}
		if (rest > 0) {
			rest--;
			return;
		}
		if (!villager.isUsingItem()) {
			if (sees && inRange) villager.startUsingItem(InteractionHand.MAIN_HAND);
			return;
		}
		if (!sees) {
			villager.stopUsingItem();
		} else if (VillagerGuardLogic.releases(villager.getTicksUsingItem(), settings.bowDrawTicks())) {
			villager.stopUsingItem();
			shoot(target, settings.arrowPower());
			rest = settings.bowRestTicks();
		}
	}

	private void shoot(LivingEntity target, float power) {
		if (!(villager.level() instanceof ServerLevel level)) return;
		ItemStack bow = villager.getMainHandItem();
		ItemStack projectile = new ItemStack(Items.ARROW);
		AbstractArrow arrow = ProjectileUtil.getMobArrow(villager, projectile, power, bow);
		arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
		double dx = target.getX() - villager.getX();
		double dy = target.getY(1.0 / 3.0) - arrow.getY();
		double dz = target.getZ() - villager.getZ();
		double distance = Math.sqrt(dx * dx + dz * dz);
		Projectile.spawnProjectileUsingShoot(arrow, level, projectile, dx, dy + distance * 0.2, dz, 1.6F, 6.0F);
		villager.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (villager.getRandom().nextFloat() * 0.4F + 0.8F));
		VillagerRepairModule.afterShot(villager);
	}
}
