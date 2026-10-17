package io.github.hellenter.eam.content.dryer;

import com.mojang.logging.LogUtils;

import io.github.hellenter.eam.content.EamBlockEntities;
import io.github.hellenter.eam.survival.spoilage.SpoilageModule;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * Food lying on a dryer and how long each item has dried. Food dries while no rain falls on it, then stays on the rack
 * until taken. It still ages while it dries, and food that spoils on the rack turns into what it spoils into.
 */
public class DryerBlockEntity extends BlockEntity {
	public static final int SLOTS = 4;
	/** Distance of a food slot from the centre on each axis, in blocks. */
	public static final float SLOT_OFFSET = 0.21875F;
	/** How often food on the rack is checked for spoiling, in ticks. */
	private static final int SPOIL_INTERVAL = 20;

	private static final Logger LOGGER = LogUtils.getLogger();

	private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	/** Ticks the food in each slot has dried. */
	private final int[] progress = new int[SLOTS];

	public DryerBlockEntity(BlockPos pos, BlockState state) {
		super(EamBlockEntities.FOOD_DRYER, pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, DryerBlockEntity dryer) {
		DryerSettings settings = DryerModule.settings();
		long now = SpoilageModule.now(level);
		boolean wet = level.isRainingAt(pos.above());
		boolean checkSpoiling = level.getGameTime() % SPOIL_INTERVAL == 0;
		boolean itemsChanged = false;
		boolean dried = false;

		for (int slot = 0; slot < SLOTS; slot++) {
			ItemStack stack = dryer.items.get(slot);
			if (stack.isEmpty()) {
				continue;
			}
			if (checkSpoiling) {
				List<ItemStack> spoiled = SpoilageModule.update(stack, now).spoiled();
				if (!spoiled.isEmpty()) {
					dryer.items.set(slot, stack.isEmpty() ? spoiled.getFirst() : stack);
					for (ItemStack left : stack.isEmpty() ? spoiled.subList(1, spoiled.size()) : spoiled) {
						Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, left);
					}
					dryer.progress[slot] = 0;
					itemsChanged = true;
					continue;
				}
			}
			if (!DryerLogic.canDry(stack.is(DryerModule.DRYABLE), stack.has(SpoilageModule.DRIED))) {
				continue;
			}
			int before = dryer.progress[slot];
			dryer.progress[slot] = DryerLogic.advance(before, wet);
			dried |= dryer.progress[slot] != before;
			if (DryerLogic.done(dryer.progress[slot], settings)) {
				dryer.items.set(slot, SpoilageModule.dry(stack, now, settings.water()));
				dryer.progress[slot] = 0;
				itemsChanged = true;
			}
		}

		// An empty rack, or one with only dried food or in the rain, has nothing new to save.
		if (dried || itemsChanged) {
			setChanged(level, pos, state);
		}
		if (itemsChanged) {
			level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
			level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(state));
		}
	}

	/** -1 or 1: the side of the centre a slot is on along x. */
	public static int slotX(int slot) {
		return slot == 0 || slot == 3 ? -1 : 1;
	}

	/** -1 or 1: the side of the centre a slot is on along z. */
	public static int slotZ(int slot) {
		return slot < 2 ? -1 : 1;
	}

	public NonNullList<ItemStack> getItems() {
		return items;
	}

	/** Whether {@code stack} can go on the rack: dryable food that is not dried yet. */
	public static boolean accepts(ItemStack stack) {
		return DryerLogic.canDry(stack.is(DryerModule.DRYABLE), stack.has(SpoilageModule.DRIED));
	}

	/** Puts one item from {@code stack} on a free slot; false when the rack is full or the item does not dry. */
	public boolean placeFood(ServerLevel level, @Nullable LivingEntity source, ItemStack stack) {
		if (!accepts(stack)) {
			return false;
		}
		for (int slot = 0; slot < SLOTS; slot++) {
			if (items.get(slot).isEmpty()) {
				items.set(slot, stack.consumeAndReturn(1, source));
				progress[slot] = 0;
				level.gameEvent(GameEvent.BLOCK_CHANGE, getBlockPos(), GameEvent.Context.of(source, getBlockState()));
				markUpdated();
				return true;
			}
		}
		return false;
	}

	/** Takes food off the rack, dried food first; empty when there is none. */
	public ItemStack takeFood() {
		int slot = -1;
		for (int i = 0; i < SLOTS; i++) {
			ItemStack stack = items.get(i);
			if (stack.isEmpty()) {
				continue;
			}
			if (slot == -1 || stack.has(SpoilageModule.DRIED) && !items.get(slot).has(SpoilageModule.DRIED)) {
				slot = i;
			}
		}
		if (slot == -1) {
			return ItemStack.EMPTY;
		}
		ItemStack food = items.get(slot);
		items.set(slot, ItemStack.EMPTY);
		progress[slot] = 0;
		markUpdated();
		return food;
	}

	public boolean hasFood() {
		return items.stream().anyMatch(stack -> !stack.isEmpty());
	}

	private void markUpdated() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
		}
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null) {
			Containers.dropContents(level, pos, items);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items.clear();
		ContainerHelper.loadAllItems(input, items);
		int[] saved = input.getIntArray("DryingTimes").orElse(new int[0]);
		Arrays.fill(progress, 0);
		System.arraycopy(saved, 0, progress, 0, Math.min(saved.length, SLOTS));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items, true);
		output.putIntArray("DryingTimes", progress);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** The client only needs the food, to draw it. */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(problemPath(), LOGGER)) {
			TagValueOutput output = TagValueOutput.createWithContext(reporter, registries);
			ContainerHelper.saveAllItems(output, items, true);
			return output.buildResult();
		}
	}
}
