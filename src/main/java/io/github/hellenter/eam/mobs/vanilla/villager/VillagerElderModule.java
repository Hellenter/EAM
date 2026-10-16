package io.github.hellenter.eam.mobs.vanilla.villager;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The cartographer is the village elder: what a player gives him through his inventory he hands out to the villagers
 * near him, one thing at a time. Armor, weapons and shields go to one villager until he has all he can take, then to
 * the next, so the village gets guards one by one. Tools go to a villager of the trade that uses them, a cleric's
 * ingredients to a cleric, food to the hungriest villager. What nobody can take stays with him. Numbers are in
 * {@code data/eam/villagers/elder.json}.
 */
public final class VillagerElderModule {
	private static final byte HAPPY_PARTICLES = 14;

	private static volatile ElderSettings settings = ElderSettings.DEFAULT;
	private static boolean enabled;

	private VillagerElderModule() {
	}

	/** Settings load whatever the config says, so the data is always valid. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(ElderSettingsLoader.ID, new ElderSettingsLoader());
	}

	public static void init() {
		enabled = true;
	}

	static void setSettings(ElderSettings value) {
		settings = value;
	}

	/** Called every tick a villager thinks on the server: now and then the elder hands one thing out. */
	public static void tick(Villager villager, ServerLevel level) {
		if (!enabled) return;
		ElderSettings current = settings;
		if (!VillagerRepairLogic.checksNow(level.getGameTime(), villager.getId(), current.intervalTicks())) return;
		if (villager.isBaby() || villager.isSleeping() || !villager.getVillagerData().profession().is(VillagerProfession.CARTOGRAPHER)) {
			return;
		}
		SimpleContainer inventory = villager.getInventory();
		if (inventory.isEmpty()) return;
		List<Villager> near = level.getEntitiesOfClass(Villager.class, villager.getBoundingBox().inflate(current.radius()),
				other -> other != villager && other.isAlive() && !other.isBaby());
		near.sort(Comparator.comparing(Villager::getUUID));
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.isEmpty()) continue;
			Villager taker = handOut(stack, near, current);
			if (taker == null) continue;
			inventory.setChanged();
			villager.getLookControl().setLookAt(taker);
			level.broadcastEntityEvent(taker, HAPPY_PARTICLES);
			return;
		}
	}

	/** Gives some of {@code stack} to the villager who should get it, and returns him; none if nobody can take it. */
	private static Villager handOut(ItemStack stack, List<Villager> near, ElderSettings current) {
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		if (equippable != null && List.of(VillagerGearContainer.ARMOR).contains(equippable.slot())) {
			return arm(stack, near, equippable.slot(), villager -> villager.getItemBySlot(equippable.slot()).isEmpty());
		}
		if (stack.is(VillagerGearModule.WEAPONS)) return arm(stack, near, EquipmentSlot.MAINHAND, VillagerElderModule::handFree);
		if (stack.is(VillagerGearModule.OFF_HAND)) {
			return arm(stack, near, EquipmentSlot.OFFHAND, villager -> villager.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty());
		}
		for (Villager villager : near) {
			if (handFree(villager) && matches(stack, VillagerGearModule.tools(villager))) {
				VillagerGearModule.give(villager, EquipmentSlot.MAINHAND, stack.split(1));
				return villager;
			}
		}
		for (Villager villager : near) {
			if (matches(stack, VillagerGearModule.goods(villager)) && give(villager, stack, stack.getCount())) return villager;
		}
		if (stack.has(DataComponents.VILLAGER_FOOD)) {
			Villager hungriest = near.stream().filter(Villager::wantsMoreFood)
					.min(Comparator.comparingInt(VillagerFoodModule::points)).orElse(null);
			return hungriest != null && give(hungriest, stack, current.foodPerGift()) ? hungriest : null;
		}
		return null;
	}

	/** Moves up to {@code count} of {@code stack} into the villager's inventory; whether any went. */
	private static boolean give(Villager villager, ItemStack stack, int count) {
		ItemStack gift = stack.split(Math.min(stack.getCount(), count));
		int size = gift.getCount();
		ItemStack left = villager.getInventory().addItem(gift);
		stack.grow(left.getCount());
		return left.getCount() < size;
	}

	/** Puts one of {@code stack} in {@code slot} of the best armed villager who can take it. */
	private static Villager arm(ItemStack stack, List<Villager> near, EquipmentSlot slot, Predicate<Villager> canTake) {
		List<Villager> takers = near.stream().filter(canTake).toList();
		int[] gear = takers.stream().mapToInt(VillagerElderModule::gear).toArray();
		int best = VillagerElderLogic.armsFirst(gear);
		if (best < 0) return null;
		Villager taker = takers.get(best);
		VillagerGearModule.give(taker, slot, stack.split(1));
		return taker;
	}

	/** Pieces of gear a player gave the villager: armor, a shield and what he holds instead of his work item. */
	private static int gear(Villager villager) {
		int gear = 0;
		for (EquipmentSlot slot : VillagerGearContainer.ARMOR) {
			if (!villager.getItemBySlot(slot).isEmpty()) gear++;
		}
		if (!villager.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty()) gear++;
		if (!handFree(villager)) gear++;
		return gear;
	}

	/** Whether the villager's main hand holds nothing a player gave him, only his work item or nothing. */
	private static boolean handFree(Villager villager) {
		ItemStack hand = VillagerGearModule.realHand(villager);
		return hand.isEmpty() || VillagerGearModule.isWorkItem(hand);
	}

	private static boolean matches(ItemStack stack, Optional<TagKey<Item>> tag) {
		return tag.isPresent() && stack.is(tag.get());
	}
}
