package io.github.hellenter.eam.mobs.vanilla.running;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mixin.mobs.vanilla.MobAccessor;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

/**
 * Melee hostile mobs in {@link #RUNNERS} run while they chase a target: faster than vanilla, still slower than a walking
 * player. Numbers are in {@code data/eam/mobs/running.json}.
 */
public final class RunningModule {
	/** Mobs that speed up while chasing a target. */
	public static final TagKey<EntityType<?>> RUNNERS = TagKey.create(Registries.ENTITY_TYPE, Eam.id("runs_when_chasing"));

	private static volatile RunningSettings settings = RunningSettings.DEFAULT;

	private RunningModule() {
	}

	/** Settings load whatever the config says, so the data is always valid. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(RunningSettingsLoader.ID, new RunningSettingsLoader());
	}

	public static void init() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Mob mob && mob.is(RUNNERS)) {
				addGoal(mob);
			}
		});
	}

	private static void addGoal(Mob mob) {
		GoalSelector goals = ((MobAccessor) mob).eam$goalSelector();
		for (WrappedGoal wrapped : goals.getAvailableGoals()) {
			if (wrapped.getGoal() instanceof RunWhenChasingGoal) {
				return; // Already added: the same entity loaded again.
			}
		}
		goals.addGoal(0, new RunWhenChasingGoal(mob));
	}

	static RunningSettings settings() {
		return settings;
	}

	static void setSettings(RunningSettings value) {
		settings = value;
	}
}
