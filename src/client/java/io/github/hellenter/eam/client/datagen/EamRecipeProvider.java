package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.survival.thirst.FilterFlaskRecipe;
import io.github.hellenter.eam.world.tools.DiamondGearModule;
import io.github.hellenter.eam.world.forging.ForgingModule;
import io.github.hellenter.eam.world.smelting.BlastFurnaceModule;
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
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
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

				silverRecipes();

				earlyToolRecipes(withConditions(output, ResourceConditions.not(EarlyToolsModule.REMOVED)));
				diamondGearRecipes(withConditions(output, ResourceConditions.not(DiamondGearModule.REMOVED)));
				ironPlateRecipes(withConditions(output, ForgingModule.ENABLED));
				ironIngotArmorRecipes(withConditions(output, ResourceConditions.not(ForgingModule.ENABLED)));
				blastFurnaceRecipe(withConditions(output, BlastFurnaceModule.IRON_ONLY));
				furnaceIronRecipes(withConditions(output, ResourceConditions.not(BlastFurnaceModule.IRON_ONLY)));
			}

			/** The blast furnace takes any ingot from {@link BlastFurnaceModule#INGOTS}, since iron can no longer be smelted without one. */
			private void blastFurnaceRecipe(RecipeOutput enabled) {
				shaped(RecipeCategory.DECORATIONS, Items.BLAST_FURNACE)
						.define('I', BlastFurnaceModule.INGOTS)
						.define('X', Items.FURNACE)
						.define('#', Items.SMOOTH_STONE)
						.pattern("III")
						.pattern("IXI")
						.pattern("###")
						.unlockedBy(getHasName(Items.SMOOTH_STONE), has(Items.SMOOTH_STONE))
						.save(enabled, "eam:blast_furnace");
			}

			/**
			 * Copies of the vanilla furnace recipes that yield iron, and of the iron ingot blast furnace recipe, under the vanilla
			 * ids. They load only with {@link BlastFurnaceModule} off; with it on the furnace cannot smelt iron at all.
			 */
			private void furnaceIronRecipes(RecipeOutput removable) {
				// Same as vanilla oreSmelting, which always saves to the unconditioned output.
				for (Item ore : List.of(Items.IRON_ORE, Items.DEEPSLATE_IRON_ORE, Items.RAW_IRON)) {
					SimpleCookingRecipeBuilder.smelting(Ingredient.of(ore), RecipeCategory.MISC, CookingBookCategory.MISC, Items.IRON_INGOT, 0.7F, 200)
							.group("iron_ingot")
							.unlockedBy(getHasName(ore), has(ore))
							.save(removable, getItemName(Items.IRON_INGOT) + "_from_smelting_" + getItemName(ore));
				}
				SimpleCookingRecipeBuilder.smelting(Ingredient.of(Items.IRON_PICKAXE, Items.IRON_SHOVEL, Items.IRON_AXE, Items.IRON_HOE,
								Items.IRON_SWORD, Items.IRON_SPEAR, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS,
								Items.IRON_BOOTS, Items.IRON_HORSE_ARMOR, Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE,
								Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS, Items.IRON_NAUTILUS_ARMOR),
								RecipeCategory.MISC, CookingBookCategory.MISC, Items.IRON_NUGGET, 0.1F, 200)
						.unlockedBy(getHasName(Items.IRON_NUGGET), has(Items.IRON_NUGGET))
						.save(removable, getSmeltingRecipeName(Items.IRON_NUGGET));
				shaped(RecipeCategory.DECORATIONS, Items.BLAST_FURNACE)
						.define('I', Items.IRON_INGOT)
						.define('X', Items.FURNACE)
						.define('#', Items.SMOOTH_STONE)
						.pattern("III")
						.pattern("IXI")
						.pattern("###")
						.unlockedBy(getHasName(Items.SMOOTH_STONE), has(Items.SMOOTH_STONE))
						.save(removable);
			}

			/** Silver smelts like copper, packs into nuggets and blocks like iron, and makes copper-shaped tools and armor. */
			private void silverRecipes() {
				for (Item smeltable : List.of(EamItems.SILVER_ORE, EamItems.DEEPSLATE_SILVER_ORE, EamItems.RAW_SILVER)) {
					SimpleCookingRecipeBuilder.smelting(Ingredient.of(smeltable), RecipeCategory.MISC, CookingBookCategory.MISC,
									EamItems.SILVER_INGOT, 0.7F, 200)
							.group("silver_ingot")
							.unlockedBy(getHasName(smeltable), has(smeltable))
							.save(output, "eam:silver_ingot_from_smelting_" + getItemName(smeltable));
					SimpleCookingRecipeBuilder.blasting(Ingredient.of(smeltable), RecipeCategory.MISC, CookingBookCategory.MISC,
									EamItems.SILVER_INGOT, 0.7F, 100)
							.group("silver_ingot")
							.unlockedBy(getHasName(smeltable), has(smeltable))
							.save(output, "eam:silver_ingot_from_blasting_" + getItemName(smeltable));
				}
				nineBlockStorageRecipes(RecipeCategory.MISC, EamItems.SILVER_INGOT, RecipeCategory.BUILDING_BLOCKS, EamItems.SILVER_BLOCK,
						"eam:silver_block", null, "eam:silver_ingot_from_silver_block", "silver_ingot");
				nineBlockStorageRecipes(RecipeCategory.MISC, EamItems.SILVER_NUGGET, RecipeCategory.MISC, EamItems.SILVER_INGOT,
						"eam:silver_ingot_from_nuggets", "silver_ingot", "eam:silver_nugget", null);

				silver(RecipeCategory.COMBAT, EamItems.SILVER_SWORD, true, "X", "X", "#");
				silver(RecipeCategory.TOOLS, EamItems.SILVER_SHOVEL, true, "X", "#", "#");
				silver(RecipeCategory.TOOLS, EamItems.SILVER_PICKAXE, true, "XXX", " # ", " # ");
				silver(RecipeCategory.TOOLS, EamItems.SILVER_AXE, true, "XX", "X#", " #");
				silver(RecipeCategory.TOOLS, EamItems.SILVER_HOE, true, "XX", " #", " #");
				silver(RecipeCategory.COMBAT, EamItems.SILVER_HELMET, false, "XXX", "X X");
				silver(RecipeCategory.COMBAT, EamItems.SILVER_CHESTPLATE, false, "X X", "XXX", "XXX");
				silver(RecipeCategory.COMBAT, EamItems.SILVER_LEGGINGS, false, "XXX", "X X", "X X");
				silver(RecipeCategory.COMBAT, EamItems.SILVER_BOOTS, false, "X X", "X X");
			}

			private void silver(RecipeCategory category, Item result, boolean tool, String... pattern) {
				var recipe = shaped(category, result)
						.define('X', EamItems.SILVER_INGOT);
				if (tool) {
					recipe.define('#', Items.STICK);
				}
				for (String row : pattern) {
					recipe.pattern(row);
				}
				recipe.unlockedBy(getHasName(EamItems.SILVER_INGOT), has(EamItems.SILVER_INGOT))
						.save(output);
			}

			/** Iron ingot to hot iron in a furnace or blast furnace, and iron armor from plates. Plates come only from the anvil. */
			private void ironPlateRecipes(RecipeOutput enabled) {
				// The furnace heats iron only while iron is not blast-furnace-only.
				SimpleCookingRecipeBuilder.smelting(Ingredient.of(Items.IRON_INGOT), RecipeCategory.MISC, CookingBookCategory.MISC,
								EamItems.HOT_IRON_INGOT, 0.0F, 200)
						.unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
						.save(withConditions(enabled, ResourceConditions.not(BlastFurnaceModule.IRON_ONLY)), "eam:hot_iron_ingot_from_smelting");
				SimpleCookingRecipeBuilder.blasting(Ingredient.of(Items.IRON_INGOT), RecipeCategory.MISC, CookingBookCategory.MISC,
								EamItems.HOT_IRON_INGOT, 0.0F, 100)
						.unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
						.save(enabled, "eam:hot_iron_ingot_from_blasting");
				ironArmor(enabled, EamItems.IRON_PLATE, true, Items.IRON_HELMET, "XXX", "X X");
				ironArmor(enabled, EamItems.IRON_PLATE, true, Items.IRON_CHESTPLATE, "X X", "XXX", "XXX");
				ironArmor(enabled, EamItems.IRON_PLATE, true, Items.IRON_LEGGINGS, "XXX", "X X", "X X");
				ironArmor(enabled, EamItems.IRON_PLATE, true, Items.IRON_BOOTS, "X X", "X X");
			}

			/** Copies of the vanilla iron armor recipes under the vanilla ids, loaded only with the plates off, like the early tools. */
			private void ironIngotArmorRecipes(RecipeOutput removable) {
				ironArmor(removable, Items.IRON_INGOT, false, Items.IRON_HELMET, "XXX", "X X");
				ironArmor(removable, Items.IRON_INGOT, false, Items.IRON_CHESTPLATE, "X X", "XXX", "XXX");
				ironArmor(removable, Items.IRON_INGOT, false, Items.IRON_LEGGINGS, "XXX", "X X", "X X");
				ironArmor(removable, Items.IRON_INGOT, false, Items.IRON_BOOTS, "X X", "X X");
			}

			/** Plate recipes get our own ids; the ingot copies keep the vanilla ones so they shadow the vanilla recipes. */
			private void ironArmor(RecipeOutput output, Item material, boolean ownId, Item result, String... pattern) {
				var recipe = shaped(RecipeCategory.COMBAT, result)
						.define('X', material);
				for (String row : pattern) {
					recipe.pattern(row);
				}
				recipe.unlockedBy(getHasName(material), has(material));
				if (ownId) {
					recipe.save(output, "eam:" + getItemName(result));
				} else {
					recipe.save(output);
				}
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

			/** Copies of the vanilla recipes for {@link DiamondGearModule#REMOVED_GEAR}, the same way as the early tools. */
			private void diamondGearRecipes(RecipeOutput removable) {
				TagKey<Item> diamond = ItemTags.DIAMOND_TOOL_MATERIALS;
				tool(removable, RecipeCategory.COMBAT, Items.DIAMOND_SWORD, diamond, "X", "X", "#");
				tool(removable, RecipeCategory.TOOLS, Items.DIAMOND_SHOVEL, diamond, "X", "#", "#");
				tool(removable, RecipeCategory.TOOLS, Items.DIAMOND_PICKAXE, diamond, "XXX", " # ", " # ");
				tool(removable, RecipeCategory.TOOLS, Items.DIAMOND_AXE, diamond, "XX", "X#", " #");
				tool(removable, RecipeCategory.TOOLS, Items.DIAMOND_HOE, diamond, "XX", " #", " #");
				tool(removable, RecipeCategory.COMBAT, Items.DIAMOND_SPEAR, diamond, "  X", " # ", "#  ");
				armor(removable, Items.DIAMOND_HELMET, "XXX", "X X");
				armor(removable, Items.DIAMOND_CHESTPLATE, "X X", "XXX", "XXX");
				armor(removable, Items.DIAMOND_LEGGINGS, "XXX", "X X", "X X");
				armor(removable, Items.DIAMOND_BOOTS, "X X", "X X");
			}

			private void armor(RecipeOutput removable, Item result, String... pattern) {
				var recipe = shaped(RecipeCategory.COMBAT, result)
						.define('X', Items.DIAMOND);
				for (String row : pattern) {
					recipe.pattern(row);
				}
				recipe.unlockedBy("has_diamond", has(Items.DIAMOND)).save(removable);
			}

			private void tool(RecipeOutput removable, RecipeCategory category, Item result, TagKey<Item> material, String... pattern) {
				var recipe = shaped(category, result)
						.define('#', Items.STICK)
						.define('X', material);
				for (String row : pattern) {
					recipe.pattern(row);
				}
				// Same unlock as vanilla: stone and diamond tools with their material, wooden ones with a stick.
				if (material == ItemTags.DIAMOND_TOOL_MATERIALS) {
					recipe.unlockedBy("has_diamond", has(material));
				} else if (material == ItemTags.STONE_TOOL_MATERIALS) {
					recipe.unlockedBy("has_cobblestone", has(material));
				} else {
					recipe.unlockedBy("has_stick", has(Items.STICK));
				}
				recipe.save(removable);
			}
		};
	}

	/** Keeps the namespace each recipe was saved with: the removed gear recipes must land on the vanilla ids. */
	@Override
	protected Identifier getRecipeIdentifier(Identifier identifier) {
		return identifier;
	}

	@Override
	public String getName() {
		return "EAM recipes";
	}
}
