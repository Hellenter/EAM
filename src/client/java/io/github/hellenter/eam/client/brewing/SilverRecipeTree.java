package io.github.hellenter.eam.client.brewing;

import io.github.hellenter.eam.content.brewing.BrewingModule;
import io.github.hellenter.eam.content.brewing.RecipeTreeLayout;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.predicates.PotionsPredicate;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.crafting.PotionIngredient;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.item.crafting.RecipeHolder;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The silver stand's recipes as a tree, built from the recipes the server synced: a recipe hangs under the recipe whose
 * result it takes, and a recipe whose input nothing brews hangs under that input (water bottles).
 */
public final class SilverRecipeTree {
	/** One bottle in the tree, with the reagent on the arrow into it ({@code EMPTY} for a root) and its parent's index. */
	public record Node(ItemStack stack, ItemStack reagent, int parent) {
	}

	private final List<Node> nodes;
	private final List<RecipeTreeLayout.Placement> placements;
	private final int columns;

	private SilverRecipeTree(List<Node> nodes) {
		this.nodes = nodes;
		int[] parents = nodes.stream().mapToInt(Node::parent).toArray();
		placements = RecipeTreeLayout.layout(parents);
		columns = RecipeTreeLayout.width(parents);
	}

	public static SilverRecipeTree build(RecipeAccess access) {
		List<RecipeHolder<BrewingRecipe>> recipes = new ArrayList<>(access.getSynchronizedRecipes().getAllOfType(BrewingModule.RECIPE_TYPE));
		recipes.sort(Comparator.comparing(holder -> holder.id().identifier().toString()));
		int count = recipes.size();
		List<ItemStack> results = new ArrayList<>(count);
		for (RecipeHolder<BrewingRecipe> holder : recipes) {
			results.add(holder.value().getOutput().create());
		}
		int[] sources = new int[count];
		for (int i = 0; i < count; i++) {
			sources[i] = -1;
			for (int j = 0; j < count && sources[i] < 0; j++) {
				if (j != i && recipes.get(i).value().getInput().test(results.get(j))) {
					sources[i] = j;
				}
			}
		}

		List<Node> nodes = new ArrayList<>();
		boolean[] added = new boolean[count];
		for (int i = 0; i < count; i++) {
			if (sources[i] >= 0) {
				continue;
			}
			ItemStack input = display(recipes.get(i).value().getInput());
			int root = -1;
			for (int n = 0; n < nodes.size(); n++) {
				if (nodes.get(n).parent() < 0 && ItemStack.isSameItemSameComponents(nodes.get(n).stack(), input)) {
					root = n;
				}
			}
			if (root < 0) {
				root = nodes.size();
				nodes.add(new Node(input, ItemStack.EMPTY, -1));
			}
			addRecipe(i, root, recipes, results, sources, added, nodes);
		}
		return new SilverRecipeTree(nodes);
	}

	/** Adds a recipe's result under {@code parent}, then every recipe that brews on from it. Recipes in a loop are skipped. */
	private static void addRecipe(int recipe, int parent, List<RecipeHolder<BrewingRecipe>> recipes, List<ItemStack> results,
			int[] sources, boolean[] added, List<Node> nodes) {
		if (added[recipe]) {
			return;
		}
		added[recipe] = true;
		int node = nodes.size();
		nodes.add(new Node(results.get(recipe), display(recipes.get(recipe).value().getReagent()), parent));
		for (int next = 0; next < recipes.size(); next++) {
			if (sources[next] == recipe) {
				addRecipe(next, node, recipes, results, sources, added, nodes);
			}
		}
	}

	/** The first item an ingredient takes, with the first potion it asks for. */
	private static ItemStack display(PotionIngredient ingredient) {
		ItemStack stack = ingredient.ingredient().items().findFirst().map(ItemStack::new).orElse(ItemStack.EMPTY);
		ingredient.potions()
				.flatMap(PotionsPredicate::potions)
				.flatMap(potions -> potions.stream().findFirst())
				.ifPresent(potion -> stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion)));
		return stack;
	}

	/** How long the bottle's first lasting effect lasts, or null when it has none (water, instant effects). */
	public static @Nullable Component duration(ItemStack stack, float tickRate) {
		PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
		if (contents == null) {
			return null;
		}
		for (MobEffectInstance effect : contents.getAllEffects()) {
			if (!effect.getEffect().value().isInstantaneous()) {
				return MobEffectUtil.formatDuration(effect, stack.getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F), tickRate);
			}
		}
		return null;
	}

	public List<Node> nodes() {
		return nodes;
	}

	public RecipeTreeLayout.Placement placement(int node) {
		return placements.get(node);
	}

	public int columns() {
		return columns;
	}

	public int depth() {
		return placements.stream().mapToInt(RecipeTreeLayout.Placement::depth).max().orElse(-1) + 1;
	}

	public boolean isEmpty() {
		return nodes.isEmpty();
	}
}
