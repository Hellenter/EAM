package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamItems;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

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

				shapeless(RecipeCategory.MISC, Items.FLINT)
						.requires(EamItems.FLINT_SHARD, 4)
						.unlockedBy(getHasName(EamItems.FLINT_SHARD), has(EamItems.FLINT_SHARD))
						.save(output, "eam:flint_from_shards");

				shapeless(RecipeCategory.DECORATIONS, EamItems.CAMP_FIRE)
						.requires(Items.STICK, 3)
						.unlockedBy(getHasName(Items.STICK), has(Items.STICK))
						.save(output);
			}
		};
	}

	@Override
	public String getName() {
		return "EAM recipes";
	}
}
