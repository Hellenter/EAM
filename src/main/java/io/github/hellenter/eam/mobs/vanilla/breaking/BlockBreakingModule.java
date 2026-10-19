package io.github.hellenter.eam.mobs.vanilla.breaking;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mixin.mobs.vanilla.MobAccessor;
import io.github.hellenter.eam.mobs.vanilla.noise.NoiseModule;
import io.github.hellenter.eam.world.mining.HandBreakingModule;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Melee mobs in {@link #BREAKERS} break through blocks a player could break bare-handed ({@link #HAND_BREAKABLE}) to
 * reach a target that walled itself in; {@link #STONE_BREAKERS} also break {@link #STONE}. Respects the {@code mob_griefing} game rule. Numbers are in
 * {@code data/eam/mobs/block_breaking.json}.
 */
public final class BlockBreakingModule {
	/** Mobs that break blocks to reach their target. */
	public static final TagKey<EntityType<?>> BREAKERS = TagKey.create(Registries.ENTITY_TYPE, Eam.id("breaks_blocks"));
	/** Mobs that also dig through stone ({@link #STONE}), with their picks: the duergar renegades. */
	public static final TagKey<EntityType<?>> STONE_BREAKERS = TagKey.create(Registries.ENTITY_TYPE, Eam.id("breaks_stone"));
	/** The rock {@link #STONE_BREAKERS} dig through. */
	public static final TagKey<Block> STONE = TagKey.create(Registries.BLOCK, Eam.id("mob_breakable_stone"));
	/** Targets mobs dig toward: players. A datapack can add villagers and other mobs. */
	public static final TagKey<EntityType<?>> TARGETS = TagKey.create(Registries.ENTITY_TYPE, Eam.id("block_breaking_targets"));
	/** Raiders of a raid also dig toward these, villagers hiding in their houses. */
	public static final TagKey<EntityType<?>> RAID_TARGETS = TagKey.create(Registries.ENTITY_TYPE, Eam.id("raid_breaking_targets"));
	/** Mobs break the same blocks a player can break bare-handed. */
	public static final TagKey<Block> HAND_BREAKABLE = HandBreakingModule.HAND_BREAKABLE;

	private static final int GOAL_PRIORITY = 1;
	/** Same priority as the vanilla "attack the nearest player" target goal. */
	private static final int SENSE_PRIORITY = 2;
	private static final int DECAY_INTERVAL = 20;
	/** Set {@code -Deam.debug.blockBreaking=true} to log how much route planning costs. */
	private static final boolean DEBUG = Boolean.getBoolean("eam.debug.blockBreaking");

	private static volatile BlockBreakingSettings settings = BlockBreakingSettings.DEFAULT;
	private static BreachPlanner planner = planner(BlockBreakingSettings.DEFAULT);

	private static long plans;
	private static long expandedNodes;
	private static long planNanos;
	private static long maxPlanNanos;

	private BlockBreakingModule() {
	}

	/** Settings load whatever the config says, so the data is always valid. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(BlockBreakingSettingsLoader.ID, new BlockBreakingSettingsLoader());
	}

	public static void init() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Mob mob && mob.is(BREAKERS)) {
				addGoal(mob);
			}
		});
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			long time = level.getGameTime();
			if (time % DECAY_INTERVAL == 0) {
				BlockDamage.tick(level, settings, DECAY_INTERVAL);
			}
			if (DEBUG && time % 200 == 0 && level.dimension() == Level.OVERWORLD) {
				logStats();
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> BlockDamage.clear());
	}

	private static void addGoal(Mob mob) {
		GoalSelector goals = ((MobAccessor) mob).eam$goalSelector();
		for (WrappedGoal wrapped : goals.getAvailableGoals()) {
			if (wrapped.getGoal() instanceof BreakThroughGoal) {
				return; // Already added: the same entity loaded again.
			}
		}
		GoalSelector targets = ((MobAccessor) mob).eam$targetSelector();
		targets.addGoal(SENSE_PRIORITY, new SensePlayerGoal(mob));
		// Before the target goals are collected: a breaker digging toward a player it heard must not forget him.
		NoiseModule.addGoal(mob);
		List<TargetGoal> targetGoals = targets.getAvailableGoals().stream()
				.map(WrappedGoal::getGoal)
				.filter(TargetGoal.class::isInstance)
				.map(TargetGoal.class::cast)
				.toList();
		for (TargetGoal goal : targetGoals) {
			goal.setUnseenMemoryTicks(settings.targetMemoryTicks());
		}
		goals.addGoal(GOAL_PRIORITY, new BreakThroughGoal(mob, targetGoals));
	}

	public static BlockBreakingSettings settings() {
		return settings;
	}

	static BreachPlanner planner() {
		return planner;
	}

	static void setSettings(BlockBreakingSettings value) {
		settings = value;
		planner = planner(value);
	}

	private static BreachPlanner planner(BlockBreakingSettings value) {
		// Walking a block takes a zombie about 5 ticks. A block of distance to the target is estimated like breaking
		// half a dirt block, so the search heads for the target instead of flooding every open cell nearby.
		return new BreachPlanner(value.maxNodes(), value.searchRadius(), 5, value.ticksPerHardness() / 4);
	}

	static void recordPlan(int expanded, long nanos) {
		plans++;
		expandedNodes += expanded;
		planNanos += nanos;
		maxPlanNanos = Math.max(maxPlanNanos, nanos);
	}

	private static void logStats() {
		if (plans > 0) {
			Eam.LOGGER.info("Block breaking: {} plans in 10 s, avg {} nodes, avg {} us, max {} us, {} ms/tick",
					plans, expandedNodes / plans, planNanos / plans / 1000, maxPlanNanos / 1000,
					String.format("%.3f", planNanos / 200.0 / 1_000_000.0));
		}
		plans = 0;
		expandedNodes = 0;
		planNanos = 0;
		maxPlanNanos = 0;
	}
}
