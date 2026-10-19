package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.brewing.BrewingModule;
import io.github.hellenter.eam.world.structures.VillageModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.advancements.triggers.KilledTrigger;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.predicates.DataComponentPredicates;
import net.minecraft.core.component.predicates.PotionsPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * The EAM advancement tab: it walks the player along the mod's progression, each description hinting at the next step.
 * {@code eam:iron_age} (full iron armor) is the one that unlocks normal villages, see {@link VillageModule}.
 */
public class EamAdvancementProvider extends FabricAdvancementProvider {
	public EamAdvancementProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	public void generateAdvancement(HolderLookup.Provider registries, Consumer<AdvancementHolder> out) {
		HolderGetter<Item> items = registries.lookupOrThrow(Registries.ITEM);

		AdvancementHolder root = Advancement.Builder.advancement()
				.rootDisplay(EamItems.FLINT_SHARD, title("root"), description("root"),
						Identifier.withDefaultNamespace("block/gravel"), AdvancementType.TASK, false, false, false)
				.addCriterion("tick", PlayerTrigger.TriggerInstance.tick())
				.build(Eam.id("root"));
		out.accept(root);

		// Stone age: what lies on the ground, flint tools and the crafting table.
		AdvancementHolder groundFinds = obtain(out, "ground_finds", root, EamItems.PEBBLE, EamItems.PEBBLE, EamItems.FLINT_SHARD);
		AdvancementHolder flintKnife = obtain(out, "flint_knife", groundFinds, EamItems.FLINT_KNIFE, EamItems.FLINT_KNIFE);
		AdvancementHolder flintHatchet = obtain(out, "flint_hatchet", flintKnife, EamItems.FLINT_HATCHET,
				EamItems.FLINT_HATCHET, EamItems.FLINT_AXE);
		obtain(out, "workbench", flintHatchet, Items.CRAFTING_TABLE, Items.CRAFTING_TABLE);
		obtain(out, "camp_fire", groundFinds, EamItems.CAMP_FIRE, EamItems.CAMP_FIRE);

		// Leather: water, the slingshot and the sieve that turns gravel into copper.
		AdvancementHolder leatherThread = obtain(out, "leather_thread", root, EamItems.LEATHER_THREAD, EamItems.LEATHER_THREAD);
		AdvancementHolder leatherFlask = obtain(out, "leather_flask", leatherThread, EamItems.LEATHER_FLASK, EamItems.LEATHER_FLASK);
		obtain(out, "filtered_flask", leatherFlask, EamItems.FILTERED_LEATHER_FLASK, EamItems.FILTERED_LEATHER_FLASK);
		obtain(out, "slingshot", leatherThread, EamItems.SLINGSHOT, EamItems.SLINGSHOT);
		AdvancementHolder sieve = obtain(out, "sieve", leatherThread, EamItems.SIEVE, EamItems.SIEVE);

		// Copper and silver.
		AdvancementHolder copperNugget = obtain(out, "copper_nugget", sieve, Items.COPPER_NUGGET, Items.COPPER_NUGGET);
		AdvancementHolder copperPickaxe = obtain(out, "copper_pickaxe", copperNugget, Items.COPPER_PICKAXE, Items.COPPER_PICKAXE);
		obtain(out, "metal_anvil", copperPickaxe, EamItems.COPPER_ANVIL,
				EamItems.COPPER_ANVIL, EamItems.SILVER_ANVIL, Items.ANVIL, EamItems.MITHRIL_ANVIL, EamItems.ADAMANTIUM_ANVIL);
		AdvancementHolder silver = obtain(out, "silver", copperPickaxe, EamItems.SILVER_INGOT, EamItems.SILVER_INGOT);
		AdvancementHolder holyWater = advancement("holy_water", silver, EamItems.SILVER_BREWING_STAND, AdvancementType.TASK)
				.addCriterion("holy_water", InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item()
						.withComponents(DataComponentMatchers.Builder.components()
								.partial(DataComponentPredicates.POTIONS, PotionsPredicate.ofPotion(BrewingModule.HOLY_WATER))
								.build())))
				.build(Eam.id("holy_water"));
		out.accept(holyWater);

		// Iron: a better knife, the blast furnace, hot ingots and plates.
		AdvancementHolder copperKnife = obtain(out, "copper_knife", copperPickaxe, EamItems.COPPER_KNIFE,
				EamItems.COPPER_KNIFE, EamItems.SILVER_KNIFE);
		AdvancementHolder blastFurnace = obtain(out, "blast_furnace", copperKnife, Items.BLAST_FURNACE, Items.BLAST_FURNACE);
		AdvancementHolder hotIron = obtain(out, "hot_iron", blastFurnace, EamItems.HOT_IRON_INGOT, EamItems.HOT_IRON_INGOT);
		AdvancementHolder ironPlate = obtain(out, "iron_plate", hotIron, EamItems.IRON_PLATE, EamItems.IRON_PLATE);
		AdvancementHolder ironAge = fullSet(out, VillageModule.UNLOCK_ADVANCEMENT.getPath(), ironPlate, AdvancementType.GOAL,
				Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);

		// Mithril and the crypt.
		AdvancementHolder obsidianFurnace = obtain(out, "obsidian_furnace", ironAge, EamItems.OBSIDIAN_BLAST_FURNACE,
				EamItems.OBSIDIAN_BLAST_FURNACE);
		AdvancementHolder mithril = obtain(out, "mithril", obsidianFurnace, EamItems.MITHRIL_INGOT, EamItems.MITHRIL_INGOT);
		fullSet(out, "mithril_armor", mithril, AdvancementType.CHALLENGE,
				EamItems.MITHRIL_HELMET, EamItems.MITHRIL_CHESTPLATE, EamItems.MITHRIL_LEGGINGS, EamItems.MITHRIL_BOOTS);
		obtain(out, "adamantium", mithril, EamItems.ADAMANTIUM_INGOT, EamItems.ADAMANTIUM_INGOT);
		out.accept(advancement("fallen_lord", ironAge, EamItems.MITHRIL_SWORD, AdvancementType.CHALLENGE)
				.addCriterion("fallen_lord", KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity()
						.of(registries.lookupOrThrow(Registries.ENTITY_TYPE), EamEntities.FALLEN_LORD)))
				.build(Eam.id("fallen_lord")));

		// Safe sleep works only in a closed, lit room, so any sleep at all is an achievement.
		out.accept(advancement("safe_sleep", root, Items.BED.red(), AdvancementType.TASK)
				.addCriterion("slept", PlayerTrigger.TriggerInstance.sleptInBed())
				.build(Eam.id("safe_sleep")));
	}

	/** Earned by holding any one of {@code items}. */
	private static AdvancementHolder obtain(Consumer<AdvancementHolder> out, String name, AdvancementHolder parent, ItemLike icon,
			ItemLike... items) {
		Advancement.Builder builder = advancement(name, parent, icon, AdvancementType.TASK);
		for (ItemLike item : items) {
			builder.addCriterion(key(item), InventoryChangeTrigger.TriggerInstance.hasItems(item));
		}
		AdvancementHolder holder = builder.requirements(AdvancementRequirements.Strategy.OR).build(Eam.id(name));
		out.accept(holder);
		return holder;
	}

	/** Earned once the player has held every piece of {@code items}, not necessarily at the same time. */
	private static AdvancementHolder fullSet(Consumer<AdvancementHolder> out, String name, AdvancementHolder parent,
			AdvancementType type, ItemLike... items) {
		Advancement.Builder builder = advancement(name, parent, items[1], type);
		for (ItemLike item : items) {
			builder.addCriterion(key(item), InventoryChangeTrigger.TriggerInstance.hasItems(item));
		}
		AdvancementHolder holder = builder.requirements(AdvancementRequirements.Strategy.AND).build(Eam.id(name));
		out.accept(holder);
		return holder;
	}

	private static Advancement.Builder advancement(String name, AdvancementHolder parent, ItemLike icon, AdvancementType type) {
		return Advancement.Builder.advancement()
				.parent(parent)
				.display(icon.asItem(), title(name), description(name), type, true, type != AdvancementType.TASK, false);
	}

	private static String key(ItemLike item) {
		return BuiltInRegistries.ITEM.getKey(item.asItem()).getPath();
	}

	private static Component title(String name) {
		return Component.translatable("advancements.eam." + name + ".title");
	}

	private static Component description(String name) {
		return Component.translatable("advancements.eam." + name + ".description");
	}
}
