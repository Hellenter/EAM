package io.github.hellenter.eam.content.silver;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Silver against undead: weapons with {@link #UNDEAD_DAMAGE_BONUS} hit undead harder, and armor with
 * {@link #UNDEAD_THORNS} hurts undead that hit the player in melee. Numbers are in {@link SilverGear}, rules in
 * {@link SilverLogic}.
 */
public final class SilverModule {
	/** Extra damage to undead as a share of the weapon's base damage: 0.5 is +50%. */
	public static final DataComponentType<Float> UNDEAD_DAMAGE_BONUS = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Eam.id("undead_damage_bonus"),
			DataComponentType.<Float>builder().persistent(ExtraCodecs.NON_NEGATIVE_FLOAT).networkSynchronized(ByteBufCodecs.FLOAT).build());

	/** Share of an undead mob's melee hit sent back to it by this worn armor piece. */
	public static final DataComponentType<Float> UNDEAD_THORNS = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Eam.id("undead_thorns"),
			DataComponentType.<Float>builder().persistent(ExtraCodecs.NON_NEGATIVE_FLOAT).networkSynchronized(ByteBufCodecs.FLOAT).build());

	private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	private static boolean enabled;

	private SilverModule() {
	}

	public static void register() {
		// Loads the class, which registers the components above.
	}

	public static void init() {
		enabled = true;
		ServerLivingEntityEvents.AFTER_DAMAGE.register(SilverModule::afterDamage);
	}

	/** Called from the player's enchanted damage: adds the weapon's undead bonus. */
	public static float attackDamage(Player player, Entity target, float baseDamage, float damage) {
		if (!enabled) {
			return damage;
		}
		Float bonus = player.getWeaponItem().get(UNDEAD_DAMAGE_BONUS);
		return bonus == null ? damage : SilverLogic.attackDamage(baseDamage, damage, bonus, isUndead(target));
	}

	private static void afterDamage(LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
		if (blocked || !(entity instanceof Player player) || !(entity.level() instanceof ServerLevel level)) {
			return;
		}
		// Melee only: the attacker struck the player itself, not with an arrow.
		if (!(source.getEntity() instanceof LivingEntity attacker) || source.getDirectEntity() != attacker
				|| !isUndead(attacker) || !attacker.isAlive()) {
			return;
		}
		float[] shares = new float[ARMOR_SLOTS.length];
		for (int i = 0; i < ARMOR_SLOTS.length; i++) {
			ItemStack armor = player.getItemBySlot(ARMOR_SLOTS[i]);
			shares[i] = armor.getOrDefault(UNDEAD_THORNS, 0.0F);
		}
		float reflected = SilverLogic.reflectedDamage(baseDamageTaken, shares);
		if (reflected > 0.0F) {
			attacker.hurtServer(level, player.damageSources().thorns(player), reflected);
		}
	}

	private static boolean isUndead(Entity entity) {
		return entity.is(EntityTypeTags.UNDEAD);
	}
}
