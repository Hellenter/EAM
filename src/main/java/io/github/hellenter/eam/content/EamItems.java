package io.github.hellenter.eam.content;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.brewing.BrewingModule;
import io.github.hellenter.eam.content.brewing.SilverBrewingLogic;
import io.github.hellenter.eam.content.ground.PebbleItem;
import io.github.hellenter.eam.content.sieve.SieveLogic;
import io.github.hellenter.eam.content.silver.SilverGear;
import io.github.hellenter.eam.content.silver.SilverModule;
import io.github.hellenter.eam.content.slingshot.SlingshotItem;
import io.github.hellenter.eam.content.tools.FlintTools;
import io.github.hellenter.eam.content.tools.KnifeItem;
import io.github.hellenter.eam.content.tools.MetalKnives;
import io.github.hellenter.eam.survival.thirst.FlaskItem;
import io.github.hellenter.eam.survival.thirst.FlaskLogic;
import io.github.hellenter.eam.survival.thirst.FlaskWater;
import io.github.hellenter.eam.survival.thirst.ThirstModule;
import io.github.hellenter.eam.world.forging.HotIronItem;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;

import java.util.function.Function;

/** Items added by EAM. Registered whatever the config says, so client and server registries match. */
public final class EamItems {
	public static final Item LEATHER_THREAD = register("leather_thread", Item::new, new Item.Properties());

	public static final Item LEATHER_FLASK = register("leather_flask", FlaskItem::new, new Item.Properties()
			.stacksTo(1)
			.component(ThirstModule.FLASK_WATER, FlaskWater.EMPTY));

	public static final Item FILTERED_LEATHER_FLASK = register("filtered_leather_flask", FlaskItem::new, new Item.Properties()
			.stacksTo(1)
			.component(ThirstModule.FLASK_WATER, FlaskWater.EMPTY)
			.component(ThirstModule.FILTER, FlaskLogic.FILTER_USES));

	/** Thrown like a snowball; sneak-use lays it on the ground. */
	public static final Item PEBBLE = register("pebble", p -> new PebbleItem(EamBlocks.PEBBLE, p),
			new Item.Properties().useBlockDescriptionPrefix().useCooldown(PebbleItem.THROW_COOLDOWN_SECONDS));

	public static final Item FLINT_SHARD = register("flint_shard", p -> new BlockItem(EamBlocks.FLINT_SHARD, p),
			new Item.Properties().useBlockDescriptionPrefix());

	public static final Item CAMP_FIRE = register("camp_fire", p -> new BlockItem(EamBlocks.CAMP_FIRE, p),
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

	/** An iron ingot heated in a furnace; forged into plates on an anvil, cools back into an ingot after a minute. */
	public static final Item HOT_IRON_INGOT = register("hot_iron_ingot", HotIronItem::new, new Item.Properties());

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

	/** Turns holy water into a light strength potion. */
	public static final Item FERMENTED_ENDER_EYE = register("fermented_ender_eye", Item::new, new Item.Properties());

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

	/** Thread for flasks and slingshots: leather thread or plain string. */
	public static final TagKey<Item> FLASK_THREADS = TagKey.create(Registries.ITEM, Eam.id("flask_threads"));

	private EamItems() {
	}

	public static void register() {
		registerBrewingTabs();
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output ->
				output.insertAfter(Items.LEATHER, LEATHER_THREAD));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(output ->
				output.insertAfter(Items.MILK_BUCKET, LEATHER_FLASK, FILTERED_LEATHER_FLASK));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output ->
				output.insertAfter(Items.FLINT, PEBBLE, FLINT_SHARD));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output ->
				output.insertAfter(Items.SNOWBALL, PEBBLE));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output ->
				output.insertAfter(Items.BOW, SLINGSHOT));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output ->
				output.insertBefore(Items.CAMPFIRE, CAMP_FIRE));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output ->
				output.insertAfter(Items.COMPOSTER, SIEVE));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output ->
				output.insertBefore(Items.ANVIL, COPPER_ANVIL, SILVER_ANVIL));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output ->
				output.insertAfter(Items.SHEARS, COPPER_HAMMER, SILVER_HAMMER, IRON_HAMMER, COPPER_SAW, SILVER_SAW, IRON_SAW));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output ->
				output.insertBefore(Items.WOODEN_SHOVEL, FLINT_HATCHET, FLINT_AXE));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output ->
				output.insertBefore(Items.WOODEN_SWORD, FLINT_KNIFE, COPPER_KNIFE, SILVER_KNIFE, IRON_KNIFE));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output ->
				output.insertAfter(Items.IRON_INGOT, HOT_IRON_INGOT, IRON_PLATE));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(output ->
				output.insertAfter(Items.DEEPSLATE_COPPER_ORE, SILVER_ORE, DEEPSLATE_SILVER_ORE));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output ->
				output.insertAfter(Items.GOLD_BLOCK, SILVER_BLOCK));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output ->
				output.insertAfter(Items.RAW_GOLD, RAW_SILVER));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output ->
				output.insertAfter(Items.GOLD_NUGGET, SILVER_NUGGET));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output ->
				output.insertAfter(Items.GOLD_INGOT, SILVER_INGOT));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output ->
				output.insertAfter(Items.COPPER_HOE, SILVER_SHOVEL, SILVER_PICKAXE, SILVER_AXE, SILVER_HOE));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			output.insertAfter(Items.COPPER_SWORD, SILVER_SWORD);
			output.insertAfter(Items.COPPER_BOOTS, SILVER_HELMET, SILVER_CHESTPLATE, SILVER_LEGGINGS, SILVER_BOOTS);
		});
	}

	private static void registerBrewingTabs() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output ->
				output.insertAfter(Items.BREWING_STAND, SILVER_BREWING_STAND));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output ->
				output.insertAfter(Items.BLAZE_POWDER, LAPIS_GUNPOWDER_MIX, SILVER_REDSTONE_DUST, FLOWER_MIX, FERMENTED_ENDER_EYE));
	}

	private static Item blockItem(String name, Block block) {
		return register(name, p -> new BlockItem(block, p), new Item.Properties().useBlockDescriptionPrefix());
	}

	private static Item silverTool(String name, Item.Properties properties) {
		return register(name, Item::new, properties.component(SilverModule.UNDEAD_DAMAGE_BONUS, SilverGear.UNDEAD_DAMAGE_BONUS));
	}

	private static Item silverArmor(String name, ArmorType type) {
		return register(name, Item::new, new Item.Properties().humanoidArmor(SilverGear.ARMOR, type)
				.component(SilverModule.UNDEAD_THORNS, SilverGear.UNDEAD_THORNS_PER_PIECE));
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Eam.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}
}
