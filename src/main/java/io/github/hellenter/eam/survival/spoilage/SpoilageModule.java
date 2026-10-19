package io.github.hellenter.eam.survival.spoilage;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.fridge.FridgeBlockEntity;
import io.github.hellenter.eam.survival.thirst.ThirstModule;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.ItemComponentTooltipProviderRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.UseRemainder;
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
	/** Food dried on a dryer: keeps longer and costs a little water when eaten. */
	public static DataComponentType<Dried> DRIED;

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
		DRIED = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Eam.id("dried"),
				DataComponentType.<Dried>builder().persistent(Dried.CODEC).networkSynchronized(Dried.STREAM_CODEC).build());
		ItemComponentTooltipProviderRegistry.addLast(DRIED);
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(SpoilageSettingsLoader.ID, new SpoilageSettingsLoader());
	}

	public static void init() {
		enabled = true;
		SpoilageStacks.activate();
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

	/**
	 * What spoiled in a stack, with what its items leave behind (a soup's bowl), and whether its times changed.
	 */
	public record Update(List<ItemStack> spoiled, boolean changed) {
		static final Update NONE = new Update(List.of(), false);
		static final Update CHANGED = new Update(List.of(), true);
	}

	/**
	 * Stamps a perishable stack and takes out what has spoiled by {@code now}. The stack shrinks by the spoiled items,
	 * possibly to empty; they come back as what they turn into, first, then what they leave behind.
	 */
	public static Update update(ItemStack stack, long now) {
		if (!enabled || stack.isEmpty()) return Update.NONE;
		Freshness freshness = stack.get(FRESHNESS);
		SpoilageSettings.Rule rule = settings.rule(stack);
		if (rule == null) {
			if (freshness == null) return Update.NONE;
			stack.remove(FRESHNESS);
			return Update.CHANGED;
		}
		List<Long> times = freshness == null ? List.of() : freshness.spoilsAt();
		long fresh = now + lifetime(stack, rule);
		List<Long> fitted = SpoilageLogic.stamp(SpoilageLogic.fit(times, stack.getCount(), fresh), fresh);
		boolean changed = freshness == null || !fitted.equals(times);
		int spoiled = SpoilageLogic.spoiled(fitted, now);
		if (spoiled == 0) {
			if (changed) stack.set(FRESHNESS, new Freshness(fitted));
			return changed ? Update.CHANGED : Update.NONE;
		}
		UseRemainder remainder = stack.get(DataComponents.USE_REMAINDER);
		stack.set(FRESHNESS, new Freshness(SpoilageLogic.withoutOldest(fitted, spoiled)));
		SpoilageStacks.quietly(() -> {
			stack.shrink(spoiled);
			return null;
		});
		List<ItemStack> out = new ArrayList<>();
		rule.becomes().ifPresent(becomes -> out.add(new ItemStack(becomes, spoiled)));
		if (remainder != null) {
			ItemStack left = remainder.convertInto().create();
			left.setCount(left.getCount() * spoiled);
			out.add(left);
		}
		return new Update(out, true);
	}

	/** Ticks the stack keeps from fresh: dried food lasts {@code dried_multiplier} times as long. */
	private static long lifetime(ItemStack stack, SpoilageSettings.Rule rule) {
		long lifetime = rule.lifetime();
		return stack.has(DRIED) ? Math.round(lifetime * settings.driedMultiplier()) : lifetime;
	}

	/**
	 * {@code food} turned into its dried item: it no longer quenches thirst, costs {@code water} when eaten instead, and
	 * each item keeps the time it had left {@code dried_multiplier} times over.
	 */
	public static ItemStack dry(ItemStack food, Item driedItem, long now, int water) {
		ItemStack dried = food.transmuteCopy(driedItem);
		dried.remove(ThirstModule.HYDRATION);
		dried.set(DRIED, new Dried(water));
		SpoilageSettings.Rule rule = settings.rule(food);
		Freshness freshness = food.get(FRESHNESS);
		if (enabled && rule != null) {
			List<Long> times = freshness == null ? List.of() : freshness.spoilsAt();
			times = SpoilageLogic.fit(times, food.getCount(), SpoilageLogic.UNSTAMPED);
			dried.set(FRESHNESS, new Freshness(SpoilageLogic.dried(times, now, rule.lifetime(), settings.driedMultiplier())));
		}
		return dried;
	}

	/** Puts a perishable stack's spoil times off by {@code ticks}: it spent them cold. */
	public static void delay(ItemStack stack, long ticks) {
		Freshness freshness = stack.get(FRESHNESS);
		if (freshness != null && ticks > 0) stack.set(FRESHNESS, new Freshness(SpoilageLogic.delay(freshness.spoilsAt(), ticks)));
	}

	/**
	 * Updates every stack in a container. A stack that spoiled whole is swapped in its slot. The rest goes to a free or
	 * matching slot when {@code storage} is set, else, or when there is no room, to {@code overflow}: a crafting grid or a
	 * furnace never gets rotten food put in another slot.
	 */
	public static void updateContainer(Container container, long now, boolean storage, Consumer<ItemStack> overflow) {
		boolean changed = false;
		List<ItemStack> leftovers = new ArrayList<>();
		for (int i = 0; i < container.getContainerSize(); i++) {
			ItemStack stack = container.getItem(i);
			Update update = update(stack, now);
			changed |= update.changed;
			if (update.spoiled.isEmpty()) continue;
			List<ItemStack> spoiled = update.spoiled;
			if (stack.isEmpty()) {
				container.setItem(i, spoiled.getFirst());
				spoiled = spoiled.subList(1, spoiled.size());
			}
			leftovers.addAll(spoiled);
		}
		for (ItemStack leftover : leftovers) {
			if (storage) insert(container, leftover);
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
			List<ItemStack> spoiled = update(stack, now).spoiled;
			if (spoiled.isEmpty()) continue;
			if (stack.isEmpty()) {
				inventory.setItem(i, spoiled.getFirst());
				spoiled = spoiled.subList(1, spoiled.size());
			}
			spoiled.forEach(give);
		}
		AbstractContainerMenu menu = player.containerMenu;
		ItemStack carried = menu.getCarried();
		List<ItemStack> spoiled = update(carried, now).spoiled;
		if (!spoiled.isEmpty()) {
			if (carried.isEmpty()) {
				menu.setCarried(spoiled.getFirst());
				spoiled = spoiled.subList(1, spoiled.size());
			}
			spoiled.forEach(give);
		}
		if (menu == player.inventoryMenu) return;
		Set<Container> containers = Collections.newSetFromMap(new IdentityHashMap<>());
		for (Slot slot : menu.slots) {
			Container container = slot.container;
			// A fridge keeps its own food, putting its times off first; a crafting result is not real food yet.
			if (container != inventory && !(container instanceof FridgeBlockEntity) && !(container instanceof ResultContainer)
					&& containers.add(container)) {
				updateContainer(container, now, false, give);
			}
		}
	}

	/** Food on the ground spoils too; what spoiled drops beside it. New drops are stamped on their first tick. */
	public static void tickItemEntity(ItemEntity entity) {
		if (!enabled || entity.isRemoved() || entity.tickCount % GROUND_INTERVAL != 0 && entity.tickCount > 1
				|| !(entity.level() instanceof ServerLevel level)) {
			return;
		}
		ItemStack stack = entity.getItem();
		Update update = update(stack, now(level));
		if (!update.changed) return;
		List<ItemStack> spoiled = update.spoiled;
		if (stack.isEmpty() && !spoiled.isEmpty()) {
			entity.setItem(spoiled.getFirst());
			spoiled = spoiled.subList(1, spoiled.size());
		} else {
			entity.setItem(stack.copy());
		}
		for (ItemStack left : spoiled) {
			ItemEntity drop = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), left);
			drop.setDefaultPickUpDelay();
			level.addFreshEntity(drop);
		}
	}
}
