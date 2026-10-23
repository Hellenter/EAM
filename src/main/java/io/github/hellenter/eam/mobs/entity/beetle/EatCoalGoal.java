package io.github.hellenter.eam.mobs.entity.beetle;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;

/**
 * A hungry ember beetle smells out coal: lumps lying on the ground first, then the fuel in a furnace or oven nearby. It
 * walks up and eats one. Fighting comes first: the goal does not start while the beetle has a target.
 */
final class EatCoalGoal extends Goal {
	/** How far it smells coal lying about, and a furnace's fuel. */
	private static final double ITEM_RANGE = 8.0;
	private static final int FURNACE_RANGE = 6;
	/** Close enough to eat: the beetle's mouth reaches a lump next to it, or a furnace it stands against. */
	private static final double EAT_DISTANCE_SQR = 1.6 * 1.6;
	private static final double FURNACE_EAT_DISTANCE_SQR = 1.9 * 1.9;
	/** It gives up on coal it cannot get to. */
	private static final int GIVE_UP_TICKS = 200;

	private final EmberBeetle beetle;
	private @Nullable ItemEntity lump;
	private @Nullable BlockPos furnace;
	private int ticks;

	EatCoalGoal(EmberBeetle beetle) {
		this.beetle = beetle;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (!beetle.hungry() || beetle.getRandom().nextInt(reducedTickDelay(40)) != 0) {
			return false;
		}
		lump = findLump();
		furnace = lump == null ? findFurnace() : null;
		return lump != null || furnace != null;
	}

	@Override
	public boolean canContinueToUse() {
		if (!beetle.hungry() || ticks > GIVE_UP_TICKS) {
			return false;
		}
		return lump != null ? lump.isAlive() && !lump.getItem().isEmpty() : furnace != null && fuelOf(furnace) != null;
	}

	@Override
	public void start() {
		ticks = 0;
		moveToTarget();
	}

	@Override
	public void stop() {
		lump = null;
		furnace = null;
		beetle.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		// The goal keeps ticking until its next check, after the coal is eaten.
		if (lump == null && furnace == null) {
			return;
		}
		ticks++;
		Vec3 at = lump != null ? lump.position() : Vec3.atCenterOf(furnace);
		beetle.getLookControl().setLookAt(at.x, at.y, at.z);
		if (!(beetle.level() instanceof ServerLevel level)) {
			return;
		}
		if (lump != null && beetle.distanceToSqr(at) < EAT_DISTANCE_SQR) {
			lump.getItem().shrink(1);
			if (lump.getItem().isEmpty()) {
				lump.discard();
			}
			beetle.eatCoal(level);
			lump = null;
		} else if (furnace != null && beetle.distanceToSqr(at) < FURNACE_EAT_DISTANCE_SQR) {
			AbstractFurnaceBlockEntity fuel = fuelOf(furnace);
			if (fuel != null) {
				fuel.removeItem(EmberBeetle.FURNACE_FUEL_SLOT, 1);
				fuel.setChanged();
				beetle.eatCoal(level);
			}
			furnace = null;
		} else if (ticks % 20 == 0) {
			moveToTarget();
		}
	}

	private void moveToTarget() {
		if (lump != null) {
			beetle.getNavigation().moveTo(lump, 1.0);
		} else if (furnace != null) {
			beetle.getNavigation().moveTo(furnace.getX() + 0.5, furnace.getY(), furnace.getZ() + 0.5, 1, 1.0);
		}
	}

	private @Nullable ItemEntity findLump() {
		return beetle.level().getEntitiesOfClass(ItemEntity.class, beetle.getBoundingBox().inflate(ITEM_RANGE, 3.0, ITEM_RANGE),
						item -> item.isAlive() && item.getItem().is(EmberBeetleModule.FOOD)).stream()
				.min(Comparator.comparingDouble(beetle::distanceToSqr))
				.orElse(null);
	}

	private @Nullable BlockPos findFurnace() {
		BlockPos centre = beetle.blockPosition();
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-FURNACE_RANGE, -2, -FURNACE_RANGE), centre.offset(FURNACE_RANGE, 2, FURNACE_RANGE))) {
			if (fuelOf(pos) != null) {
				double distance = pos.distSqr(centre);
				if (distance < bestDistance) {
					bestDistance = distance;
					best = pos.immutable();
				}
			}
		}
		return best;
	}

	/** The furnace at {@code pos} when it has coal in its fuel slot. */
	private @Nullable AbstractFurnaceBlockEntity fuelOf(BlockPos pos) {
		BlockEntity entity = beetle.level().getBlockEntity(pos);
		return entity instanceof AbstractFurnaceBlockEntity furnace && furnace.getItem(EmberBeetle.FURNACE_FUEL_SLOT).is(EmberBeetleModule.FOOD)
				? furnace : null;
	}
}
