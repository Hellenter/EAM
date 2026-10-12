package io.github.hellenter.eam.mobs.entity.wight;

import io.github.hellenter.eam.mobs.entity.wight.WightLogic.Plan;
import io.github.hellenter.eam.mobs.entity.wight.WightLogic.Point;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * The wight's whole mind: it stalks its player from a distance, out of their sight where it can; raids them now and then
 * while they are busy; hunts them pass after pass from behind once they are weak; runs off after every raid, pass or
 * hurt; and while it burns it forgets the player and staggers about looking for water.
 */
final class WightStalkGoal extends Goal {
	private enum Phase { STALK, APPROACH, STRIKE, FLEE }

	/** Full speed: 1.7 times a sprinting player. */
	private static final double RUN = 1.0;
	/** Keeping pace while it stalks. */
	private static final double PROWL = 0.55;
	/** Burning, it staggers slower than a walking player. */
	private static final double STAGGER = 0.6;
	/** A hunt goes round to a point this far behind the player before it darts in. */
	private static final double FLANK_DISTANCE = 3.5;
	/** The player's view cone, half its angle in degrees, that a hunting wight keeps out of. */
	private static final float VIEW_HALF_ANGLE = 70.0F;
	private static final int APPROACH_TIMEOUT = 300;
	private static final int STRIKE_TIMEOUT = 100;
	private static final int FLEE_TIME = 80;
	private static final int WATER_SEARCH_RADIUS = 12;

	private final Wight wight;
	private Phase phase = Phase.STALK;
	private boolean hunting;
	private int hitsLeft;
	private int hitTimer;
	private int phaseTicks;
	private int repath;
	private float offset;
	private @Nullable BlockPos water;
	private boolean burning;

	WightStalkGoal(Wight wight) {
		this.wight = wight;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		return !wight.summoned() && (wight.isOnFire() || wight.stalked() != null);
	}

	@Override
	public boolean canContinueToUse() {
		return canUse();
	}

	@Override
	public void start() {
		setPhase(Phase.STALK);
	}

	@Override
	public void stop() {
		wight.getNavigation().stop();
		setPhase(Phase.STALK);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	private void setPhase(Phase next) {
		phase = next;
		phaseTicks = 0;
		repath = 0;
	}

	@Override
	public void tick() {
		if (!(wight.level() instanceof ServerLevel level)) {
			return;
		}
		if (wight.isOnFire()) {
			burning = true;
			stagger(level);
			return;
		}
		if (burning) {
			// The fire is out: away from the player before anything else.
			burning = false;
			water = null;
			setPhase(Phase.FLEE);
		}
		Player player = wight.stalked();
		if (player == null) {
			return;
		}
		if (wight.takeFleeRequest() && phase != Phase.FLEE) {
			setPhase(Phase.FLEE);
		}
		phaseTicks++;
		repath--;
		wight.getLookControl().setLookAt(player, 30.0F, 30.0F);
		double distance = Math.sqrt(wight.distanceToSqr(player));
		WightSettings settings = Wight.settings();
		switch (phase) {
			case STALK -> stalk(player, distance, settings);
			case APPROACH -> approach(player, distance, settings);
			case STRIKE -> strike(level, player, distance, settings);
			case FLEE -> flee(player, distance, settings);
		}
	}

	private void stalk(Player player, double distance, WightSettings settings) {
		if (wight.tickCount % 20 == 0) {
			boolean weak = WightLogic.weak(player.getHealth(), player.getMaxHealth(), settings.huntHealth());
			Plan plan = WightLogic.plan(weak, Wight.busy(player), wight.getSensing().hasLineOfSight(player), wight.raidCooldown(),
					wight.huntCooldown(), wight.getRandom().nextFloat(), settings.raidChance());
			if (plan != Plan.STALK) {
				hunting = plan == Plan.HUNT;
				hitsLeft = hunting ? settings.huntHits() : settings.raidHits();
				setPhase(Phase.APPROACH);
				return;
			}
		}
		if (repath <= 0 || wight.getNavigation().isDone()) {
			repath = 30 + wight.getRandom().nextInt(30);
			if (wight.getRandom().nextInt(3) == 0) {
				offset = (wight.getRandom().nextFloat() - 0.5F) * 120.0F;
			}
			Point spot = WightLogic.behind(player.getX(), player.getZ(), player.getYRot(), settings.stalkDistance(), offset);
			double keep = settings.stalkDistance();
			double speed = distance > keep + 6.0 ? RUN : PROWL;
			moveTo(spot, player, speed);
		}
	}

	private void approach(Player player, double distance, WightSettings settings) {
		if (phaseTicks > APPROACH_TIMEOUT) {
			setPhase(Phase.FLEE);
			return;
		}
		if (distance <= settings.reach()) {
			hitTimer = 0;
			setPhase(Phase.STRIKE);
			return;
		}
		if (repath > 0 && !wight.getNavigation().isDone()) {
			return;
		}
		repath = 5;
		if (hunting && distance > FLANK_DISTANCE + 1.0
				&& !WightLogic.outOfSight(player.getX(), player.getZ(), player.getYRot(), wight.getX(), wight.getZ(), VIEW_HALF_ANGLE)) {
			// Round to their back first.
			Point spot = WightLogic.behind(player.getX(), player.getZ(), player.getYRot(), FLANK_DISTANCE, offset * 0.25F);
			moveTo(spot, player, RUN);
		} else {
			wight.getNavigation().moveTo(player, RUN);
		}
	}

	private void strike(ServerLevel level, Player player, double distance, WightSettings settings) {
		if (phaseTicks > STRIKE_TIMEOUT) {
			endPass();
			return;
		}
		if (distance > settings.reach()) {
			if (repath <= 0) {
				repath = 4;
				wight.getNavigation().moveTo(player, RUN);
			}
		} else {
			wight.getNavigation().stop();
		}
		if (--hitTimer <= 0 && distance <= settings.reach() + 0.3 && wight.getSensing().hasLineOfSight(player)) {
			wight.strike(level, player);
			hitTimer = settings.hitInterval();
			// A raid is a couple of blows, not a kill: it backs off once the player is weak or one more blow would finish
			// them, and leaves the killing to a hunt.
			if (--hitsLeft <= 0 || !hunting && raidShouldStop(player, settings)) {
				endPass();
			}
		}
	}

	private boolean raidShouldStop(Player player, WightSettings settings) {
		return WightLogic.weak(player.getHealth(), player.getMaxHealth(), settings.huntHealth())
				|| player.getHealth() <= wight.getAttributeValue(Attributes.ATTACK_DAMAGE);
	}

	private void endPass() {
		if (hunting) {
			wight.huntPassDone();
		} else {
			wight.raided();
		}
		setPhase(Phase.FLEE);
	}

	private void flee(Player player, double distance, WightSettings settings) {
		if (phaseTicks > FLEE_TIME || distance >= settings.stalkDistance() * 0.8) {
			offset = (wight.getRandom().nextFloat() - 0.5F) * 120.0F;
			setPhase(Phase.STALK);
			return;
		}
		if (repath <= 0 || wight.getNavigation().isDone()) {
			repath = 10;
			Vec3 away = wight.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
			away = away.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, wight.getYRot() + 180.0F) : away.normalize();
			away = away.yRot((wight.getRandom().nextFloat() - 0.5F) * 1.0F);
			double run = settings.stalkDistance();
			moveTo(new Point(player.getX() + away.x * run, player.getZ() + away.z * run), player, RUN);
		}
	}

	/** On fire: to the nearest water if there is some, else about at random; slow enough to be caught. */
	private void stagger(ServerLevel level) {
		if (water == null || wight.tickCount % 20 == 0) {
			water = findWater(level);
		}
		if (water != null) {
			if (repath-- <= 0 || wight.getNavigation().isDone()) {
				repath = 10;
				wight.getNavigation().moveTo(water.getX() + 0.5, water.getY(), water.getZ() + 0.5, STAGGER);
			}
		} else if (wight.getNavigation().isDone()) {
			Vec3 pos = LandRandomPos.getPos(wight, 6, 3);
			if (pos != null) {
				wight.getNavigation().moveTo(pos.x, pos.y, pos.z, STAGGER);
			}
		}
	}

	private @Nullable BlockPos findWater(ServerLevel level) {
		BlockPos origin = wight.blockPosition();
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-WATER_SEARCH_RADIUS, -3, -WATER_SEARCH_RADIUS),
				origin.offset(WATER_SEARCH_RADIUS, 2, WATER_SEARCH_RADIUS))) {
			if (level.getFluidState(pos).is(FluidTags.WATER)) {
				double d = pos.distSqr(origin);
				if (d < bestDistance) {
					bestDistance = d;
					best = pos.immutable();
				}
			}
		}
		return best;
	}

	/** Walks to the ground at {@code spot}, or to the player's height where the ground there is far above or below them. */
	private void moveTo(Point spot, Player player, double speed) {
		int ground = wight.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(spot.x()), (int) Math.floor(spot.z()));
		double y = Math.abs(ground - player.getY()) > 6.0 ? player.getY() : ground;
		wight.getNavigation().moveTo(spot.x(), y, spot.z(), speed);
	}
}
