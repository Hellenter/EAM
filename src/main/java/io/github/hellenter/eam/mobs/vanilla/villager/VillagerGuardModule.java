package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mixin.mobs.vanilla.MobAccessor;

import com.google.common.collect.ImmutableSet;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ProjectileWeaponItem;

/**
 * A grown villager given a weapon and armor becomes a guard: he no longer flees from monsters but fights them, with
 * his weapon or, given a bow or crossbow, from a distance. While he fights, the vanilla villager brain (work, panic,
 * sleep) waits. He never turns on players, villagers or golems. Numbers are in {@code data/eam/villagers/guards.json}.
 * <p>
 * Guarding is a profession of its own, {@code eam:guard}, with no job site and no trades. A jobless villager armed as a
 * guard takes it instead of looking for a job, and a player can make any grown villager a guard with the button in his
 * inventory. A guard of the profession fights with a weapon alone, armor or not.
 */
public final class VillagerGuardModule {
	public static final ResourceKey<VillagerProfession> GUARD = ResourceKey.create(Registries.VILLAGER_PROFESSION, Eam.id("guard"));
	private static final int PROFESSION_CHECK_TICKS = 20;
	private static final byte HAPPY_PARTICLES = 14;

	private static volatile GuardSettings settings = GuardSettings.DEFAULT;
	private static boolean enabled;

	private VillagerGuardModule() {
	}

	/** Settings, the profession and the payload register whatever the config says, so client and server always match. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(GuardSettingsLoader.ID, new GuardSettingsLoader());
		Registry.register(BuiltInRegistries.VILLAGER_PROFESSION, GUARD, new VillagerProfession(
				Component.translatable("entity." + GUARD.identifier().getNamespace() + ".villager." + GUARD.identifier().getPath()),
				poi -> false, poi -> false, ImmutableSet.of(), ImmutableSet.of(), null, new Int2ObjectOpenHashMap<>()));
		PayloadTypeRegistry.serverboundPlay().register(VillagerGuardPayload.TYPE, VillagerGuardPayload.STREAM_CODEC);
	}

	public static void init() {
		enabled = true;
		ServerPlayNetworking.registerGlobalReceiver(VillagerGuardPayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof VillagerGearMenu menu && menu.villager() != null
					&& !menu.villager().isBaby() && !isGuardProfession(menu.villager())) {
				makeGuard(menu.villager(), context.player().level());
			}
		});
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
		return VillagerGuardLogic.isGuard(!villager.isBaby(), VillagerGearModule.realHand(villager).is(VillagerGearModule.WEAPONS), armor,
				isGuardProfession(villager));
	}

	public static boolean isGuardProfession(Villager villager) {
		return villager.getVillagerData().profession().is(GUARD);
	}

	/** Called every tick a villager thinks on the server: a jobless villager armed as a guard takes the profession. */
	public static void tick(Villager villager, ServerLevel level) {
		if (!enabled || !VillagerRepairLogic.checksNow(level.getGameTime(), villager.getId(), PROFESSION_CHECK_TICKS)) return;
		if (VillagerGuardLogic.becomesGuard(villager.getVillagerData().profession().is(VillagerProfession.NONE), isGuard(villager))) {
			makeGuard(villager, level);
		}
	}

	/** Gives the villager the guard profession: he leaves his job site, and his trades and work item go. */
	static void makeGuard(Villager villager, ServerLevel level) {
		villager.releasePoi(MemoryModuleType.JOB_SITE);
		villager.releasePoi(MemoryModuleType.POTENTIAL_JOB_SITE);
		villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
		villager.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
		villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(), GUARD));
		// Vanilla takes the profession of a villager with no job site, no experience and no level back.
		if (villager.getVillagerXp() == 0) villager.setVillagerXp(1);
		villager.refreshBrain(level);
		level.broadcastEntityEvent(villager, HAPPY_PARTICLES);
		villager.makeSound(SoundEvents.VILLAGER_CELEBRATE);
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
