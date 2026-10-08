package io.github.hellenter.eam.mobs.vanilla.breaking;

import io.github.hellenter.eam.mixin.mobs.vanilla.TargetGoalAccessor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * A mob whose target is walled in breaks its way through. Every {@code replan_ticks} it plans the cheapest route
 * through the blocks with {@link BreachPlanner}, walks to the first block in the way and hits it until the shared
 * {@link BlockDamage} breaks it. It only starts when vanilla pathfinding cannot reach the target, and stops as soon
 * as the route is open so the vanilla attack goal takes over.
 */
final class BreakThroughGoal extends Goal {
	private static final double REACH_SQR = 2.5 * 2.5;
	private static final double CLOSE_SQR = 3.0 * 3.0;
	private static final int SWING_INTERVAL = 20;
	private static final int STUCK_TICKS = 100;
	private static final int BLOCKED_BACKOFF = 4;

	private final Mob mob;
	private final List<TargetGoal> targetGoals;
	/** Game time of the next check whether the target is walled in. */
	private long nextCheck;
	private int replanIn;
	private int stuckTicks;
	private int workTicks;
	private LivingEntity target;
	private BreachPlanner.Plan plan;
	/** Where the mob stands to break the blocks of {@link #work}. */
	private BlockPos standAt;
	private long[] work;
	/** The last plan found no way to the target at all. */
	private boolean lastPlanBlocked;
	/** The mob was hitting a block last tick. */
	private boolean hitting;

	BreakThroughGoal(Mob mob, List<TargetGoal> targetGoals) {
		this.mob = mob;
		this.targetGoals = targetGoals;
		// Spread the checks of many mobs over different ticks.
		this.nextCheck = mob.getRandom().nextInt(BlockBreakingModule.settings().replanTicks());
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		long now = mob.level().getGameTime();
		if (now < nextCheck) {
			return false;
		}
		int interval = BlockBreakingModule.settings().replanTicks();
		nextCheck = now + interval;
		if (!validTarget(mob.getTarget()) || targetReachable()) {
			return false;
		}
		target = mob.getTarget();
		if (replan()) {
			return true;
		}
		target = null;
		if (plan == null && lastPlanBlocked) {
			// Walled in by blocks mobs cannot break: the search used its whole budget, so wait longer.
			nextCheck = now + (long) interval * BLOCKED_BACKOFF;
		}
		return false;
	}

	@Override
	public boolean canContinueToUse() {
		return plan != null && mob.getTarget() == target && validTarget(target) && !archerSees(target);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void start() {
		hitting = false;
		replanIn = BlockBreakingModule.settings().replanTicks();
		stuckTicks = 0;
		workTicks = 0;
	}

	@Override
	public void stop() {
		plan = null;
		work = null;
		standAt = null;
		target = null;
		mob.getNavigation().stop();
		nextCheck = mob.level().getGameTime() + BlockBreakingModule.settings().replanTicks();
	}

	@Override
	public void tick() {
		// The walls hide the target; digging toward it must not make the mob forget it.
		for (TargetGoal goal : targetGoals) {
			((TargetGoalAccessor) goal).eam$setUnseenTicks(0);
		}
		// While the mob is hitting a block the route stays the same: re-plan once it breaks or the mob gets stuck.
		if (--replanIn <= 0 && !hitting || stuckTicks > STUCK_TICKS) {
			replanIn = BlockBreakingModule.settings().replanTicks();
			stuckTicks = 0;
			if (!replan()) {
				return;
			}
		}

		ServerLevel level = getServerLevel(mob);
		BlockPos pos = nextBlock(level);
		if (pos == null) {
			// This step is open now: plan the next one.
			if (!replan()) {
				return;
			}
			pos = nextBlock(level);
			if (pos == null) {
				plan = null; // Only the block under the mob is left: let vanilla movement take over.
				return;
			}
		}

		hitting = false;
		if (mob.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > REACH_SQR) {
			stuckTicks++;
			if (mob.getNavigation().isDone()) {
				mob.getNavigation().moveTo(standAt.getX() + 0.5, standAt.getY(), standAt.getZ() + 0.5, 1.0);
			}
			return;
		}

		mob.getNavigation().stop();
		mob.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
		BlockState state = level.getBlockState(pos);
		if (workTicks++ % SWING_INTERVAL == 0) {
			mob.swingForAttack(InteractionHand.MAIN_HAND);
			SoundType sound = state.getSoundType();
			level.playSound(null, pos, sound.getHitSound(), SoundSource.HOSTILE, sound.getVolume(), sound.getPitch() * 0.8F);
		}
		int total = breakTicks(level, pos, state);
		if (total < 0) {
			replanIn = 0; // Someone put an unbreakable block here.
			return;
		}
		stuckTicks = 0;
		hitting = !BlockDamage.work(level, pos, state, total, mob);
	}

	/** The next block of the current step that still stands, or null when the step is open. */
	private BlockPos nextBlock(ServerLevel level) {
		if (work == null) {
			return null;
		}
		BlockPos below = mob.blockPosition().below();
		for (long cell : work) {
			BlockPos pos = BlockPos.of(cell);
			if (pos.equals(below)) {
				continue; // Never break the block the mob stands on.
			}
			if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
				return pos;
			}
		}
		return null;
	}

	/** Plans a route to the target; false (and the goal ends) when there is nothing to break on it. */
	private boolean replan() {
		LivingEntity goal = target != null ? target : mob.getTarget();
		if (goal == null) {
			return false;
		}
		ServerLevel level = getServerLevel(mob);
		BlockBreakingSettings settings = BlockBreakingModule.settings();
		BreachPlanner planner = BlockBreakingModule.planner();
		BlockPos from = mob.blockPosition();
		BlockPos to = goal.blockPosition();
		int height = Math.max(1, Mth.ceil(mob.getBbHeight()));
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

		long started = System.nanoTime();
		plan = planner.plan((x, y, z) -> cell(level, cursor.set(x, y, z), settings), from.getX(), from.getY(), from.getZ(),
				to.getX(), to.getY(), to.getZ(), height);
		BlockBreakingModule.recordPlan(plan.expanded(), System.nanoTime() - started);

		int index = plan.firstBreakingStep();
		lastPlanBlocked = index < 0 && !plan.reaches();
		if (index < 0) {
			plan = null;
			work = null;
			return false;
		}
		work = plan.steps().get(index).toBreak();
		standAt = index == 0 ? from : BlockPos.of(plan.steps().get(index - 1).feet());
		return true;
	}

	/** How the planner sees one cell. */
	private static int cell(ServerLevel level, BlockPos pos, BlockBreakingSettings settings) {
		if (!level.isLoaded(pos)) {
			return BreachPlanner.BLOCKED;
		}
		BlockState state = level.getBlockState(pos);
		if (state.isAir()) {
			return BreachPlanner.AIR;
		}
		if (state.getFluidState().is(FluidTags.LAVA) || state.is(BlockTags.FIRE)) {
			return BreachPlanner.BLOCKED;
		}
		if (state.getCollisionShape(level, pos).isEmpty()) {
			return BreachPlanner.AIR;
		}
		int total = breakTicks(level, pos, state);
		if (total < 0) {
			return BreachPlanner.SOLID;
		}
		return Math.max(1, total - (int) BlockDamage.done(level, pos.asLong()));
	}

	private static int breakTicks(ServerLevel level, BlockPos pos, BlockState state) {
		return BlockBreakingLogic.breakTicks(state.getDestroySpeed(level, pos), state.is(BlockBreakingModule.HAND_BREAKABLE),
				BlockBreakingModule.settings().ticksPerHardness());
	}

	private boolean validTarget(LivingEntity entity) {
		if (entity == null || !entity.isAlive() || entity.level() != mob.level() || !entity.is(BlockBreakingModule.TARGETS)) {
			return false;
		}
		ServerLevel level = getServerLevel(mob);
		if (!level.getGameRules().get(GameRules.MOB_GRIEFING)) {
			return false;
		}
		int radius = BlockBreakingModule.settings().searchRadius();
		return mob.distanceToSqr(entity) <= (double) radius * radius;
	}

	/** An archer that sees its target (skeletons see through leaves) shoots instead of digging. */
	private boolean archerSees(LivingEntity goal) {
		return mob instanceof RangedAttackMob && mob.getSensing().hasLineOfSight(goal);
	}

	/** Vanilla pathfinding already gets there: nothing to break. */
	private boolean targetReachable() {
		LivingEntity goal = mob.getTarget();
		boolean seen = mob.getSensing().hasLineOfSight(goal);
		if (archerSees(goal)) {
			return true;
		}
		if (mob.distanceToSqr(goal) < CLOSE_SQR) {
			// Right next to the target the path always "reaches" it, but a block may still be in the way of the
			// attack (a roof over a player in a hole): only line of sight tells.
			return seen;
		}
		Path path = mob.getNavigation().getPath();
		return path != null && path.canReach();
	}
}
