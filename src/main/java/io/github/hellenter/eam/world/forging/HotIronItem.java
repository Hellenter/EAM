package io.github.hellenter.eam.world.forging;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Hot metal from the furnace: hot iron, or hot mithril from the obsidian blast furnace. Its minute starts on its first
 * tick in an inventory, which is when it has been taken out of the furnace; once the minute is up it turns back into
 * the ingot it was heated from. Stacks taken out at different times keep different timers and do not merge.
 */
public class HotIronItem extends Item {
	private final Item cooled;

	/** @param cooled the ingot it turns back into */
	public HotIronItem(Item cooled, Properties properties) {
		super(properties);
		this.cooled = cooled;
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		long now = level.getGameTime();
		if (!stack.has(ForgingModule.COOLS_AT)) {
			stack.set(ForgingModule.COOLS_AT, ForgingLogic.coolsAt(now));
		} else if (ForgingLogic.isCooled(ForgingModule.coolsAt(stack), now)) {
			replace(stack, new ItemStack(cooled, stack.getCount()), owner, slot);
		}
	}

	private static void replace(ItemStack stack, ItemStack ingots, Entity owner, @Nullable EquipmentSlot slot) {
		if (owner instanceof Player player) {
			List<ItemStack> items = player.getInventory().getNonEquipmentItems();
			for (int i = 0; i < items.size(); i++) {
				if (items.get(i) == stack) {
					player.getInventory().setItem(i, ingots);
					return;
				}
			}
		}
		if (slot != null && owner instanceof LivingEntity living && living.getItemBySlot(slot) == stack) {
			living.setItemSlot(slot, ingots);
		}
	}
}
