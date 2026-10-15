package io.github.hellenter.eam.survival.spoilage;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Keeps each item's spoil time with it as stacks split and merge. Stacks that differ only in their {@link Freshness}
 * stack together. Taking items from a stack takes its oldest; vanilla then grows the stack they go to, and that stack
 * picks up the times just taken. Where vanilla grows the target before it shrinks the source, the mixins hand both
 * stacks over instead. Items whose times are not known are {@link SpoilageLogic#UNSTAMPED}: fresh food the server
 * stamps on its next look.
 */
public final class SpoilageStacks {
	/** Taken times only count for a grow right after the shrink, in the same vanilla method. */
	private static final long TAKEN_NANOS = 2_000_000L;

	/** Set once spoilage is on; until then every hook does nothing. */
	private static volatile boolean active;

	/** The times last taken off a stack on this thread, for the stack they go to. */
	private static final ThreadLocal<@Nullable Taken> TAKEN = new ThreadLocal<>();
	/** Above zero while a hook sets times itself, so count changes inside it leave them alone. */
	private static final ThreadLocal<int[]> QUIET = ThreadLocal.withInitial(() -> new int[1]);

	private record Taken(Item item, List<Long> times, long at) {
	}

	private SpoilageStacks() {
	}

	static void activate() {
		active = true;
	}

	private static @Nullable Freshness freshness(ItemStack stack) {
		return active ? stack.get(SpoilageModule.FRESHNESS) : null;
	}

	/** The stack's times fitted to its count; none when it has none yet, so the server stamps it fresh. */
	static List<Long> times(ItemStack stack, Freshness freshness) {
		List<Long> times = freshness.spoilsAt();
		return times.isEmpty() ? times : SpoilageLogic.fit(times, stack.getCount(), times.getFirst());
	}

	private static List<Long> times(ItemStack stack) {
		Freshness freshness = freshness(stack);
		return freshness == null ? List.of() : times(stack, freshness);
	}

	/** {@link ItemStack#isSameItemSameComponents}: perishable stacks that differ only in freshness still stack. */
	public static boolean sameIgnoringFreshness(ItemStack a, ItemStack b, boolean same) {
		if (same || !active || !a.is(b.getItem()) || !a.has(SpoilageModule.FRESHNESS) && !b.has(SpoilageModule.FRESHNESS)) return same;
		ItemStack plainA = a.copy();
		ItemStack plainB = b.copy();
		plainA.remove(SpoilageModule.FRESHNESS);
		plainB.remove(SpoilageModule.FRESHNESS);
		return ItemStack.isSameItemSameComponents(plainA, plainB);
	}

	/** {@link ItemStack#matches}: a stack whose times changed has changed, so menus send it to the client. */
	public static boolean matchesWithFreshness(ItemStack a, ItemStack b, boolean matches) {
		if (!matches || a == b || !active) return matches;
		return Objects.equals(a.get(SpoilageModule.FRESHNESS), b.get(SpoilageModule.FRESHNESS));
	}

	/** {@link ItemStack#setCount}, before the count changes. */
	public static void onSetCount(ItemStack stack, int count) {
		if (!active || QUIET.get()[0] > 0) return;
		Freshness freshness = stack.get(SpoilageModule.FRESHNESS);
		Taken taken = TAKEN.get();
		TAKEN.remove();
		int before = stack.getCount();
		boolean fromShrink = taken != null && taken.item == stack.getItem() && System.nanoTime() - taken.at < TAKEN_NANOS;
		// An unstamped stack takes on times only when stamped items join it.
		if (count == before || freshness == null && (count < before || !fromShrink)) return;
		List<Long> times = freshness == null ? SpoilageLogic.take(List.of(), before) : times(stack, freshness);
		if (count < before) {
			int removed = before - Math.max(0, count);
			TAKEN.set(new Taken(stack.getItem(), SpoilageLogic.take(times, removed), System.nanoTime()));
			stack.set(SpoilageModule.FRESHNESS, new Freshness(SpoilageLogic.withoutOldest(times, removed)));
			return;
		}
		int added = count - before;
		List<Long> moved = SpoilageLogic.take(fromShrink ? taken.times : List.of(), added);
		stack.set(SpoilageModule.FRESHNESS, new Freshness(SpoilageLogic.merge(times, moved)));
	}

	/** {@link ItemStack#copyWithCount}: the copy gets the source's oldest times, as the source loses them when it shrinks. */
	public static ItemStack copyWithCount(ItemStack source, int count, Supplier<ItemStack> copy) {
		Freshness freshness = freshness(source);
		ItemStack result = quietly(copy);
		if (freshness != null && !result.isEmpty()) {
			result.set(SpoilageModule.FRESHNESS, new Freshness(SpoilageLogic.take(times(source, freshness), count)));
		}
		return result;
	}

	/**
	 * Moves {@code amount} items' times from {@code source} to {@code target} where vanilla grows the target first and
	 * shrinks the source after: the source then loses the same oldest times.
	 */
	public static void growFrom(ItemStack target, int amount, ItemStack source, Operation<Void> grow) {
		if (!active || freshness(source) == null && freshness(target) == null) {
			grow.call(target, amount);
			return;
		}
		List<Long> kept = times(target);
		List<Long> moved = SpoilageLogic.take(times(source), amount);
		quietly(() -> {
			grow.call(target, amount);
			return null;
		});
		target.set(SpoilageModule.FRESHNESS, new Freshness(SpoilageLogic.merge(kept, moved)));
	}

	/**
	 * A drag over slots puts a copy of the carried stack, grown by what the slot held, in each slot. The slot keeps its
	 * own items' times and gets the carried stack's oldest for the rest; those leave the carried stack, so the next slot
	 * gets the next ones.
	 */
	public static ItemStack dragCopy(ItemStack source, int count, ItemStack existing, Supplier<ItemStack> copy) {
		ItemStack result = copy.get();
		Freshness from = freshness(source);
		Freshness there = existing.isEmpty() ? null : freshness(existing);
		if (result.isEmpty() || from == null && there == null) return result;
		int added = count - existing.getCount();
		List<Long> carried = times(source);
		result.set(SpoilageModule.FRESHNESS, new Freshness(SpoilageLogic.merge(times(existing), SpoilageLogic.take(carried, added))));
		if (from != null) source.set(SpoilageModule.FRESHNESS, new Freshness(SpoilageLogic.withoutOldest(carried, added)));
		return result;
	}

	/** {@code ItemEntity.merge}: the merged stack is a copy of the target grown by the source's oldest items. */
	public static ItemStack mergeItemEntities(ItemStack to, ItemStack from, Supplier<ItemStack> merge) {
		boolean tracked = freshness(from) != null || freshness(to) != null;
		List<Long> fromTimes = times(from);
		List<Long> toTimes = times(to);
		int before = to.getCount();
		ItemStack merged = merge.get();
		TAKEN.remove();
		if (tracked) {
			int moved = merged.getCount() - before;
			merged.set(SpoilageModule.FRESHNESS, new Freshness(SpoilageLogic.merge(toTimes, SpoilageLogic.take(fromTimes, moved))));
		}
		return merged;
	}

	/** Runs {@code action} with count changes leaving times alone; the caller sets them. */
	static <T> T quietly(Supplier<T> action) {
		int[] depth = QUIET.get();
		depth[0]++;
		try {
			return action.get();
		} finally {
			depth[0]--;
		}
	}
}
