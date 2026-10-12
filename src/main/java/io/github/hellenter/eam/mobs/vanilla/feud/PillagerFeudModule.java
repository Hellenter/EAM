package io.github.hellenter.eam.mobs.vanilla.feud;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mixin.mobs.vanilla.MobAccessor;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

/**
 * Monsters and pillagers are at war: monsters in {@link #ATTACKERS} go for any mob in {@link #PILLAGERS} they see,
 * and pillagers go for those monsters. Both only after their goals for players and villagers, so raids and patrols
 * keep hunting players first.
 */
public final class PillagerFeudModule {
	/** The pillager family: pillagers, vindicators, guards, elite crossbowmen and the Pillager King. */
	public static final TagKey<EntityType<?>> PILLAGERS = TagKey.create(Registries.ENTITY_TYPE, Eam.id("pillagers"));
	/** Monsters that go for pillagers, and that pillagers go for. */
	public static final TagKey<EntityType<?>> ATTACKERS = TagKey.create(Registries.ENTITY_TYPE, Eam.id("attacks_pillagers"));
	/** After their goals for players (2) and villagers and golems (3): a player still comes first. */
	public static final int TARGET_PRIORITY = 4;

	private PillagerFeudModule() {
	}

	public static void init() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (!(entity instanceof Mob mob)) {
				return;
			}
			if (mob.is(ATTACKERS)) {
				addTargetGoal(mob, PILLAGERS);
			} else if (mob.is(PILLAGERS)) {
				addTargetGoal(mob, ATTACKERS);
			}
		});
	}

	private static void addTargetGoal(Mob mob, TagKey<EntityType<?>> enemies) {
		GoalSelector targets = ((MobAccessor) mob).eam$targetSelector();
		for (WrappedGoal wrapped : targets.getAvailableGoals()) {
			if (wrapped.getGoal() instanceof FeudTargetGoal) {
				return; // Already added: the same entity loaded again.
			}
		}
		targets.addGoal(TARGET_PRIORITY, new FeudTargetGoal(mob, enemies));
	}

	/** The nearest visible enemy of the other side; raid allies (a raid witch and her pillagers) leave each other alone. */
	public static final class FeudTargetGoal extends NearestAttackableTargetGoal<Mob> {
		public FeudTargetGoal(Mob mob, TagKey<EntityType<?>> enemies) {
			super(mob, Mob.class, true, (target, level) -> PillagerFeudLogic.isEnemy(mob, target, enemies));
		}
	}
}
