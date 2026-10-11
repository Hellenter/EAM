package io.github.hellenter.eam.mobs.entity.lost;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * An archer of the Lost keeps the player at bow range: he backs away when the player comes close, walks closer when
 * the player is far or out of sight, and otherwise stands and steps to the side now and then. Before each shot he stops,
 * turns to face the player and holds still for a moment.
 */
final class LostBowGoal extends Goal {
	private static final int REPATH_INTERVAL = 10;
	/** He lowers a drawn bow after this many ticks without sight of the target. */
	private static final int BLIND_TICKS = 40;
	private static final double RETREAT_SPEED = 1.15;
	/** How fast he turns to the target while aiming, degrees per tick. */
	private static final float AIM_TURN = 40.0F;
	/** He looses only once he faces the target this closely, in degrees. */
	private static final float AIM_TOLERANCE = 12.0F;

	private final Lost lost;
	private int cooldown;
	private int repath;
	private int blind;
	private int strafeTicks;
	private boolean strafeRight;
	/** Ticks he has stood aiming with the bow fully drawn, or -1 while he is not aiming. */
	private int aimTicks = -1;

	LostBowGoal(Lost lost) {
		this.lost = lost;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = lost.getTarget();
		return lost.role() == LostRole.ARCHER && target != null && target.isAlive() && lost.isHolding(Items.BOW);
	}

	@Override
	public void start() {
		lost.setAggressive(true);
		repath = 0;
	}

	@Override
	public void stop() {
		lost.setAggressive(false);
		lost.stopUsingItem();
		aimTicks = -1;
		lost.getNavigation().stop();
		lost.getMoveControl().strafe(0.0F, 0.0F);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity target = lost.getTarget();
		if (target == null) {
			return;
		}
		LostSettings.Archer archer = LostModule.settings().archer();
		double distance = lost.distanceTo(target);
		boolean sees = lost.getSensing().hasLineOfSight(target);
		lost.getLookControl().setLookAt(target, 30.0F, 30.0F);
		repath--;
		if (aim(target, sees, archer)) {
			return;
		}
		switch (LostLogic.archerMove(distance, sees, archer)) {
			case RETREAT -> {
				if (repath <= 0 || lost.getNavigation().isDone()) {
					repath = REPATH_INTERVAL;
					Vec3 away = DefaultRandomPos.getPosAway(lost, (int) Math.ceil(archer.minRange()) + 4, 4, target.position());
					if (away != null) {
						lost.getNavigation().moveTo(away.x, away.y, away.z, RETREAT_SPEED);
					}
				}
			}
			case APPROACH -> {
				if (repath <= 0 || lost.getNavigation().isDone()) {
					repath = REPATH_INTERVAL;
					lost.getNavigation().moveTo(target, 1.0);
				}
			}
			case HOLD -> {
				lost.getNavigation().stop();
				if (--strafeTicks <= 0) {
					strafeTicks = 30 + lost.getRandom().nextInt(40);
					strafeRight = lost.getRandom().nextBoolean();
				}
				lost.getMoveControl().strafe(0.0F, strafeRight ? 0.4F : -0.4F);
				lost.lookAt(target, 30.0F, 30.0F);
			}
		}
		shoot(target, distance, sees, archer);
	}

	/**
	 * With the bow fully drawn and the target in sight he stops where he is, turns to face it and holds still for a
	 * moment, then looses: never over his shoulder while backing off. Returns whether he is aiming this tick.
	 */
	private boolean aim(LivingEntity target, boolean sees, LostSettings.Archer archer) {
		if (!lost.isUsingItem() || !sees || lost.getTicksUsingItem() < archer.drawTicks()) {
			aimTicks = -1;
			return false;
		}
		if (aimTicks < 0) {
			aimTicks = 0;
			lost.getNavigation().stop();
			lost.getMoveControl().strafe(0.0F, 0.0F);
		}
		aimTicks++;
		// Stand still: no strafing or walking while he aims. The move control keeps the sideways input of an earlier
		// strafe, so clear it here.
		lost.getMoveControl().setWantedPosition(lost.getX(), lost.getY(), lost.getZ(), 0.0);
		lost.setXxa(0.0F);
		float yaw = (float) (Mth.atan2(target.getZ() - lost.getZ(), target.getX() - lost.getX()) * Mth.RAD_TO_DEG) - 90.0F;
		lost.setYRot(LostLogic.turnTowards(lost.getYRot(), yaw, AIM_TURN));
		lost.setYBodyRot(lost.getYRot());
		lost.setYHeadRot(lost.getYRot());
		lost.getLookControl().setLookAt(target, 30.0F, 30.0F);
		if (aimTicks >= archer.aimTicks() && Mth.degreesDifferenceAbs(lost.getYRot(), yaw) <= AIM_TOLERANCE) {
			int drawn = lost.getTicksUsingItem();
			lost.stopUsingItem();
			lost.performRangedAttack(target, BowItem.getPowerForTime(drawn));
			cooldown = archer.cooldown();
			aimTicks = -1;
		}
		return true;
	}

	private void shoot(LivingEntity target, double distance, boolean sees, LostSettings.Archer archer) {
		if (lost.isUsingItem()) {
			blind = sees ? 0 : blind + 1;
			if (blind > BLIND_TICKS) {
				lost.stopUsingItem();
			}
		} else if (--cooldown <= 0 && sees && distance <= archer.maxRange() + 4.0) {
			blind = 0;
			lost.startUsingItem(ProjectileUtil.getWeaponHoldingHand(lost, Items.BOW));
		}
	}
}
