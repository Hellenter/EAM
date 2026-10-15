package io.github.hellenter.eam.survival.spoilage;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.fridge.FridgeBlockEntity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Food spoilage. Foods in {@code eam:spoils_fast} (raw meat and fish) keep five days, foods in {@code eam:spoils}
 * (fruit, vegetables, cooked food) ten; then each item turns into what its rule says. The server stamps a perishable
 * stack when it first sees it: in a player's inventory or open container, on the ground, or in a fridge. Times are
 * absolute, so food in a chest nobody opens still rots on schedule and is swapped when someone looks. A fridge with ice
 * puts its food's times off for as long as it stays cold, see {@link FridgeBlockEntity}.
 */
public final class SpoilageModule {
	/** When each item of a perishable stack spoils. */
	public static DataComponentType<Freshness> FRESHNESS;

	/** How often players' inventories and open containers are checked, in ticks. */
	private static final int PLAYER_INTERVAL = 10;
	/** How often food on the ground is checked, in ticks. */
	private static final int GROUND_INTERVAL = 20;

	private static volatile SpoilageSettings settings = SpoilageSettings.DEFAULT;
	private static boolean enabled;

	private SpoilageModule() {
	}

	/** Registers the component whatever the config says, so client and server registries match. */
	public static void registerTypes() {
		FRESHNESS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Eam.id("freshness"),
				DataComponentType.<Freshness>builder().persistent(Freshness.CODEC).networkSynchronized(Freshness.STREAM_CODEC).build());
		SpoilageStacks.activate();
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(SpoilageSettingsLoader.ID, new SpoilageSettingsLoader());
	}

	public static void init() {
		enabled = true;
		ServerTickEvents.START_SERVER_TICK.register(server -> SpoilageStacks.forgetTaken());
		ServerTickEvents.END_SERVER_TICK.register(SpoilageModule::tickPlayers);
	}

	public static boolean enabled() {
		return enabled;
	}

	public static SpoilageSettings settings() {
		return settings;
	}

	static void setSettings(SpoilageSettings value) {
		settings = value;
	}

	/** The clock spoil times count in. */
	public static long now(Level level) {
		return level.getOverworldClockTime();
	}

	/** What spoiled in a stack and whether its times changed. */
	public record Update(ItemStack spoiled, boolean changed) {
		static final Update NONE = new Update(ItemStack.EMPTY, false);
	}

	/**
	 * Stamps a perishable stack and takes out what has spoiled by {@code now}. The stack shrinks by the spoiled items,
	 * possibly to empty; they come back as what they turn into.
	 */
	public static Update update(ItemStack stack, long now) {
		if (!enabled || stack.isEmpty()) return Update.NONE;
		Freshness freshness = stack.get(FRESHNESS);
		SpoilageSettings.Rule rule = settings.rule(stack);
		if (rule == null) {
			if (freshness == null) return Update.NONE;
			stack.remove(FRESHNESS);
			return new Update(ItemStack.EMPTY, true);
		}
		List<Long> times = freshness == null ? List.of() : freshness.spoilsAt();
		List<Long> fitted = SpoilageLogic.fit(times, stack.getCount(), now + rule.lifetime());
		boolean changed = freshness == null || !fitted.equals(times);
		int spoiled = SpoilageLogic.spoiled(fitted, now);
		if (spoiled == 0) {
			if (changed) stack.set(FRESHNESS, new Freshness(fitted));
			return changed ? new Update(ItemStack.EMPTY, true) : Update.NONE;
		}
		stack.set(FRESHNESS, new Freshness(SpoilageLogic.withoutOldest(fitted, spoiled)));
		SpoilageStacks.quietly(() -> {
			stack.shrink(spoiled);
			return null;
		});
		return new Update(new ItemStack(rule.becomes(), spoiled), true);
	}

	/** Puts a perishable stack's spoil times off by {@code ticks}: it spent them cold. */
	public static void delay(ItemStack stack, long ticks) {
		Freshness freshness = stack.get(FRESHNESS);
		if (freshness != null && ticks > 0) stack.set(FRESHNESS, new Freshness(SpoilageLogic.delay(freshness.spoilsAt(), ticks)));
	}

	/**
	 * Updates every stack in a container. A stack that spoiled whole is swapped in its slot; partly spoiled food goes to
	 * a free or matching slot, or to {@code overflow} when there is none.
	 */
	public static void updateContainer(Container container, long now, Consumer<ItemStack> overflow) {
		boolean changed = false;
		List<ItemStack> leftovers = new ArrayList<>();
		for (int i = 0; i < container.getContainerSize(); i++) {
			ItemStack stack = container.getItem(i);
			Update update = update(stack, now);
			changed |= update.changed;
			if (update.spoiled.isEmpty()) continue;
			if (stack.isEmpty()) {
				container.setItem(i, update.spoiled);
			} else {
				leftovers.add(update.spoiled);
			}
		}
		for (ItemStack leftover : leftovers) {
			insert(container, leftover);
			if (!leftover.isEmpty()) overflow.accept(leftover);
		}
		if (changed) container.setChanged();
	}

	private static void insert(Container container, ItemStack stack) {
		for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
			for (int i = 0; i < container.getContainerSize() && !stack.isEmpty(); i++) {
				ItemStack there = container.getItem(i);
				if (!container.canPlaceItem(i, stack)) continue;
				if (pass == 0 && !there.isEmpty() && ItemStack.isSameItemSameComponents(there, stack)) {
					int moved = Math.min(stack.getCount(), container.getMaxStackSize(there) - there.getCount());
					if (moved > 0) {
						there.grow(moved);
						stack.shrink(moved);
					}
				} else if (pass == 1 && there.isEmpty()) {
					container.setItem(i, stack.split(Math.min(stack.getCount(), container.getMaxStackSize(stack))));
				}
			}
		}
	}

	private static void tickPlayers(MinecraftServer server) {
		if (server.getTickCount() % PLAYER_INTERVAL != 0) return;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!player.isSpectator()) updatePlayer(player);
		}
	}

	/** The player's inventory, the stack on the cursor and whatever container the player has open. */
	private static void updatePlayer(ServerPlayer player) {
		long now = now(player.level());
		Inventory inventory = player.getInventory();
		Consumer<ItemStack> give = spoiled -> inventory.placeItemBackInInventory(spoiled, Prediction.SERVER_ONLY);
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			Update update = update(stack, now);
			if (update.spoiled.isEmpty()) continue;
			if (stack.isEmpty()) {
				inventory.setItem(i, update.spoiled);
			} else {
				give.accept(update.spoiled);
			}
		}
		AbstractContainerMenu menu = player.containerMenu;
		ItemStack carried = menu.getCarried();
		Update update = update(carried, now);
		if (!update.spoiled.isEmpty()) {
			if (carried.isEmpty()) {
				menu.setCarried(update.spoiled);
			} else {
				give.accept(update.spoiled);
			}
		}
		if (menu == player.inventoryMenu) return;
		Set<Container> containers = Collections.newSetFromMap(new IdentityHashMap<>());
		for (Slot slot : menu.slots) {
			Container container = slot.container;
			// A fridge keeps its own food, putting its times off first.
			if (container != inventory && !(container instanceof FridgeBlockEntity) && containers.add(container)) {
				updateContainer(container, now, give);
			}
		}
	}

	/** Food on the ground spoils too; what spoiled drops beside it. */
	public static void tickItemEntity(ItemEntity entity) {
		if (!enabled || entity.tickCount % GROUND_INTERVAL != 0 || !(entity.level() instanceof ServerLevel level)) return;
		ItemStack stack = entity.getItem();
		Update update = update(stack, now(level));
		if (update.spoiled.isEmpty()) {
			if (update.changed) entity.setItem(stack.copy());
			return;
		}
		if (stack.isEmpty()) {
			entity.setItem(update.spoiled);
		} else {
			entity.setItem(stack.copy());
			ItemEntity spoiled = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), update.spoiled);
			spoiled.setDefaultPickUpDelay();
			level.addFreshEntity(spoiled);
		}
	}
}
