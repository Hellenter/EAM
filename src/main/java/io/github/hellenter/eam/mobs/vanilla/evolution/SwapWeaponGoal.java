package io.github.hellenter.eam.mobs.vanilla.evolution;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * An evolved skeleton swaps its bow for the blade it carries once its target comes close, and back once the target
 * backs off. Vanilla skeletons pick their bow or melee attack by what they hold, so the swap alone changes how they
 * fight. The weapon not in hand is kept in {@link MonsterEvolutionModule#STOWED_WEAPON}. It runs in the target selector,
 * since the swap rebuilds the skeleton's attack goals.
 */
class SwapWeaponGoal extends Goal {
	private final Mob mob;

	SwapWeaponGoal(Mob mob) {
		this.mob = mob;
	}

	@Override
	public boolean canUse() {
		ItemStack stowed = mob.getAttached(MonsterEvolutionModule.STOWED_WEAPON);
		if (stowed == null || stowed.isEmpty()) {
			return false;
		}
		EvolutionSettings.Melee melee = MonsterEvolutionModule.settings().melee();
		LivingEntity target = mob.getTarget();
		boolean holdingBlade = !mob.getMainHandItem().is(Items.BOW);
		boolean hasTarget = target != null && target.isAlive();
		double distance = hasTarget ? mob.distanceTo(target) : 0.0;
		return EvolutionLogic.wantsBlade(holdingBlade, hasTarget, distance, melee.drawRange(), melee.sheatheRange()) != holdingBlade;
	}

	@Override
	public boolean canContinueToUse() {
		return false;
	}

	@Override
	public void start() {
		ItemStack inHand = mob.getMainHandItem();
		ItemStack stowed = mob.getAttached(MonsterEvolutionModule.STOWED_WEAPON);
		mob.setAttached(MonsterEvolutionModule.STOWED_WEAPON, inHand.copy());
		mob.setItemSlot(EquipmentSlot.MAINHAND, stowed.copy());
	}
}
