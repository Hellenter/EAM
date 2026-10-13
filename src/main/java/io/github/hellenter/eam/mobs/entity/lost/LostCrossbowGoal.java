package io.github.hellenter.eam.mobs.entity.lost;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * The shield wall's commander keeps behind his row at crossbow range: he backs away from an enemy that comes close,
 * walks closer to one far off or out of sight, and otherwise stands still, so his row stands still in front of him.
 * He loads, then stops, turns to the enemy, holds his aim for a moment and shoots.
 */
final class LostCrossbowGoal extends Goal {
	private static final int REPATH_INTERVAL = 10;
	private static final double RETREAT_SPEED = 1.1;
	/** How fast he turns to the target while aiming, degrees per tick. */
	private static final float AIM_TURN = 40.0F;
	/** He shoots only once he faces the target this closely, in degrees. */
	private static final float AIM_TOLERANCE = 12.0F;

	private enum Phase {
		IDLE,
		LOADING,
		AIMING
	}

	private final LostWallCommander commander;
	private Phase phase = Phase.IDLE;
	private int phaseTicks;
	private int cooldown;
	private int repath;

	LostCrossbowGoal(LostWallCommander commander) {
		this.commander = commander;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = commander.getTarget();
		return commander.role() == LostRole.WALL_COMMANDER && target != null && target.isAlive() && commander.isHolding(Items.CROSSBOW);
	}

	@Override
	public void start() {
		commander.setAggressive(true);
		repath = 0;
	}

	@Override
	public void stop() {
		commander.setAggressive(false);
		reset();
		commander.getNavigation().stop();
		commander.getMoveControl().strafe(0.0F, 0.0F);
	}

	private void reset() {
		if (commander.isUsingItem()) {
			commander.stopUsingItem();
		}
		commander.setLoaded(false);
		phase = Phase.IDLE;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity target = commander.getTarget();
		if (target == null || !(commander.level() instanceof ServerLevel level)) {
			return;
		}
		ShieldWallSettings.Crossbow crossbow = LostModule.settings().shieldWall().crossbow();
		double distance = commander.distanceTo(target);
		boolean sees = commander.getSensing().hasLineOfSight(target);
		commander.getLookControl().setLookAt(target, 30.0F, 30.0F);
		repath--;
		phaseTicks++;

		boolean aiming = phase == Phase.AIMING && sees;
		if (aiming) {
			holdAim(target);
		} else {
			move(target, distance, sees, crossbow);
		}

		switch (phase) {
			case IDLE -> {
				if (--cooldown <= 0 && sees && distance <= crossbow.maxRange() + 4.0) {
					commander.startUsingItem(ProjectileUtil.getWeaponHoldingHand(commander, Items.CROSSBOW));
					setPhase(Phase.LOADING);
				}
			}
			case LOADING -> {
				if (phaseTicks >= crossbow.loadTicks()) {
					commander.stopUsingItem();
					commander.setLoaded(true);
					commander.playSound(SoundEvents.CROSSBOW_LOADING_END.value(), 1.0F,
							1.0F / (commander.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F);
					setPhase(Phase.AIMING);
				}
			}
			case AIMING -> {
				if (aiming && phaseTicks >= crossbow.aimTicks() && facing(target)) {
					commander.shoot(level, target, crossbow);
					cooldown = crossbow.cooldown();
					reset();
				}
			}
		}
	}

	private void setPhase(Phase next) {
		phase = next;
		phaseTicks = 0;
	}

	/** He stands still and turns to face the target. */
	private void holdAim(LivingEntity target) {
		commander.getNavigation().stop();
		commander.getMoveControl().setWantedPosition(commander.getX(), commander.getY(), commander.getZ(), 0.0);
		commander.setXxa(0.0F);
		float yaw = yawTo(target);
		commander.setYRot(LostLogic.turnTowards(commander.getYRot(), yaw, AIM_TURN));
		commander.setYBodyRot(commander.getYRot());
		commander.setYHeadRot(commander.getYRot());
	}

	private boolean facing(LivingEntity target) {
		return Mth.degreesDifferenceAbs(commander.getYRot(), yawTo(target)) <= AIM_TOLERANCE;
	}

	private float yawTo(LivingEntity target) {
		return (float) (Mth.atan2(target.getZ() - commander.getZ(), target.getX() - commander.getX()) * Mth.RAD_TO_DEG) - 90.0F;
	}

	private void move(LivingEntity target, double distance, boolean sees, ShieldWallSettings.Crossbow crossbow) {
		// Slower while he loads, like a pillager.
		double speed = phase == Phase.LOADING ? 0.6 : 1.0;
		switch (LostLogic.commanderMove(distance, sees, crossbow)) {
			case RETREAT -> {
				if (repath <= 0 || commander.getNavigation().isDone()) {
					repath = REPATH_INTERVAL;
					Vec3 away = DefaultRandomPos.getPosAway(commander, (int) Math.ceil(crossbow.minRange()) + 4, 4, target.position());
					if (away != null) {
						commander.getNavigation().moveTo(away.x, away.y, away.z, RETREAT_SPEED * speed);
					}
				}
			}
			case APPROACH -> {
				if (repath <= 0 || commander.getNavigation().isDone()) {
					repath = REPATH_INTERVAL;
					commander.getNavigation().moveTo(target, speed);
				}
			}
			case HOLD -> {
				commander.getNavigation().stop();
				commander.lookAt(target, 30.0F, 30.0F);
			}
		}
	}
}
