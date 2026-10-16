package io.github.hellenter.eam.mobs.vanilla.villager;

import io.netty.buffer.ByteBuf;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * A villager's inventory: armor on the left beside his figure, then the main hand (a weapon, or a tool of his trade),
 * the off hand (a shield), his own locked work item, and the eight slots he keeps food and seeds in.
 */
public class VillagerGearMenu extends AbstractContainerMenu {
	public static final int SLOT = 18;
	/** Below the title; slot positions are of the item inside the 18-pixel frame. */
	public static final int TOP = 18;
	public static final int ARMOR_X = 8;
	public static final int HANDS_X = 80;
	public static final int INVENTORY_X = 98;
	public static final int INVENTORY_COLUMNS = 4;
	/** Height of the part above the player's inventory: four rows, like a chest with four rows. */
	public static final int TOP_HEIGHT = TOP - 1 + 4 * SLOT;
	public static final int PLAYER_INVENTORY_TOP = TOP_HEIGHT + 14;
	private static final Identifier[] ARMOR_ICONS = {InventoryMenu.EMPTY_ARMOR_SLOT_HELMET, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
			InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS};

	private final Container gear;
	private final OpenData data;
	private final @Nullable Villager villager;
	private final Map<Item, Integer> feedBefore;
	private final int pointsBefore;

	/** Client side: an empty copy the server fills. */
	public VillagerGearMenu(int containerId, Inventory inventory, OpenData data) {
		this(containerId, inventory, data, new SimpleContainer(VillagerGearContainer.SIZE), null);
	}

	VillagerGearMenu(int containerId, Inventory inventory, OpenData data, Container gear, @Nullable Villager villager) {
		super(VillagerGearModule.MENU, containerId);
		this.gear = gear;
		this.data = data;
		this.villager = villager;
		this.feedBefore = villager != null ? VillagerFoodModule.feed(villager) : Map.of();
		this.pointsBefore = villager != null ? VillagerFoodModule.points(villager) : 0;
		checkContainerSize(gear, VillagerGearContainer.SIZE);
		for (int i = 0; i < VillagerGearContainer.ARMOR.length; i++) {
			addSlot(new ArmorSlot(gear, i, VillagerGearContainer.ARMOR[i], ARMOR_ICONS[i]));
		}
		Optional<TagKey<Item>> tools = data.tools();
		Optional<TagKey<Item>> goods = data.goods();
		addSlot(new GearSlot(gear, VillagerGearContainer.MAIN_HAND, HANDS_X, TOP + SLOT, null) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(VillagerGearModule.WEAPONS) || tools.isPresent() && stack.is(tools.get());
			}
		});
		addSlot(new GearSlot(gear, VillagerGearContainer.OFF_HAND, HANDS_X, TOP + 2 * SLOT, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(VillagerGearModule.OFF_HAND);
			}
		});
		addSlot(new Slot(gear, VillagerGearContainer.WORK_ITEM, HANDS_X, TOP) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}

			@Override
			public boolean mayPickup(Player player) {
				return false;
			}
		});
		for (int i = 0; i < VillagerGearContainer.INVENTORY_SIZE; i++) {
			addSlot(new Slot(gear, VillagerGearContainer.INVENTORY + i, INVENTORY_X + i % INVENTORY_COLUMNS * SLOT, TOP + i / INVENTORY_COLUMNS * SLOT) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return stack.is(ItemTags.VILLAGER_PICKS_UP) || stack.has(DataComponents.VILLAGER_FOOD)
							|| goods.isPresent() && stack.is(goods.get());
				}
			});
		}
		addStandardInventorySlots(inventory, 8, PLAYER_INVENTORY_TOP);
	}

	public OpenData data() {
		return data;
	}

	/** The villager, on the server only. */
	@Nullable Villager villager() {
		return villager;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		if (villager != null) VillagerFoodModule.afterGiving(villager, feedBefore, pointsBefore);
	}

	@Override
	public boolean stillValid(Player player) {
		return gear.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		ItemStack clicked = ItemStack.EMPTY;
		Slot slot = slots.get(slotIndex);
		if (slot.hasItem() && slot.mayPickup(player)) {
			ItemStack stack = slot.getItem();
			clicked = stack.copy();
			int size = VillagerGearContainer.SIZE;
			if (slotIndex < size) {
				if (!moveItemStackTo(stack, size, slots.size(), true)) return ItemStack.EMPTY;
			} else if (!moveItemStackTo(stack, 0, size, false)) {
				return ItemStack.EMPTY;
			}
			if (stack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}
		return clicked;
	}

	/**
	 * What the client needs to lay the menu out: the villager, to draw him, and the tag of tools his trade can take.
	 */
	public record OpenData(int villagerId, Optional<TagKey<Item>> tools, Optional<TagKey<Item>> goods) {
		private static final StreamCodec<ByteBuf, Optional<TagKey<Item>>> TAG = ByteBufCodecs.optional(
				Identifier.STREAM_CODEC.map(id -> TagKey.create(Registries.ITEM, id), TagKey::location));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenData> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, OpenData::villagerId,
				TAG, OpenData::tools,
				TAG, OpenData::goods,
				OpenData::new);
	}

	/** One item, with the empty-slot icon {@code icon}. */
	private static class GearSlot extends Slot {
		private final @Nullable Identifier icon;

		GearSlot(Container container, int index, int x, int y, @Nullable Identifier icon) {
			super(container, index, x, y);
			this.icon = icon;
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

	private static final class ArmorSlot extends GearSlot {
		private final EquipmentSlot slot;

		ArmorSlot(Container container, int index, EquipmentSlot slot, Identifier icon) {
			super(container, index, ARMOR_X, TOP + index * SLOT, icon);
			this.slot = slot;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
			return equippable != null && equippable.slot() == slot;
		}
	}
}
