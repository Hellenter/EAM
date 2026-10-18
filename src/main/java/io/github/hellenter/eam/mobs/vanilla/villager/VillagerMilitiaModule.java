package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Villagers who are not guards keep the armor, weapon and shield a player gave them put away, and arm themselves only
 * when the village is in danger: a bell rings near them, a monster comes close or hurts them. Armed, they fight as
 * guards do, but first run to the nearest guard to fight beside him. Once it has been quiet for a while they put their
 * gear away again. Guards wear theirs all the time. The gear put away still shows in the villager's inventory, in its
 * armor and hand slots. Numbers are in {@code data/eam/villagers/guards.json}.
 */
public final class VillagerMilitiaModule {
	/** Gear a villager who is no guard has put away, by the slot it goes in. */
	public static final AttachmentType<Map<EquipmentSlot, ItemStack>> STOWED = AttachmentRegistry.create(Eam.id("villager_stowed_gear"),
			builder -> builder.persistent(Codec.unboundedMap(EquipmentSlot.CODEC, ItemStack.CODEC)));

	private static final int CHECK_TICKS = 10;
	/** Ticks a monster's blow keeps a villager armed for, counted like the other dangers. */
	private static final int HURT_TICKS = 100;
	/** How close, in blocks, a monster may be for a villager running to a guard to stop and fight it. */
	private static final double FIGHT_BACK_DISTANCE = 3.0;
	private static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
			EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND);

	/** When each villager last saw danger. Per-tick state, not saved. */
	private static final Map<Villager, Long> LAST_DANGER = new WeakHashMap<>();
	/** Armed villagers on their way to a guard. */
	private static final Set<Villager> RUNNING = Collections.newSetFromMap(new WeakHashMap<>());

	private VillagerMilitiaModule() {
	}

	/** What the villager has for {@code slot}: worn, or put away. His work item does not count. */
	public static ItemStack gear(Villager villager, EquipmentSlot slot) {
		ItemStack worn = slot == EquipmentSlot.MAINHAND ? VillagerGearModule.realHand(villager) : villager.getItemBySlot(slot);
		if (!worn.isEmpty() && !VillagerGearModule.isWorkItem(worn)) return worn;
		return stowed(villager, slot);
	}

	/** The gear the villager has put away for {@code slot}, or nothing. */
	public static ItemStack stowed(Villager villager, EquipmentSlot slot) {
		Map<EquipmentSlot, ItemStack> stowed = villager.getAttached(STOWED);
		return stowed == null ? ItemStack.EMPTY : stowed.getOrDefault(slot, ItemStack.EMPTY);
	}

	/** Clears what the villager put away for {@code slot}, when a player takes it from his inventory or gives him other gear. */
	static void clearStowed(Villager villager, EquipmentSlot slot) {
		setStowed(villager, slot, ItemStack.EMPTY);
	}

	/** Whether the villager has armor and a weapon, worn or put away: what makes a jobless villager a guard. */
	static boolean hasArms(Villager villager) {
		boolean armor = false;
		for (EquipmentSlot slot : VillagerGearContainer.ARMOR) {
			if (!gear(villager, slot).isEmpty()) armor = true;
		}
		return armor && gear(villager, EquipmentSlot.MAINHAND).is(VillagerGearModule.WEAPONS);
	}

	/** Whether the villager is running to a guard: his brain waits and he picks no new target on the way. */
	public static boolean runsToGuard(Villager villager) {
		return RUNNING.contains(villager);
	}

	/** Called from the guard tick: arms the villager in danger, takes him to a guard, and puts his gear away when quiet. */
	static void tick(Villager villager, ServerLevel level, GuardSettings settings) {
		if (!VillagerRepairLogic.checksNow(level.getGameTime(), villager.getId(), CHECK_TICKS)) return;
		if (villager.isBaby()) return;
		boolean guard = VillagerGuardModule.isGuardProfession(villager);
		long now = level.getGameTime();
		boolean danger = !guard && danger(villager, level, settings);
		if (danger) LAST_DANGER.put(villager, now);
		long last = LAST_DANGER.getOrDefault(villager, Long.MIN_VALUE / 2);
		if (VillagerGuardLogic.arms(guard, danger)) {
			arm(villager);
			if (guard) {
				RUNNING.remove(villager);
				return;
			}
		} else if (VillagerGuardLogic.stows(guard, now - last, settings.calmTicks())) {
			RUNNING.remove(villager);
			LAST_DANGER.remove(villager);
			stow(villager);
			return;
		}
		if (!danger || !VillagerGuardModule.isGuard(villager)) {
			RUNNING.remove(villager);
			return;
		}
		runToGuard(villager, level, settings);
	}

	/** Puts on all the gear the villager put away. */
	static void arm(Villager villager) {
		Map<EquipmentSlot, ItemStack> stowed = villager.getAttached(STOWED);
		if (stowed == null || stowed.isEmpty()) return;
		boolean armed = false;
		for (EquipmentSlot slot : SLOTS) {
			ItemStack stack = stowed.getOrDefault(slot, ItemStack.EMPTY);
			if (stack.isEmpty()) continue;
			ItemStack worn = slot == EquipmentSlot.MAINHAND ? VillagerGearModule.realHand(villager) : villager.getItemBySlot(slot);
			if (!worn.isEmpty() && !VillagerGearModule.isWorkItem(worn)) continue;
			clearStowed(villager, slot);
			VillagerGearModule.give(villager, slot, stack);
			armed = true;
		}
		if (armed) villager.makeSound(SoundEvents.ARMOR_EQUIP_GENERIC.value());
	}

	/** Puts away the armor, shield and weapon the villager wears; a tool of his trade stays in his hand. */
	private static void stow(Villager villager) {
		for (EquipmentSlot slot : SLOTS) {
			if (!stowed(villager, slot).isEmpty()) continue;
			if (slot == EquipmentSlot.MAINHAND) {
				ItemStack hand = VillagerGearModule.realHand(villager);
				boolean tool = VillagerGearModule.tools(villager).map(hand::is).orElse(false);
				if (hand.isEmpty() || VillagerGearModule.isWorkItem(hand) || !hand.is(VillagerGearModule.WEAPONS) || tool) continue;
				setStowed(villager, slot, hand.copy());
				VillagerGearModule.give(villager, slot, ItemStack.EMPTY);
			} else {
				ItemStack worn = villager.getItemBySlot(slot);
				if (worn.isEmpty() || VillagerGearModule.isWorkItem(worn)) continue;
				setStowed(villager, slot, worn.copy());
				villager.setItemSlot(slot, ItemStack.EMPTY);
			}
		}
	}

	/** A bell ringing near the villager, a monster near him, or one that hurt him just now. */
	private static boolean danger(Villager villager, ServerLevel level, GuardSettings settings) {
		if (VillagerAlarmModule.alarmed(level, villager)) return true;
		if (villager.getLastHurtByMob() instanceof Enemy && villager.tickCount - villager.getLastHurtByMobTimestamp() < HURT_TICKS) return true;
		if (villager.isSleeping()) return false;
		return !level.getEntitiesOfClass(Mob.class, villager.getBoundingBox().inflate(settings.armRadius()),
				mob -> mob instanceof Enemy && mob.isAlive() && villager.hasLineOfSight(mob)).isEmpty();
	}

	/** Takes the armed villager to the nearest guard, unless a monster is already at him or he is beside a guard. */
	private static void runToGuard(Villager villager, ServerLevel level, GuardSettings settings) {
		Villager guard = nearestGuard(villager, level, settings.guardSearchRadius());
		LivingEntity target = villager.getTarget();
		double targetDistance = target == null ? Double.MAX_VALUE : villager.distanceTo(target);
		if (guard == null || !VillagerGuardLogic.runsToGuard(villager.distanceTo(guard), settings.guardReach(), targetDistance, FIGHT_BACK_DISTANCE)) {
			RUNNING.remove(villager);
			return;
		}
		RUNNING.add(villager);
		if (villager.isSleeping()) villager.stopSleeping();
		if (target != null) villager.setTarget(null);
		villager.getNavigation().moveTo(guard, settings.meleeSpeed());
	}

	private static @Nullable Villager nearestGuard(Villager villager, ServerLevel level, int radius) {
		Villager nearest = null;
		double best = Double.MAX_VALUE;
		for (Villager other : level.getEntitiesOfClass(Villager.class, villager.getBoundingBox().inflate(radius),
				other -> other != villager && other.isAlive() && VillagerGuardModule.isGuardProfession(other))) {
			double distance = villager.distanceToSqr(other);
			if (distance < best) {
				best = distance;
				nearest = other;
			}
		}
		return nearest;
	}

	private static void setStowed(Villager villager, EquipmentSlot slot, ItemStack stack) {
		Map<EquipmentSlot, ItemStack> stowed = new EnumMap<>(EquipmentSlot.class);
		Map<EquipmentSlot, ItemStack> before = villager.getAttached(STOWED);
		if (before != null) stowed.putAll(before);
		if (stack.isEmpty()) {
			stowed.remove(slot);
		} else {
			stowed.put(slot, stack);
		}
		if (stowed.isEmpty()) {
			villager.removeAttached(STOWED);
		} else {
			villager.setAttached(STOWED, Map.copyOf(stowed));
		}
	}

	/** Everything the villager put away, taken from him: it drops when he dies or goes with him when he turns. */
	static List<ItemStack> takeStowed(Villager villager) {
		Map<EquipmentSlot, ItemStack> stowed = villager.removeAttached(STOWED);
		return stowed == null ? List.of() : List.copyOf(stowed.values());
	}
}
