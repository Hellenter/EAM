package io.github.hellenter.eam.world.bloodmoon;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;

/** Under a blood moon the mob goes after animals it can see when no player is around; it gives up when the moon sets. */
final class HuntAnimalsGoal extends NearestAttackableTargetGoal<Animal> {
	HuntAnimalsGoal(Mob mob) {
		super(mob, Animal.class, 10, true, false, null);
	}

	@Override
	public boolean canUse() {
		return BloodMoonModule.activeIn(mob.level()) && super.canUse();
	}

	@Override
	public boolean canContinueToUse() {
		return BloodMoonModule.activeIn(mob.level()) && super.canContinueToUse();
	}
}
