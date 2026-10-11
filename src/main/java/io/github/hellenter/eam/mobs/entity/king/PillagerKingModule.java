package io.github.hellenter.eam.mobs.entity.king;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.mobs.entity.boss.BossParry;

import io.github.hellenter.eam.mixin.mobs.vanilla.MobAccessor;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

/**
 * The Pillager King, the first boss: an armored giant with two axes. Not placed in the world yet: spawn him with the
 * spawn egg or {@code /summon eam:pillager_king}. Numbers are in {@code data/eam/mobs/pillager_king.json}.
 */
public final class PillagerKingModule {
	/** The blow of both axes at the end of his onslaught: a shield only softens it (by hand, not as a block). */
	public static final ResourceKey<DamageType> ONSLAUGHT_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Eam.id("pillager_king_onslaught"));

	/** Monsters that go for the king when they see him, as they would for a player he outranks for them. */
	public static final TagKey<EntityType<?>> ATTACKERS = TagKey.create(Registries.ENTITY_TYPE, Eam.id("attacks_pillager_king"));
	/** After their goals for players and villagers: a player still comes first. */
	private static final int ATTACK_PRIORITY = 4;

	private static volatile PillagerKingSettings settings = PillagerKingSettings.DEFAULT;

	private PillagerKingModule() {
	}

	public static void register() {
		BossParry.register();
		FabricDefaultAttributeRegistry.register(EamEntities.PILLAGER_KING, PillagerKing.createAttributes());
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(PillagerKingSettingsLoader.ID, new PillagerKingSettingsLoader());
	}

	/** Undead and other monsters attack the king; he fights back at whoever hits him. */
	public static void init() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Mob mob && mob.is(ATTACKERS)) {
				addTargetGoal(mob);
			}
		});
	}

	private static void addTargetGoal(Mob mob) {
		GoalSelector targets = ((MobAccessor) mob).eam$targetSelector();
		for (WrappedGoal wrapped : targets.getAvailableGoals()) {
			if (wrapped.getGoal() instanceof AttackKingGoal) {
				return; // Already added: the same entity loaded again.
			}
		}
		targets.addGoal(ATTACK_PRIORITY, new AttackKingGoal(mob));
	}

	private static final class AttackKingGoal extends NearestAttackableTargetGoal<PillagerKing> {
		AttackKingGoal(Mob mob) {
			super(mob, PillagerKing.class, true);
		}
	}

	public static PillagerKingSettings settings() {
		return settings;
	}

	static void setSettings(PillagerKingSettings value) {
		settings = value;
	}
}
