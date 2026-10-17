package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.arrows.ArrowHeads;
import io.github.hellenter.eam.content.brewing.BrewingModule;
import io.github.hellenter.eam.content.brewing.SilverBrewingRecipe;
import io.github.hellenter.eam.survival.thirst.FilterFlaskRecipe;
import io.github.hellenter.eam.survival.thirst.RawWater;
import io.github.hellenter.eam.world.tools.DiamondGearModule;
import io.github.hellenter.eam.world.forging.ForgingModule;
import io.github.hellenter.eam.world.smelting.BlastFurnaceModule;
import io.github.hellenter.eam.world.tools.EarlyToolsModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.recipe.v1.ingredient.DefaultCustomIngredients;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.predicates.PotionsPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.PotionIngredient;
import net.minecraft.data.recipes.CustomCraftingRecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.RecipeUnlockAdvancementBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;

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
				// Swamp flowers give dye like vanilla flowers: the tall fireweed two, the small marigold one.
				shapeless(RecipeCategory.MISC, Items.DYE.pick(DyeColor.MAGENTA), 2)
						.requires(EamItems.FIREWEED)
						.unlockedBy(getHasName(EamItems.FIREWEED), has(EamItems.FIREWEED))
						.save(output, "eam:magenta_dye_from_fireweed");
				shapeless(RecipeCategory.MISC, Items.DYE.pick(DyeColor.YELLOW))
						.requires(EamItems.MARSH_MARIGOLD)
						.unlockedBy(getHasName(EamItems.MARSH_MARIGOLD), has(EamItems.MARSH_MARIGOLD))
						.save(output, "eam:yellow_dye_from_marsh_marigold");
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

				shapeless(RecipeCategory.TOOLS, EamItems.FILTERED_GLASS_FLASK)
						.requires(Items.GLASS, 3)
						.requires(ItemTags.COALS)
						.requires(EamItems.SILVER_NUGGET, 3)
						.unlockedBy(getHasName(EamItems.SILVER_NUGGET), has(EamItems.SILVER_NUGGET))
						.save(output);
				// A spent charcoal-silver filter is replaced with the same coal and silver; the water stays.
				CustomCraftingRecipeBuilder.customCrafting(RecipeCategory.TOOLS, (common, book) -> new FilterFlaskRecipe(common, book,
								new ItemStackTemplate(EamItems.FILTERED_GLASS_FLASK),
								List.of(Ingredient.of(EamItems.FILTERED_GLASS_FLASK), tag(ItemTags.COALS), Ingredient.of(EamItems.SILVER_NUGGET),
										Ingredient.of(EamItems.SILVER_NUGGET), Ingredient.of(EamItems.SILVER_NUGGET))))
						.unlockedBy(getHasName(EamItems.FILTERED_GLASS_FLASK), has(EamItems.FILTERED_GLASS_FLASK))
						.save(output, "eam:filtered_glass_flask_new_filter");

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

				// Metal knives: ingot and stick, made in a crafting table with a knife of the tier below.
				knife(EamItems.COPPER_KNIFE, Items.COPPER_INGOT);
				knife(EamItems.SILVER_KNIFE, EamItems.SILVER_INGOT);
				knife(EamItems.IRON_KNIFE, Items.IRON_INGOT);
				knife(EamItems.ANCIENT_METAL_KNIFE, EamItems.ANCIENT_METAL_INGOT);
				knife(EamItems.MITHRIL_KNIFE, EamItems.MITHRIL_INGOT);
				knife(EamItems.ADAMANTIUM_KNIFE, EamItems.ADAMANTIUM_INGOT);

				// Anvils like the vanilla one, from their own metal; hammers: two ingots across a stick handle.
				anvil(EamItems.COPPER_ANVIL, Items.COPPER_BLOCK.weathering().unaffected(), Items.COPPER_INGOT);
				anvil(EamItems.SILVER_ANVIL, EamItems.SILVER_BLOCK, EamItems.SILVER_INGOT);
				anvil(EamItems.MITHRIL_ANVIL, EamItems.MITHRIL_BLOCK, EamItems.MITHRIL_INGOT);
				anvil(EamItems.ADAMANTIUM_ANVIL, EamItems.ADAMANTIUM_BLOCK, EamItems.ADAMANTIUM_INGOT);
				hammer(EamItems.COPPER_HAMMER, Items.COPPER_INGOT);
				hammer(EamItems.SILVER_HAMMER, EamItems.SILVER_INGOT);
				hammer(EamItems.IRON_HAMMER, Items.IRON_INGOT);
				hammer(EamItems.ANCIENT_METAL_HAMMER, EamItems.ANCIENT_METAL_INGOT);
				hammer(EamItems.MITHRIL_HAMMER, EamItems.MITHRIL_INGOT);
				hammer(EamItems.ADAMANTIUM_HAMMER, EamItems.ADAMANTIUM_INGOT);

				// Bars, doors and trapdoors like the iron ones; copper has its own in vanilla.
				metalBuilding(EamItems.SILVER_BARS, EamItems.SILVER_DOOR, EamItems.SILVER_TRAPDOOR, EamItems.SILVER_INGOT);
				metalBuilding(EamItems.MITHRIL_BARS, EamItems.MITHRIL_DOOR, EamItems.MITHRIL_TRAPDOOR, EamItems.MITHRIL_INGOT);
				// Buckets: the vanilla shape with another metal.
				bucket(EamItems.COPPER_BUCKET, Items.COPPER_INGOT);
				bucket(EamItems.SILVER_BUCKET, EamItems.SILVER_INGOT);
				bucket(EamItems.GOLDEN_BUCKET, Items.GOLD_INGOT);
				bucket(EamItems.MITHRIL_BUCKET, EamItems.MITHRIL_INGOT);
				// Saws: two ingots in a diagonal blade with a stick handle.
				saw(EamItems.COPPER_SAW, Items.COPPER_INGOT);
				saw(EamItems.SILVER_SAW, EamItems.SILVER_INGOT);
				saw(EamItems.IRON_SAW, Items.IRON_INGOT);
				saw(EamItems.ANCIENT_METAL_SAW, EamItems.ANCIENT_METAL_INGOT);
				saw(EamItems.MITHRIL_SAW, EamItems.MITHRIL_INGOT);
				saw(EamItems.ADAMANTIUM_SAW, EamItems.ADAMANTIUM_INGOT);
				// Chainmail: a ring of four nuggets, rings in the shapes ingot armor takes ingots. Iron chainmail is the vanilla one.
				chainmail(Items.COPPER_NUGGET, EamItems.COPPER_RING, EamItems.COPPER_CHAINMAIL_HELMET, EamItems.COPPER_CHAINMAIL_CHESTPLATE,
						EamItems.COPPER_CHAINMAIL_LEGGINGS, EamItems.COPPER_CHAINMAIL_BOOTS);
				chainmail(EamItems.SILVER_NUGGET, EamItems.SILVER_RING, EamItems.SILVER_CHAINMAIL_HELMET, EamItems.SILVER_CHAINMAIL_CHESTPLATE,
						EamItems.SILVER_CHAINMAIL_LEGGINGS, EamItems.SILVER_CHAINMAIL_BOOTS);
				chainmail(Items.IRON_NUGGET, EamItems.IRON_RING, Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE,
						Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS);
				chainmail(EamItems.ANCIENT_METAL_NUGGET, EamItems.ANCIENT_METAL_RING, EamItems.ANCIENT_METAL_CHAINMAIL_HELMET,
						EamItems.ANCIENT_METAL_CHAINMAIL_CHESTPLATE, EamItems.ANCIENT_METAL_CHAINMAIL_LEGGINGS, EamItems.ANCIENT_METAL_CHAINMAIL_BOOTS);
				chainmail(EamItems.MITHRIL_NUGGET, EamItems.MITHRIL_RING, EamItems.MITHRIL_CHAINMAIL_HELMET, EamItems.MITHRIL_CHAINMAIL_CHESTPLATE,
						EamItems.MITHRIL_CHAINMAIL_LEGGINGS, EamItems.MITHRIL_CHAINMAIL_BOOTS);
				// Arrows like the vanilla one with a metal nugget for a head instead of flint.
				arrow(EamItems.COPPER_ARROW, Items.COPPER_NUGGET);
				arrow(EamItems.SILVER_ARROW, EamItems.SILVER_NUGGET);
				arrow(EamItems.IRON_ARROW, Items.IRON_NUGGET);
				arrow(EamItems.ANCIENT_METAL_ARROW, EamItems.ANCIENT_METAL_NUGGET);
				arrow(EamItems.MITHRIL_ARROW, EamItems.MITHRIL_NUGGET);

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

				// Heart in a jar: glass jar with a lapis lid held by silver clasps.
				shaped(RecipeCategory.FOOD, EamItems.HEART_IN_JAR)
						.define('N', EamItems.SILVER_NUGGET)
						.define('L', Items.LAPIS_LAZULI)
						.define('G', Items.GLASS)
						.define('H', EamItems.VAMPIRE_HEART)
						.pattern("NLN")
						.pattern("GHG")
						.pattern("NGN")
						.unlockedBy(getHasName(EamItems.VAMPIRE_HEART), has(EamItems.VAMPIRE_HEART))
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

				// Like a fence, but of stripped logs: the posts of the underworld mines' supports.
				shaped(RecipeCategory.DECORATIONS, EamItems.STRIPPED_POPLAR_FENCE, 3)
						.define('L', Items.STRIPPED_POPLAR_LOG)
						.define('S', Items.STICK)
						.pattern("LSL")
						.pattern("LSL")
						.unlockedBy(getHasName(Items.STRIPPED_POPLAR_LOG), has(Items.STRIPPED_POPLAR_LOG))
						.save(output);

				// A trough of planks under a rack of sticks.
				shaped(RecipeCategory.DECORATIONS, EamItems.FEEDER)
						.define('P', ItemTags.PLANKS)
						.define('S', Items.STICK)
						.pattern("S S")
						.pattern("P P")
						.pattern("PPP")
						.unlockedBy(getHasName(Items.WHEAT), has(Items.WHEAT))
						.save(output);
				// The wooden feeder rebuilt in metal: copper or iron ingots around it.
				for (Item ingot : List.of(Items.COPPER_INGOT, Items.IRON_INGOT)) {
					shaped(RecipeCategory.DECORATIONS, EamItems.METAL_FEEDER)
							.define('I', ingot)
							.define('F', EamItems.FEEDER)
							.pattern("I I")
							.pattern("IFI")
							.unlockedBy(getHasName(EamItems.FEEDER), has(EamItems.FEEDER))
							.save(output, "eam:metal_feeder_from_" + BuiltInRegistries.ITEM.getKey(ingot).getPath());
				}

				// A wooden box lined with wool, and a bigger iron one with a bucket to catch the melt water.
				shaped(RecipeCategory.DECORATIONS, EamItems.SMALL_FRIDGE)
						.define('P', ItemTags.PLANKS)
						.define('W', ItemTags.WOOL)
						.pattern("PPP")
						.pattern("W W")
						.pattern("PPP")
						.unlockedBy(getHasName(Items.ICE), has(Items.ICE))
						.save(output);
				shaped(RecipeCategory.DECORATIONS, EamItems.LARGE_FRIDGE)
						.define('I', EamItems.IRON_PLATE)
						.define('W', ItemTags.WOOL)
						.define('B', ConventionalItemTags.EMPTY_BUCKETS)
						.pattern("IWI")
						.pattern("WBW")
						.pattern("IWI")
						.unlockedBy(getHasName(EamItems.IRON_PLATE), has(EamItems.IRON_PLATE))
						.save(output);

				bowlFoodRecipes();
				ashRecipes();
				silverRecipes();
				brewingRecipes();
				mithrilRecipes();
				mithrilPlateRecipes(withConditions(output, ForgingModule.ENABLED));
				mithrilIngotArmorRecipes(withConditions(output, ResourceConditions.not(ForgingModule.ENABLED)));
				adamantiumRecipes();
				adamantiumPlateRecipes(withConditions(output, ForgingModule.ENABLED));
				adamantiumIngotArmorRecipes(withConditions(output, ResourceConditions.not(ForgingModule.ENABLED)));
				netheriteUpgradeRecipes();
				ancientMetalRecipes();
				ancientMetalPlateRecipes(withConditions(output, ForgingModule.ENABLED));
				ancientMetalIngotArmorRecipes(withConditions(output, ResourceConditions.not(ForgingModule.ENABLED)));

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
			private void knife(Item knife, Item ingot) {
				shapeless(RecipeCategory.COMBAT, knife)
						.requires(Items.STICK)
						.requires(ingot)
						.unlockedBy(getHasName(ingot), has(ingot))
						.save(output);
			}

			private void anvil(Item anvil, Item block, Item ingot) {
				shaped(RecipeCategory.DECORATIONS, anvil)
						.define('B', block)
						.define('I', ingot)
						.pattern("BBB")
						.pattern(" I ")
						.pattern("III")
						.unlockedBy(getHasName(block), has(block))
						.save(output);
			}

			private void metalBuilding(Item bars, Item door, Item trapdoor, Item ingot) {
				shaped(RecipeCategory.DECORATIONS, bars, 16)
						.define('#', ingot)
						.pattern("###")
						.pattern("###")
						.unlockedBy(getHasName(ingot), has(ingot))
						.save(output);
				shaped(RecipeCategory.REDSTONE, door, 3)
						.define('#', ingot)
						.pattern("##")
						.pattern("##")
						.pattern("##")
						.unlockedBy(getHasName(ingot), has(ingot))
						.save(output);
				shaped(RecipeCategory.REDSTONE, trapdoor)
						.define('#', ingot)
						.pattern("##")
						.pattern("##")
						.unlockedBy(getHasName(ingot), has(ingot))
						.save(output);
			}

			private void hammer(Item hammer, Item ingot) {
				shaped(RecipeCategory.TOOLS, hammer)
						.define('I', ingot)
						.define('S', Items.STICK)
						.pattern("ISI")
						.pattern(" S ")
						.pattern(" S ")
						.unlockedBy(getHasName(ingot), has(ingot))
						.save(output);
			}

			private void arrow(Item arrow, Item head) {
				shaped(RecipeCategory.COMBAT, arrow, ArrowHeads.RECIPE_COUNT)
						.define('H', head)
						.define('S', Items.STICK)
						.define('F', Items.FEATHER)
						.pattern("H")
						.pattern("S")
						.pattern("F")
						.unlockedBy(getHasName(head), has(head))
						.save(output);
			}

			private void bucket(Item bucket, Item ingot) {
				shaped(RecipeCategory.TOOLS, bucket)
						.define('I', ingot)
						.pattern("I I")
						.pattern(" I ")
						.unlockedBy(getHasName(ingot), has(ingot))
						.save(output);
			}

			private void chainmail(Item nugget, Item ring, Item helmet, Item chestplate, Item leggings, Item boots) {
				shaped(RecipeCategory.MISC, ring)
						.define('N', nugget)
						.pattern("NN")
						.pattern("NN")
						.unlockedBy(getHasName(nugget), has(nugget))
						.save(output);
				ringArmor(ring, helmet, "RRR", "R R");
				ringArmor(ring, chestplate, "R R", "RRR", "RRR");
				ringArmor(ring, leggings, "RRR", "R R", "R R");
				ringArmor(ring, boots, "R R", "R R");
			}

			/** Vanilla chainmail is saved under our id: vanilla has no chainmail recipe to replace. */
			private void ringArmor(Item ring, Item result, String... pattern) {
				var recipe = shaped(RecipeCategory.COMBAT, result)
						.define('R', ring);
				for (String row : pattern) {
					recipe.pattern(row);
				}
				recipe.unlockedBy(getHasName(ring), has(ring));
				if (BuiltInRegistries.ITEM.getKey(result).getNamespace().equals("minecraft")) {
					recipe.save(output, "eam:" + getItemName(result));
				} else {
					recipe.save(output);
				}
			}

			private void saw(Item saw, Item ingot) {
				shaped(RecipeCategory.TOOLS, saw)
						.define('I', ingot)
						.define('S', Items.STICK)
						.pattern("I  ")
						.pattern(" I ")
						.pattern("  S")
						.unlockedBy(getHasName(ingot), has(ingot))
						.save(output);
			}

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

			/** The silver brewing stand, its fuel and reagents, and what it brews. */
			private void brewingRecipes() {
				shaped(RecipeCategory.BREWING, EamItems.SILVER_BREWING_STAND)
						.define('S', EamItems.SILVER_INGOT)
						.define('#', ItemTags.STONE_CRAFTING_MATERIALS)
						.pattern(" S ")
						.pattern(" S ")
						.pattern("###")
						.unlockedBy(getHasName(EamItems.SILVER_INGOT), has(EamItems.SILVER_INGOT))
						.save(output);
				shapeless(RecipeCategory.BREWING, EamItems.LAPIS_GUNPOWDER_MIX)
						.requires(Items.LAPIS_LAZULI, 2)
						.requires(Items.GUNPOWDER)
						.unlockedBy(getHasName(EamItems.SILVER_BREWING_STAND), has(EamItems.SILVER_BREWING_STAND))
						.save(output);
				shapeless(RecipeCategory.BREWING, EamItems.SILVER_REDSTONE_DUST)
						.requires(EamItems.SILVER_NUGGET, 3)
						.requires(Items.REDSTONE)
						.unlockedBy(getHasName(EamItems.SILVER_BREWING_STAND), has(EamItems.SILVER_BREWING_STAND))
						.save(output);
				shapeless(RecipeCategory.BREWING, EamItems.FLOWER_MIX)
						.requires(Items.DANDELION)
						.requires(Items.POPPY)
						.requires(Items.CORNFLOWER)
						.unlockedBy(getHasName(EamItems.SILVER_BREWING_STAND), has(EamItems.SILVER_BREWING_STAND))
						.save(output);
				shapeless(RecipeCategory.BREWING, EamItems.GARLIC_ORCHID_MIX)
						.requires(EamItems.GARLIC)
						.requires(Items.BLUE_ORCHID)
						.unlockedBy(getHasName(EamItems.GARLIC), has(EamItems.GARLIC))
						.save(output);
				// Any tulip goes; lily of the valley is the only vanilla lily.
				shapeless(RecipeCategory.BREWING, EamItems.HERBAL_HONEY)
						.requires(Items.HONEY_BOTTLE)
						.requires(Items.LILY_OF_THE_VALLEY)
						.requires(Ingredient.of(Items.RED_TULIP, Items.ORANGE_TULIP, Items.WHITE_TULIP, Items.PINK_TULIP))
						.unlockedBy(getHasName(Items.HONEY_BOTTLE), has(Items.HONEY_BOTTLE))
						.save(output);
				shapeless(RecipeCategory.BREWING, EamItems.FERMENTED_ENDER_EYE)
						.requires(Items.ENDER_EYE)
						.requires(EamItems.SILVER_NUGGET, 2)
						.requires(Items.REDSTONE)
						.unlockedBy(getHasName(EamItems.SILVER_BREWING_STAND), has(EamItems.SILVER_BREWING_STAND))
						.save(output);

				silverBrewing(Potions.WATER, EamItems.SILVER_REDSTONE_DUST, BrewingModule.HOLY_WATER);
				silverBrewing(BrewingModule.HOLY_WATER, Items.GLOW_BERRIES, BrewingModule.LIGHT_NIGHT_VISION);
				silverBrewing(BrewingModule.HOLY_WATER, EamItems.FLOWER_MIX, BrewingModule.LIGHT_REGENERATION);
				silverBrewing(BrewingModule.HOLY_WATER, Items.SUGAR, BrewingModule.LIGHT_SWIFTNESS);
				silverBrewing(BrewingModule.HOLY_WATER, Items.GLISTERING_MELON_SLICE, BrewingModule.LIGHT_HEALING_POTION);
				silverBrewing(BrewingModule.HOLY_WATER, Items.FERMENTED_SPIDER_EYE, BrewingModule.LIGHT_WEAKNESS);
				// A warg's strength, in its meat.
				silverBrewing(BrewingModule.HOLY_WATER, EamItems.WOLF_MEAT, BrewingModule.LIGHT_STRENGTH);
				silverBrewing(BrewingModule.HOLY_WATER, EamItems.GARLIC_ORCHID_MIX, BrewingModule.BLACK_BLOOD);
				silverBrewing(BrewingModule.HOLY_WATER, EamItems.HERBAL_HONEY, BrewingModule.WHITE_HONEY);
				// Gunpowder makes a splash potion of the same light potion, as in the vanilla stand.
				for (Holder<Potion> potion : BrewingModule.LIGHT_POTIONS) {
					silverBrewing(Items.POTION, potion, Items.GUNPOWDER, Items.SPLASH_POTION, potion,
							"splash_" + potion.unwrapKey().orElseThrow().identifier().getPath());
				}
			}

			private void silverBrewing(Holder<Potion> input, Item reagent, Holder<Potion> result) {
				silverBrewing(Items.POTION, input, reagent, Items.POTION, result, result.unwrapKey().orElseThrow().identifier().getPath());
			}

			private void silverBrewing(Item inputBottle, Holder<Potion> input, Item reagent, Item resultBottle, Holder<Potion> result, String name) {
				SilverBrewingRecipe recipe = new SilverBrewingRecipe(
						PotionIngredient.of(inputBottle, PotionsPredicate.ofPotion(input)),
						PotionIngredient.of(reagent),
						new ItemStackTemplate(resultBottle, DataComponentPatch.builder()
								.set(DataComponents.POTION_CONTENTS, new PotionContents(result)).build()));
				output.accept(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("eam", "silver_brewing/" + name)), recipe, null);
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

			/**
			 * The obsidian blast furnace (a furnace in iron plates on obsidian) and mithril: it smelts only in that furnace,
			 * packs into blocks and makes diamond-shaped tools. Its armor is in {@link #mithrilPlateRecipes}.
			 */
			private void mithrilRecipes() {
				shaped(RecipeCategory.DECORATIONS, EamItems.OBSIDIAN_BLAST_FURNACE)
						.define('P', EamItems.IRON_PLATE)
						.define('F', Items.FURNACE)
						.define('O', Items.OBSIDIAN)
						.pattern("PPP")
						.pattern("PFP")
						.pattern("OOO")
						.unlockedBy(getHasName(Items.OBSIDIAN), has(Items.OBSIDIAN))
						.save(output);
				for (Item smeltable : List.of(EamItems.MITHRIL_ORE, EamItems.DEEPSLATE_MITHRIL_ORE, EamItems.RAW_MITHRIL)) {
					SimpleCookingRecipeBuilder.blasting(Ingredient.of(smeltable), RecipeCategory.MISC, CookingBookCategory.MISC,
									EamItems.MITHRIL_INGOT, 1.0F, 100)
							.group("mithril_ingot")
							.unlockedBy(getHasName(smeltable), has(smeltable))
							.save(output, "eam:mithril_ingot_from_blasting_" + getItemName(smeltable));
				}
				nineBlockStorageRecipes(RecipeCategory.MISC, EamItems.MITHRIL_INGOT, RecipeCategory.BUILDING_BLOCKS, EamItems.MITHRIL_BLOCK,
						"eam:mithril_block", null, "eam:mithril_ingot_from_mithril_block", "mithril_ingot");
				nineBlockStorageRecipes(RecipeCategory.MISC, EamItems.MITHRIL_NUGGET, RecipeCategory.MISC, EamItems.MITHRIL_INGOT,
						"eam:mithril_ingot_from_nuggets", "mithril_ingot", "eam:mithril_nugget", null);
				mithril(RecipeCategory.COMBAT, EamItems.MITHRIL_SWORD, "X", "X", "#");
				mithril(RecipeCategory.TOOLS, EamItems.MITHRIL_SHOVEL, "X", "#", "#");
				mithril(RecipeCategory.TOOLS, EamItems.MITHRIL_PICKAXE, "XXX", " # ", " # ");
				mithril(RecipeCategory.TOOLS, EamItems.MITHRIL_AXE, "XX", "X#", " #");
				mithril(RecipeCategory.TOOLS, EamItems.MITHRIL_HOE, "XX", " #", " #");
			}

			private void mithril(RecipeCategory category, Item result, String... pattern) {
				var recipe = shaped(category, result)
						.define('X', EamItems.MITHRIL_INGOT)
						.define('#', Items.STICK);
				for (String row : pattern) {
					recipe.pattern(row);
				}
				recipe.unlockedBy(getHasName(EamItems.MITHRIL_INGOT), has(EamItems.MITHRIL_INGOT))
						.save(output);
			}

			/**
			 * Adamantium: smelts only in the obsidian blast furnace like mithril, packs into blocks and makes tools of the
			 * usual shapes. Its armor is in {@link #adamantiumPlateRecipes}.
			 */
			private void adamantiumRecipes() {
				for (Item smeltable : List.of(EamItems.ADAMANTIUM_ORE, EamItems.DEEPSLATE_ADAMANTIUM_ORE, EamItems.RAW_ADAMANTIUM)) {
					SimpleCookingRecipeBuilder.blasting(Ingredient.of(smeltable), RecipeCategory.MISC, CookingBookCategory.MISC,
									EamItems.ADAMANTIUM_INGOT, 2.0F, 200)
							.group("adamantium_ingot")
							.unlockedBy(getHasName(smeltable), has(smeltable))
							.save(output, "eam:adamantium_ingot_from_blasting_" + getItemName(smeltable));
				}
				nineBlockStorageRecipes(RecipeCategory.MISC, EamItems.ADAMANTIUM_INGOT, RecipeCategory.BUILDING_BLOCKS, EamItems.ADAMANTIUM_BLOCK,
						"eam:adamantium_block", null, "eam:adamantium_ingot_from_adamantium_block", "adamantium_ingot");
				nineBlockStorageRecipes(RecipeCategory.MISC, EamItems.ADAMANTIUM_NUGGET, RecipeCategory.MISC, EamItems.ADAMANTIUM_INGOT,
						"eam:adamantium_ingot_from_nuggets", "adamantium_ingot", "eam:adamantium_nugget", null);
				adamantium(RecipeCategory.COMBAT, EamItems.ADAMANTIUM_SWORD, "X", "X", "#");
				adamantium(RecipeCategory.TOOLS, EamItems.ADAMANTIUM_SHOVEL, "X", "#", "#");
				adamantium(RecipeCategory.TOOLS, EamItems.ADAMANTIUM_PICKAXE, "XXX", " # ", " # ");
				adamantium(RecipeCategory.TOOLS, EamItems.ADAMANTIUM_AXE, "XX", "X#", " #");
				adamantium(RecipeCategory.TOOLS, EamItems.ADAMANTIUM_HOE, "XX", " #", " #");
			}

			private void adamantium(RecipeCategory category, Item result, String... pattern) {
				var recipe = shaped(category, result)
						.define('X', EamItems.ADAMANTIUM_INGOT)
						.define('#', Items.STICK);
				for (String row : pattern) {
					recipe.pattern(row);
				}
				recipe.unlockedBy(getHasName(EamItems.ADAMANTIUM_INGOT), has(EamItems.ADAMANTIUM_INGOT))
						.save(output);
			}

			/** Adamantium ingot to hot adamantium in the obsidian blast furnace, and adamantium armor from plates and ingots. */
			private void adamantiumPlateRecipes(RecipeOutput enabled) {
				SimpleCookingRecipeBuilder.blasting(Ingredient.of(EamItems.ADAMANTIUM_INGOT), RecipeCategory.MISC, CookingBookCategory.MISC,
								EamItems.HOT_ADAMANTIUM_INGOT, 0.0F, 200)
						.unlockedBy(getHasName(EamItems.ADAMANTIUM_INGOT), has(EamItems.ADAMANTIUM_INGOT))
						.save(enabled, "eam:hot_adamantium_ingot_from_blasting");
				adamantiumArmor(enabled, EamItems.ADAMANTIUM_HELMET, "", "PPP", "I I");
				adamantiumArmor(enabled, EamItems.ADAMANTIUM_CHESTPLATE, "", "I I", "PPP", "IPI");
				adamantiumArmor(enabled, EamItems.ADAMANTIUM_LEGGINGS, "", "PIP", "P P", "I I");
				adamantiumArmor(enabled, EamItems.ADAMANTIUM_BOOTS, "", "I I", "P P");
			}

			/** With the plates off, adamantium armor takes ingots only, like vanilla armor. */
			private void adamantiumIngotArmorRecipes(RecipeOutput disabled) {
				adamantiumArmor(disabled, EamItems.ADAMANTIUM_HELMET, "_from_ingots", "III", "I I");
				adamantiumArmor(disabled, EamItems.ADAMANTIUM_CHESTPLATE, "_from_ingots", "I I", "III", "III");
				adamantiumArmor(disabled, EamItems.ADAMANTIUM_LEGGINGS, "_from_ingots", "III", "I I", "I I");
				adamantiumArmor(disabled, EamItems.ADAMANTIUM_BOOTS, "_from_ingots", "I I", "I I");
			}

			private void adamantiumArmor(RecipeOutput output, Item result, String suffix, String... pattern) {
				var recipe = shaped(RecipeCategory.COMBAT, result)
						.define('I', EamItems.ADAMANTIUM_INGOT);
				if (String.join("", pattern).contains("P")) {
					recipe.define('P', EamItems.ADAMANTIUM_PLATE);
				}
				for (String row : pattern) {
					recipe.pattern(row);
				}
				recipe.unlockedBy(getHasName(EamItems.ADAMANTIUM_INGOT), has(EamItems.ADAMANTIUM_INGOT));
				if (suffix.isEmpty()) {
					recipe.save(output);
				} else {
					recipe.save(output, "eam:" + getItemName(result) + suffix);
				}
			}

			/**
			 * Netherite is the upgrade of adamantium: the vanilla smithing recipes, saved under their vanilla ids, take
			 * adamantium gear instead of diamond gear (the spear keeps diamond, which cannot be crafted). The upgrade
			 * template can only be found: its copying recipe is saved under the vanilla id with a condition that never
			 * holds, so neither it nor the vanilla one loads.
			 */
			private void netheriteUpgradeRecipes() {
				netheriteUpgrade(RecipeCategory.COMBAT, EamItems.ADAMANTIUM_SWORD, Items.NETHERITE_SWORD);
				netheriteUpgrade(RecipeCategory.TOOLS, EamItems.ADAMANTIUM_SHOVEL, Items.NETHERITE_SHOVEL);
				netheriteUpgrade(RecipeCategory.TOOLS, EamItems.ADAMANTIUM_PICKAXE, Items.NETHERITE_PICKAXE);
				netheriteUpgrade(RecipeCategory.TOOLS, EamItems.ADAMANTIUM_AXE, Items.NETHERITE_AXE);
				netheriteUpgrade(RecipeCategory.TOOLS, EamItems.ADAMANTIUM_HOE, Items.NETHERITE_HOE);
				netheriteUpgrade(RecipeCategory.COMBAT, EamItems.ADAMANTIUM_HELMET, Items.NETHERITE_HELMET);
				netheriteUpgrade(RecipeCategory.COMBAT, EamItems.ADAMANTIUM_CHESTPLATE, Items.NETHERITE_CHESTPLATE);
				netheriteUpgrade(RecipeCategory.COMBAT, EamItems.ADAMANTIUM_LEGGINGS, Items.NETHERITE_LEGGINGS);
				netheriteUpgrade(RecipeCategory.COMBAT, EamItems.ADAMANTIUM_BOOTS, Items.NETHERITE_BOOTS);

				shaped(RecipeCategory.MISC, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 2)
						.define('#', Items.DIAMOND)
						.define('C', Items.NETHERRACK)
						.define('S', Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)
						.pattern("#S#")
						.pattern("#C#")
						.pattern("###")
						.unlockedBy(getHasName(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), has(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE))
						.save(withConditions(output, ResourceConditions.alwaysFalse()));
			}

			private void netheriteUpgrade(RecipeCategory category, Item adamantium, Item netherite) {
				SmithingTransformRecipeBuilder.smithing(Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), Ingredient.of(adamantium),
								tag(ItemTags.NETHERITE_TOOL_MATERIALS), category, netherite)
						.unlocks(getHasName(Items.NETHERITE_INGOT), has(Items.NETHERITE_INGOT))
						.save(output, "minecraft:" + getItemName(netherite) + "_smithing");
			}

			/** Mithril ingot to hot mithril in the obsidian blast furnace, and mithril armor from plates and ingots like iron's. */
			private void mithrilPlateRecipes(RecipeOutput enabled) {
				SimpleCookingRecipeBuilder.blasting(Ingredient.of(EamItems.MITHRIL_INGOT), RecipeCategory.MISC, CookingBookCategory.MISC,
								EamItems.HOT_MITHRIL_INGOT, 0.0F, 100)
						.unlockedBy(getHasName(EamItems.MITHRIL_INGOT), has(EamItems.MITHRIL_INGOT))
						.save(enabled, "eam:hot_mithril_ingot_from_blasting");
				mithrilArmor(enabled, EamItems.MITHRIL_HELMET, "", "PPP", "I I");
				mithrilArmor(enabled, EamItems.MITHRIL_CHESTPLATE, "", "I I", "PPP", "IPI");
				mithrilArmor(enabled, EamItems.MITHRIL_LEGGINGS, "", "PIP", "P P", "I I");
				mithrilArmor(enabled, EamItems.MITHRIL_BOOTS, "", "I I", "P P");
			}

			/** With the plates off, mithril armor takes ingots only, like vanilla armor. */
			private void mithrilIngotArmorRecipes(RecipeOutput disabled) {
				mithrilArmor(disabled, EamItems.MITHRIL_HELMET, "_from_ingots", "III", "I I");
				mithrilArmor(disabled, EamItems.MITHRIL_CHESTPLATE, "_from_ingots", "I I", "III", "III");
				mithrilArmor(disabled, EamItems.MITHRIL_LEGGINGS, "_from_ingots", "III", "I I", "I I");
				mithrilArmor(disabled, EamItems.MITHRIL_BOOTS, "_from_ingots", "I I", "I I");
			}

			private void mithrilArmor(RecipeOutput output, Item result, String suffix, String... pattern) {
				var recipe = shaped(RecipeCategory.COMBAT, result)
						.define('I', EamItems.MITHRIL_INGOT);
				if (String.join("", pattern).contains("P")) {
					recipe.define('P', EamItems.MITHRIL_PLATE);
				}
				for (String row : pattern) {
					recipe.pattern(row);
				}
				recipe.unlockedBy(getHasName(EamItems.MITHRIL_INGOT), has(EamItems.MITHRIL_INGOT));
				if (suffix.isEmpty()) {
					recipe.save(output);
				} else {
					recipe.save(output, "eam:" + getItemName(result) + suffix);
				}
			}

			/**
			 * Ancient metal has no source yet, so nothing makes the ingot. It packs into nuggets and blocks, makes iron-shaped
			 * tools; its chainmail is made of rings like every other chainmail.
			 */
			private void ancientMetalRecipes() {
				nineBlockStorageRecipes(RecipeCategory.MISC, EamItems.ANCIENT_METAL_INGOT, RecipeCategory.BUILDING_BLOCKS,
						EamItems.ANCIENT_METAL_BLOCK, "eam:ancient_metal_block", null, "eam:ancient_metal_ingot_from_ancient_metal_block",
						"ancient_metal_ingot");
				nineBlockStorageRecipes(RecipeCategory.MISC, EamItems.ANCIENT_METAL_NUGGET, RecipeCategory.MISC, EamItems.ANCIENT_METAL_INGOT,
						"eam:ancient_metal_ingot_from_nuggets", "ancient_metal_ingot", "eam:ancient_metal_nugget", null);
				ancientMetal(output, RecipeCategory.COMBAT, EamItems.ANCIENT_METAL_SWORD, "", "I", "I", "#");
				ancientMetal(output, RecipeCategory.TOOLS, EamItems.ANCIENT_METAL_SHOVEL, "", "I", "#", "#");
				ancientMetal(output, RecipeCategory.TOOLS, EamItems.ANCIENT_METAL_PICKAXE, "", "III", " # ", " # ");
				ancientMetal(output, RecipeCategory.TOOLS, EamItems.ANCIENT_METAL_AXE, "", "II", "I#", " #");
				ancientMetal(output, RecipeCategory.TOOLS, EamItems.ANCIENT_METAL_HOE, "", "II", " #", " #");
			}

			/** Ancient metal ingot to hot ancient metal in a blast furnace, and its armor from plates and ingots like iron's. */
			private void ancientMetalPlateRecipes(RecipeOutput enabled) {
				SimpleCookingRecipeBuilder.blasting(Ingredient.of(EamItems.ANCIENT_METAL_INGOT), RecipeCategory.MISC, CookingBookCategory.MISC,
								EamItems.HOT_ANCIENT_METAL_INGOT, 0.0F, 100)
						.unlockedBy(getHasName(EamItems.ANCIENT_METAL_INGOT), has(EamItems.ANCIENT_METAL_INGOT))
						.save(enabled, "eam:hot_ancient_metal_ingot_from_blasting");
				ancientMetal(enabled, RecipeCategory.COMBAT, EamItems.ANCIENT_METAL_HELMET, "", "PPP", "I I");
				ancientMetal(enabled, RecipeCategory.COMBAT, EamItems.ANCIENT_METAL_CHESTPLATE, "", "I I", "PPP", "IPI");
				ancientMetal(enabled, RecipeCategory.COMBAT, EamItems.ANCIENT_METAL_LEGGINGS, "", "PIP", "P P", "I I");
				ancientMetal(enabled, RecipeCategory.COMBAT, EamItems.ANCIENT_METAL_BOOTS, "", "I I", "P P");
			}

			/** With the plates off, ancient metal armor takes ingots only, like vanilla armor. */
			private void ancientMetalIngotArmorRecipes(RecipeOutput disabled) {
				ancientMetal(disabled, RecipeCategory.COMBAT, EamItems.ANCIENT_METAL_HELMET, "_from_ingots", "III", "I I");
				ancientMetal(disabled, RecipeCategory.COMBAT, EamItems.ANCIENT_METAL_CHESTPLATE, "_from_ingots", "I I", "III", "III");
				ancientMetal(disabled, RecipeCategory.COMBAT, EamItems.ANCIENT_METAL_LEGGINGS, "_from_ingots", "III", "I I", "I I");
				ancientMetal(disabled, RecipeCategory.COMBAT, EamItems.ANCIENT_METAL_BOOTS, "_from_ingots", "I I", "I I");
			}

			/** I ingot, N nugget, P plate, # stick; only the keys the pattern uses are defined. */
			private void ancientMetal(RecipeOutput output, RecipeCategory category, Item result, String suffix, String... pattern) {
				var recipe = shaped(category, result);
				String keys = String.join("", pattern);
				if (keys.contains("I")) {
					recipe.define('I', EamItems.ANCIENT_METAL_INGOT);
				}
				if (keys.contains("N")) {
					recipe.define('N', EamItems.ANCIENT_METAL_NUGGET);
				}
				if (keys.contains("P")) {
					recipe.define('P', EamItems.ANCIENT_METAL_PLATE);
				}
				if (keys.contains("#")) {
					recipe.define('#', Items.STICK);
				}
				for (String row : pattern) {
					recipe.pattern(row);
				}
				recipe.unlockedBy(getHasName(EamItems.ANCIENT_METAL_INGOT), has(EamItems.ANCIENT_METAL_INGOT));
				if (suffix.isEmpty()) {
					recipe.save(output);
				} else {
					recipe.save(output, "eam:" + getItemName(result) + suffix);
				}
			}

			/** MITE bowl foods with vanilla stand-ins: beetroot for onion, apple for orange, sweet berries for blueberries. */
			private void bowlFoodRecipes() {
				// A bucket of any metal fills up to four bowls and comes back empty.
				for (int bowls = 1; bowls <= 4; bowls++) {
					shapeless(RecipeCategory.FOOD, EamItems.WATER_BOWL, bowls)
							.requires(ConventionalItemTags.WATER_BUCKETS)
							.requires(Items.BOWL, bowls)
							.unlockedBy(getHasName(Items.BOWL), has(Items.BOWL))
							.save(output, "eam:water_bowl_from_bucket_" + bowls);
					shapeless(RecipeCategory.FOOD, EamItems.MILK_BOWL, bowls)
							.requires(ConventionalItemTags.MILK_BUCKETS)
							.requires(Items.BOWL, bowls)
							.unlockedBy(getHasName(Items.MILK_BUCKET), has(Items.MILK_BUCKET))
							.save(output, "eam:milk_bowl_from_bucket_" + bowls);
				}

				// Boiling makes the water safe to drink.
				Ingredient water = Ingredient.of(EamItems.WATER_BOWL);
				SimpleCookingRecipeBuilder.smelting(water, RecipeCategory.FOOD, CookingBookCategory.FOOD, EamItems.BOILED_WATER_BOWL, 0.0F, 200)
						.unlockedBy(getHasName(EamItems.WATER_BOWL), has(EamItems.WATER_BOWL))
						.save(output);
				SimpleCookingRecipeBuilder.smoking(water, RecipeCategory.FOOD, EamItems.BOILED_WATER_BOWL, 0.0F, 100)
						.unlockedBy(getHasName(EamItems.WATER_BOWL), has(EamItems.WATER_BOWL))
						.save(output, "eam:boiled_water_bowl_from_smoking");
				SimpleCookingRecipeBuilder.campfireCooking(water, RecipeCategory.FOOD, EamItems.BOILED_WATER_BOWL, 0.0F, 600)
						.unlockedBy(getHasName(EamItems.WATER_BOWL), has(EamItems.WATER_BOWL))
						.save(output, "eam:boiled_water_bowl_from_campfire_cooking");

				boiledWaterBottleRecipes();

				// Warg meat cooks like mutton.
				Ingredient wolfMeat = Ingredient.of(EamItems.WOLF_MEAT);
				SimpleCookingRecipeBuilder.smelting(wolfMeat, RecipeCategory.FOOD, CookingBookCategory.FOOD, EamItems.COOKED_WOLF_MEAT, 0.35F, 200)
						.unlockedBy(getHasName(EamItems.WOLF_MEAT), has(EamItems.WOLF_MEAT))
						.save(output);
				SimpleCookingRecipeBuilder.smoking(wolfMeat, RecipeCategory.FOOD, EamItems.COOKED_WOLF_MEAT, 0.35F, 100)
						.unlockedBy(getHasName(EamItems.WOLF_MEAT), has(EamItems.WOLF_MEAT))
						.save(output, "eam:cooked_wolf_meat_from_smoking");
				SimpleCookingRecipeBuilder.campfireCooking(wolfMeat, RecipeCategory.FOOD, EamItems.COOKED_WOLF_MEAT, 0.35F, 600)
						.unlockedBy(getHasName(EamItems.WOLF_MEAT), has(EamItems.WOLF_MEAT))
						.save(output, "eam:cooked_wolf_meat_from_campfire_cooking");

				shapeless(RecipeCategory.FOOD, EamItems.CHEESE)
						.requires(ConventionalItemTags.MILK_BUCKETS)
						.unlockedBy(getHasName(Items.MILK_BUCKET), has(Items.MILK_BUCKET))
						.save(output);
				shapeless(RecipeCategory.FOOD, EamItems.CHOCOLATE)
						.requires(Items.COCOA_BEANS)
						.requires(Items.SUGAR)
						.unlockedBy(getHasName(Items.COCOA_BEANS), has(Items.COCOA_BEANS))
						.save(output);

				waterDish(EamItems.BEEF_STEW, Items.COOKED_BEEF, Items.BROWN_MUSHROOM, Items.POTATO);
				waterDish(EamItems.CHICKEN_SOUP, Items.COOKED_CHICKEN, Items.CARROT, Items.BEETROOT);
				waterDish(EamItems.VEGETABLE_SOUP, Items.POTATO, Items.CARROT, Items.BEETROOT);
				waterDish(EamItems.PUMPKIN_SOUP, Items.PUMPKIN);
				waterDish(EamItems.PORRIDGE, Items.WHEAT_SEEDS, Items.SWEET_BERRIES, Items.SUGAR);
				milkDish(EamItems.CREAM_OF_MUSHROOM_SOUP, "", Items.BROWN_MUSHROOM, Items.BROWN_MUSHROOM);
				milkDish(EamItems.CREAM_OF_VEGETABLE_SOUP, "", Items.POTATO, Items.CARROT, Items.BEETROOT);
				milkDish(EamItems.MASHED_POTATO, "", Items.BAKED_POTATO, EamItems.CHEESE);
				milkDish(EamItems.CEREAL, "", Items.WHEAT, Items.SUGAR);
				milkDish(EamItems.ICE_CREAM, "", EamItems.CHOCOLATE, Items.SNOWBALL);
				milkDish(EamItems.ICE_CREAM, "_from_cocoa", Items.COCOA_BEANS, Items.SUGAR, Items.SNOWBALL);
				emptyBowlDish(EamItems.SALAD, Items.DANDELION, Items.DANDELION, Items.DANDELION);
				emptyBowlDish(EamItems.SORBET, Items.APPLE, Items.SUGAR, Items.SNOWBALL);
			}

			/**
			 * A water bottle must be boiled before it is safe to drink: 5 seconds in a furnace, smoker or blast furnace,
			 * 30 on a campfire like a bowl of water. Boiled water is still water, named by its potion {@code custom_name}.
			 */
			private void boiledWaterBottleRecipes() {
				Ingredient raw = rawWaterBottle();
				ItemStackTemplate boiled = new ItemStackTemplate(Items.POTION,
						DataComponentPatch.builder().set(DataComponents.POTION_CONTENTS, RawWater.boiledWater()).build());
				cooking(raw, boiled, 100, SmeltingRecipe::new, "eam:boiled_water_bottle");
				cooking(raw, boiled, 100, SmokingRecipe::new, "eam:boiled_water_bottle_from_smoking");
				cooking(raw, boiled, 100, BlastingRecipe::new, "eam:boiled_water_bottle_from_blasting");
				cooking(raw, boiled, 600, CampfireCookingRecipe::new, "eam:boiled_water_bottle_from_campfire_cooking");
			}

			/** A plain water bottle; a boiled one has another potion {@code custom_name}, so it does not match. */
			private Ingredient rawWaterBottle() {
				return DefaultCustomIngredients.components(Ingredient.of(Items.POTION),
						DataComponentPatch.builder().set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER)).build());
			}

			/** A cooking recipe whose result carries components, which {@link SimpleCookingRecipeBuilder} cannot make. */
			private void cooking(Ingredient ingredient, ItemStackTemplate result, int ticks, AbstractCookingRecipe.Factory<?> factory, String id) {
				ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Identifier.parse(id));
				RecipeUnlockAdvancementBuilder advancement = new RecipeUnlockAdvancementBuilder();
				advancement.unlockedBy("has_potion", has(Items.POTION));
				output.accept(key, factory.create(RecipeBuilder.createCraftingCommonInfo(true),
								new AbstractCookingRecipe.CookingBookInfo(CookingBookCategory.FOOD, ""), ingredient, result, 0.0F, ticks),
						advancement.build(output, key, RecipeCategory.FOOD));
			}

			/** Ash from overcooked food and a water bottle make black dye; the bottle comes back empty. */
			private void ashRecipes() {
				Ingredient waterBottle = DefaultCustomIngredients.any(rawWaterBottle(),
						DefaultCustomIngredients.components(Ingredient.of(Items.POTION),
								DataComponentPatch.builder().set(DataComponents.POTION_CONTENTS, RawWater.boiledWater()).build()));
				shapeless(RecipeCategory.MISC, Items.DYE.pick(DyeColor.BLACK))
						.requires(EamItems.ASH)
						.requires(waterBottle)
						.unlockedBy(getHasName(EamItems.ASH), has(EamItems.ASH))
						.save(output, "eam:black_dye_from_ash");
			}

			/** A dish made in a bowl of water, plain or boiled. */
			private void waterDish(Item result, Item... ingredients) {
				var recipe = shapeless(RecipeCategory.FOOD, result).requires(EamItems.WATER_BOWLS);
				for (Item ingredient : ingredients) {
					recipe.requires(ingredient);
				}
				recipe.unlockedBy(getHasName(ingredients[0]), has(ingredients[0])).save(output);
			}

			private void milkDish(Item result, String suffix, Item... ingredients) {
				dish(result, EamItems.MILK_BOWL, suffix, ingredients);
			}

			private void emptyBowlDish(Item result, Item... ingredients) {
				dish(result, Items.BOWL, "", ingredients);
			}

			private void dish(Item result, Item bowl, String suffix, Item... ingredients) {
				var recipe = shapeless(RecipeCategory.FOOD, result).requires(bowl);
				for (Item ingredient : ingredients) {
					recipe.requires(ingredient);
				}
				recipe.unlockedBy(getHasName(ingredients[0]), has(ingredients[0]));
				if (suffix.isEmpty()) {
					recipe.save(output);
				} else {
					recipe.save(output, "eam:" + getItemName(result) + suffix);
				}
			}

			/** Iron ingot to hot iron in a furnace or blast furnace, and iron armor from plates and ingots. Plates come only from the anvil. */
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
				plateArmor(enabled, Items.IRON_HELMET, "PPP", "I I");
				plateArmor(enabled, Items.IRON_CHESTPLATE, "I I", "PPP", "IPI");
				plateArmor(enabled, Items.IRON_LEGGINGS, "PIP", "P P", "I I");
				plateArmor(enabled, Items.IRON_BOOTS, "I I", "P P");
			}

			/** Copies of the vanilla iron armor recipes under the vanilla ids, loaded only with the plates off, like the early tools. */
			private void ironIngotArmorRecipes(RecipeOutput removable) {
				ironArmor(removable, Items.IRON_HELMET, "XXX", "X X");
				ironArmor(removable, Items.IRON_CHESTPLATE, "X X", "XXX", "XXX");
				ironArmor(removable, Items.IRON_LEGGINGS, "XXX", "X X", "X X");
				ironArmor(removable, Items.IRON_BOOTS, "X X", "X X");
			}

			/** Iron armor from about as many plates as ingots, under our own ids. */
			private void plateArmor(RecipeOutput output, Item result, String... pattern) {
				var recipe = shaped(RecipeCategory.COMBAT, result)
						.define('P', EamItems.IRON_PLATE)
						.define('I', Items.IRON_INGOT);
				for (String row : pattern) {
					recipe.pattern(row);
				}
				recipe.unlockedBy(getHasName(EamItems.IRON_PLATE), has(EamItems.IRON_PLATE))
						.save(output, "eam:" + getItemName(result));
			}

			/** The ingot copies keep the vanilla ids so they shadow the vanilla recipes. */
			private void ironArmor(RecipeOutput output, Item result, String... pattern) {
				var recipe = shaped(RecipeCategory.COMBAT, result)
						.define('X', Items.IRON_INGOT);
				for (String row : pattern) {
					recipe.pattern(row);
				}
				recipe.unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
						.save(output);
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
