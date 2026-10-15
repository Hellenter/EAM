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
 * stacks over instead. A merge nothing tracked copies the target's oldest time, so food never gets fresher by stacking.
 */
public final class SpoilageStacks {
	/** Set once the component is registered; until then every hook does nothing. */
	private static volatile boolean active;

	/** The times last taken off a stack on this thread, for the stack they go to. */
	private static final ThreadLocal<@Nullable Taken> TAKEN = new ThreadLocal<>();
	/** Above zero while a hook sets times itself, so count changes inside it leave them alone. */
	private static final ThreadLocal<int[]> QUIET = ThreadLocal.withInitial(() -> new int[1]);

	private record Taken(Item item, List<Long> times) {
	}

	private SpoilageStacks() {
	}

	static void activate() {
		active = true;
	}

	/** Forgets times nobody picked up, once a tick, so they never land on an unrelated stack. */
	static void forgetTaken() {
		TAKEN.remove();
	}

	private static @Nullable Freshness freshness(ItemStack stack) {
		return active ? stack.get(SpoilageModule.FRESHNESS) : null;
	}

	/** The stack's times fitted to its count; none when it has none yet, so the server stamps it fresh. */
	static List<Long> times(ItemStack stack, Freshness freshness) {
		List<Long> times = freshness.spoilsAt();
		return times.isEmpty() ? times : SpoilageLogic.fit(times, stack.getCount(), times.getFirst());
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
		if (QUIET.get()[0] > 0) return;
		Freshness freshness = freshness(stack);
		if (freshness == null) return;
		int before = stack.getCount();
		if (count == before) return;
		List<Long> times = times(stack, freshness);
		if (count < before) {
			int removed = before - Math.max(0, count);
			TAKEN.set(new Taken(stack.getItem(), SpoilageLogic.oldest(times, removed)));
			stack.set(SpoilageModule.FRESHNESS, new Freshness(SpoilageLogic.withoutOldest(times, removed)));
			return;
		}
		int added = count - before;
		Taken taken = TAKEN.get();
		if (taken != null && taken.item == stack.getItem()) {
			List<Long> moved = SpoilageLogic.oldest(taken.times, added);
			List<Long> rest = SpoilageLogic.withoutOldest(taken.times, added);
			TAKEN.set(rest.isEmpty() ? null : new Taken(taken.item, rest));
			times = SpoilageLogic.merge(times, moved);
		}
		// Whatever the taken times did not cover copies the oldest time when the stack is next fitted.
		stack.set(SpoilageModule.FRESHNESS, new Freshness(times));
	}

	/** {@link ItemStack#copyWithCount}: the copy gets the source's oldest times, as the source loses them when it shrinks. */
	public static ItemStack copyWithCount(ItemStack source, int count, Supplier<ItemStack> copy) {
		Freshness freshness = freshness(source);
		ItemStack result = quietly(copy);
		if (freshness != null && !result.isEmpty()) {
			List<Long> times = times(source, freshness);
			result.set(SpoilageModule.FRESHNESS, new Freshness(count <= times.size() ? SpoilageLogic.oldest(times, count) : times));
		}
		return result;
	}

	/**
	 * Moves {@code amount} items' times from {@code source} to {@code target} where vanilla grows the target first and
	 * shrinks the source after: the source then loses the same oldest times.
	 */
	public static void growFrom(ItemStack target, int amount, ItemStack source, Operation<Void> grow) {
		Freshness from = freshness(source);
		Freshness to = freshness(target);
		if (from == null) {
			grow.call(target, amount);
			return;
		}
		List<Long> moved = SpoilageLogic.oldest(times(source, from), amount);
		List<Long> kept = to == null ? List.of() : times(target, to);
		quietly(() -> {
			grow.call(target, amount);
			return null;
		});
		target.set(SpoilageModule.FRESHNESS, new Freshness(SpoilageLogic.merge(kept, moved)));
	}

	/**
	 * A drag over slots puts a copy of the carried stack, grown by what the slot held, in each slot. The slot keeps its
	 * own items' times and gets the carried stack's oldest for the rest.
	 */
	public static ItemStack dragCopy(ItemStack source, int count, ItemStack existing, Supplier<ItemStack> copy) {
		ItemStack result = copy.get();
		Freshness from = freshness(source);
		Freshness there = existing.isEmpty() ? null : freshness(existing);
		if (result.isEmpty() || existing.isEmpty() || from == null && there == null) return result;
		List<Long> kept = there == null ? List.of() : times(existing, there);
		List<Long> added = from == null ? List.of() : SpoilageLogic.oldest(times(source, from), count - existing.getCount());
		result.set(SpoilageModule.FRESHNESS, new Freshness(SpoilageLogic.merge(kept, added)));
		return result;
	}

	/** {@code ItemEntity.merge}: the merged stack is a copy of the target grown by the source's oldest items. */
	public static ItemStack mergeItemEntities(ItemStack to, ItemStack from, Supplier<ItemStack> merge) {
		Freshness fromFreshness = freshness(from);
		Freshness toFreshness = freshness(to);
		List<Long> fromTimes = fromFreshness == null ? List.of() : times(from, fromFreshness);
		List<Long> toTimes = toFreshness == null ? List.of() : times(to, toFreshness);
		int before = to.getCount();
		ItemStack merged = merge.get();
		TAKEN.remove();
		if (fromFreshness != null || toFreshness != null) {
			int moved = merged.getCount() - before;
			merged.set(SpoilageModule.FRESHNESS, new Freshness(SpoilageLogic.merge(toTimes, SpoilageLogic.oldest(fromTimes, moved))));
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
