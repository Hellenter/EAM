package io.github.hellenter.eam.mobs.entity.draugr;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A thrall in his master's row of shields: he takes the spot his master gives him, faces the enemy with his shield up
 * and stands there, lowering it only to swing at the enemy when it comes into his reach, or while an axe has knocked it
 * out of the guard.
 */
final class DraugrWallGoal extends Goal {
	/** Further than this from his spot he walks there by path. */
	private static final double PATH_DISTANCE = 2.0;
	/** Closer than this he is on his spot. */
	private static final double ON_SPOT = 0.3;
	/** How hard he steps onto his spot without turning from the enemy, blocks a tick at most. */
	private static final double STEP = 0.12;
	private static final int ATTACK_INTERVAL = 30;
	/** Ticks his shield stays down after a blow. */
	private static final int SHIELD_DOWN = 10;

	private final Draugr draugr;
	private int repath;
	private int attackCooldown;
	private int shieldDown;

	DraugrWallGoal(Draugr draugr) {
		this.draugr = draugr;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		return draugr.state() == DraugrState.AWAKE && draugr.wallOrder() != null;
	}

	@Override
	public void stop() {
		draugr.stopUsingItem();
		draugr.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		Draugr.WallOrder order = draugr.wallOrder();
		if (order == null || !(draugr.level() instanceof ServerLevel level)) {
			return;
		}
		if (attackCooldown > 0) attackCooldown--;
		if (shieldDown > 0) shieldDown--;
		repath--;
		LivingEntity enemy = order.facing();
		Vec3 spot = order.spot();
		double offX = spot.x - draugr.getX();
		double offZ = spot.z - draugr.getZ();
		double off = Math.hypot(offX, offZ);
		draugr.getLookControl().setLookAt(enemy, 30.0F, 30.0F);
		if (off > PATH_DISTANCE || Math.abs(spot.y - draugr.getY()) > 1.5) {
			if (repath <= 0 || draugr.getNavigation().isDone()) {
				repath = 5;
				draugr.getNavigation().moveTo(spot.x, spot.y, spot.z, 1.2);
			}
			return;
		}
		// Close: face and shield to the enemy, and slide onto the spot without turning to it.
		draugr.getNavigation().stop();
		float yaw = (float) (Mth.atan2(enemy.getZ() - draugr.getZ(), enemy.getX() - draugr.getX()) * Mth.RAD_TO_DEG) - 90.0F;
		draugr.setYRot(Mth.approachDegrees(draugr.getYRot(), yaw, 20.0F));
		draugr.setYBodyRot(draugr.getYRot());
		if (off > ON_SPOT) {
			double speed = Math.min(STEP, off * 0.3) / off;
			Vec3 v = draugr.getDeltaMovement();
			draugr.setDeltaMovement(offX * speed, v.y, offZ * speed);
		}
		if (attackCooldown <= 0 && draugr.distanceTo(enemy) <= order.reach() && draugr.getSensing().hasLineOfSight(enemy)) {
			draugr.stopUsingItem();
			draugr.swingForAttack(InteractionHand.MAIN_HAND);
			draugr.doHurtTarget(level, enemy);
			attackCooldown = ATTACK_INTERVAL;
			shieldDown = SHIELD_DOWN;
		} else if (shieldDown <= 0 && !draugr.shieldKnocked() && !draugr.isUsingItem()
				&& draugr.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS)) {
			draugr.startUsingItem(InteractionHand.OFF_HAND);
		}
	}
}
