package io.github.hellenter.eam.mobs.vanilla.villager;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * What a player gives a villager wears out: armor when he is hurt, a weapon when he strikes, a bow when he shoots, a
 * hoe when he harvests, and breaks like a player's. Now and then each villager checks his gear, and if a master of the
 * village can mend it, it gets mended: the armorer mends armor and shields, the toolsmith and the weaponsmith mend
 * tools and weapons. Nobody walks anywhere; a master just has to live near, and each one serves a limited number of
 * villagers. Numbers are in {@code data/eam/villagers/repair.json}.
 */
public final class VillagerRepairModule {
	/** A kind of repair and the professions that do it. */
	private enum Craft {
		ARMOR(VillagerProfession.ARMORER),
		TOOLS(VillagerProfession.TOOLSMITH, VillagerProfession.WEAPONSMITH);

		private final List<ResourceKey<VillagerProfession>> masters;

		@SafeVarargs
		Craft(ResourceKey<VillagerProfession>... masters) {
			this.masters = List.of(masters);
		}

		boolean masteredBy(Villager villager) {
			return !villager.isBaby() && masters.stream().anyMatch(villager.getVillagerData().profession()::is);
		}

		List<ItemStack> worn(Villager villager) {
			List<ItemStack> worn = new ArrayList<>();
			if (this == ARMOR) {
				for (EquipmentSlot slot : VillagerGearContainer.ARMOR) addWorn(worn, villager.getItemBySlot(slot));
				addWorn(worn, villager.getItemBySlot(EquipmentSlot.OFFHAND));
			} else {
				addWorn(worn, VillagerGearModule.realHand(villager));
			}
			return worn;
		}

		private static void addWorn(List<ItemStack> worn, ItemStack stack) {
			if (stack.isDamaged() && !VillagerGearModule.isWorkItem(stack)) worn.add(stack);
		}
	}

	private static final byte HAPPY_PARTICLES = 14;

	private static volatile RepairSettings settings = RepairSettings.DEFAULT;
	private static boolean enabled;

	private VillagerRepairModule() {
	}

	/** Settings load whatever the config says, so the data is always valid. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(RepairSettingsLoader.ID, new RepairSettingsLoader());
	}

	public static void init() {
		enabled = true;
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (entity instanceof Villager villager && !blocked) wearArmor(villager, source, baseDamage);
			if (!blocked && damageTaken > 0.0F && source.getDirectEntity() instanceof Villager villager
					&& source.getEntity() == villager && entity != villager) {
				wearWeapon(villager, entity);
			}
		});
	}

	static void setSettings(RepairSettings value) {
		settings = value;
	}

	/** Called every tick a villager thinks on the server: now and then his worn gear gets mended. */
	public static void tick(Villager villager, ServerLevel level) {
		if (!enabled) return;
		RepairSettings current = settings;
		if (!VillagerRepairLogic.checksNow(level.getGameTime(), villager.getId(), current.intervalTicks())) return;
		List<Villager> near = null;
		boolean mended = false;
		for (Craft craft : Craft.values()) {
			List<ItemStack> worn = craft.worn(villager);
			if (worn.isEmpty()) continue;
			if (near == null) {
				near = level.getEntitiesOfClass(Villager.class, villager.getBoundingBox().inflate(current.villageRadius()), Villager::isAlive);
			}
			if (!served(villager, craft, near, current)) continue;
			for (ItemStack stack : worn) {
				stack.setDamageValue(VillagerRepairLogic.mended(stack.getDamageValue(), stack.getMaxDamage(), current.repairShare()));
			}
			mended = true;
		}
		if (mended) level.broadcastEntityEvent(villager, HAPPY_PARTICLES);
	}

	/** A guard's bow or crossbow wears with each shot. */
	public static void afterShot(Villager villager) {
		wear(villager, EquipmentSlot.MAINHAND, 1);
	}

	/** A farmer's hoe wears with each crop he harvests. */
	public static void afterHarvest(Villager villager, boolean harvested) {
		if (harvested) wear(villager, EquipmentSlot.MAINHAND, 1);
	}

	/** Whether the masters near {@code villager} reach him: those needing the same repair take turns by a fixed order. */
	private static boolean served(Villager villager, Craft craft, List<Villager> near, RepairSettings current) {
		int masters = (int) near.stream().filter(craft::masteredBy).count();
		if (masters == 0) return false;
		List<Villager> clients = near.stream()
				.filter(other -> other == villager || !craft.worn(other).isEmpty())
				.sorted(Comparator.comparing(Villager::getUUID))
				.toList();
		return VillagerRepairLogic.served(clients.indexOf(villager), masters, current.clientsPerMaster());
	}

	/** Armor wears like a player's: a quarter of the damage before armor, at least one point. */
	private static void wearArmor(Villager villager, DamageSource source, float damage) {
		if (damage <= 0.0F || source.is(DamageTypeTags.BYPASSES_ARMOR)) return;
		int amount = (int) Math.max(1.0F, damage / 4.0F);
		for (EquipmentSlot slot : VillagerGearContainer.ARMOR) {
			ItemStack stack = villager.getItemBySlot(slot);
			Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
			if (equippable != null && equippable.damageOnHurt() && stack.canBeHurtBy(source)) wear(villager, slot, amount);
		}
	}

	/** A weapon wears like a player's when it strikes. */
	private static void wearWeapon(Villager villager, LivingEntity target) {
		ItemStack weapon = villager.getMainHandItem();
		if (weapon.isEmpty() || VillagerGearModule.isWorkItem(weapon)) return;
		weapon.postHurtEnemy(target, villager);
		VillagerGearModule.afterWear(villager);
	}

	/** Wears what a player gave the villager in {@code slot}; his own work item never wears. */
	private static void wear(Villager villager, EquipmentSlot slot, int amount) {
		if (!enabled) return;
		ItemStack stack = villager.getItemBySlot(slot);
		if (stack.isEmpty() || VillagerGearModule.isWorkItem(stack) || !stack.isDamageableItem()) return;
		stack.hurtAndBreak(amount, villager, slot);
		VillagerGearModule.afterWear(villager);
	}
}
