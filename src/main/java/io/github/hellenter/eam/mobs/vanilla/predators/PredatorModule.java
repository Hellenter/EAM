package io.github.hellenter.eam.mobs.vanilla.predators;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.feeder.FeederModule;
import io.github.hellenter.eam.mixin.mobs.vanilla.MobAccessor;
import io.github.hellenter.eam.world.bluemoon.BlueMoonModule;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.pathfinder.Path;

/**
 * Predators raid livestock ({@code eam:livestock}) every night. Wild wolves ({@link #HUNTERS}) and warg packs go for any
 * animal they can walk up to; zombies ({@link #RAIDERS}) only for one in an open pen that is dark. So a pen needs a
 * whole fence with a shut gate, and against zombies light inside as well. Numbers are in
 * {@code data/eam/mobs/predators.json}.
 */
public final class PredatorModule {
	/** Mobs that hunt livestock every night wherever they can reach it. Wargs hunt as a pack in their own code. */
	public static final TagKey<EntityType<?>> HUNTERS = TagKey.create(Registries.ENTITY_TYPE, Eam.id("livestock_hunters"));
	/** Mobs that go for livestock at night only in an open pen with little light. */
	public static final TagKey<EntityType<?>> RAIDERS = TagKey.create(Registries.ENTITY_TYPE, Eam.id("pen_raiders"));
	/** After the goals for players and villagers; with the vanilla wolf's own prey goal and the blood moon's hunt. */
	private static final int TARGET_PRIORITY = 4;
	/** A path that ends this close (in blocks, along the grid) to the prey counts as reaching it; a fence takes two. */
	private static final int REACH = 1;

	private static volatile PredatorSettings settings = PredatorSettings.DEFAULT;
	private static boolean enabled;

	private PredatorModule() {
	}

	/** Settings load whatever the config says, so the data is always valid. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(PredatorSettingsLoader.ID, new PredatorSettingsLoader());
	}

	public static void init() {
		enabled = true;
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Mob mob) {
				if (mob.is(HUNTERS)) {
					addGoal(mob, false);
				} else if (mob.is(RAIDERS)) {
					addGoal(mob, true);
				}
			}
		});
	}

	static PredatorSettings settings() {
		return settings;
	}

	static void setSettings(PredatorSettings value) {
		settings = value;
	}

	static PredatorSettings.Group group(boolean raider) {
		return raider ? settings.raiders() : settings.hunters();
	}

	/** Whether {@code mob} is out hunting now: at night but not under a blue moon, and a tame wolf never. */
	public static boolean huntsNow(Mob mob) {
		return enabled && mob.level().isDarkOutside() && !BlueMoonModule.activeIn(mob.level())
				&& !(mob instanceof TamableAnimal tame && tame.isTame());
	}

	/** Whether {@code target} is livestock a predator of this kind would go for where it stands now. */
	public static boolean isPrey(LivingEntity target, boolean raider) {
		return target.is(FeederModule.LIVESTOCK) && target.isAlive() && PredatorLogic.mayHunt(target.level().isDarkOutside(),
				target.level().getBrightness(LightLayer.BLOCK, target.blockPosition()), group(raider));
	}

	/** How far a pack of wargs looks for livestock. */
	public static double hunterRange() {
		return settings.hunters().range();
	}

	/** Whether {@code mob} can walk right up to {@code target}: a fence or a shut gate between them says no. */
	public static boolean reachable(Mob mob, LivingEntity target) {
		Path path = mob.getNavigation().createPath(target, REACH);
		return path != null && path.canReach();
	}

	private static void addGoal(Mob mob, boolean raider) {
		GoalSelector targets = ((MobAccessor) mob).eam$targetSelector();
		for (WrappedGoal wrapped : targets.getAvailableGoals()) {
			if (wrapped.getGoal() instanceof LivestockHuntGoal) {
				return; // Already added: the same entity loaded again.
			}
		}
		targets.addGoal(TARGET_PRIORITY, new LivestockHuntGoal(mob, raider));
	}
}
