package io.github.hellenter.eam.content;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.brewing.BrewingModule;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** EAM's own creative tabs: every mod item lives here instead of the vanilla tabs. */
public final class EamCreativeTabs {
	private static final List<Item> POTION_CONTAINERS = List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION,
			Items.TIPPED_ARROW);

	public static final CreativeModeTab SURVIVAL = register("survival", () -> new ItemStack(EamItems.LEATHER_FLASK),
			EamItems.LEATHER_THREAD, EamItems.LEATHER_FLASK, EamItems.FILTERED_LEATHER_FLASK, EamItems.CAMP_FIRE, EamItems.SIEVE,
			EamItems.PEBBLE, EamItems.FLINT_SHARD, EamItems.FLINT_HATCHET, EamItems.FLINT_AXE, EamItems.FLINT_KNIFE,
			EamItems.SLINGSHOT);

	public static final CreativeModeTab METALS = register("metals", () -> new ItemStack(EamItems.SILVER_INGOT),
			EamItems.SILVER_ORE, EamItems.DEEPSLATE_SILVER_ORE, EamItems.RAW_SILVER, EamItems.SILVER_NUGGET, EamItems.SILVER_INGOT,
			EamItems.SILVER_BLOCK,
			EamItems.HOT_IRON_INGOT, EamItems.IRON_PLATE,
			EamItems.MITHRIL_ORE, EamItems.DEEPSLATE_MITHRIL_ORE, EamItems.RAW_MITHRIL, EamItems.MITHRIL_INGOT,
			EamItems.HOT_MITHRIL_INGOT, EamItems.MITHRIL_PLATE, EamItems.MITHRIL_BLOCK);

	public static final CreativeModeTab TOOLS = register("tools", () -> new ItemStack(EamItems.MITHRIL_PICKAXE),
			EamItems.COPPER_KNIFE, EamItems.SILVER_KNIFE, EamItems.IRON_KNIFE, EamItems.MITHRIL_KNIFE,
			EamItems.COPPER_SAW, EamItems.SILVER_SAW, EamItems.IRON_SAW, EamItems.MITHRIL_SAW,
			EamItems.COPPER_HAMMER, EamItems.SILVER_HAMMER, EamItems.IRON_HAMMER, EamItems.MITHRIL_HAMMER,
			EamItems.SILVER_SHOVEL, EamItems.SILVER_PICKAXE, EamItems.SILVER_AXE, EamItems.SILVER_HOE,
			EamItems.MITHRIL_SHOVEL, EamItems.MITHRIL_PICKAXE, EamItems.MITHRIL_AXE, EamItems.MITHRIL_HOE,
			EamItems.RUSTY_IRON_SHOVEL, EamItems.RUSTY_IRON_PICKAXE, EamItems.RUSTY_IRON_AXE, EamItems.RUSTY_IRON_HOE);

	public static final CreativeModeTab COMBAT = register("combat", () -> new ItemStack(EamItems.SILVER_SWORD),
			EamItems.SILVER_SWORD, EamItems.MITHRIL_SWORD,
			EamItems.SILVER_HELMET, EamItems.SILVER_CHESTPLATE, EamItems.SILVER_LEGGINGS, EamItems.SILVER_BOOTS,
			EamItems.MITHRIL_HELMET, EamItems.MITHRIL_CHESTPLATE, EamItems.MITHRIL_LEGGINGS, EamItems.MITHRIL_BOOTS,
			EamItems.RUSTY_IRON_SWORD,
			EamItems.RUSTY_CHAINMAIL_HELMET, EamItems.RUSTY_CHAINMAIL_CHESTPLATE, EamItems.RUSTY_CHAINMAIL_LEGGINGS,
			EamItems.RUSTY_CHAINMAIL_BOOTS,
			EamItems.RUSTY_IRON_HELMET, EamItems.RUSTY_IRON_CHESTPLATE, EamItems.RUSTY_IRON_LEGGINGS, EamItems.RUSTY_IRON_BOOTS);

	public static final CreativeModeTab WORKSHOP = register("workshop", () -> new ItemStack(EamItems.COPPER_ANVIL),
			EamItems.COPPER_ANVIL, EamItems.SILVER_ANVIL, EamItems.MITHRIL_ANVIL, EamItems.OBSIDIAN_BLAST_FURNACE,
			EamItems.SILVER_BREWING_STAND);

	public static final CreativeModeTab BREWING = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, key("brewing"),
			FabricCreativeModeTab.builder()
					.title(Component.translatable("itemGroup.eam.brewing"))
					.icon(() -> PotionContents.createItemStack(Items.POTION, BrewingModule.HOLY_WATER))
					.displayItems((parameters, output) -> {
						output.accept(EamItems.LAPIS_GUNPOWDER_MIX);
						output.accept(EamItems.SILVER_REDSTONE_DUST);
						output.accept(EamItems.FLOWER_MIX);
						output.accept(EamItems.FERMENTED_ENDER_EYE);
						output.acceptAll(potionStacks());
					})
					.build());

	private static final Set<CreativeModeTab> OWN_TABS = Set.of(SURVIVAL, METALS, TOOLS, COMBAT, WORKSHOP, BREWING);

	private EamCreativeTabs() {
	}

	/** Vanilla lists every registered potion in its own tabs; ours stay only in the brewing tab. */
	public static void register() {
		CreativeModeTabEvents.MODIFY_OUTPUT_ALL.register((tab, output) -> {
			if (OWN_TABS.contains(tab)) {
				return;
			}
			output.getDisplayStacks().removeIf(EamCreativeTabs::isEamPotion);
			output.getSearchTabStacks().removeIf(EamCreativeTabs::isEamPotion);
		});
	}

	private static boolean isEamPotion(ItemStack stack) {
		PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
		return contents != null && contents.potion()
				.flatMap(Holder::unwrapKey)
				.filter(key -> key.identifier().getNamespace().equals(Eam.MOD_ID))
				.isPresent();
	}

	private static List<ItemStack> potionStacks() {
		List<Holder<Potion>> potions = new ArrayList<>();
		potions.add(BrewingModule.HOLY_WATER);
		potions.addAll(BrewingModule.LIGHT_POTIONS);
		List<ItemStack> stacks = new ArrayList<>();
		for (Item container : POTION_CONTAINERS) {
			for (Holder<Potion> potion : potions) {
				stacks.add(PotionContents.createItemStack(container, potion));
			}
		}
		return stacks;
	}

	private static CreativeModeTab register(String name, Supplier<ItemStack> icon, ItemLike... items) {
		return Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, key(name), FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.eam." + name))
				.icon(icon)
				.displayItems((parameters, output) -> {
					for (ItemLike item : items) {
						output.accept(item);
					}
				})
				.build());
	}

	private static ResourceKey<CreativeModeTab> key(String name) {
		return ResourceKey.create(Registries.CREATIVE_MODE_TAB, Eam.id(name));
	}
}
