package io.github.hellenter.eam.content.fridge;

import io.github.hellenter.eam.content.EamBlockEntities;
import io.github.hellenter.eam.survival.spoilage.SpoilageLogic;
import io.github.hellenter.eam.survival.spoilage.SpoilageModule;
import io.github.hellenter.eam.survival.spoilage.SpoilageSettings;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

import java.util.stream.IntStream;

/**
 * A fridge's ice and food. Every second it works out how long it has been cold since the last check: first the cold left
 * from the ice melting now, then one more ice from its ice slots at a time. The food's spoil times are put off by that
 * long, so food in a fridge with ice never ages. The check counts from the last one, so a fridge in an unloaded chunk
 * catches up, ice and all, when it loads again. Ice lasts shorter in a hot biome and longer in a snowy one or next to
 * snow and ice blocks; see {@link SpoilageSettings.Climate}.
 */
public class FridgeBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, ExtendedMenuProvider<FridgeMenu.Size> {
	private static final int INTERVAL = 20;
	/** No check yet: the first one only starts the clock. */
	private static final long UNSET = Long.MIN_VALUE;
	/** Cold, in ticks, saved up before it is put on the food. */
	private static final long APPLY_COLD = 1200;

	private final FridgeMenu.Size size;
	private NonNullList<ItemStack> items;
	/** Cold left from the ice melting now, in ticks. */
	private long charge;
	/** Overworld clock time of the last check. */
	private long checkedAt = UNSET;
	/** Cold since the food's times were last put off. */
	private long pendingCold;
	/** How long ice lasts here against a temperate place, from the last check. */
	private double iceFactor = 1.0;

	/** Hours of cold left, for the menu. */
	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int dataId) {
			return (int) Math.min(Short.MAX_VALUE, coldLeft() / SpoilageLogic.HOUR);
		}

		@Override
		public void set(int dataId, int value) {
		}

		@Override
		public int getCount() {
			return 1;
		}
	};

	public FridgeBlockEntity(BlockPos pos, BlockState state) {
		super(EamBlockEntities.FRIDGE, pos, state);
		size = state.getBlock() instanceof FridgeBlock fridge ? fridge.size() : FridgeMenu.Size.SMALL;
		items = NonNullList.withSize(size.slots(), ItemStack.EMPTY);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, FridgeBlockEntity fridge) {
		if (level instanceof ServerLevel serverLevel && (level.getGameTime() + pos.asLong()) % INTERVAL == 0) fridge.refresh(serverLevel, false);
	}

	/**
	 * Melts the ice the time since the last check took. The cold builds up and is put on the food's times once a minute,
	 * or when someone opens the fridge, so the fridge is not saved and resent every second; food is checked for spoiling
	 * against the clock minus the cold not yet put on it.
	 */
	void refresh(ServerLevel level, boolean opened) {
		long now = SpoilageModule.now(level);
		if (!SpoilageModule.enabled() || checkedAt == UNSET || now < checkedAt) {
			checkedAt = now;
			return;
		}
		SpoilageSettings settings = SpoilageModule.settings();
		iceFactor = settings.climate().factor(level.getBiome(worldPosition).value().getBaseTemperature(), chilled(level));
		SpoilageLogic.Cooling cooling = SpoilageLogic.cool(now - checkedAt, charge, ice(), settings.ticksPerIce(iceFactor));
		checkedAt = now;
		charge = cooling.charge();
		pendingCold += cooling.cooled();
		boolean changed = cooling.melted() > 0;
		melt(cooling.melted());
		if (pendingCold >= APPLY_COLD || opened && pendingCold > 0) {
			for (int i = size.ice(); i < items.size(); i++) SpoilageModule.delay(items.get(i), pendingCold);
			pendingCold = 0;
			changed = true;
		}
		SpoilageModule.updateContainer(this, now - pendingCold, true, rotten -> Containers.dropItemStack(level,
				worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, rotten));
		if (changed) setChanged();
	}

	private boolean chilled(ServerLevel level) {
		for (Direction direction : Direction.values()) {
			if (level.getBlockState(worldPosition.relative(direction)).is(FridgeModule.CHILLERS)) return true;
		}
		return false;
	}

	private int ice() {
		int count = 0;
		for (int i = 0; i < size.ice(); i++) count += items.get(i).getCount();
		return count;
	}

	private void melt(int count) {
		for (int i = 0; i < size.ice() && count > 0; i++) {
			ItemStack ice = items.get(i);
			int melted = Math.min(count, ice.getCount());
			ice.shrink(melted);
			count -= melted;
		}
	}

	/** Ticks the fridge stays cold with the ice it has. */
	private long coldLeft() {
		if (!SpoilageModule.enabled()) return 0;
		return charge + ice() * SpoilageModule.settings().ticksPerIce(iceFactor);
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
		return items.size();
	}

	/** Ice slots take only ice; food slots take anything, like a chest. */
	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot >= size.ice() || stack.is(FridgeModule.ICE);
	}

	/** Hoppers fill ice and food from the top and sides and take only food from below. */
	@Override
	public int[] getSlotsForFace(Direction direction) {
		return IntStream.range(direction == Direction.DOWN ? size.ice() : 0, items.size()).toArray();
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
		return slot >= size.ice();
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new FridgeMenu(containerId, inventory, size, this, data);
	}

	@Override
	public FridgeMenu.Size getScreenOpeningData(ServerPlayer player) {
		return size;
	}

	@Override
	public void startOpen(ContainerUser user) {
		playSound(size.large() ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.BARREL_OPEN);
	}

	@Override
	public void stopOpen(ContainerUser user) {
		playSound(size.large() ? SoundEvents.IRON_TRAPDOOR_CLOSE : SoundEvents.BARREL_CLOSE);
	}

	private void playSound(SoundEvent sound) {
		if (level != null && !level.isClientSide()) {
			level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(size.slots(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		charge = input.getLongOr("Charge", 0L);
		checkedAt = input.getLongOr("CheckedAt", UNSET);
		pendingCold = input.getLongOr("PendingCold", 0L);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.putLong("Charge", charge);
		output.putLong("CheckedAt", checkedAt);
		output.putLong("PendingCold", pendingCold);
	}
}
