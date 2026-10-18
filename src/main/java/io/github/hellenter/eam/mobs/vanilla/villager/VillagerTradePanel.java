package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.mixin.world.crafting.AbstractContainerMenuAccessor;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.trading.Merchant;

import org.jspecify.annotations.Nullable;

/**
 * A villager's trades open with his gear beside them: his figure, his armor, his main hand (a weapon or a tool of his
 * trade) and his off hand, a button to his whole inventory and, for the elder, one to the village menu. The slots are
 * in every trade menu, after the player's inventory, so client and server always have the same; they only show once
 * the server turned the panel on.
 */
public final class VillagerTradePanel {
	/** The first panel slot: vanilla's trade menu has three trade slots and the player's 36. */
	public static final int FIRST_SLOT = 39;
	public static final int SLOTS = 6;
	/** Panel bounds, relative to the trade screen's corner; the vanilla screen is 276 wide. */
	public static final int LEFT = 277;
	public static final int RIGHT = 351;
	public static final int TOP = 0;
	public static final int BOTTOM = 130;
	/** The bottom of the elder's panel, which has the village button under the inventory button. */
	public static final int ELDER_BOTTOM = 152;
	public static final int ARMOR_X = LEFT + 6;
	public static final int ARMOR_Y = 8;
	public static final int HANDS_Y = 84;
	public static final int SLOT = 18;
	public static final int BUTTON_Y = 104;
	public static final int VILLAGE_BUTTON_Y = 126;

	private VillagerTradePanel() {
	}

	/** Called at the end of the trade menu's constructor on both sides: adds the gear slots. */
	public static void addSlots(MerchantMenu menu, Merchant merchant) {
		Container gear = merchant instanceof Villager villager ? new VillagerGearContainer(villager) : new SimpleContainer(SLOTS);
		TradePanelMenu panelMenu = (TradePanelMenu) menu;
		AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) menu;
		for (int i = 0; i < VillagerGearContainer.ARMOR.length; i++) {
			accessor.eam$addSlot(new GearSlot(panelMenu, gear, i, VillagerGearContainer.ARMOR[i], ARMOR_X, ARMOR_Y + i * SLOT,
					VillagerGearMenu.ARMOR_ICONS[i]));
		}
		accessor.eam$addSlot(new GearSlot(panelMenu, gear, VillagerGearContainer.MAIN_HAND, EquipmentSlot.MAINHAND, ARMOR_X, HANDS_Y, null));
		accessor.eam$addSlot(new GearSlot(panelMenu, gear, VillagerGearContainer.OFF_HAND, EquipmentSlot.OFFHAND, ARMOR_X + SLOT, HANDS_Y,
				InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD));
	}

	/** After the server opened the villager's trades for {@code player}: turns the panel on, here and on his client. */
	static void show(Villager villager, ServerPlayer player) {
		if (!(player.containerMenu instanceof MerchantMenu menu) || ((TradePanelMenu) menu).eam$trader() != villager) return;
		VillagerTradePanelPayload payload = new VillagerTradePanelPayload(menu.containerId, villager.getId(), VillagerGearModule.tools(villager),
				VillagerElderModule.opensVillage(villager));
		((TradePanelMenu) menu).eam$setPanel(payload.panel());
		ServerPlayNetworking.send(player, payload);
	}

	/** Where the panel ends, relative to the trade screen's corner. */
	public static int bottom(TradePanel panel) {
		return panel.elder() ? ELDER_BOTTOM : BOTTOM;
	}

	/** The villager whose trades {@code player} has open with the panel, if any. */
	static @Nullable Villager trading(ServerPlayer player) {
		if (!(player.containerMenu instanceof TradePanelMenu menu) || menu.eam$panel() == null) return null;
		return menu.eam$trader() instanceof Villager villager ? villager : null;
	}

	public static boolean isPanelSlot(int slotIndex) {
		return slotIndex >= FIRST_SLOT && slotIndex < FIRST_SLOT + SLOTS;
	}

	/** Shift click on a panel slot: the item goes to the player's inventory. */
	public static ItemStack quickMove(MerchantMenu menu, Player player, int slotIndex) {
		Slot slot = menu.slots.get(slotIndex);
		if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack clicked = stack.copy();
		if (!((AbstractContainerMenuAccessor) menu).eam$moveItemStackTo(stack, 3, FIRST_SLOT, true)) return ItemStack.EMPTY;
		slot.setByPlayer(stack.isEmpty() ? ItemStack.EMPTY : stack);
		return clicked;
	}

	/** One piece of the villager's gear; shown and usable only while the panel is on. */
	private static final class GearSlot extends Slot {
		private final TradePanelMenu menu;
		private final EquipmentSlot equipment;
		private final @Nullable Identifier icon;

		GearSlot(TradePanelMenu menu, Container container, int index, EquipmentSlot equipment, int x, int y, @Nullable Identifier icon) {
			super(container, index, x, y);
			this.menu = menu;
			this.equipment = equipment;
			this.icon = icon;
		}

		@Override
		public boolean isActive() {
			return menu.eam$panel() != null;
		}

		@Override
		public boolean mayPickup(Player player) {
			return isActive();
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			TradePanel panel = menu.eam$panel();
			if (panel == null) return false;
			if (equipment == EquipmentSlot.MAINHAND) {
				return stack.is(VillagerGearModule.WEAPONS) || panel.tools().isPresent() && stack.is(panel.tools().get());
			}
			if (equipment == EquipmentSlot.OFFHAND) return stack.is(VillagerGearModule.OFF_HAND);
			Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
			return equippable != null && equippable.slot() == equipment;
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}

		@Override
		public @Nullable Identifier getNoItemIcon() {
			return icon;
		}
	}
}
