package io.github.hellenter.eam.content.arrows;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.silver.SilverModule;

import net.fabricmc.fabric.api.item.v1.ItemComponentTooltipProviderRegistry;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;

/**
 * Arrows with metal heads: an arrow item with {@link #ARROW_HEAD} hits harder, and one with
 * {@link SilverModule#UNDEAD_DAMAGE_BONUS} hits undead harder still. The fired arrow keeps its item as the pickup stack,
 * so the components travel with it. Numbers are in {@link ArrowHeads}, rules in {@link ArrowHeadLogic}.
 */
public final class ArrowHeadModule {
	public static final DataComponentType<ArrowHead> ARROW_HEAD = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Eam.id("arrow_head"),
			DataComponentType.<ArrowHead>builder().persistent(ArrowHead.CODEC).networkSynchronized(ArrowHead.STREAM_CODEC).build());

	private static boolean enabled;

	private ArrowHeadModule() {
	}

	public static void register() {
		ItemComponentTooltipProviderRegistry.addLast(ARROW_HEAD);
	}

	public static void init() {
		enabled = true;
	}

	/** Called when an arrow hits an entity: the arrow's base damage after its head's bonuses. */
	public static double baseDamage(AbstractArrow arrow, Entity target, double baseDamage) {
		if (!enabled) {
			return baseDamage;
		}
		ItemStack stack = arrow.getPickupItemStackOrigin();
		ArrowHead head = stack.get(ARROW_HEAD);
		Float undeadBonus = stack.get(SilverModule.UNDEAD_DAMAGE_BONUS);
		if (head == null && undeadBonus == null) {
			return baseDamage;
		}
		return ArrowHeadLogic.baseDamage(baseDamage, head == null ? 0.0F : head.damageBonus(),
				undeadBonus == null ? 0.0F : undeadBonus, target.is(EntityTypeTags.UNDEAD));
	}
}
