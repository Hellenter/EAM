package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.survival.thirst.FilterFlaskRecipe;
import io.github.hellenter.eam.world.tools.EarlyToolsModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.CustomCraftingRecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EamRecipeProvider extends FabricRecipeProvider {
	public EamRecipeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
		return new RecipeProvider(recipes, advancements) {
			@Override
			public void buildRecipes() {
				shapeless(RecipeCategory.MISC, EamItems.LEATHER_THREAD, 4)
						.requires(Items.LEATHER)
						.unlockedBy(getHasName(Items.LEATHER), has(Items.LEATHER))
						.save(output);

				shapeless(RecipeCategory.TOOLS, EamItems.LEATHER_FLASK)
						.requires(Items.LEATHER, 3)
						.requires(EamItems.FLASK_THREADS)
						.unlockedBy(getHasName(Items.LEATHER), has(Items.LEATHER))
						.save(output);

				shapeless(RecipeCategory.TOOLS, EamItems.FILTERED_LEATHER_FLASK)
						.requires(Items.LEATHER, 3)
						.requires(ItemTags.COALS)
						.requires(EamItems.FLASK_THREADS)
						.unlockedBy(getHasName(Items.LEATHER), has(Items.LEATHER))
						.save(output);

				// Upgrading a flask keeps the water in it.
				CustomCraftingRecipeBuilder.customCrafting(RecipeCategory.TOOLS, (common, book) -> new FilterFlaskRecipe(common, book,
								new ItemStackTemplate(EamItems.FILTERED_LEATHER_FLASK),
								List.of(Ingredient.of(EamItems.LEATHER_FLASK), tag(ItemTags.COALS), tag(EamItems.FLASK_THREADS))))
						.unlockedBy(getHasName(EamItems.LEATHER_FLASK), has(EamItems.LEATHER_FLASK))
						.save(output, "eam:filtered_leather_flask_from_flask");

				shapeless(RecipeCategory.MISC, Items.FLINT)
						.requires(EamItems.FLINT_SHARD, 4)
						.unlockedBy(getHasName(EamItems.FLINT_SHARD), has(EamItems.FLINT_SHARD))
						.save(output, "eam:flint_from_shards");

				// Four ingredients fit the inventory grid: the hatchet is made without a crafting table.
				shapeless(RecipeCategory.TOOLS, EamItems.FLINT_HATCHET)
						.requires(Items.STICK, 2)
						.requires(Items.FLINT)
						.requires(EamItems.LEATHER_THREAD)
						.unlockedBy(getHasName(Items.FLINT), has(Items.FLINT))
						.save(output);

				shapeless(RecipeCategory.COMBAT, EamItems.FLINT_KNIFE)
						.requires(Items.STICK)
						.requires(Items.FLINT)
						.unlockedBy(getHasName(Items.FLINT), has(Items.FLINT))
						.save(output);

				shapeless(RecipeCategory.TOOLS, EamItems.FLINT_AXE)
						.requires(Items.STICK, 2)
						.requires(Items.FLINT, 3)
						.requires(EamItems.LEATHER_THREAD)
						.unlockedBy(getHasName(Items.FLINT), has(Items.FLINT))
						.save(output);

				shapeless(RecipeCategory.COMBAT, EamItems.SLINGSHOT)
						.requires(Items.STICK)
						.requires(Items.LEATHER)
						.requires(EamItems.FLASK_THREADS)
						.requires(EamItems.FLASK_THREADS)
						.unlockedBy(getHasName(EamItems.PEBBLE), has(EamItems.PEBBLE))
						.save(output);

				shapeless(RecipeCategory.DECORATIONS, EamItems.CAMP_FIRE)
						.requires(Items.STICK, 3)
						.unlockedBy(getHasName(Items.STICK), has(Items.STICK))
						.save(output);

				// Threads stretched over a frame of planks on stick legs.
				shaped(RecipeCategory.DECORATIONS, EamItems.SIEVE)
						.define('T', EamItems.FLASK_THREADS)
						.define('P', ItemTags.PLANKS)
						.define('S', Items.STICK)
						.pattern("TTT")
						.pattern("P P")
						.pattern("S S")
						.unlockedBy(getHasName(Items.GRAVEL), has(Items.GRAVEL))
						.save(output);

				earlyToolRecipes(withConditions(output, ResourceConditions.not(EarlyToolsModule.REMOVED)));
			}

			/**
			 * Copies of the vanilla recipes for {@link EarlyToolsModule#REMOVED_TOOLS}, saved under the vanilla ids. They load
			 * only with the module off; with it on they are skipped, and the vanilla recipe they override is gone.
			 */
			private void earlyToolRecipes(RecipeOutput removable) {
				tool(removable, RecipeCategory.TOOLS, Items.WOODEN_PICKAXE, ItemTags.WOODEN_TOOL_MATERIALS, "XXX", " # ", " # ");
				tool(removable, RecipeCategory.TOOLS, Items.WOODEN_AXE, ItemTags.WOODEN_TOOL_MATERIALS, "XX", "X#", " #");
				tool(removable, RecipeCategory.COMBAT, Items.STONE_SWORD, ItemTags.STONE_TOOL_MATERIALS, "X", "X", "#");
				tool(removable, RecipeCategory.TOOLS, Items.STONE_SHOVEL, ItemTags.STONE_TOOL_MATERIALS, "X", "#", "#");
				tool(removable, RecipeCategory.TOOLS, Items.STONE_PICKAXE, ItemTags.STONE_TOOL_MATERIALS, "XXX", " # ", " # ");
				tool(removable, RecipeCategory.TOOLS, Items.STONE_AXE, ItemTags.STONE_TOOL_MATERIALS, "XX", "X#", " #");
				tool(removable, RecipeCategory.TOOLS, Items.STONE_HOE, ItemTags.STONE_TOOL_MATERIALS, "XX", " #", " #");
				tool(removable, RecipeCategory.COMBAT, Items.STONE_SPEAR, ItemTags.STONE_TOOL_MATERIALS, "  X", " # ", "#  ");
			}

			private void tool(RecipeOutput removable, RecipeCategory category, Item result, TagKey<Item> material, String... pattern) {
				var recipe = shaped(category, result)
						.define('#', Items.STICK)
						.define('X', material);
				for (String row : pattern) {
					recipe.pattern(row);
				}
				// Same unlock as vanilla: stone tools with their material, wooden ones with a stick.
				if (material == ItemTags.STONE_TOOL_MATERIALS) {
					recipe.unlockedBy("has_cobblestone", has(material));
				} else {
					recipe.unlockedBy("has_stick", has(Items.STICK));
				}
				recipe.save(removable);
			}
		};
	}

	/** Keeps the namespace each recipe was saved with: the early tool recipes must land on the vanilla ids. */
	@Override
	protected Identifier getRecipeIdentifier(Identifier identifier) {
		return identifier;
	}

	@Override
	public String getName() {
		return "EAM recipes";
	}
}
