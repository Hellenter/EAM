package io.github.hellenter.eam.content.campfire;

import com.mojang.logging.LogUtils;

import io.github.hellenter.eam.content.EamBlockEntities;
import io.github.hellenter.eam.content.EamItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Arrays;

/**
 * Food on the fire, its cooking timers, the fuel timer and how many sticks the fire has burnt in its life. The
 * stick count and whether it burns are block state ({@link CampFireBlock#STICKS}, {@link CampFireBlock#LIT}).
 */
public class CampFireBlockEntity extends BlockEntity {
	public static final int SLOTS = 4;
	/** Distance of a food slot from the centre on each axis, in blocks. */
	public static final float SLOT_OFFSET = 0.3125F;

	private static final Logger LOGGER = LogUtils.getLogger();

	private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	/** Ticks since the food was put on (raw) or since its last change of stage. */
	private final int[] progress = new int[SLOTS];
	/** Stage of the food in the slot: {@link CampFireLogic#RAW}, {@code COOKED}, {@code BURNT} or {@code DONE}. */
	private final int[] cooked = new int[SLOTS];
	private int fuelTicks;
	private int burntSticks;
	/** Ticks the old sticks have burnt; they blacken with it. */
	private int litTicks;
	/** Ticks the sticks put in later have burnt; see {@link StickPile}. */
	private int newLitTicks;

	public CampFireBlockEntity(BlockPos pos, BlockState state) {
		super(EamBlockEntities.CAMP_FIRE, pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, CampFireBlockEntity fire) {
		ServerLevel serverLevel = (ServerLevel) level;
		CampFireSettings settings = CampFireModule.settings();
		boolean itemsChanged = false;

		for (int slot = 0; slot < SLOTS; slot++) {
			ItemStack stack = fire.items.get(slot);
			if (stack.isEmpty()) {
				continue;
			}
			fire.progress[slot]++;
			switch (CampFireLogic.foodStep(fire.cooked[slot], fire.progress[slot], settings)) {
				case COOK -> {
					SingleRecipeInput input = new SingleRecipeInput(stack);
					ItemStack result = serverLevel.recipeAccess().getRecipeFor(RecipeType.CAMPFIRE_COOKING, input, serverLevel)
							.map(recipe -> recipe.value().assemble(input))
							.orElse(stack);
					fire.items.set(slot, result);
					fire.cooked[slot] = CampFireLogic.COOKED;
					fire.progress[slot] = 0;
					itemsChanged = true;
				}
				case BURN -> {
					ItemStack burnt = Overcooking.burn(stack);
					if (burnt != null) {
						fire.items.set(slot, burnt);
						level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3F, 1.5F);
						itemsChanged = true;
					}
					// Food that goes straight to ash, and what does not burn at all, changes no more.
					fire.cooked[slot] = burnt != null && !burnt.is(EamItems.ASH) ? CampFireLogic.BURNT : CampFireLogic.DONE;
					fire.progress[slot] = 0;
				}
				case ASH -> {
					fire.items.set(slot, Overcooking.ash());
					fire.cooked[slot] = CampFireLogic.DONE;
					fire.progress[slot] = 0;
					level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3F, 1.5F);
					itemsChanged = true;
				}
				case WAIT -> {
				}
			}
		}

		fire.fuelTicks++;
		StickPile pile = CampFireBlock.pile(state);
		fire.litTicks++;
		if (pile.fresh() > 0) {
			fire.newLitTicks++;
		}
		StickPile charred = pile.withChar(CampFireLogic.charStage(fire.litTicks, settings), CampFireLogic.charStage(fire.newLitTicks, settings));
		if (!charred.equals(pile)) {
			pile = charred;
			state = CampFireBlock.withPile(state, pile);
			level.setBlock(pos, state, Block.UPDATE_ALL);
		}
		if (CampFireLogic.burnsStick(fire.fuelTicks, settings)) {
			fire.fuelTicks = 0;
			fire.burntSticks++;
			if (pile.burnPromotes()) {
				fire.litTicks = fire.newLitTicks;
				fire.newLitTicks = 0;
			}
			pile = pile.burn();
			BlockState burnt = CampFireBlock.withPile(state, pile)
					.setValue(CampFireBlock.COALS, CampFireLogic.coalStage(fire.burntSticks));
			if (pile.sticks() == 0) {
				// An empty pit starts over: new sticks put in it are fresh.
				burnt = burnt.setValue(CampFireBlock.LIT, false);
				fire.litTicks = 0;
				fire.newLitTicks = 0;
				level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.0F);
			}
			level.setBlock(pos, burnt, Block.UPDATE_ALL);
			level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(burnt));
			state = burnt;
		}

		setChanged(level, pos, state);
		if (itemsChanged) {
			level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
			level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(state));
		}
	}

	/** Smoke from the fire and from the food on it, as on a vanilla campfire. */
	public static void clientTick(Level level, BlockPos pos, BlockState state, CampFireBlockEntity fire) {
		RandomSource random = level.getRandom();
		if (random.nextFloat() < 0.11F) {
			for (int i = 0; i < random.nextInt(2) + 2; i++) {
				CampfireBlock.makeParticles(level, pos, false, false);
			}
		}
		for (int slot = 0; slot < SLOTS; slot++) {
			if (!fire.items.get(slot).isEmpty() && random.nextFloat() < 0.2F) {
				double x = pos.getX() + 0.5 + slotX(slot) * SLOT_OFFSET;
				double z = pos.getZ() + 0.5 + slotZ(slot) * SLOT_OFFSET;
				for (int i = 0; i < 4; i++) {
					level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + 0.2, z, 0.0, 5.0E-4, 0.0);
				}
			}
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

	/** A stick put on sticks that already blacken starts a group of its own, fresh. */
	public void startNewGroup() {
		newLitTicks = 0;
		setChanged();
	}

	public NonNullList<ItemStack> getItems() {
		return items;
	}

	/** Whether the item has a recipe on a fire, components included. */
	public static boolean cooks(ServerLevel level, ItemStack stack) {
		return level.recipeAccess().getRecipeFor(RecipeType.CAMPFIRE_COOKING, new SingleRecipeInput(stack), level).isPresent();
	}

	/** Puts one item from {@code stack} on a free slot; false when the fire is full or the item does not cook. */
	public boolean placeFood(ServerLevel level, @Nullable LivingEntity source, ItemStack stack) {
		if (!cooks(level, stack)) {
			return false;
		}
		for (int slot = 0; slot < SLOTS; slot++) {
			if (items.get(slot).isEmpty()) {
				items.set(slot, stack.consumeAndReturn(1, source));
				progress[slot] = 0;
				cooked[slot] = 0;
				level.gameEvent(GameEvent.BLOCK_CHANGE, getBlockPos(), GameEvent.Context.of(source, getBlockState()));
				markUpdated();
				return true;
			}
		}
		return false;
	}

	/** Takes food off the fire, cooked food first, then burnt food and ash; empty when there is none. */
	public ItemStack takeFood() {
		int slot = -1;
		for (int i = 0; i < SLOTS; i++) {
			if (!items.get(i).isEmpty() && (slot == -1 || CampFireLogic.takePriority(cooked[i]) > CampFireLogic.takePriority(cooked[slot]))) {
				slot = i;
			}
		}
		if (slot == -1) {
			return ItemStack.EMPTY;
		}
		ItemStack food = items.get(slot);
		items.set(slot, ItemStack.EMPTY);
		progress[slot] = 0;
		cooked[slot] = 0;
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
		if (level == null) {
			return;
		}
		Containers.dropContents(level, pos, items);
		CampFireSettings settings = CampFireModule.settings();
		int charcoal = CampFireLogic.charcoal(burntSticks, settings);
		if (charcoal > 0) {
			Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.CHARCOAL, charcoal));
		}
		if (state.hasProperty(CampFireBlock.STICKS)) {
			int sticks = CampFireLogic.sticksDropped(state.getValue(CampFireBlock.STICKS), state.getValue(CampFireBlock.LIT));
			if (sticks > 0) {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.STICK, sticks));
			}
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items.clear();
		ContainerHelper.loadAllItems(input, items);
		copyInto(input.getIntArray("CookingTimes").orElse(new int[0]), progress);
		copyInto(input.getIntArray("Cooked").orElse(new int[0]), cooked);
		fuelTicks = input.getIntOr("FuelTicks", 0);
		burntSticks = input.getIntOr("BurntSticks", 0);
		litTicks = input.getIntOr("LitTicks", 0);
		newLitTicks = input.getIntOr("NewLitTicks", 0);
	}

	private static void copyInto(int[] from, int[] to) {
		Arrays.fill(to, 0);
		System.arraycopy(from, 0, to, 0, Math.min(from.length, to.length));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items, true);
		output.putIntArray("CookingTimes", progress);
		output.putIntArray("Cooked", cooked);
		output.putInt("FuelTicks", fuelTicks);
		output.putInt("BurntSticks", burntSticks);
		output.putInt("LitTicks", litTicks);
		output.putInt("NewLitTicks", newLitTicks);
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
