package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.mixin.mobs.vanilla.MobAccessor;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ProjectileWeaponItem;

/**
 * A grown villager given a weapon and armor becomes a guard: he no longer flees from monsters but fights them, with
 * his weapon or, given a bow or crossbow, from a distance. While he fights, the vanilla villager brain (work, panic,
 * sleep) waits. He never turns on players, villagers or golems. Numbers are in {@code data/eam/villagers/guards.json}.
 */
public final class VillagerGuardModule {
	private static volatile GuardSettings settings = GuardSettings.DEFAULT;
	private static boolean enabled;

	private VillagerGuardModule() {
	}

	/** Settings load whatever the config says, so the data is always valid. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(GuardSettingsLoader.ID, new GuardSettingsLoader());
	}

	public static void init() {
		enabled = true;
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Villager villager) addGoals(villager);
		});
	}

	static GuardSettings settings() {
		return settings;
	}

	static void setSettings(GuardSettings value) {
		settings = value;
	}

	/** A grown villager with a weapon in his hand and any armor on. */
	public static boolean isGuard(Villager villager) {
		if (!enabled) return false;
		int armor = 0;
		for (EquipmentSlot slot : VillagerGearContainer.ARMOR) {
			if (!villager.getItemBySlot(slot).isEmpty()) armor++;
		}
		return VillagerGuardLogic.isGuard(!villager.isBaby(), VillagerGearModule.realHand(villager).is(VillagerGearModule.WEAPONS), armor);
	}

	static boolean hasBow(Villager villager) {
		return VillagerGearModule.realHand(villager).getItem() instanceof ProjectileWeaponItem;
	}

	/**
	 * Whether the vanilla villager brain runs this tick: not while a guard fights. A sleeping guard with a target wakes
	 * up, and a villager who is no longer a guard drops his target.
	 */
	public static boolean thinks(Villager villager) {
		if (!enabled) return true;
		LivingEntity target = villager.getTarget();
		if (target == null) return true;
		if (!isGuard(villager) || !target.isAlive()) {
			villager.setTarget(null);
			return true;
		}
		if (villager.isSleeping()) villager.stopSleeping();
		return false;
	}

	/** Whether a hurt villager or one near a monster panics: a guard does not. */
	public static boolean panics(Villager villager, boolean vanilla) {
		return vanilla && !isGuard(villager);
	}

	private static void addGoals(Villager villager) {
		GoalSelector goals = ((MobAccessor) villager).eam$goalSelector();
		if (goals.getAvailableGoals().stream().anyMatch(wrapped -> wrapped.getGoal() instanceof GuardMeleeGoal)) return;
		goals.addGoal(1, new GuardBowGoal(villager));
		goals.addGoal(1, new GuardMeleeGoal(villager));
		GoalSelector targets = ((MobAccessor) villager).eam$targetSelector();
		targets.addGoal(1, new HurtByTargetGoal(villager, Player.class, AbstractVillager.class, IronGolem.class) {
			@Override
			public boolean canUse() {
				return isGuard(villager) && villager.getLastHurtByMob() instanceof Enemy && super.canUse();
			}
		});
		targets.addGoal(2, new NearestAttackableTargetGoal<>(villager, Mob.class, 5, true, false,
				(target, level) -> target instanceof Enemy && !(target instanceof Creeper)) {
			@Override
			public boolean canUse() {
				return isGuard(villager) && super.canUse();
			}
		});
	}
}
