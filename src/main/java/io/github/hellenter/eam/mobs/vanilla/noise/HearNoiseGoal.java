package io.github.hellenter.eam.mobs.vanilla.noise;

import io.github.hellenter.eam.mixin.mobs.vanilla.TargetGoalAccessor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * The mob hears a noisy player within the player's noise radius ({@link NoiseModule#radius}) and goes for him without
 * seeing him. It keeps the target while it sees or still hears him and forgets him after {@code forget_ticks} of
 * silence out of sight.
 */
final class HearNoiseGoal extends TargetGoal {
	private static final int CHECK_INTERVAL = 10;

	private Player found;

	HearNoiseGoal(Mob mob) {
		super(mob, true);
		setFlags(EnumSet.of(Flag.TARGET));
	}

	@Override
	public boolean canUse() {
		if (mob.getTarget() != null || mob.getRandom().nextInt(reducedTickDelay(CHECK_INTERVAL)) != 0) {
			return false;
		}
		// A neutral mob (a zombified piglin) that is not angry leaves players be: a target would anger its whole group.
		if (mob instanceof NeutralMob neutral && !neutral.isAngry()) {
			return false;
		}
		// Spiders keep their vanilla calm by day: in daylight they only smell a player right next to them.
		if (mob instanceof Spider && mob.getLightLevelDependentMagicValue() >= 0.5F) {
			return false;
		}
		ServerLevel level = getServerLevel(mob);
		found = null;
		double nearest = Double.MAX_VALUE;
		for (ServerPlayer player : level.players()) {
			float radius = NoiseModule.radius(player);
			double distance = mob.distanceToSqr(player);
			if (distance < nearest && NoiseLogic.hears(distance, radius)
					&& TargetingConditions.forCombat().range(radius).ignoreLineOfSight().test(level, mob, player)) {
				found = player;
				nearest = distance;
			}
		}
		return found != null;
	}

	@Override
	public boolean canContinueToUse() {
		// Still making noise nearby: the mob keeps coming even out of sight.
		LivingEntity target = mob.getTarget();
		if (target instanceof Player player && NoiseLogic.hears(mob.distanceToSqr(player), NoiseModule.radius(player))) {
			((TargetGoalAccessor) (Object) this).eam$setUnseenTicks(0);
		}
		return super.canContinueToUse();
	}

	@Override
	public void start() {
		setUnseenMemoryTicks(NoiseModule.settings().forgetTicks());
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
