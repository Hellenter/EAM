package io.github.hellenter.eam.content.dryer;

import io.github.hellenter.eam.content.EamItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Food that dries on a food dryer and what it becomes: fruit, berries, root vegetables, bread and cooked meat and fish. */
public final class DriedFoods {
	/** Fresh food and its dried item, in creative tab order. */
	public static final Map<Item, Item> DRIED;

	static {
		Map<Item, Item> dried = new LinkedHashMap<>();
		dried.put(Items.APPLE, EamItems.DRIED_APPLE);
		dried.put(Items.CARROT, EamItems.DRIED_CARROT);
		dried.put(Items.BEETROOT, EamItems.DRIED_BEETROOT);
		dried.put(Items.MELON_SLICE, EamItems.DRIED_MELON_SLICE);
		dried.put(Items.SWEET_BERRIES, EamItems.DRIED_SWEET_BERRIES);
		dried.put(Items.GLOW_BERRIES, EamItems.DRIED_GLOW_BERRIES);
		dried.put(EamItems.CRANBERRY, EamItems.DRIED_CRANBERRY);
		dried.put(EamItems.WOLFBERRY, EamItems.DRIED_WOLFBERRY);
		dried.put(EamItems.GARLIC, EamItems.DRIED_GARLIC);
		dried.put(Items.BREAD, EamItems.DRIED_BREAD);
		dried.put(Items.COOKED_BEEF, EamItems.DRIED_BEEF);
		dried.put(Items.COOKED_PORKCHOP, EamItems.DRIED_PORKCHOP);
		dried.put(Items.COOKED_MUTTON, EamItems.DRIED_MUTTON);
		dried.put(Items.COOKED_CHICKEN, EamItems.DRIED_CHICKEN);
		dried.put(Items.COOKED_RABBIT, EamItems.DRIED_RABBIT);
		dried.put(EamItems.COOKED_WOLF_MEAT, EamItems.DRIED_WOLF_MEAT);
		dried.put(Items.COOKED_COD, EamItems.DRIED_COD);
		dried.put(Items.COOKED_SALMON, EamItems.DRIED_SALMON);
		DRIED = Collections.unmodifiableMap(dried);
	}

	private static final Set<Item> ALL_DRIED = Set.copyOf(DRIED.values());

	private DriedFoods() {
	}

	/** What {@code fresh} becomes on a dryer, or null when it does not dry. */
	public static @Nullable Item dried(Item fresh) {
		return DRIED.get(fresh);
	}

	/** Whether {@code item} is dried food. */
	public static boolean isDried(Item item) {
		return ALL_DRIED.contains(item);
	}
}
