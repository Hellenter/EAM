package io.github.hellenter.eam.content.brewing;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.mobs.entity.upyr.UpyrModule;

import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * The silver brewing stand: brews holy water and light potions from its own recipe type ({@code eam:silver_brewing}),
 * burns the lapis-gunpowder mix, and stands in generated structures where vanilla puts a brewing stand.
 */
public final class BrewingModule {
	public static final RecipeType<BrewingRecipe> RECIPE_TYPE = Registry.register(BuiltInRegistries.RECIPE_TYPE, Eam.id("silver_brewing"),
			new RecipeType<>() {
				@Override
				public String toString() {
					return "eam:silver_brewing";
				}
			});

	static {
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Eam.id("silver_brewing"), SilverBrewingRecipe.SERIALIZER);
		// The client draws the stand's recipe book from these.
		RecipeSynchronization.synchronizeRecipeSerializer(SilverBrewingRecipe.SERIALIZER);
	}

	/** Brews one item of this fuels in the silver stand. The vanilla stand ignores it. */
	public static final DataComponentType<Integer> SILVER_BREWING_FUEL = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Eam.id("silver_brewing_fuel"),
			DataComponentType.<Integer>builder().persistent(ExtraCodecs.POSITIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

	/** Items the silver stand takes in its reagent slot. Recipes decide what they brew into. */
	public static final TagKey<Item> REAGENTS = TagKey.create(Registries.ITEM, Eam.id("silver_brewing_reagents"));

	public static final MenuType<SilverBrewingStandMenu> MENU = Registry.register(BuiltInRegistries.MENU, Eam.id("silver_brewing_stand"),
			new MenuType<>(SilverBrewingStandMenu::new, FeatureFlags.VANILLA_SET));

	/** Vanilla Instant Health's color, so a light healing potion looks like a vanilla one. */
	private static final int LIGHT_HEALING_COLOR = 0xF82423;

	public static final Holder<MobEffect> LIGHT_HEALING = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Eam.id("light_healing"),
			new LightHealingEffect(MobEffectCategory.BENEFICIAL, LIGHT_HEALING_COLOR));

	/** Brewed from water with silver-redstone dust; the base of every light potion. No effect of its own. */
	public static final Holder<Potion> HOLY_WATER = potion("holy_water");

	// Light potions: half the duration of vanilla level I, or half the healing.
	public static final Holder<Potion> LIGHT_NIGHT_VISION = potion("light_night_vision",
			new MobEffectInstance(MobEffects.NIGHT_VISION, SilverBrewingLogic.lightDuration(3600)));
	public static final Holder<Potion> LIGHT_REGENERATION = potion("light_regeneration",
			new MobEffectInstance(MobEffects.REGENERATION, SilverBrewingLogic.lightDuration(900)));
	public static final Holder<Potion> LIGHT_SWIFTNESS = potion("light_swiftness",
			new MobEffectInstance(MobEffects.SPEED, SilverBrewingLogic.lightDuration(3600)));
	public static final Holder<Potion> LIGHT_HEALING_POTION = potion("light_healing", new MobEffectInstance(LIGHT_HEALING, 1));
	public static final Holder<Potion> LIGHT_WEAKNESS = potion("light_weakness",
			new MobEffectInstance(MobEffects.WEAKNESS, SilverBrewingLogic.lightDuration(1800)));
	public static final Holder<Potion> LIGHT_STRENGTH = potion("light_strength",
			new MobEffectInstance(MobEffects.STRENGTH, SilverBrewingLogic.lightDuration(3600)));

	/** Holy water with garlic and blue orchid: for 10 minutes upyrs and vampire bats that hurt the drinker are hurt back. */
	public static final Holder<Potion> BLACK_BLOOD = potion("black_blood", new MobEffectInstance(UpyrModule.BLACK_BLOOD, 12000));

	/** Potions with an effect; gunpowder turns each into its splash version, and they get the silver stopper. */
	public static final List<Holder<Potion>> LIGHT_POTIONS = List.of(LIGHT_NIGHT_VISION, LIGHT_REGENERATION, LIGHT_SWIFTNESS,
			LIGHT_HEALING_POTION, LIGHT_WEAKNESS, LIGHT_STRENGTH, BLACK_BLOOD);

	private static boolean replaceInStructures;

	private BrewingModule() {
	}

	public static void register() {
		// Loads the class, which registers the types above.
	}

	public static void init() {
		replaceInStructures = true;
	}

	/** Called for every structure piece being placed: vanilla brewing stands become silver ones, contents and bottles kept. */
	public static List<StructureTemplate.StructureBlockInfo> replaceStands(List<StructureTemplate.StructureBlockInfo> blocks) {
		if (!replaceInStructures) {
			return blocks;
		}
		List<StructureTemplate.StructureBlockInfo> result = null;
		for (int i = 0; i < blocks.size(); i++) {
			StructureTemplate.StructureBlockInfo info = blocks.get(i);
			if (info.state().is(Blocks.BREWING_STAND)) {
				if (result == null) {
					result = new ArrayList<>(blocks);
				}
				result.set(i, new StructureTemplate.StructureBlockInfo(info.pos(), silverStand(info.state()), info.nbt()));
			}
		}
		return result == null ? blocks : result;
	}

	private static BlockState silverStand(BlockState vanilla) {
		BlockState state = EamBlocks.SILVER_BREWING_STAND.defaultBlockState();
		for (var hasBottle : BrewingStandBlock.HAS_BOTTLE) {
			state = state.setValue(hasBottle, vanilla.getValue(hasBottle));
		}
		return state;
	}

	private static Holder<Potion> potion(String name, MobEffectInstance... effects) {
		return Registry.registerForHolder(BuiltInRegistries.POTION, Eam.id(name), new Potion(name, effects));
	}
}
