package io.github.hellenter.eam.content;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.ancient.AncientMetalGear;
import io.github.hellenter.eam.content.arrows.ArrowHeadModule;
import io.github.hellenter.eam.content.arrows.ArrowHeads;
import io.github.hellenter.eam.content.brewing.BrewingModule;
import io.github.hellenter.eam.content.brewing.SilverBrewingLogic;
import io.github.hellenter.eam.content.bucket.MetalBucketItem;
import io.github.hellenter.eam.content.bucket.MetalPowderSnowBucketItem;
import io.github.hellenter.eam.content.campfire.CampFireLogic;
import io.github.hellenter.eam.content.chainmail.ChainmailGear;
import io.github.hellenter.eam.content.ground.PebbleItem;
import io.github.hellenter.eam.content.heart.HeartInJarItem;
import io.github.hellenter.eam.content.horn.CommanderHornItem;
import io.github.hellenter.eam.content.horn.GhostHorn;
import io.github.hellenter.eam.content.horn.GhostHornItem;
import io.github.hellenter.eam.content.adamantium.AdamantiumGear;
import io.github.hellenter.eam.content.mithril.MithrilGear;
import io.github.hellenter.eam.content.rusty.RustyGear;
import io.github.hellenter.eam.content.sieve.SieveLogic;
import io.github.hellenter.eam.content.silver.SilverGear;
import io.github.hellenter.eam.content.silver.SilverModule;
import io.github.hellenter.eam.content.longbow.LongbowItem;
import io.github.hellenter.eam.content.slingshot.PowerSlingshotItem;
import io.github.hellenter.eam.content.slingshot.SlingshotItem;
import io.github.hellenter.eam.content.slingshot.SlingshotLogic;
import io.github.hellenter.eam.content.talisman.BlackTalismanItem;
import io.github.hellenter.eam.content.tools.FlintTools;
import io.github.hellenter.eam.content.tools.KnifeItem;
import io.github.hellenter.eam.content.tools.TomahawkItem;
import io.github.hellenter.eam.content.tools.MetalKnives;
import io.github.hellenter.eam.mobs.entity.duergar.DuergarBombItem;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiNeedleItem;
import io.github.hellenter.eam.mobs.entity.lost.LostWave;
import io.github.hellenter.eam.mobs.entity.upyr.UpyrModule;
import io.github.hellenter.eam.mobs.entity.yaga.grave.YagaGraveMapItem;
import io.github.hellenter.eam.mobs.vanilla.villager.BlueprintItem;
import io.github.hellenter.eam.survival.hunger.HungerModule;
import io.github.hellenter.eam.survival.hunger.Nutrients;
import io.github.hellenter.eam.survival.thirst.FlaskItem;
import io.github.hellenter.eam.survival.thirst.FlaskLogic;
import io.github.hellenter.eam.survival.thirst.FlaskWater;
import io.github.hellenter.eam.survival.thirst.Hydration;
import io.github.hellenter.eam.survival.thirst.RawWater;
import io.github.hellenter.eam.survival.thirst.ThirstModule;
import io.github.hellenter.eam.world.forging.HotIronItem;
import io.github.hellenter.eam.world.structures.crypt.CryptMapItem;
import io.github.hellenter.eam.world.structures.crypt.FermentedEnderEyeItem;
import io.github.hellenter.eam.world.structures.outpost.KingCastleMapItem;
import io.github.hellenter.eam.world.structures.village.LairMapItem;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ClearAllStatusEffectsConsumeEffect;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.List;
import java.util.function.Function;

/** Items added by EAM. Registered whatever the config says, so client and server registries match. */
public final class EamItems {
	/** How long eaten garlic wards off bloodsuckers: 30 seconds. */
	private static final int GARLIC_WARD_TICKS = 600;

	public static final Item LEATHER_THREAD = register("leather_thread", Item::new, new Item.Properties());

	public static final Item LEATHER_FLASK = register("leather_flask", p -> new FlaskItem(FlaskLogic.LEATHER, p), new Item.Properties()
			.stacksTo(1)
			.component(ThirstModule.FLASK_WATER, FlaskWater.EMPTY));

	public static final Item FILTERED_LEATHER_FLASK = register("filtered_leather_flask", p -> new FlaskItem(FlaskLogic.LEATHER, p),
			new Item.Properties()
					.stacksTo(1)
					.component(ThirstModule.FLASK_WATER, FlaskWater.EMPTY)
					.component(ThirstModule.FILTER, FlaskLogic.LEATHER.filterUses()));

	/** Glass flask with a charcoal-silver filter: holds and pours half as much again as the leather one. */
	public static final Item FILTERED_GLASS_FLASK = register("filtered_glass_flask", p -> new FlaskItem(FlaskLogic.GLASS, p),
			new Item.Properties()
					.stacksTo(1)
					.component(ThirstModule.FLASK_WATER, FlaskWater.EMPTY)
					.component(ThirstModule.FILTER, FlaskLogic.GLASS.filterUses()));

	/** Thrown like a snowball; sneak-use lays it on the ground. */
	public static final Item PEBBLE = register("pebble", p -> new PebbleItem(EamBlocks.PEBBLE, p),
			new Item.Properties().useBlockDescriptionPrefix().useCooldown(PebbleItem.THROW_COOLDOWN_SECONDS));

	public static final Item FLINT_SHARD = register("flint_shard", p -> new BlockItem(EamBlocks.FLINT_SHARD, p),
			new Item.Properties().useBlockDescriptionPrefix());

	public static final Item CAMP_FIRE = register("camp_fire", p -> new BlockItem(EamBlocks.CAMP_FIRE, p),
			new Item.Properties().useBlockDescriptionPrefix());

	public static final Item FOOD_DRYER = blockItem("food_dryer", EamBlocks.FOOD_DRYER);
	public static final Item CLAY_OVEN = blockItem("clay_oven", EamBlocks.CLAY_OVEN);

	public static final Item UNLIT_TORCH = register("unlit_torch",
			p -> new StandingAndWallBlockItem(EamBlocks.UNLIT_TORCH, EamBlocks.UNLIT_WALL_TORCH, Direction.DOWN, p),
			new Item.Properties().useBlockDescriptionPrefix());

	public static final Item FLINT_HATCHET = register("flint_hatchet", Item::new, new Item.Properties()
			.axe(FlintTools.HATCHET, FlintTools.HATCHET_DAMAGE, FlintTools.HATCHET_ATTACK_SPEED));

	public static final Item FLINT_AXE = register("flint_axe", Item::new, new Item.Properties()
			.axe(FlintTools.AXE, FlintTools.AXE_DAMAGE, FlintTools.AXE_ATTACK_SPEED));

	public static final Item FLINT_KNIFE = register("flint_knife", KnifeItem::new, new Item.Properties()
			.sword(FlintTools.KNIFE, FlintTools.KNIFE_DAMAGE, FlintTools.KNIFE_ATTACK_SPEED));

	public static final Item COPPER_KNIFE = register("copper_knife", KnifeItem::new, new Item.Properties()
			.sword(MetalKnives.COPPER, MetalKnives.TIER_2_DAMAGE, FlintTools.KNIFE_ATTACK_SPEED));

	/** Hits undead harder, like the other silver weapons. */
	public static final Item SILVER_KNIFE = register("silver_knife", KnifeItem::new, new Item.Properties()
			.sword(MetalKnives.SILVER, MetalKnives.TIER_2_DAMAGE, FlintTools.KNIFE_ATTACK_SPEED)
			.component(SilverModule.UNDEAD_DAMAGE_BONUS, SilverGear.UNDEAD_DAMAGE_BONUS));

	public static final Item IRON_KNIFE = register("iron_knife", KnifeItem::new, new Item.Properties()
			.sword(MetalKnives.IRON, MetalKnives.IRON_DAMAGE, FlintTools.KNIFE_ATTACK_SPEED));

	/** Drawn like a bow, shoots pebbles. */
	public static final Item SLINGSHOT = register("slingshot", SlingshotItem::new, new Item.Properties().durability(128));

	/** The sieve block; its damage is how many gravel it has sifted. */
	public static final Item SIEVE = register("sieve", p -> new BlockItem(EamBlocks.SIEVE, p),
			new Item.Properties().useBlockDescriptionPrefix().durability(SieveLogic.USES));

	/** The feeder block. */
	public static final Item FEEDER = register("feeder", p -> new BlockItem(EamBlocks.FEEDER, p), new Item.Properties().useBlockDescriptionPrefix());

	/** The metal feeder block. */
	public static final Item METAL_FEEDER = register("metal_feeder", p -> new BlockItem(EamBlocks.METAL_FEEDER, p),
			new Item.Properties().useBlockDescriptionPrefix());

	public static final Item SMALL_FRIDGE = blockItem("small_fridge", EamBlocks.SMALL_FRIDGE);
	public static final Item LOCKED_CHEST = blockItem("locked_chest", EamBlocks.LOCKED_CHEST);
	public static final Item LARGE_FRIDGE = blockItem("large_fridge", EamBlocks.LARGE_FRIDGE);

	/** An iron ingot heated in a furnace; forged into plates on an anvil, cools back into an ingot after a minute. */
	public static final Item HOT_IRON_INGOT = register("hot_iron_ingot", p -> new HotIronItem(Items.IRON_INGOT, p), new Item.Properties());

	/** Forged from hot iron on an anvil; iron armor is made of these. */
	public static final Item IRON_PLATE = register("iron_plate", Item::new, new Item.Properties().stacksTo(16));

	public static final Item SILVER_ORE = blockItem("silver_ore", EamBlocks.SILVER_ORE);
	public static final Item DEEPSLATE_SILVER_ORE = blockItem("deepslate_silver_ore", EamBlocks.DEEPSLATE_SILVER_ORE);
	public static final Item SILVER_BLOCK = blockItem("silver_block", EamBlocks.SILVER_BLOCK);
	public static final Item RAW_SILVER = register("raw_silver", Item::new, new Item.Properties());
	public static final Item SILVER_INGOT = register("silver_ingot", Item::new, new Item.Properties());
	public static final Item SILVER_NUGGET = register("silver_nugget", Item::new, new Item.Properties());

	/** Silver tools have copper's stats and hit undead harder. */
	public static final Item SILVER_SWORD = silverTool("silver_sword", new Item.Properties().sword(SilverGear.TOOL, 3.0F, -2.4F));
	public static final Item SILVER_SHOVEL = silverTool("silver_shovel", new Item.Properties().shovel(SilverGear.TOOL, 1.5F, -3.0F));
	public static final Item SILVER_PICKAXE = silverTool("silver_pickaxe", new Item.Properties().pickaxe(SilverGear.TOOL, 1.0F, -2.8F));
	public static final Item SILVER_AXE = silverTool("silver_axe", new Item.Properties().axe(SilverGear.TOOL, 7.0F, -3.2F));
	public static final Item SILVER_HOE = silverTool("silver_hoe", new Item.Properties().hoe(SilverGear.TOOL, -1.0F, -2.0F));

	/** Silver armor has copper's stats and hurts undead that hit its wearer. */
	public static final Item SILVER_HELMET = silverArmor("silver_helmet", ArmorType.HELMET);
	public static final Item SILVER_CHESTPLATE = silverArmor("silver_chestplate", ArmorType.CHESTPLATE);
	public static final Item SILVER_LEGGINGS = silverArmor("silver_leggings", ArmorType.LEGGINGS);
	public static final Item SILVER_BOOTS = silverArmor("silver_boots", ArmorType.BOOTS);

	public static final Item SILVER_BREWING_STAND = blockItem("silver_brewing_stand", EamBlocks.SILVER_BREWING_STAND);

	/** Fuel of the silver brewing stand: one is enough for three brews. */
	public static final Item LAPIS_GUNPOWDER_MIX = register("lapis_gunpowder_mix", Item::new, new Item.Properties()
			.component(BrewingModule.SILVER_BREWING_FUEL, SilverBrewingLogic.MIX_FUEL_USES));

	/** Turns water bottles into holy water in the silver stand. */
	public static final Item SILVER_REDSTONE_DUST = register("silver_redstone_dust", Item::new, new Item.Properties());

	/** Turns holy water into a light regeneration potion. */
	public static final Item FLOWER_MIX = register("flower_mix", Item::new, new Item.Properties());

	/** Turns holy water into black blood. */
	public static final Item GARLIC_ORCHID_MIX = register("garlic_orchid_mix", Item::new, new Item.Properties());

	/** Turns holy water into white honey. */
	public static final Item HERBAL_HONEY = register("herbal_honey", Item::new, new Item.Properties());

	/** Ground glowshrooms: in the silver stand it brings a light potion up to level II, as glowstone does in the vanilla stand. */
	public static final Item GLOWING_POWDER = register("glowing_powder", Item::new, new Item.Properties());

	/** Thrown, it flies to the nearest crypt entrance. */
	public static final Item FERMENTED_ENDER_EYE = register("fermented_ender_eye", FermentedEnderEyeItem::new, new Item.Properties());

	/** From the pillager outpost's red chest: in hand, it turns into a map to the Pillager King's castle. */
	public static final Item KING_CASTLE_MAP = register("king_castle_map", KingCastleMapItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

	/** Dropped by the Pillager King: his helm, set up on a pike as a trophy that keeps raids away. */
	public static final Item PILLAGER_KING_HELM = register("pillager_king_helm", p -> new BlockItem(EamBlocks.PILLAGER_KING_HELM, p),
			new Item.Properties().useBlockDescriptionPrefix().stacksTo(1).rarity(Rarity.RARE).fireResistant()
					.component(DataComponents.LORE, new ItemLore(List.of(
							Component.translatable("block.eam.pillager_king_helm.desc").withStyle(ChatFormatting.GRAY)))));

	/** Dropped by the Pillager King: in hand, it turns into a map to the nearest crypt. */
	public static final Item CRYPT_MAP = register("crypt_map", CryptMapItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

	/** From the village elder: in hand it shows its building's ghost, and set down the villagers build it. */
	public static final Item BLUEPRINT = register("blueprint", BlueprintItem::new, new Item.Properties().stacksTo(1));

	/** Sold by duergar elders: on the surface, in hand, it turns into a map to the nearest boss's lair. */
	public static final Item LAIR_MAP = register("lair_map", LairMapItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

	/** A duergar renegade's iron bomb: lit and thrown, it bursts a moment after it lands. */
	public static final Item DUERGAR_BOMB = register("duergar_bomb", DuergarBombItem::new, new Item.Properties().stacksTo(16));
	/** A ram rider's light axe: hits and chops, and is thrown like a trident. */
	public static final Item TOMAHAWK = register("tomahawk", TomahawkItem::new, new Item.Properties()
			.axe(TomahawkItem.MATERIAL, TomahawkItem.ATTACK_DAMAGE, TomahawkItem.ATTACK_SPEED));

	/** Baba Yaga's drop: read, it raises her grave in her swamp and turns into a map marking it. */
	public static final Item YAGA_GRAVE_MAP = register("yaga_grave_map", YagaGraveMapItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

	/** A tine of the leshy's antlers, its trophy. */
	public static final Item LESHY_ANTLER = register("leshy_antler", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));
	/** Vampire bat drop, 15%: the core of the heart in a jar. */
	public static final Item VAMPIRE_HEART = register("vampire_heart", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));

	/** A golden apple used in an instant: heals two hearts, Regeneration II and Saturation II. Stacks to 4. */
	public static final Item HEART_IN_JAR = register("heart_in_jar", HeartInJarItem::new,
			new Item.Properties().stacksTo(4).rarity(Rarity.RARE));

	/** The Fallen Lord's drop: the only thing that opens a nether portal in the overworld. Loses 1 durability per portal. */
	public static final Item BLACK_TALISMAN = register("black_talisman", BlackTalismanItem::new,
			new Item.Properties().durability(16).rarity(Rarity.EPIC).fireResistant()
					.component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.eam.black_talisman.desc")
							.withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false))))));

	/**
	 * Breaks the deep bedrock between the underworld's two levels. A placeholder until the boss that drops it exists:
	 * creative only for now.
	 */
	public static final Item BEDROCK_BREAKER = register("bedrock_breaker", Item::new, new Item.Properties().durability(64).rarity(Rarity.EPIC)
			.fireResistant()
			.component(DataComponents.TOOL, new Tool(List.of(Tool.Rule.minesAndDrops(
					HolderSet.direct(Block::builtInRegistryHolder, EamBlocks.DEEP_BEDROCK), 20.0F)), 1.0F, 1, false)));
	/** An admin item, creative or commands only: whoever carries it commands the Lost and calls their waves. */
	public static final Item COMMANDER_HORN = register("commander_horn", CommanderHornItem::new, new Item.Properties().stacksTo(1)
			.rarity(Rarity.EPIC).fireResistant().component(CommanderHornItem.WAVE, LostWave.SQUAD));
	/** Baba Yaga's drop: the cursed horse skull off her staff. Set on the ground as a trophy; goes into the enchanted horn. */
	public static final Item CURSED_HORSE_SKULL = register("cursed_horse_skull", p -> new BlockItem(EamBlocks.CURSED_HORSE_SKULL, p),
			new Item.Properties().useBlockDescriptionPrefix().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
	/** Calls the ghost horse out and back in; it holds the horse's health and armor while the horse rests in it. */
	public static final Item GHOST_HORN = register("ghost_horn", GhostHornItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
			.fireResistant().component(GhostHornItem.HORSE, GhostHorn.FRESH).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
	/** The enchanted horn after its ghost horse was killed; two mithril ingots enchant it again. */
	public static final Item EMPTY_GHOST_HORN = register("empty_ghost_horn", Item::new, new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
			.fireResistant().component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.eam.empty_ghost_horn.desc")
					.withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false))))));
	/** The needle that held Koschei's death, dropped by him; used, it bores to the bedrock and bursts. */
	public static final Item KOSCHEI_NEEDLE = register("koschei_needle", KoscheiNeedleItem::new, new Item.Properties().stacksTo(1)
			.rarity(Rarity.EPIC).fireResistant().component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
	public static final Item DEEP_BEDROCK = blockItem("deep_bedrock", EamBlocks.DEEP_BEDROCK);
	public static final Item CHANTERELLE_CAP = blockItem("chanterelle_cap", EamBlocks.CHANTERELLE_CAP);
	public static final Item CHANTERELLE_STEM = blockItem("chanterelle_stem", EamBlocks.CHANTERELLE_STEM);
	public static final Item CHAMPIGNON_CAP = blockItem("champignon_cap", EamBlocks.CHAMPIGNON_CAP);
	public static final Item GLOWING_CHAMPIGNON_CAP = blockItem("glowing_champignon_cap", EamBlocks.GLOWING_CHAMPIGNON_CAP);
	public static final Item BURNT_MUSHROOM_CAP = blockItem("burnt_mushroom_cap", EamBlocks.BURNT_MUSHROOM_CAP);
	public static final Item BURNT_MUSHROOM_STEM = blockItem("burnt_mushroom_stem", EamBlocks.BURNT_MUSHROOM_STEM);
	public static final Item SMOLDERING_MUSHROOM_CAP = blockItem("smoldering_mushroom_cap", EamBlocks.SMOLDERING_MUSHROOM_CAP);
	public static final Item GLOWSHROOM_VINES = blockItem("glowshroom_vines", EamBlocks.GLOWSHROOM_VINES);
	/** A cap cut from a ripe glowshroom tip: eaten raw, a little protein and sugar, or packed into a lamp. */
	public static final Item GLOWSHROOM = register("glowshroom", Item::new, new Item.Properties()
			.food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.2F).build())
			.component(HungerModule.NUTRIENTS, new Nutrients(1, 0, 1))
			.compostable(ContextIntProviders.COMPOSTABLE_MEDIUM));
	public static final Item GLOWSHROOM_LAMP = blockItem("glowshroom_lamp", EamBlocks.GLOWSHROOM_LAMP);

	public static final Item COPPER_ANVIL = blockItem("copper_anvil", EamBlocks.COPPER_ANVIL);
	public static final Item SILVER_ANVIL = blockItem("silver_anvil", EamBlocks.SILVER_ANVIL);

	/** Go in the anvil's hammer slot and make forging faster. */
	public static final Item COPPER_HAMMER = register("copper_hammer", Item::new, new Item.Properties().stacksTo(1));
	public static final Item SILVER_HAMMER = register("silver_hammer", Item::new, new Item.Properties().stacksTo(1));
	public static final Item IRON_HAMMER = register("iron_hammer", Item::new, new Item.Properties().stacksTo(1));

	/** Go in the crafting table's tool slots and make wooden and decorative crafts faster. */
	public static final Item COPPER_SAW = register("copper_saw", Item::new, new Item.Properties().stacksTo(1));
	public static final Item SILVER_SAW = register("silver_saw", Item::new, new Item.Properties().stacksTo(1));
	public static final Item IRON_SAW = register("iron_saw", Item::new, new Item.Properties().stacksTo(1));

	public static final Item MITHRIL_ORE = blockItem("mithril_ore", EamBlocks.MITHRIL_ORE);
	public static final Item DEEPSLATE_MITHRIL_ORE = blockItem("deepslate_mithril_ore", EamBlocks.DEEPSLATE_MITHRIL_ORE);
	public static final Item MITHRIL_BLOCK = blockItem("mithril_block", EamBlocks.MITHRIL_BLOCK);
	public static final Item RAW_MITHRIL = register("raw_mithril", Item::new, new Item.Properties());
	public static final Item MITHRIL_INGOT = register("mithril_ingot", Item::new, new Item.Properties());
	/** A mithril ingot heated in the obsidian blast furnace; cools back into an ingot after a minute, like hot iron. */
	public static final Item MITHRIL_NUGGET = register("mithril_nugget", Item::new, new Item.Properties());
	public static final Item HOT_MITHRIL_INGOT = register("hot_mithril_ingot", p -> new HotIronItem(MITHRIL_INGOT, p), new Item.Properties());
	/** Forged from hot mithril on the mithril anvil; mithril armor is made of these. */
	public static final Item MITHRIL_PLATE = register("mithril_plate", Item::new, new Item.Properties().stacksTo(16));

	/** Mithril tools hit and last like diamond ones. */
	public static final Item MITHRIL_SWORD = register("mithril_sword", Item::new, new Item.Properties().sword(MithrilGear.TOOL, 3.0F, -2.4F));
	public static final Item MITHRIL_SHOVEL = register("mithril_shovel", Item::new, new Item.Properties().shovel(MithrilGear.TOOL, 1.5F, -3.0F));
	public static final Item MITHRIL_PICKAXE = register("mithril_pickaxe", Item::new, new Item.Properties().pickaxe(MithrilGear.TOOL, 1.0F, -2.8F));
	public static final Item MITHRIL_AXE = register("mithril_axe", Item::new, new Item.Properties().axe(MithrilGear.TOOL, 5.0F, -3.0F));
	public static final Item MITHRIL_HOE = register("mithril_hoe", Item::new, new Item.Properties().hoe(MithrilGear.TOOL, -3.0F, 0.0F));

	/** Mithril armor protects and lasts like diamond, before the gear module's cut. */
	public static final Item MITHRIL_HELMET = mithrilArmor("mithril_helmet", ArmorType.HELMET);
	public static final Item MITHRIL_CHESTPLATE = mithrilArmor("mithril_chestplate", ArmorType.CHESTPLATE);
	public static final Item MITHRIL_LEGGINGS = mithrilArmor("mithril_leggings", ArmorType.LEGGINGS);
	public static final Item MITHRIL_BOOTS = mithrilArmor("mithril_boots", ArmorType.BOOTS);

	public static final Item MITHRIL_KNIFE = register("mithril_knife", KnifeItem::new, new Item.Properties()
			.sword(MithrilGear.KNIFE, MithrilGear.KNIFE_DAMAGE, FlintTools.KNIFE_ATTACK_SPEED));
	public static final Item MITHRIL_SAW = register("mithril_saw", Item::new, new Item.Properties().stacksTo(1));
	public static final Item MITHRIL_HAMMER = register("mithril_hammer", Item::new, new Item.Properties().stacksTo(1));
	public static final Item MITHRIL_ANVIL = blockItem("mithril_anvil", EamBlocks.MITHRIL_ANVIL);

	public static final Item ADAMANTIUM_ORE = blockItem("adamantium_ore", EamBlocks.ADAMANTIUM_ORE);
	public static final Item DEEPSLATE_ADAMANTIUM_ORE = blockItem("deepslate_adamantium_ore", EamBlocks.DEEPSLATE_ADAMANTIUM_ORE);
	public static final Item ADAMANTIUM_BLOCK = blockItem("adamantium_block", EamBlocks.ADAMANTIUM_BLOCK);
	public static final Item RAW_ADAMANTIUM = register("raw_adamantium", Item::new, new Item.Properties());
	public static final Item ADAMANTIUM_INGOT = register("adamantium_ingot", Item::new, new Item.Properties());
	public static final Item ADAMANTIUM_NUGGET = register("adamantium_nugget", Item::new, new Item.Properties());
	/** An adamantium ingot heated in the obsidian blast furnace; cools back into an ingot after a minute, like hot iron. */
	public static final Item HOT_ADAMANTIUM_INGOT = register("hot_adamantium_ingot", p -> new HotIronItem(ADAMANTIUM_INGOT, p), new Item.Properties());
	/** Forged from hot adamantium on the adamantium anvil; adamantium armor is made of these. */
	public static final Item ADAMANTIUM_PLATE = register("adamantium_plate", Item::new, new Item.Properties().stacksTo(16));

	/** Adamantium tools hit and last like vanilla netherite ones; netherite is their smithing upgrade. */
	public static final Item ADAMANTIUM_SWORD = register("adamantium_sword", Item::new, new Item.Properties().sword(AdamantiumGear.TOOL, 3.0F, -2.4F));
	public static final Item ADAMANTIUM_SHOVEL = register("adamantium_shovel", Item::new, new Item.Properties().shovel(AdamantiumGear.TOOL, 1.5F, -3.0F));
	public static final Item ADAMANTIUM_PICKAXE = register("adamantium_pickaxe", Item::new, new Item.Properties().pickaxe(AdamantiumGear.TOOL, 1.0F, -2.8F));
	public static final Item ADAMANTIUM_AXE = register("adamantium_axe", Item::new, new Item.Properties().axe(AdamantiumGear.TOOL, 5.0F, -3.0F));
	public static final Item ADAMANTIUM_HOE = register("adamantium_hoe", Item::new, new Item.Properties().hoe(AdamantiumGear.TOOL, -4.0F, 0.0F));

	/** Adamantium armor protects and lasts like vanilla netherite, before the gear module's cut. */
	public static final Item ADAMANTIUM_HELMET = armor("adamantium_helmet", AdamantiumGear.ARMOR, ArmorType.HELMET);
	public static final Item ADAMANTIUM_CHESTPLATE = armor("adamantium_chestplate", AdamantiumGear.ARMOR, ArmorType.CHESTPLATE);
	public static final Item ADAMANTIUM_LEGGINGS = armor("adamantium_leggings", AdamantiumGear.ARMOR, ArmorType.LEGGINGS);
	public static final Item ADAMANTIUM_BOOTS = armor("adamantium_boots", AdamantiumGear.ARMOR, ArmorType.BOOTS);

	public static final Item ADAMANTIUM_KNIFE = register("adamantium_knife", KnifeItem::new, new Item.Properties()
			.sword(AdamantiumGear.KNIFE, AdamantiumGear.KNIFE_DAMAGE, FlintTools.KNIFE_ATTACK_SPEED));
	public static final Item ADAMANTIUM_SAW = register("adamantium_saw", Item::new, new Item.Properties().stacksTo(1));
	public static final Item ADAMANTIUM_HAMMER = register("adamantium_hammer", Item::new, new Item.Properties().stacksTo(1));
	public static final Item ADAMANTIUM_ANVIL = blockItem("adamantium_anvil", EamBlocks.ADAMANTIUM_ANVIL);

	public static final Item OBSIDIAN_BLAST_FURNACE = blockItem("obsidian_blast_furnace", EamBlocks.OBSIDIAN_BLAST_FURNACE);

	/** Rusty iron gear is found in dungeons, never crafted: a little worse than iron, mended with iron like it. */
	public static final Item RUSTY_IRON_SWORD = register("rusty_iron_sword", Item::new, new Item.Properties().sword(RustyGear.TOOL, 3.0F, -2.4F));
	public static final Item RUSTY_IRON_SHOVEL = register("rusty_iron_shovel", Item::new, new Item.Properties().shovel(RustyGear.TOOL, 1.5F, -3.0F));
	public static final Item RUSTY_IRON_PICKAXE = register("rusty_iron_pickaxe", Item::new, new Item.Properties().pickaxe(RustyGear.TOOL, 1.0F, -2.8F));
	public static final Item RUSTY_IRON_AXE = register("rusty_iron_axe", Item::new, new Item.Properties().axe(RustyGear.TOOL, 6.0F, -3.1F));
	public static final Item RUSTY_IRON_HOE = register("rusty_iron_hoe", Item::new, new Item.Properties().hoe(RustyGear.TOOL, -1.0F, -1.0F));

	public static final Item RUSTY_IRON_HELMET = armor("rusty_iron_helmet", RustyGear.IRON_ARMOR, ArmorType.HELMET);
	public static final Item RUSTY_IRON_CHESTPLATE = armor("rusty_iron_chestplate", RustyGear.IRON_ARMOR, ArmorType.CHESTPLATE);
	public static final Item RUSTY_IRON_LEGGINGS = armor("rusty_iron_leggings", RustyGear.IRON_ARMOR, ArmorType.LEGGINGS);
	public static final Item RUSTY_IRON_BOOTS = armor("rusty_iron_boots", RustyGear.IRON_ARMOR, ArmorType.BOOTS);

	public static final Item FALLEN_LORD_SPAWN_EGG = register("fallen_lord_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.FALLEN_LORD));
	public static final Item SKELETON_KNIGHT_SPAWN_EGG = register("skeleton_knight_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.SKELETON_KNIGHT));
	public static final Item LOST_SPAWN_EGG = register("lost_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.LOST));
	public static final Item LOST_CENTURION_SPAWN_EGG = register("lost_centurion_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.LOST_CENTURION));
	public static final Item LOST_WALL_COMMANDER_SPAWN_EGG = register("lost_wall_commander_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.LOST_WALL_COMMANDER));
	public static final Item LOST_LONGBOWMAN_SPAWN_EGG = register("lost_longbowman_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.LOST_LONGBOWMAN));
	public static final Item LOST_CAVALRYMAN_SPAWN_EGG = register("lost_cavalryman_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.LOST_CAVALRYMAN));
	public static final Item UNDEAD_WARLOCK_SPAWN_EGG = register("undead_warlock_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.UNDEAD_WARLOCK));
	public static final Item PILLAGER_KING_SPAWN_EGG = register("pillager_king_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.PILLAGER_KING));
	public static final Item ELITE_CROSSBOWMAN_SPAWN_EGG = register("elite_crossbowman_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.ELITE_CROSSBOWMAN));
	public static final Item PILLAGER_GUARD_SPAWN_EGG = register("pillager_guard_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.PILLAGER_GUARD));
	public static final Item SWAMP_LURKER_SPAWN_EGG = register("swamp_lurker_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.SWAMP_LURKER));
	public static final Item KOSCHEI_SPAWN_EGG = register("koschei_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.KOSCHEI));
	public static final Item BABA_YAGA_SPAWN_EGG = register("baba_yaga_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.BABA_YAGA));
	public static final Item DROWNED_SOLDIER_SPAWN_EGG = register("drowned_soldier_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.DROWNED_SOLDIER));
	public static final Item UPYR_SPAWN_EGG = register("upyr_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.UPYR));
	public static final Item LESHY_SPAWN_EGG = register("leshy_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.LESHY));
	public static final Item WIGHT_SPAWN_EGG = register("wight_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.WIGHT));
	public static final Item DRAUGR_SPAWN_EGG = register("draugr_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.DRAUGR));
	public static final Item ZOMBIE_DUERGAR_SPAWN_EGG = register("zombie_duergar_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.ZOMBIE_DUERGAR));
	public static final Item DUERGAR_RENEGADE_SPAWN_EGG = register("duergar_renegade_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.DUERGAR_RENEGADE));
	public static final Item DUERGAR_VILLAGER_SPAWN_EGG = register("duergar_villager_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.DUERGAR_VILLAGER));
	public static final Item WOOD_SPIDER_SPAWN_EGG = register("wood_spider_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.WOOD_SPIDER));
	public static final Item BLACK_WIDOW_SPAWN_EGG = register("black_widow_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.BLACK_WIDOW));
	public static final Item VAMPIRE_BAT_SPAWN_EGG = register("vampire_bat_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.VAMPIRE_BAT));
	public static final Item WARG_SPAWN_EGG = register("warg_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.WARG));
	/** Brings the leader with his whole pack. */
	public static final Item WARG_LEADER_SPAWN_EGG = register("warg_leader_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.WARG_LEADER));
	public static final Item BOAR_SPAWN_EGG = register("boar_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.BOAR));
	public static final Item CAVE_RAM_SPAWN_EGG = register("cave_ram_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.CAVE_RAM));
	/** Brings the leader with his whole squad, each on his ram. */
	public static final Item DUERGAR_RAM_RIDER_SPAWN_EGG = register("duergar_ram_rider_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(EamEntities.DUERGAR_RAM_RIDER));

	public static final Item RUSTY_CHAINMAIL_HELMET = armor("rusty_chainmail_helmet", RustyGear.CHAINMAIL_ARMOR, ArmorType.HELMET);
	public static final Item RUSTY_CHAINMAIL_CHESTPLATE = armor("rusty_chainmail_chestplate", RustyGear.CHAINMAIL_ARMOR, ArmorType.CHESTPLATE);
	public static final Item RUSTY_CHAINMAIL_LEGGINGS = armor("rusty_chainmail_leggings", RustyGear.CHAINMAIL_ARMOR, ArmorType.LEGGINGS);
	public static final Item RUSTY_CHAINMAIL_BOOTS = armor("rusty_chainmail_boots", RustyGear.CHAINMAIL_ARMOR, ArmorType.BOOTS);

	/** Arrows with metal heads hit harder than the flint one; silver also hits undead harder. Fired from bows and crossbows. */
	public static final Item COPPER_ARROW = arrow("copper_arrow", new Item.Properties().component(ArrowHeadModule.ARROW_HEAD, ArrowHeads.COPPER));
	public static final Item SILVER_ARROW = arrow("silver_arrow", new Item.Properties().component(ArrowHeadModule.ARROW_HEAD, ArrowHeads.SILVER)
			.component(SilverModule.UNDEAD_DAMAGE_BONUS, SilverGear.UNDEAD_DAMAGE_BONUS));
	public static final Item IRON_ARROW = arrow("iron_arrow", new Item.Properties().component(ArrowHeadModule.ARROW_HEAD, ArrowHeads.IRON));
	public static final Item MITHRIL_ARROW = arrow("mithril_arrow", new Item.Properties().component(ArrowHeadModule.ARROW_HEAD, ArrowHeads.MITHRIL));

	/** The Lost longbowman's bow: drawn twice as long, shoots twice as fast and hits two and a half times as hard. */
	public static final Item LONGBOW = register("longbow", LongbowItem::new, new Item.Properties().durability(384).enchantable(1));

	/** Ancient metal sits between iron and mithril; it has no source yet and will come from another dimension. */
	public static final Item ANCIENT_METAL_BLOCK = blockItem("ancient_metal_block", EamBlocks.ANCIENT_METAL_BLOCK);
	public static final Item ANCIENT_METAL_INGOT = register("ancient_metal_ingot", Item::new, new Item.Properties());
	public static final Item ANCIENT_METAL_NUGGET = register("ancient_metal_nugget", Item::new, new Item.Properties());
	/** An ancient metal ingot heated in a blast furnace; cools back into an ingot after a minute, like hot iron. */
	public static final Item HOT_ANCIENT_METAL_INGOT = register("hot_ancient_metal_ingot", p -> new HotIronItem(ANCIENT_METAL_INGOT, p),
			new Item.Properties());

	/**
	 * Metal-forked slingshots: harder pebbles by tier, and they shoot splash or lingering potions held in the other hand.
	 * Declared after every ingot they repair with.
	 */
	public static final Item COPPER_POWER_SLINGSHOT = powerSlingshot("copper", SlingshotLogic.COPPER, 256, Items.COPPER_INGOT);
	public static final Item SILVER_POWER_SLINGSHOT = powerSlingshot("silver", SlingshotLogic.SILVER, 256, SILVER_INGOT);
	public static final Item IRON_POWER_SLINGSHOT = powerSlingshot("iron", SlingshotLogic.IRON, 384, Items.IRON_INGOT);
	public static final Item ANCIENT_METAL_POWER_SLINGSHOT = powerSlingshot("ancient_metal", SlingshotLogic.ANCIENT_METAL, 640,
			ANCIENT_METAL_INGOT);
	public static final Item MITHRIL_POWER_SLINGSHOT = powerSlingshot("mithril", SlingshotLogic.MITHRIL, 1200, MITHRIL_INGOT);
	public static final Item ADAMANTIUM_POWER_SLINGSHOT = powerSlingshot("adamantium", SlingshotLogic.ADAMANTIUM, 1600, ADAMANTIUM_INGOT);

	/** Forged from hot ancient metal on an iron or mithril anvil; ancient metal armor is made of these. */
	public static final Item ANCIENT_METAL_PLATE = register("ancient_metal_plate", Item::new, new Item.Properties().stacksTo(16));

	public static final Item ANCIENT_METAL_SWORD = register("ancient_metal_sword", Item::new,
			new Item.Properties().sword(AncientMetalGear.TOOL, 3.0F, -2.4F));
	public static final Item ANCIENT_METAL_SHOVEL = register("ancient_metal_shovel", Item::new,
			new Item.Properties().shovel(AncientMetalGear.TOOL, 1.5F, -3.0F));
	public static final Item ANCIENT_METAL_PICKAXE = register("ancient_metal_pickaxe", Item::new,
			new Item.Properties().pickaxe(AncientMetalGear.TOOL, 1.0F, -2.8F));
	public static final Item ANCIENT_METAL_AXE = register("ancient_metal_axe", Item::new,
			new Item.Properties().axe(AncientMetalGear.TOOL, 5.5F, -3.05F));
	public static final Item ANCIENT_METAL_HOE = register("ancient_metal_hoe", Item::new,
			new Item.Properties().hoe(AncientMetalGear.TOOL, -2.5F, -0.5F));

	public static final Item ANCIENT_METAL_HELMET = armor("ancient_metal_helmet", AncientMetalGear.ARMOR, ArmorType.HELMET);
	public static final Item ANCIENT_METAL_CHESTPLATE = armor("ancient_metal_chestplate", AncientMetalGear.ARMOR, ArmorType.CHESTPLATE);
	public static final Item ANCIENT_METAL_LEGGINGS = armor("ancient_metal_leggings", AncientMetalGear.ARMOR, ArmorType.LEGGINGS);
	public static final Item ANCIENT_METAL_BOOTS = armor("ancient_metal_boots", AncientMetalGear.ARMOR, ArmorType.BOOTS);

	public static final Item ANCIENT_METAL_CHAINMAIL_HELMET = armor("ancient_metal_chainmail_helmet", AncientMetalGear.CHAINMAIL_ARMOR,
			ArmorType.HELMET);
	public static final Item ANCIENT_METAL_CHAINMAIL_CHESTPLATE = armor("ancient_metal_chainmail_chestplate", AncientMetalGear.CHAINMAIL_ARMOR,
			ArmorType.CHESTPLATE);
	public static final Item ANCIENT_METAL_CHAINMAIL_LEGGINGS = armor("ancient_metal_chainmail_leggings", AncientMetalGear.CHAINMAIL_ARMOR,
			ArmorType.LEGGINGS);
	public static final Item ANCIENT_METAL_CHAINMAIL_BOOTS = armor("ancient_metal_chainmail_boots", AncientMetalGear.CHAINMAIL_ARMOR,
			ArmorType.BOOTS);

	public static final Item ANCIENT_METAL_KNIFE = register("ancient_metal_knife", KnifeItem::new, new Item.Properties()
			.sword(AncientMetalGear.KNIFE, AncientMetalGear.KNIFE_DAMAGE, FlintTools.KNIFE_ATTACK_SPEED));
	public static final Item ANCIENT_METAL_SAW = register("ancient_metal_saw", Item::new, new Item.Properties().stacksTo(1));
	public static final Item ANCIENT_METAL_HAMMER = register("ancient_metal_hammer", Item::new, new Item.Properties().stacksTo(1));
	public static final Item ANCIENT_METAL_ARROW = arrow("ancient_metal_arrow",
			new Item.Properties().component(ArrowHeadModule.ARROW_HEAD, ArrowHeads.ANCIENT_METAL));

	/** Chainmail rings, four nuggets each; chainmail takes them in the shapes ingot armor takes ingots. */
	public static final Item COPPER_RING = register("copper_ring", Item::new, new Item.Properties());
	public static final Item SILVER_RING = register("silver_ring", Item::new, new Item.Properties());
	public static final Item IRON_RING = register("iron_ring", Item::new, new Item.Properties());
	public static final Item ANCIENT_METAL_RING = register("ancient_metal_ring", Item::new, new Item.Properties());
	public static final Item MITHRIL_RING = register("mithril_ring", Item::new, new Item.Properties());

	/** Chainmail of each metal protects a little better than the plate armor of the tier below; the iron set is vanilla's. */
	public static final Item COPPER_CHAINMAIL_HELMET = armor("copper_chainmail_helmet", ChainmailGear.COPPER, ArmorType.HELMET);
	public static final Item COPPER_CHAINMAIL_CHESTPLATE = armor("copper_chainmail_chestplate", ChainmailGear.COPPER, ArmorType.CHESTPLATE);
	public static final Item COPPER_CHAINMAIL_LEGGINGS = armor("copper_chainmail_leggings", ChainmailGear.COPPER, ArmorType.LEGGINGS);
	public static final Item COPPER_CHAINMAIL_BOOTS = armor("copper_chainmail_boots", ChainmailGear.COPPER, ArmorType.BOOTS);
	/** Silver chainmail hurts undead that hit its wearer, like silver armor. */
	public static final Item SILVER_CHAINMAIL_HELMET = silverChainmail("silver_chainmail_helmet", ArmorType.HELMET);
	public static final Item SILVER_CHAINMAIL_CHESTPLATE = silverChainmail("silver_chainmail_chestplate", ArmorType.CHESTPLATE);
	public static final Item SILVER_CHAINMAIL_LEGGINGS = silverChainmail("silver_chainmail_leggings", ArmorType.LEGGINGS);
	public static final Item SILVER_CHAINMAIL_BOOTS = silverChainmail("silver_chainmail_boots", ArmorType.BOOTS);
	public static final Item MITHRIL_CHAINMAIL_HELMET = armor("mithril_chainmail_helmet", ChainmailGear.MITHRIL, ArmorType.HELMET);
	public static final Item MITHRIL_CHAINMAIL_CHESTPLATE = armor("mithril_chainmail_chestplate", ChainmailGear.MITHRIL, ArmorType.CHESTPLATE);
	public static final Item MITHRIL_CHAINMAIL_LEGGINGS = armor("mithril_chainmail_leggings", ChainmailGear.MITHRIL, ArmorType.LEGGINGS);
	public static final Item MITHRIL_CHAINMAIL_BOOTS = armor("mithril_chainmail_boots", ChainmailGear.MITHRIL, ArmorType.BOOTS);

	public static final Item DUCKWEED = register("duckweed", p -> new PlaceOnWaterBlockItem(EamBlocks.DUCKWEED, p),
			plantProperties(ContextIntProviders.COMPOSTABLE_LOW));
	public static final Item CATTAIL = plantItem("cattail", EamBlocks.CATTAIL, ContextIntProviders.COMPOSTABLE_MEDIUM);
	public static final Item SWAMP_GRASS = plantItem("swamp_grass", EamBlocks.SWAMP_GRASS, ContextIntProviders.COMPOSTABLE_LOW);
	public static final Item TALL_SWAMP_GRASS = plantItem("tall_swamp_grass", EamBlocks.TALL_SWAMP_GRASS, ContextIntProviders.COMPOSTABLE_LOW_MEDIUM);
	public static final Item SWAMP_SHRUB = plantItem("swamp_shrub", EamBlocks.SWAMP_SHRUB, ContextIntProviders.COMPOSTABLE_LOW);
	public static final Item DENSE_SHRUB = plantItem("dense_shrub", EamBlocks.DENSE_SHRUB, ContextIntProviders.COMPOSTABLE_MEDIUM);
	public static final Item BIG_SHRUB_STEM = blockItem("big_shrub_stem", EamBlocks.BIG_SHRUB_STEM);
	public static final Item BIG_SHRUB_LEAVES = plantItem("big_shrub_leaves", EamBlocks.BIG_SHRUB_LEAVES, ContextIntProviders.COMPOSTABLE_MEDIUM);
	/** Picked from small swamp shrubs: a sour snack, mostly water and a little sugar. */
	public static final Item CRANBERRY = register("cranberry", Item::new, new Item.Properties()
			.food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.1F).build())
			.component(HungerModule.NUTRIENTS, new Nutrients(0, 0, 2))
			.component(ThirstModule.HYDRATION, new Hydration(1)));
	/** Picked from big swamp shrubs, and poisonous: eating one poisons and sickens. */
	public static final Item WOLFBERRY = register("wolfberry", Item::new, wolfberry());
	/**
	 * A garlic bulb: planted on farmland like a carrot, or eaten raw. Eaten, it wards off bloodsuckers for a while: upyrs
	 * and vampire bats hit for less and upyrs will not drink.
	 */
	public static final Item GARLIC = register("garlic", p -> new BlockItem(EamBlocks.GARLIC_CROP, p), garlic()
			.useItemDescriptionPrefix()
			.compostable(ContextIntProviders.COMPOSTABLE_MEDIUM));
	public static final Item WILD_GARLIC = plantItem("wild_garlic", EamBlocks.WILD_GARLIC, ContextIntProviders.COMPOSTABLE_MEDIUM);
	public static final Item STRIPPED_POPLAR_FENCE = blockItem("stripped_poplar_fence", EamBlocks.STRIPPED_POPLAR_FENCE);
	public static final Item SHELF_MUSHROOM = plantItem("shelf_mushroom", EamBlocks.SHELF_MUSHROOM, ContextIntProviders.COMPOSTABLE_MEDIUM);
	public static final List<Item> HOLLOW_LOGS = EamBlocks.HOLLOW_LOGS.values().stream()
			.map(block -> blockItem(BuiltInRegistries.BLOCK.getKey(block).getPath(), block)).toList();
	public static final Item FIREWEED = plantItem("fireweed", EamBlocks.FIREWEED, ContextIntProviders.COMPOSTABLE_MEDIUM);

	/** The elite crossbowman's net: he shoots it as this item, and creative players can place it. */
	public static final Item NET = blockItem("net", EamBlocks.NET);
	public static final Item MARSH_MARIGOLD = plantItem("marsh_marigold", EamBlocks.MARSH_MARIGOLD, ContextIntProviders.COMPOSTABLE_MEDIUM);

	public static final Item SILVER_BARS = blockItem("silver_bars", EamBlocks.SILVER_BARS);
	public static final Item SILVER_DOOR = doorItem("silver_door", EamBlocks.SILVER_DOOR);
	public static final Item SILVER_TRAPDOOR = blockItem("silver_trapdoor", EamBlocks.SILVER_TRAPDOOR);
	public static final Item MITHRIL_BARS = blockItem("mithril_bars", EamBlocks.MITHRIL_BARS);
	public static final Item MITHRIL_DOOR = doorItem("mithril_door", EamBlocks.MITHRIL_DOOR);
	public static final Item MITHRIL_TRAPDOOR = blockItem("mithril_trapdoor", EamBlocks.MITHRIL_TRAPDOOR);

	/** Metal buckets work like the vanilla one, which counts as iron; the weaker ones may melt when they scoop lava. */
	public static final Item COPPER_BUCKET = register("copper_bucket", p -> new MetalBucketItem(Fluids.EMPTY, p), new Item.Properties().stacksTo(16));
	public static final Item COPPER_WATER_BUCKET = fluidBucket("copper_water_bucket", Fluids.WATER, COPPER_BUCKET, new Item.Properties());
	public static final Item COPPER_LAVA_BUCKET = fluidBucket("copper_lava_bucket", Fluids.LAVA, COPPER_BUCKET,
			new Item.Properties().cookingFuel(ContextIntProviders.COOKING_TIME_LAVA_BUCKET));
	public static final Item COPPER_POWDER_SNOW_BUCKET = register("copper_powder_snow_bucket", MetalPowderSnowBucketItem::new,
			new Item.Properties().stacksTo(1));
	public static final Item COPPER_MILK_BUCKET = milkBucket("copper_milk_bucket", COPPER_BUCKET);

	public static final Item SILVER_BUCKET = register("silver_bucket", p -> new MetalBucketItem(Fluids.EMPTY, p), new Item.Properties().stacksTo(16));
	public static final Item SILVER_WATER_BUCKET = fluidBucket("silver_water_bucket", Fluids.WATER, SILVER_BUCKET, new Item.Properties());
	public static final Item SILVER_LAVA_BUCKET = fluidBucket("silver_lava_bucket", Fluids.LAVA, SILVER_BUCKET,
			new Item.Properties().cookingFuel(ContextIntProviders.COOKING_TIME_LAVA_BUCKET));
	public static final Item SILVER_POWDER_SNOW_BUCKET = register("silver_powder_snow_bucket", MetalPowderSnowBucketItem::new,
			new Item.Properties().stacksTo(1));
	public static final Item SILVER_MILK_BUCKET = milkBucket("silver_milk_bucket", SILVER_BUCKET);

	public static final Item GOLDEN_BUCKET = register("golden_bucket", p -> new MetalBucketItem(Fluids.EMPTY, p), new Item.Properties().stacksTo(16));
	public static final Item GOLDEN_WATER_BUCKET = fluidBucket("golden_water_bucket", Fluids.WATER, GOLDEN_BUCKET, new Item.Properties());
	public static final Item GOLDEN_LAVA_BUCKET = fluidBucket("golden_lava_bucket", Fluids.LAVA, GOLDEN_BUCKET,
			new Item.Properties().cookingFuel(ContextIntProviders.COOKING_TIME_LAVA_BUCKET));
	public static final Item GOLDEN_POWDER_SNOW_BUCKET = register("golden_powder_snow_bucket", MetalPowderSnowBucketItem::new,
			new Item.Properties().stacksTo(1));
	public static final Item GOLDEN_MILK_BUCKET = milkBucket("golden_milk_bucket", GOLDEN_BUCKET);

	public static final Item MITHRIL_BUCKET = register("mithril_bucket", p -> new MetalBucketItem(Fluids.EMPTY, p), new Item.Properties().stacksTo(16));
	public static final Item MITHRIL_WATER_BUCKET = fluidBucket("mithril_water_bucket", Fluids.WATER, MITHRIL_BUCKET, new Item.Properties());
	public static final Item MITHRIL_LAVA_BUCKET = fluidBucket("mithril_lava_bucket", Fluids.LAVA, MITHRIL_BUCKET,
			new Item.Properties().cookingFuel(ContextIntProviders.COOKING_TIME_LAVA_BUCKET));
	public static final Item MITHRIL_POWDER_SNOW_BUCKET = register("mithril_powder_snow_bucket", MetalPowderSnowBucketItem::new,
			new Item.Properties().stacksTo(1));
	public static final Item MITHRIL_MILK_BUCKET = milkBucket("mithril_milk_bucket", MITHRIL_BUCKET);

	/**
	 * MITE bowl foods. A bowl of water is filled at a water source and may poison until boiled; soups, stews and
	 * the rest are crafted from it or from a bowl of milk. All of them give back the bowl, like vanilla stews.
	 */
	public static final Item WATER_BOWL = register("water_bowl", Item::new, bowl()
			.component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
			.component(ThirstModule.HYDRATION, new Hydration(4))
			.component(ThirstModule.RAW_WATER, RawWater.INSTANCE));
	public static final Item BOILED_WATER_BOWL = register("boiled_water_bowl", Item::new, bowl()
			.component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
			.component(ThirstModule.HYDRATION, new Hydration(4)));
	/** Clears effects like a bucket of milk. */
	public static final Item MILK_BOWL = register("milk_bowl", Item::new, bowl()
			.food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.2F).alwaysEdible().build(),
					Consumables.defaultDrink().onConsume(ClearAllStatusEffectsConsumeEffect.INSTANCE).build())
			.component(HungerModule.NUTRIENTS, new Nutrients(4, 4, 3))
			.component(ThirstModule.HYDRATION, new Hydration(3)));
	//                                               nutrition, saturation, protein, fat, carbs, water
	public static final Item BEEF_STEW = bowlFood("beef_stew", 8, 0.6F, 16, 8, 10, 3);
	public static final Item CHICKEN_SOUP = bowlFood("chicken_soup", 5, 0.6F, 12, 3, 8, 4);
	public static final Item VEGETABLE_SOUP = bowlFood("vegetable_soup", 3, 0.5F, 2, 0, 12, 4);
	public static final Item CREAM_OF_MUSHROOM_SOUP = bowlFood("cream_of_mushroom_soup", 4, 0.5F, 5, 4, 6, 3);
	public static final Item CREAM_OF_VEGETABLE_SOUP = bowlFood("cream_of_vegetable_soup", 4, 0.5F, 4, 4, 12, 3);
	public static final Item PUMPKIN_SOUP = bowlFood("pumpkin_soup", 2, 0.4F, 1, 1, 7, 4);
	public static final Item MASHED_POTATO = bowlFood("mashed_potato", 5, 0.7F, 6, 6, 14, 1);
	public static final Item SALAD = bowlFood("salad", 1, 0.3F, 1, 0, 4, 1);
	public static final Item ICE_CREAM = bowlFood("ice_cream", 2, 0.4F, 2, 6, 8, 1);
	public static final Item SORBET = bowlFood("sorbet", 2, 0.3F, 0, 0, 8, 2);
	public static final Item PORRIDGE = bowlFood("porridge", 2, 0.4F, 1, 1, 9, 2);
	public static final Item CEREAL = bowlFood("cereal", 2, 0.4F, 3, 2, 8, 2);
	public static final Item CHOCOLATE = register("chocolate", Item::new, new Item.Properties()
			.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build())
			.component(HungerModule.NUTRIENTS, new Nutrients(1, 5, 6)));
	/**
	 * Meat of a warg: tough and lean, worth a little less than mutton. The hunger module sets its MITE values and
	 * nutrients like the vanilla meats'; it also brews holy water into a light potion of strength.
	 */
	public static final Item WOLF_MEAT = register("wolf_meat", Item::new, new Item.Properties().food(Foods.MUTTON));
	public static final Item COOKED_WOLF_MEAT = register("cooked_wolf_meat", Item::new, new Item.Properties().food(Foods.COOKED_MUTTON));
	/** Made from a bucket of milk; mashed potato needs it. */
	public static final Item CHEESE = register("cheese", Item::new, new Item.Properties()
			.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.6F).build())
			.component(HungerModule.NUTRIENTS, new Nutrients(8, 7, 1)));

	/**
	 * Food overcooked on a camp fire: two thirds of the cooked food's value (the hunger module sets the MITE values the
	 * same way). Ash is what is left after another minute; with a water bottle it makes black dye.
	 */
	public static final Item BURNT_BEEF = burntFood("burnt_beef", Foods.COOKED_BEEF);
	public static final Item BURNT_PORKCHOP = burntFood("burnt_porkchop", Foods.COOKED_PORKCHOP);
	public static final Item BURNT_MUTTON = burntFood("burnt_mutton", Foods.COOKED_MUTTON);
	public static final Item BURNT_CHICKEN = burntFood("burnt_chicken", Foods.COOKED_CHICKEN);
	public static final Item BURNT_RABBIT = burntFood("burnt_rabbit", Foods.COOKED_RABBIT);
	public static final Item BURNT_COD = burntFood("burnt_cod", Foods.COOKED_COD);
	public static final Item BURNT_SALMON = burntFood("burnt_salmon", Foods.COOKED_SALMON);
	public static final Item BURNT_POTATO = burntFood("burnt_potato", Foods.BAKED_POTATO);
	public static final Item BURNT_WOLF_MEAT = burntFood("burnt_wolf_meat", Foods.COOKED_MUTTON);
	public static final Item ASH = register("ash", Item::new, new Item.Properties());
	/** What spoiled fruit, vegetables and cooked food turn into: not food any more, but good compost. */
	public static final Item ROTTEN_FOOD = register("rotten_food", Item::new, new Item.Properties().compostable(ContextIntProviders.COMPOSTABLE_MEDIUM_HIGH));

	/**
	 * Food taken off a food dryer: it keeps longer and costs a little water when eaten (the dryer module gives each the
	 * {@code eam:dried} component), but feeds as much as the fresh food. The hunger module copies the fresh food's MITE
	 * values onto the vanilla ones; ours copy them here. Dried wolfberries still poison and dried garlic still wards.
	 */
	public static final Item DRIED_APPLE = driedFood("dried_apple", Foods.APPLE);
	public static final Item DRIED_CARROT = driedFood("dried_carrot", Foods.CARROT);
	public static final Item DRIED_BEETROOT = driedFood("dried_beetroot", Foods.BEETROOT);
	public static final Item DRIED_MELON_SLICE = driedFood("dried_melon_slice", Foods.MELON_SLICE);
	public static final Item DRIED_SWEET_BERRIES = driedFood("dried_sweet_berries", Foods.SWEET_BERRIES);
	public static final Item DRIED_GLOW_BERRIES = driedFood("dried_glow_berries", Foods.GLOW_BERRIES);
	public static final Item DRIED_CRANBERRY = register("dried_cranberry", Item::new, new Item.Properties()
			.food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.1F).build())
			.component(HungerModule.NUTRIENTS, new Nutrients(0, 0, 2)));
	public static final Item DRIED_WOLFBERRY = register("dried_wolfberry", Item::new, wolfberry());
	public static final Item DRIED_GARLIC = register("dried_garlic", Item::new, garlic());
	public static final Item DRIED_BREAD = driedFood("dried_bread", Foods.BREAD);
	public static final Item DRIED_BEEF = driedFood("dried_beef", Foods.COOKED_BEEF);
	public static final Item DRIED_PORKCHOP = driedFood("dried_porkchop", Foods.COOKED_PORKCHOP);
	public static final Item DRIED_MUTTON = driedFood("dried_mutton", Foods.COOKED_MUTTON);
	public static final Item DRIED_CHICKEN = driedFood("dried_chicken", Foods.COOKED_CHICKEN);
	public static final Item DRIED_RABBIT = driedFood("dried_rabbit", Foods.COOKED_RABBIT);
	public static final Item DRIED_WOLF_MEAT = driedFood("dried_wolf_meat", Foods.COOKED_MUTTON);
	public static final Item DRIED_COD = driedFood("dried_cod", Foods.COOKED_COD);
	public static final Item DRIED_SALMON = driedFood("dried_salmon", Foods.COOKED_SALMON);

	/** A bowl of plain or boiled water, for soup recipes. */
	public static final TagKey<Item> WATER_BOWLS = TagKey.create(Registries.ITEM, Eam.id("water_bowls"));

	/** Thread for flasks and slingshots: leather thread or plain string. */
	public static final TagKey<Item> FLASK_THREADS = TagKey.create(Registries.ITEM, Eam.id("flask_threads"));

	private EamItems() {
	}

	public static void register() {
		EamCreativeTabs.register();
	}

	private static Item.Properties bowl() {
		return new Item.Properties().stacksTo(1).usingConvertsTo(Items.BOWL);
	}

	private static Item bowlFood(String name, int nutrition, float saturation, int protein, int fat, int carbs, int water) {
		return register(name, Item::new, bowl()
				.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build())
				.component(HungerModule.NUTRIENTS, new Nutrients(protein, fat, carbs))
				.component(ThirstModule.HYDRATION, new Hydration(water)));
	}

	/** Same saturation modifier as the cooked food, so saturation drops with nutrition. */
	private static Item burntFood(String name, FoodProperties cooked) {
		return register(name, Item::new, new Item.Properties().food(new FoodProperties.Builder()
				.nutrition(CampFireLogic.burntValue(cooked.nutrition()))
				.saturationModifier(cooked.saturation() / (cooked.nutrition() * 2.0F))
				.build()));
	}

	/** Poisonous berries: eating one poisons and sickens. */
	private static Item.Properties wolfberry() {
		return new Item.Properties()
				.food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.0F).alwaysEdible().build(),
						Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
								new MobEffectInstance(MobEffects.POISON, 200), new MobEffectInstance(MobEffects.NAUSEA, 200)))).build())
				.component(HungerModule.NUTRIENTS, new Nutrients(0, 0, 1));
	}

	/** Garlic eaten wards off bloodsuckers for a while. */
	private static Item.Properties garlic() {
		return new Item.Properties()
				.food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.1F).alwaysEdible().build(),
						Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(
								new MobEffectInstance(UpyrModule.GARLIC, GARLIC_WARD_TICKS))).build())
				.component(HungerModule.NUTRIENTS, new Nutrients(0, 0, 1));
	}

	private static Item driedFood(String name, FoodProperties fresh) {
		return register(name, Item::new, new Item.Properties().food(fresh));
	}

	private static Item blockItem(String name, Block block) {
		return register(name, p -> new BlockItem(block, p), new Item.Properties().useBlockDescriptionPrefix());
	}

	/** A plant's item; two-block plants place both halves, like vanilla tall grass. */
	private static Item plantItem(String name, Block block, ResourceKey<ContextIntProvider> compostChance) {
		if (block instanceof DoublePlantBlock) {
			return register(name, p -> new DoubleHighBlockItem(block, p), plantProperties(compostChance));
		}
		return register(name, p -> new BlockItem(block, p), plantProperties(compostChance));
	}

	private static Item.Properties plantProperties(ResourceKey<ContextIntProvider> compostChance) {
		return new Item.Properties().useBlockDescriptionPrefix().compostable(compostChance);
	}

	/** A door places both halves, like the vanilla ones. */
	private static Item doorItem(String name, Block block) {
		return register(name, p -> new DoubleHighBlockItem(block, p), new Item.Properties().useBlockDescriptionPrefix());
	}

	private static Item arrow(String name, Item.Properties properties) {
		Item item = register(name, ArrowItem::new, properties);
		DispenserBlock.registerProjectileBehavior(item);
		return item;
	}

	/** A metal bucket of water or lava; it leaves its empty bucket behind in crafting and furnaces. */
	private static Item fluidBucket(String name, Fluid fluid, Item empty, Item.Properties properties) {
		return register(name, p -> new MetalBucketItem(fluid, p), properties.craftRemainder(empty).stacksTo(1));
	}

	/** A metal bucket of milk: drunk like the vanilla one, leaving its empty bucket. */
	private static Item milkBucket(String name, Item empty) {
		return register(name, Item::new, new Item.Properties().craftRemainder(empty).component(DataComponents.CONSUMABLE, Consumables.MILK_BUCKET)
				.usingConvertsTo(empty).stacksTo(1));
	}

	private static Item silverTool(String name, Item.Properties properties) {
		return register(name, Item::new, properties.component(SilverModule.UNDEAD_DAMAGE_BONUS, SilverGear.UNDEAD_DAMAGE_BONUS));
	}

	private static Item silverArmor(String name, ArmorType type) {
		return register(name, Item::new, new Item.Properties().humanoidArmor(SilverGear.ARMOR, type)
				.component(SilverModule.UNDEAD_THORNS, SilverGear.UNDEAD_THORNS_PER_PIECE));
	}

	private static Item silverChainmail(String name, ArmorType type) {
		return register(name, Item::new, new Item.Properties().humanoidArmor(ChainmailGear.SILVER, type)
				.component(SilverModule.UNDEAD_THORNS, SilverGear.UNDEAD_THORNS_PER_PIECE));
	}

	private static Item mithrilArmor(String name, ArmorType type) {
		return armor(name, MithrilGear.ARMOR, type);
	}

	private static Item armor(String name, ArmorMaterial material, ArmorType type) {
		return register(name, Item::new, new Item.Properties().humanoidArmor(material, type));
	}

	private static Item powerSlingshot(String metal, SlingshotLogic.PowerTier tier, int durability, Item ingot) {
		return register(metal + "_power_slingshot", properties -> new PowerSlingshotItem(tier, properties),
				new Item.Properties().durability(durability).repairable(ingot));
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Eam.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}
}
