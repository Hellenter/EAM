package io.github.hellenter.eam.mobs.entity.lost;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * Vanilla moving plus {@link #step}: walking in any direction without turning, at any speed. Vanilla strafing is stuck
 * at a quarter of the walking speed and, on ground it does not like, walks forward instead.
 */
final class LostMoveControl extends MoveControl<Lost> {
	private static final int BLOCKED_TICKS = 40;

	private boolean stepping;
	private boolean stepped;
	/** Ticks left he walks by path after a step met a ledge or a wall. */
	private int blockedTicks;
	private float stepForward;
	private float stepLeft;
	private double stepSpeed;

	LostMoveControl(Lost lost) {
		super(lost);
	}

	/** Walks by {@code forward} and {@code left} (see {@link LostLogic#stepInput}) this tick, keeping his facing. */
	void step(float forward, float left, double speedModifier) {
		stepping = true;
		stepForward = forward;
		stepLeft = left;
		stepSpeed = speedModifier;
		operation = Operation.WAIT;
	}

	/** Whether a step lately met a ledge, a wall or worse and he stood instead: then he had better walk by path. */
	boolean stepBlocked() {
		return blockedTicks > 0;
	}

	@Override
	public void tick() {
		if (blockedTicks > 0) {
			blockedTicks--;
		}
		if (stepping) {
			stepping = false;
			stepped = true;
			float yaw = mob.getYRot() * ((float) Math.PI / 180.0F);
			double dx = -stepForward * Math.sin(yaw) + stepLeft * Math.cos(yaw);
			double dz = stepForward * Math.cos(yaw) + stepLeft * Math.sin(yaw);
			if (!walkable(dx, dz)) {
				blockedTicks = BLOCKED_TICKS;
				mob.setZza(0.0F);
				mob.setXxa(0.0F);
				return;
			}
			mob.setSpeed((float) (stepSpeed * mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
			mob.setZza(stepForward);
			mob.setXxa(stepLeft);
			return;
		}
		if (stepped) {
			// Vanilla never clears the sideways input outside strafing.
			stepped = false;
			mob.setXxa(0.0F);
		}
		super.tick();
	}

	/** Whether the block a step that way leads onto is plain ground: he does not back off ledges or into lava. */
	private boolean walkable(double dx, double dz) {
		NodeEvaluator evaluator = mob.getNavigation().getNodeEvaluator();
		if (evaluator == null) {
			return true;
		}
		double length = Math.sqrt(dx * dx + dz * dz);
		if (length < 1.0E-4) {
			return true;
		}
		double reach = 0.6 / length;
		BlockPos ahead = BlockPos.containing(mob.getX() + dx * reach, mob.getBlockY(), mob.getZ() + dz * reach);
		return evaluator.getPathType(mob, ahead) == PathType.WALKABLE;
	}
}
