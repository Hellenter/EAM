package io.github.hellenter.eam.mobs.entity.leshy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * Where the leshy goes: to an offering on a stump, at its target in a fight, or, while it haunts, round its player at a
 * distance, from tree to tree. Its actions (roots, the howl, the flight, eating) hold it still.
 */
final class LeshyGoal extends Goal {
	private static final double WALK = 0.8;
	private static final double CHASE = 1.0;
	/** Close enough to the offering to eat it. */
	private static final double EAT_DISTANCE = 2.5;
	private static final int REPATH = 40;

	private final Leshy leshy;
	private int repath;

	LeshyGoal(Leshy leshy) {
		this.leshy = leshy;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		return !leshy.leaving() && (leshy.busy() || leshy.offering() != null || leshy.fighting() && leshy.getTarget() != null
				|| leshy.haunted() != null);
	}

	@Override
	public boolean canContinueToUse() {
		return canUse();
	}

	@Override
	public void start() {
		repath = 0;
	}

	@Override
	public void stop() {
		leshy.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (!(leshy.level() instanceof ServerLevel level) || leshy.busy()) {
			return;
		}
		repath--;
		ItemEntity offering = leshy.offering();
		if (offering != null) {
			leshy.getLookControl().setLookAt(offering, 30.0F, 30.0F);
			if (leshy.distanceToSqr(offering) < EAT_DISTANCE * EAT_DISTANCE) {
				leshy.getNavigation().stop();
				leshy.eatOffering();
			} else if (repath <= 0) {
				repath = 20;
				leshy.getNavigation().moveTo(offering, WALK);
			}
			return;
		}
		LivingEntity target = leshy.getTarget();
		if (leshy.fighting() && target != null) {
			fight(level, target);
			return;
		}
		Player player = leshy.haunted();
		if (player != null) {
			haunt(level, player);
		}
	}

	private void fight(ServerLevel level, LivingEntity target) {
		leshy.getLookControl().setLookAt(target, 30.0F, 30.0F);
		double reach = leshy.reach();
		double distanceSqr = leshy.distanceToSqr(target);
		if (distanceSqr <= reach * reach && leshy.getSensing().hasLineOfSight(target)) {
			leshy.getNavigation().stop();
			if (leshy.canHit()) {
				leshy.strike(level, target);
			}
			return;
		}
		if (repath <= 0 || leshy.getNavigation().isDone()) {
			repath = 10;
			leshy.getNavigation().moveTo(target, CHASE);
		}
	}

	/** Keeps between the near and far haunting distances from the player, standing by trees where it can. */
	private void haunt(ServerLevel level, Player player) {
		LeshySettings.Haunt haunt = Leshy.settings().haunt();
		double distance = Math.sqrt(leshy.distanceToSqr(player));
		boolean tooClose = distance < haunt.keepMin();
		boolean tooFar = distance > haunt.keepMax();
		if (!tooClose && !tooFar) {
			if (leshy.getNavigation().isDone()) {
				// It stands still among the trunks and watches.
				leshy.getLookControl().setLookAt(player, 10.0F, 10.0F);
			}
			return;
		}
		if (repath > 0 && !leshy.getNavigation().isDone()) {
			return;
		}
		repath = REPATH;
		double want = (haunt.keepMin() + haunt.keepMax()) / 2.0;
		Vec3 spot = spotNear(level, player, want);
		if (spot != null) {
			leshy.getNavigation().moveTo(spot.x, spot.y, spot.z, tooClose ? CHASE : WALK);
		}
	}

	/** A spot about {@code want} from the player on its own side of them, by a tree if one is near. */
	private @Nullable Vec3 spotNear(ServerLevel level, Player player, double want) {
		Vec3 away = leshy.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
		if (away.lengthSqr() < 1.0E-4) {
			away = new Vec3(1.0, 0.0, 0.0);
		}
		away = away.normalize();
		Vec3 fallback = null;
		for (int i = 0; i < 10; i++) {
			double turn = (leshy.getRandom().nextDouble() - 0.5) * Math.PI * 0.8;
			double cos = Math.cos(turn);
			double sin = Math.sin(turn);
			double dx = away.x * cos - away.z * sin;
			double dz = away.x * sin + away.z * cos;
			int x = (int) Math.floor(player.getX() + dx * want);
			int z = (int) Math.floor(player.getZ() + dz * want);
			BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
			Vec3 at = Vec3.atBottomCenterOf(ground);
			if (Leshy.logNear(level, ground, 2, 0, 2)) {
				return at;
			}
			if (fallback == null) {
				fallback = at;
			}
		}
		return fallback;
	}
}
