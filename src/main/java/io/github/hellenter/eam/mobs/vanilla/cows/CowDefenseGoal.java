package io.github.hellenter.eam.mobs.vanilla.cows;

import io.github.hellenter.eam.mobs.vanilla.cows.CowDefenseLogic.Phase;
import io.github.hellenter.eam.mobs.vanilla.cows.CowDefenseLogic.State;
import io.github.hellenter.eam.mobs.vanilla.cows.CowDefenseLogic.Step;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A grown cow hit by a player turns on him: charges, hits once, backs off. Runs above the vanilla panic goal, which
 * takes over again once this goal is done.
 */
final class CowDefenseGoal extends Goal {
	private static final int REPATH_INTERVAL = 10;
	/** The cow drops the fight when the attacker gets this far away. */
	private static final double MAX_DISTANCE_SQR = 16.0 * 16.0;

	private final PathfinderMob cow;
	private Player attacker;
	private State state = State.START;
	private int handledHurtTime = -1;

	CowDefenseGoal(PathfinderMob cow) {
		this.cow = cow;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (cow.isBaby() || cow.getLastHurtByMobTimestamp() == handledHurtTime) {
			return false;
		}
		if (cow.getLastHurtByMob() instanceof Player player && canFight(player)) {
			attacker = player;
			return true;
		}
		return false;
	}

	@Override
	public boolean canContinueToUse() {
		return state.phase() != Phase.DONE && !cow.isBaby() && canFight(attacker);
	}

	private boolean canFight(Player player) {
		return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator()
				&& cow.distanceToSqr(player) < MAX_DISTANCE_SQR;
	}

	@Override
	public void start() {
		handledHurtTime = cow.getLastHurtByMobTimestamp();
		state = State.START;
	}

	@Override
	public void stop() {
		attacker = null;
		state = State.START;
		cow.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		boolean hurtAgain = cow.getLastHurtByMob() == attacker && cow.getLastHurtByMobTimestamp() != handledHurtTime;
		if (hurtAgain) {
			handledHurtTime = cow.getLastHurtByMobTimestamp();
		}
		Phase before = state.phase();
		Step step = CowDefenseLogic.tick(state, cow.isWithinMeleeAttackRange(attacker), hurtAgain, CowDefenseModule.settings());
		state = step.state();
		if (step.attack()) {
			attack();
		}

		switch (state.phase()) {
			case CHARGE -> {
				cow.getLookControl().setLookAt(attacker, 30.0F, 30.0F);
				if (before != Phase.CHARGE || state.ticks() % REPATH_INTERVAL == 0) {
					cow.getNavigation().moveTo(attacker, CowDefenseModule.settings().chaseSpeed());
				}
			}
			case RETREAT -> {
				if (before != Phase.RETREAT || cow.getNavigation().isDone()) {
					Vec3 away = DefaultRandomPos.getPosAway(cow, 8, 4, attacker.position());
					if (away != null) {
						cow.getNavigation().moveTo(away.x, away.y, away.z, CowDefenseModule.settings().retreatSpeed());
					}
				}
			}
			case DONE -> {
			}
		}
	}

	private void attack() {
		if (!(cow.level() instanceof ServerLevel level)) {
			return;
		}
		CowDefenseSettings settings = CowDefenseModule.settings();
		DamageSource source = cow.damageSources().mobAttack(cow);
		if (attacker.hurtServer(level, source, settings.damage())) {
			double dx = cow.getX() - attacker.getX();
			double dz = cow.getZ() - attacker.getZ();
			attacker.knockback(settings.knockback(), dx, dz, source, settings.damage());
			cow.setLastHurtMob(attacker);
			cow.playAmbientSound();
		}
	}
}
