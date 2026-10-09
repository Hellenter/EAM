package io.github.hellenter.eam.world.forging;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.enchantment.Repairable;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Repair on the anvil: free of experience and without raising the item's prior work cost, so later enchanting and
 * combining cost what they would on a new item. Metal gear takes nuggets (or ingots, worth nine), as many for 75%
 * durability as its recipe takes ingots and plates; other gear takes its vanilla repair item, a quarter each. The numbers
 * are in {@link RepairLogic}.
 */
public final class AnvilRepair {
	/** Items whose recipe has not been found are counted as this many ingots. */
	private static final int DEFAULT_INGOTS = 4;
	private static final String RUSTY_PREFIX = "rusty_";

	private static boolean enabled;
	private static @Nullable RecipeManager cachedRecipes;
	private static final Map<Item, Integer> INGOTS = new HashMap<>();

	private AnvilRepair() {
	}

	public static void init() {
		enabled = true;
	}

	/** Whether this server repairs for free. The client learns it from the menu's synced flags, not from here. */
	static boolean enabled() {
		return enabled;
	}

	/**
	 * Both sides, before the vanilla anvil result: when the right slot holds what mends the damaged item on the left,
	 * the server puts the mended item in the result slot. Returns the number of repair items it uses up, or -1 when this
	 * is no repair and the vanilla anvil goes on.
	 */
	public static int update(ForgingAnvilMenu menu, Player player, @Nullable String name) {
		ItemStack input = menu.eam$inputSlots().getItem(0);
		ItemStack addition = menu.eam$inputSlots().getItem(1);
		float worth = worth(input, addition);
		AnvilForge forge = menu.eam$forge();
		forge.setRepairing(false);
		if (!forge.repairActive() || worth <= 0 || !input.isDamageableItem() || input.getDamageValue() <= 0) {
			return -1;
		}
		forge.setRepairing(true);
		if (!(player.level() instanceof ServerLevel level)) {
			return 0;
		}
		int damage = input.getDamageValue();
		float perItem = isMetal(input)
				? RepairLogic.perNugget(input.getMaxDamage(), ingots(level.recipeAccess(), input.getItem())) * worth
				: input.getMaxDamage() * RepairLogic.OTHER_SHARE;
		int used = RepairLogic.itemsUsed(damage, perItem, addition.getCount());
		ItemStack result = input.copy();
		result.setDamageValue(damage - RepairLogic.repaired(damage, perItem, used));
		if (name != null && !StringUtil.isBlank(name)) {
			if (!name.equals(input.getHoverName().getString())) {
				result.set(DataComponents.CUSTOM_NAME, Component.literal(name));
			}
		} else {
			result.remove(DataComponents.CUSTOM_NAME);
		}
		menu.eam$resultSlots().setItem(0, result);
		return used;
	}

	/** Nuggets one {@code addition} is worth on metal gear, 1 on other gear when it is the repair item, else 0. */
	private static float worth(ItemStack input, ItemStack addition) {
		if (addition.isEmpty()) {
			return 0;
		}
		Repairable repairable = input.get(DataComponents.REPAIRABLE);
		if (repairable == null) {
			return 0;
		}
		boolean metal = false;
		for (Holder<Item> holder : repairable.items()) {
			Identifier id = BuiltInRegistries.ITEM.getKey(holder.value());
			if (!id.getPath().endsWith("_ingot")) {
				continue;
			}
			metal = true;
			if (addition.is(holder.value())) {
				return RepairLogic.NUGGETS_PER_INGOT;
			}
			Item nugget = nugget(id);
			if (nugget != null && addition.is(nugget)) {
				return 1;
			}
		}
		return !metal && repairable.isValidRepairItem(addition) ? 1 : 0;
	}

	/** Gear mended with an ingot (iron, copper, silver, gold, mithril...) is metal. */
	private static boolean isMetal(ItemStack input) {
		Repairable repairable = input.get(DataComponents.REPAIRABLE);
		if (repairable == null) {
			return false;
		}
		for (Holder<Item> holder : repairable.items()) {
			if (BuiltInRegistries.ITEM.getKey(holder.value()).getPath().endsWith("_ingot")) {
				return true;
			}
		}
		return false;
	}

	/** The nugget of an ingot by name: {@code iron_ingot} to {@code iron_nugget}; null when there is none (mithril). */
	private static @Nullable Item nugget(Identifier ingot) {
		Identifier id = Identifier.fromNamespaceAndPath(ingot.getNamespace(), ingot.getPath().replace("_ingot", "_nugget"));
		return BuiltInRegistries.ITEM.getOptional(id).filter(item -> item != Items.AIR).orElse(null);
	}

	/**
	 * Ingots (and plates) in the item's crafting recipe: every ingredient but sticks. A chestplate of 4 plates and 4
	 * ingots counts 8, a pickaxe 3. Rusty gear has no recipe and counts as the vanilla item it copies.
	 */
	private static int ingots(RecipeManager recipes, Item item) {
		if (recipes != cachedRecipes) {
			cachedRecipes = recipes;
			INGOTS.clear();
		}
		return INGOTS.computeIfAbsent(item, key -> {
			int count = recipeIngots(recipes, key);
			if (count == 0) {
				Item unrusted = unrusted(key);
				count = unrusted == null ? 0 : recipeIngots(recipes, unrusted);
			}
			return count > 0 ? count : DEFAULT_INGOTS;
		});
	}

	/** Non-stick ingredients of the first shaped recipe that makes {@code item}; 0 when there is none. */
	private static int recipeIngots(RecipeManager recipes, Item item) {
		for (RecipeHolder<?> holder : recipes.getRecipes()) {
			if (holder.value() instanceof ShapedRecipe shaped && shaped.assemble(CraftingInput.EMPTY).is(item)) {
				int count = 0;
				for (Ingredient ingredient : shaped.placementInfo().ingredients()) {
					if (!ingredient.acceptsItem(Items.STICK.builtInRegistryHolder())) {
						count++;
					}
				}
				if (count > 0) {
					return count;
				}
			}
		}
		return 0;
	}

	/** The vanilla item a rusty one copies: {@code eam:rusty_iron_axe} to {@code iron_axe}; null for other items. */
	private static @Nullable Item unrusted(Item item) {
		Identifier id = BuiltInRegistries.ITEM.getKey(item);
		if (!id.getPath().startsWith(RUSTY_PREFIX)) {
			return null;
		}
		return BuiltInRegistries.ITEM.getOptional(Identifier.withDefaultNamespace(id.getPath().substring(RUSTY_PREFIX.length())))
				.filter(found -> found != Items.AIR).orElse(null);
	}
}
