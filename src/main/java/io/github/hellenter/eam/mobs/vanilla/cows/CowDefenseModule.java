package io.github.hellenter.eam.mobs.vanilla.cows;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mixin.mobs.vanilla.MobAccessor;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

/**
 * Semi-passive animals in {@link #DEFENDERS}: a grown one hit by a player hits back once and backs off, then flees
 * like in vanilla. Calves only flee. Numbers are in {@code data/eam/mobs/cow_defense.json}.
 */
public final class CowDefenseModule {
	/** Animals that fight back when a player hits them. */
	public static final TagKey<EntityType<?>> DEFENDERS = TagKey.create(Registries.ENTITY_TYPE, Eam.id("defends_itself"));

	private static volatile CowDefenseSettings settings = CowDefenseSettings.DEFAULT;

	private CowDefenseModule() {
	}

	/** Settings load whatever the config says, so the data is always valid. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(CowDefenseSettingsLoader.ID, new CowDefenseSettingsLoader());
	}

	public static void init() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof PathfinderMob mob && mob.is(DEFENDERS)) {
				addGoal(mob);
			}
		});
	}

	private static void addGoal(PathfinderMob mob) {
		GoalSelector goals = ((MobAccessor) mob).eam$goalSelector();
		for (WrappedGoal wrapped : goals.getAvailableGoals()) {
			if (wrapped.getGoal() instanceof CowDefenseGoal) {
				return; // Already added: the same entity loaded again.
			}
		}
		// Ahead of the vanilla panic goal (priority 1); swimming (0) uses other flags.
		goals.addGoal(0, new CowDefenseGoal(mob));
	}

	static CowDefenseSettings settings() {
		return settings;
	}

	static void setSettings(CowDefenseSettings value) {
		settings = value;
	}
}
