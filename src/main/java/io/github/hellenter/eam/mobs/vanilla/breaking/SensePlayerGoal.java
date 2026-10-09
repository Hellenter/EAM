package io.github.hellenter.eam.mobs.vanilla.breaking;

import io.github.hellenter.eam.world.bloodmoon.BloodMoonModule;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * The mob smells a player within {@code sense_radius} blocks through walls: it targets the player without seeing
 * them, so a player who walled himself in nearby is found and dug out.
 */
final class SensePlayerGoal extends TargetGoal {
	private static final int CHECK_INTERVAL = 10;

	private Player found;

	SensePlayerGoal(Mob mob) {
		super(mob, false);
		setFlags(EnumSet.of(Flag.TARGET));
	}

	@Override
	public boolean canUse() {
		if (mob.getTarget() != null || mob.getRandom().nextInt(reducedTickDelay(CHECK_INTERVAL)) != 0) {
			return false;
		}
		// A blood moon sharpens their senses.
		int radius = Math.max(BlockBreakingModule.settings().senseRadius(), BloodMoonModule.senseRadius(mob.level()));
		if (radius <= 0) {
			return false;
		}
		TargetingConditions conditions = TargetingConditions.forCombat().range(radius).ignoreLineOfSight();
		ServerLevel level = getServerLevel(mob);
		found = level.getNearestPlayer(conditions, mob, mob.getX(), mob.getEyeY(), mob.getZ());
		return found != null;
	}

	@Override
	public void start() {
		mob.setTarget(found);
		targetMob = found;
		super.start();
	}

	@Override
	public void stop() {
		super.stop();
		found = null;
	}
}
