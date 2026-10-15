package io.github.hellenter.eam.mobs.vanilla.predators;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;

/**
 * At night the mob goes for the nearest livestock it can walk up to. A fence (or a shut gate) keeps it out; for raiders,
 * light in the pen does too. Players and its own enemies come first: the goal sits after them.
 */
public class LivestockHuntGoal extends NearestAttackableTargetGoal<Animal> {
	private final boolean raider;
	private long nextRecheck;

	public LivestockHuntGoal(Mob mob, boolean raider) {
		super(mob, Animal.class, 10, false, false, (target, level) -> PredatorModule.isPrey(target, raider));
		this.raider = raider;
	}

	@Override
	public boolean canUse() {
		if (!PredatorModule.huntsNow(mob) || !super.canUse()) {
			return false;
		}
		if (!PredatorModule.reachable(mob, target)) {
			target = null;
			return false;
		}
		return true;
	}

	@Override
	public void start() {
		super.start();
		nextRecheck = mob.level().getGameTime() + PredatorModule.settings().recheckTicks();
	}

	@Override
	public boolean canContinueToUse() {
		if (!PredatorModule.huntsNow(mob) || !(mob.getTarget() instanceof Animal prey) || !PredatorModule.isPrey(prey, raider)) {
			return false;
		}
		long now = mob.level().getGameTime();
		if (now >= nextRecheck) {
			// A gate shut, or the prey slipped into the pen.
			nextRecheck = now + PredatorModule.settings().recheckTicks();
			if (!PredatorModule.reachable(mob, prey)) {
				return false;
			}
		}
		return super.canContinueToUse();
	}

	@Override
	protected double getFollowDistance() {
		// The parent constructor calls this before raider is set; the search area, asked later, gets it right.
		return PredatorLogic.searchRange(super.getFollowDistance(), PredatorModule.group(raider));
	}
}
