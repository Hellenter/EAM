package io.github.hellenter.eam.mobs.entity.lost;

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
 * the player is far or out of sight, and otherwise stands, steps to the side now and then, and shoots.
 */
final class LostBowGoal extends Goal {
	private static final int REPATH_INTERVAL = 10;
	/** He lowers a drawn bow after this many ticks without sight of the target. */
	private static final int BLIND_TICKS = 40;
	private static final double RETREAT_SPEED = 1.15;

	private final Lost lost;
	private int cooldown;
	private int repath;
	private int blind;
	private int strafeTicks;
	private boolean strafeRight;

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

	private void shoot(LivingEntity target, double distance, boolean sees, LostSettings.Archer archer) {
		if (lost.isUsingItem()) {
			blind = sees ? 0 : blind + 1;
			int drawn = lost.getTicksUsingItem();
			if (blind > BLIND_TICKS) {
				lost.stopUsingItem();
			} else if (sees && drawn >= archer.drawTicks()) {
				lost.stopUsingItem();
				lost.performRangedAttack(target, BowItem.getPowerForTime(drawn));
				cooldown = archer.cooldown();
			}
		} else if (--cooldown <= 0 && sees && distance <= archer.maxRange() + 4.0) {
			blind = 0;
			lost.startUsingItem(ProjectileUtil.getWeaponHoldingHand(lost, Items.BOW));
		}
	}
}
