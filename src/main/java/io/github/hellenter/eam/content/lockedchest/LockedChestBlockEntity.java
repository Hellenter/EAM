package io.github.hellenter.eam.content.lockedchest;

import io.github.hellenter.eam.content.EamBlockEntities;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * A locked chest's three rows of items and whose they are. A villager owns it, or the village (its elder keeps the
 * village's goods in it), or nobody. Only the player who placed it opens it, and players in creative; a chest a villager
 * placed himself no player opens. Hoppers cannot reach into it.
 */
public class LockedChestBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, ExtendedMenuProvider<LockedChestMenu.OpenData> {
	public static final int SIZE = 27;
	private static final int[] NO_SLOTS = new int[0];

	private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
	private @Nullable UUID placer;
	private @Nullable UUID owner;
	private boolean village;
	private boolean villagerMade;

	public LockedChestBlockEntity(BlockPos pos, BlockState state) {
		super(EamBlockEntities.LOCKED_CHEST, pos, state);
	}

	public @Nullable UUID placer() {
		return placer;
	}

	public @Nullable UUID owner() {
		return owner;
	}

	/** Whether the chest is the village's store, kept by its elder. */
	public boolean village() {
		return village;
	}

	/** Whether a villager placed it himself: it breaks into nothing but its items. */
	public boolean villagerMade() {
		return villagerMade;
	}

	void setPlacer(@Nullable UUID player) {
		placer = player;
		setChanged();
	}

	/** Gives the chest to a villager, or to the village with {@code village}, or to nobody. */
	public void setOwner(@Nullable UUID villager, boolean village) {
		owner = villager;
		this.village = village;
		setChanged();
	}

	/** A villager placed it for himself, or for the village if he is its elder. */
	public void claimByVillager(UUID villager, boolean village) {
		villagerMade = true;
		setOwner(villager, village);
	}

	/**
	 * Moves as much of {@code stack} as fits into the chest, onto stacks of the same item first; {@code stack} keeps
	 * the rest. Whether any went in.
	 */
	public boolean insert(ItemStack stack) {
		int before = stack.getCount();
		for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
			for (int i = 0; i < SIZE && !stack.isEmpty(); i++) {
				ItemStack slot = items.get(i);
				if (pass == 0 && !slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
					int moved = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
					if (moved <= 0) continue;
					slot.grow(moved);
					stack.shrink(moved);
				} else if (pass == 1 && slot.isEmpty()) {
					items.set(i, stack.split(stack.getCount()));
				}
			}
		}
		if (stack.getCount() == before) return false;
		setChanged();
		return true;
	}

	/** Whether {@code player} may look inside. */
	public boolean opens(Player player) {
		return player.isCreative() || placer != null && placer.equals(player.getUUID());
	}

	/** Who the chest is for, as the chest's menu shows it. */
	public Component ownerName(ServerLevel level) {
		if (village) return Component.translatable("container.eam.locked_chest.village");
		if (owner == null) return Component.translatable("container.eam.locked_chest.nobody");
		Entity entity = level.getEntity(owner);
		return entity != null && entity.isAlive() ? entity.getDisplayName() : Component.translatable("container.eam.locked_chest.gone");
	}

	@Override
	protected Component getDefaultName() {
		return getBlockState().getBlock().getName();
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	public int getContainerSize() {
		return SIZE;
	}

	@Override
	public int[] getSlotsForFace(Direction direction) {
		return NO_SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
		return false;
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new LockedChestMenu(containerId, inventory, this, getScreenOpeningData((ServerPlayer) inventory.player));
	}

	/** Who has it now, as the menu marks it in its owner list. */
	int ownerId(ServerLevel level) {
		if (village) return LockedChestMenu.OpenData.VILLAGE;
		if (owner == null) return LockedChestMenu.OpenData.NOBODY;
		Entity entity = level.getEntity(owner);
		return entity != null && entity.isAlive() ? entity.getId() : LockedChestMenu.OpenData.GONE;
	}

	@Override
	public LockedChestMenu.OpenData getScreenOpeningData(ServerPlayer player) {
		ServerLevel level = player.level();
		boolean canChange = placer != null && placer.equals(player.getUUID()) || player.isCreative();
		return new LockedChestMenu.OpenData(ownerName(level), canChange, ownerId(level),
				canChange ? LockedChestModule.candidates(level, getBlockPos()) : List.of());
	}

	@Override
	public void startOpen(ContainerUser user) {
		playSound(SoundEvents.CHEST_OPEN);
	}

	@Override
	public void stopOpen(ContainerUser user) {
		playSound(SoundEvents.CHEST_CLOSE);
	}

	private void playSound(SoundEvent sound) {
		if (level != null && !level.isClientSide()) {
			level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		placer = input.read("Placer", UUIDUtil.CODEC).orElse(null);
		owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
		village = input.getBooleanOr("Village", false);
		villagerMade = input.getBooleanOr("VillagerMade", false);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.storeNullable("Placer", UUIDUtil.CODEC, placer);
		output.storeNullable("Owner", UUIDUtil.CODEC, owner);
		output.putBoolean("Village", village);
		output.putBoolean("VillagerMade", villagerMade);
	}
}
