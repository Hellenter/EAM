package io.github.hellenter.eam.mobs.entity.lost;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * One of the Lost serving the player with the commander's horn ({@link LostCommander}): he goes for whoever that player
 * fights, and for any hostile mob near him when the player fights no one. A new foe of the player draws him off the one
 * he chose himself.
 */
public class CommanderTargetGoal extends TargetGoal {
	private static final int SCAN_INTERVAL = 10;

	private @Nullable Player commander;
	private @Nullable LivingEntity chosen;
	private int nextScan;

	public CommanderTargetGoal(Mob mob) {
		super(mob, false);
		setFlags(EnumSet.of(Goal.Flag.TARGET));
	}

	@Override
	public boolean canUse() {
		if (--nextScan > 0) {
			return false;
		}
		nextScan = reducedTickDelay(SCAN_INTERVAL);
		commander = LostCommander.near(mob);
		if (commander == null) {
			return false;
		}
		chosen = commandersFoe(commander);
		if (chosen == null) {
			chosen = nearestEnemy();
		}
		return chosen != null;
	}

	@Override
	public boolean canContinueToUse() {
		if (!super.canContinueToUse()) {
			return false;
		}
		if (commander == null || !LostCommander.commands(commander)) {
			// He has put the horn away: the fight goes on without his orders.
			return true;
		}
		LivingEntity foe = commandersFoe(commander);
		return foe == null || foe == mob.getTarget();
	}

	@Override
	public void start() {
		mob.setTarget(chosen);
		targetMob = chosen;
		super.start();
	}

	@Override
	public void stop() {
		super.stop();
		chosen = null;
	}

	/** Whom the commander fights: whoever he hit last, or else whoever hit him, while fresh. */
	private @Nullable LivingEntity commandersFoe(Player commander) {
		int memory = LostCommander.settings().memoryTicks();
		LivingEntity victim = commander.getLastHurtMob();
		if (victim != null && commander.tickCount - commander.getLastHurtMobTimestamp() <= memory && fitFoe(victim)) {
			return victim;
		}
		LivingEntity attacker = commander.getLastHurtByMob();
		if (attacker != null && commander.tickCount - commander.getLastHurtByMobTimestamp() <= memory && fitFoe(attacker)) {
			return attacker;
		}
		return null;
	}

	private boolean fitFoe(LivingEntity foe) {
		return foe != mob && foe.isAlive() && !LostCommander.isKin(foe) && mob.canAttack(foe)
				&& mob.distanceToSqr(foe) <= getFollowDistance() * getFollowDistance();
	}

	/** The nearest hostile mob he can see near him that is not one of the Lost. */
	private @Nullable LivingEntity nearestEnemy() {
		double range = LostCommander.settings().enemyRange();
		TargetingConditions conditions = TargetingConditions.forCombat().range(range)
				.selector((target, level) -> target instanceof Enemy && !LostCommander.isKin(target));
		return getServerLevel(mob).getNearestEntity(mob.level().getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(range), m -> true),
				conditions, mob, mob.getX(), mob.getEyeY(), mob.getZ());
	}
}
