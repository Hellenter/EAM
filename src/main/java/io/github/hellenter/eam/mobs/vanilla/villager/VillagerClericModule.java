package io.github.hellenter.eam.mobs.vanilla.villager;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.Map;

/**
 * The cleric looks after the village: now and then he heals the hurt villagers near him. Given brewing ingredients
 * through his inventory, he brews potions at work and hands them to the guards near him, who drink a healing one when
 * down to half their health and any other in a fight. Which ingredient makes which potion, and the other numbers, are in
 * {@code data/eam/villagers/cleric.json}.
 */
public final class VillagerClericModule {
	private static final byte HAPPY_PARTICLES = 14;
	private static final int DRINK_CHECK_TICKS = 20;

	private static volatile ClericSettings settings = ClericSettings.DEFAULT;
	private static boolean enabled;

	private VillagerClericModule() {
	}

	/** Settings load whatever the config says, so the data is always valid. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(ClericSettingsLoader.ID, new ClericSettingsLoader());
	}

	public static void init() {
		enabled = true;
	}

	static void setSettings(ClericSettings value) {
		settings = value;
	}

	/** Called every tick a villager thinks on the server: a cleric heals and brews, a guard drinks his potions. */
	public static void tick(Villager villager, ServerLevel level) {
		if (!enabled || villager.isBaby()) return;
		ClericSettings current = settings;
		long time = level.getGameTime();
		if (villager.getVillagerData().profession().is(VillagerProfession.CLERIC)) {
			if (VillagerRepairLogic.checksNow(time, villager.getId(), current.healIntervalTicks())) tend(level, villager, current);
			if (VillagerRepairLogic.checksNow(time, villager.getId(), current.brewIntervalTicks())) brew(level, villager, current);
		}
		if (VillagerRepairLogic.checksNow(time, villager.getId(), DRINK_CHECK_TICKS) && VillagerGuardModule.isGuard(villager)) {
			drink(level, villager);
		}
	}

	/** Heals the hurt villagers near the cleric and hands a potion to each guard short of them. */
	private static void tend(ServerLevel level, Villager cleric, ClericSettings current) {
		for (Villager other : level.getEntitiesOfClass(Villager.class, cleric.getBoundingBox().inflate(current.radius()), Villager::isAlive)) {
			if (other.getHealth() < other.getMaxHealth()) {
				other.heal(current.healAmount());
				level.broadcastEntityEvent(other, HAPPY_PARTICLES);
			}
			if (other != cleric && VillagerGuardModule.isGuard(other) && potions(other.getInventory()) < current.potionMax()) {
				handOver(cleric.getInventory(), other.getInventory());
			}
		}
	}

	/** At work, turns one ingredient a player gave the cleric into its potion while he has room for more. */
	private static void brew(ServerLevel level, Villager cleric, ClericSettings current) {
		if (!cleric.getBrain().isActive(Activity.WORK)) return;
		SimpleContainer inventory = cleric.getInventory();
		if (potions(inventory) >= current.potionMax()) return;
		for (Map.Entry<Item, Holder<Potion>> recipe : current.potions().entrySet()) {
			if (inventory.countItem(recipe.getKey()) <= 0) continue;
			ItemStack potion = PotionContents.createItemStack(Items.POTION, recipe.getValue());
			if (!inventory.canAddItem(potion)) return;
			inventory.removeItemType(recipe.getKey(), 1);
			inventory.addItem(potion);
			level.playSound(null, cleric.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.NEUTRAL, 0.8F, 1.0F);
			return;
		}
	}

	/** A guard drinks the first potion in his inventory that helps him now. */
	private static void drink(ServerLevel level, Villager guard) {
		SimpleContainer inventory = guard.getInventory();
		boolean fighting = guard.getTarget() != null;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
			if (contents == null || !stack.is(Items.POTION)) continue;
			boolean heals = false;
			boolean hasEffect = true;
			for (MobEffectInstance effect : contents.getAllEffects()) {
				heals |= effect.is(MobEffects.INSTANT_HEALTH) || effect.is(MobEffects.REGENERATION);
				hasEffect &= guard.hasEffect(effect.getEffect());
			}
			if (!VillagerClericLogic.drinks(heals, guard.getHealth(), guard.getMaxHealth(), fighting, hasEffect)) continue;
			stack.finishUsingItem(level, guard);
			inventory.setChanged();
			return;
		}
	}

	private static void handOver(SimpleContainer from, SimpleContainer to) {
		for (int i = 0; i < from.getContainerSize(); i++) {
			ItemStack stack = from.getItem(i);
			if (!stack.is(Items.POTION) || !to.canAddItem(stack)) continue;
			to.addItem(from.removeItem(i, 1));
			return;
		}
	}

	private static int potions(SimpleContainer inventory) {
		return inventory.countItem(Items.POTION);
	}
}
