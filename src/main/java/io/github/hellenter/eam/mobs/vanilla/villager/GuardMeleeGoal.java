package io.github.hellenter.eam.mobs.vanilla.villager;

import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.npc.villager.Villager;

/** A guard with a melee weapon walks up to his target and strikes. */
final class GuardMeleeGoal extends MeleeAttackGoal {
	private final Villager villager;

	GuardMeleeGoal(Villager villager) {
		super(villager, VillagerGuardModule.settings().meleeSpeed(), true);
		this.villager = villager;
	}

	@Override
	public boolean canUse() {
		return VillagerGuardModule.isGuard(villager) && !VillagerGuardModule.hasBow(villager) && super.canUse();
	}

	@Override
	public boolean canContinueToUse() {
		return VillagerGuardModule.isGuard(villager) && !VillagerGuardModule.hasBow(villager) && super.canContinueToUse();
	}
}
