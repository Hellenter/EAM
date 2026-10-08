package io.github.hellenter.eam.content;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.ground.PebbleItem;
import io.github.hellenter.eam.content.slingshot.SlingshotItem;
import io.github.hellenter.eam.content.tools.FlintTools;
import io.github.hellenter.eam.content.tools.KnifeItem;
import io.github.hellenter.eam.survival.thirst.FlaskItem;
import io.github.hellenter.eam.survival.thirst.FlaskLogic;
import io.github.hellenter.eam.survival.thirst.FlaskWater;
import io.github.hellenter.eam.survival.thirst.ThirstModule;

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

	/** Drawn like a bow, shoots pebbles. */
	public static final Item SLINGSHOT = register("slingshot", SlingshotItem::new, new Item.Properties().durability(128));

	/** Thread for flasks and slingshots: leather thread or plain string. */
	public static final TagKey<Item> FLASK_THREADS = TagKey.create(Registries.ITEM, Eam.id("flask_threads"));

	private EamItems() {
	}

	public static void register() {
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
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output ->
				output.insertBefore(Items.WOODEN_SHOVEL, FLINT_HATCHET, FLINT_AXE));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output ->
				output.insertBefore(Items.WOODEN_SWORD, FLINT_KNIFE));
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Eam.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}
}
