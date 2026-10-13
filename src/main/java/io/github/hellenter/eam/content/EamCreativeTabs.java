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
			EamItems.SLINGSHOT,
			EamItems.COPPER_BUCKET, EamItems.COPPER_WATER_BUCKET, EamItems.COPPER_LAVA_BUCKET, EamItems.COPPER_POWDER_SNOW_BUCKET,
			EamItems.COPPER_MILK_BUCKET,
			EamItems.SILVER_BUCKET, EamItems.SILVER_WATER_BUCKET, EamItems.SILVER_LAVA_BUCKET, EamItems.SILVER_POWDER_SNOW_BUCKET,
			EamItems.SILVER_MILK_BUCKET,
			EamItems.GOLDEN_BUCKET, EamItems.GOLDEN_WATER_BUCKET, EamItems.GOLDEN_LAVA_BUCKET, EamItems.GOLDEN_POWDER_SNOW_BUCKET,
			EamItems.GOLDEN_MILK_BUCKET,
			EamItems.MITHRIL_BUCKET, EamItems.MITHRIL_WATER_BUCKET, EamItems.MITHRIL_LAVA_BUCKET, EamItems.MITHRIL_POWDER_SNOW_BUCKET,
			EamItems.MITHRIL_MILK_BUCKET,
			EamItems.WATER_BOWL, EamItems.BOILED_WATER_BOWL, EamItems.MILK_BOWL, EamItems.BEEF_STEW, EamItems.CHICKEN_SOUP,
			EamItems.VEGETABLE_SOUP, EamItems.CREAM_OF_MUSHROOM_SOUP, EamItems.CREAM_OF_VEGETABLE_SOUP, EamItems.PUMPKIN_SOUP,
			EamItems.MASHED_POTATO, EamItems.SALAD, EamItems.ICE_CREAM, EamItems.SORBET, EamItems.PORRIDGE, EamItems.CEREAL,
			EamItems.CHOCOLATE, EamItems.CHEESE, EamItems.WOLF_MEAT, EamItems.COOKED_WOLF_MEAT,
			EamItems.BURNT_BEEF, EamItems.BURNT_PORKCHOP, EamItems.BURNT_MUTTON, EamItems.BURNT_CHICKEN, EamItems.BURNT_RABBIT,
			EamItems.BURNT_COD, EamItems.BURNT_SALMON, EamItems.BURNT_POTATO, EamItems.BURNT_WOLF_MEAT, EamItems.ASH,
			EamItems.DUCKWEED, EamItems.CATTAIL, EamItems.SWAMP_GRASS, EamItems.TALL_SWAMP_GRASS, EamItems.SWAMP_SHRUB, EamItems.DENSE_SHRUB, EamItems.BIG_SHRUB_STEM, EamItems.BIG_SHRUB_LEAVES, EamItems.CRANBERRY, EamItems.WOLFBERRY, EamItems.FIREWEED,
			EamItems.MARSH_MARIGOLD, EamItems.SHELF_MUSHROOM, EamItems.GARLIC, EamItems.WILD_GARLIC, EamItems.HOLLOW_LOGS,
			EamItems.CHANTERELLE_CAP, EamItems.CHANTERELLE_STEM, EamItems.CHAMPIGNON_CAP, EamItems.GLOWING_CHAMPIGNON_CAP,
			EamItems.VAMPIRE_HEART, EamItems.HEART_IN_JAR);

	public static final CreativeModeTab METALS = register("metals", () -> new ItemStack(EamItems.SILVER_INGOT),
			EamItems.SILVER_ORE, EamItems.DEEPSLATE_SILVER_ORE, EamItems.RAW_SILVER, EamItems.SILVER_NUGGET, EamItems.SILVER_INGOT,
			EamItems.SILVER_BLOCK,
			EamItems.HOT_IRON_INGOT, EamItems.IRON_PLATE,
			EamItems.ANCIENT_METAL_NUGGET, EamItems.ANCIENT_METAL_INGOT, EamItems.HOT_ANCIENT_METAL_INGOT, EamItems.ANCIENT_METAL_PLATE,
			EamItems.ANCIENT_METAL_BLOCK,
			EamItems.DEEP_BEDROCK,
			EamItems.MITHRIL_ORE, EamItems.DEEPSLATE_MITHRIL_ORE, EamItems.RAW_MITHRIL, EamItems.MITHRIL_NUGGET, EamItems.MITHRIL_INGOT,
			EamItems.HOT_MITHRIL_INGOT, EamItems.MITHRIL_PLATE, EamItems.MITHRIL_BLOCK,
			EamItems.COPPER_RING, EamItems.SILVER_RING, EamItems.IRON_RING, EamItems.ANCIENT_METAL_RING, EamItems.MITHRIL_RING);

	public static final CreativeModeTab TOOLS = register("tools", () -> new ItemStack(EamItems.MITHRIL_PICKAXE),
			EamItems.COPPER_KNIFE, EamItems.SILVER_KNIFE, EamItems.IRON_KNIFE, EamItems.ANCIENT_METAL_KNIFE, EamItems.MITHRIL_KNIFE,
			EamItems.COPPER_SAW, EamItems.SILVER_SAW, EamItems.IRON_SAW, EamItems.ANCIENT_METAL_SAW, EamItems.MITHRIL_SAW,
			EamItems.COPPER_HAMMER, EamItems.SILVER_HAMMER, EamItems.IRON_HAMMER, EamItems.ANCIENT_METAL_HAMMER, EamItems.MITHRIL_HAMMER,
			EamItems.SILVER_SHOVEL, EamItems.SILVER_PICKAXE, EamItems.SILVER_AXE, EamItems.SILVER_HOE,
			EamItems.ANCIENT_METAL_SHOVEL, EamItems.ANCIENT_METAL_PICKAXE, EamItems.ANCIENT_METAL_AXE, EamItems.ANCIENT_METAL_HOE,
			EamItems.MITHRIL_SHOVEL, EamItems.MITHRIL_PICKAXE, EamItems.MITHRIL_AXE, EamItems.MITHRIL_HOE,
			EamItems.RUSTY_IRON_SHOVEL, EamItems.RUSTY_IRON_PICKAXE, EamItems.RUSTY_IRON_AXE, EamItems.RUSTY_IRON_HOE);

	public static final CreativeModeTab COMBAT = register("combat", () -> new ItemStack(EamItems.SILVER_SWORD),
			EamItems.SILVER_SWORD, EamItems.ANCIENT_METAL_SWORD, EamItems.MITHRIL_SWORD,
			EamItems.COPPER_ARROW, EamItems.SILVER_ARROW, EamItems.IRON_ARROW, EamItems.ANCIENT_METAL_ARROW, EamItems.MITHRIL_ARROW,
			EamItems.COPPER_CHAINMAIL_HELMET, EamItems.COPPER_CHAINMAIL_CHESTPLATE, EamItems.COPPER_CHAINMAIL_LEGGINGS,
			EamItems.COPPER_CHAINMAIL_BOOTS,
			EamItems.SILVER_CHAINMAIL_HELMET, EamItems.SILVER_CHAINMAIL_CHESTPLATE, EamItems.SILVER_CHAINMAIL_LEGGINGS,
			EamItems.SILVER_CHAINMAIL_BOOTS,
			EamItems.SILVER_HELMET, EamItems.SILVER_CHESTPLATE, EamItems.SILVER_LEGGINGS, EamItems.SILVER_BOOTS,
			EamItems.ANCIENT_METAL_CHAINMAIL_HELMET, EamItems.ANCIENT_METAL_CHAINMAIL_CHESTPLATE, EamItems.ANCIENT_METAL_CHAINMAIL_LEGGINGS,
			EamItems.ANCIENT_METAL_CHAINMAIL_BOOTS,
			EamItems.ANCIENT_METAL_HELMET, EamItems.ANCIENT_METAL_CHESTPLATE, EamItems.ANCIENT_METAL_LEGGINGS, EamItems.ANCIENT_METAL_BOOTS,
			EamItems.MITHRIL_CHAINMAIL_HELMET, EamItems.MITHRIL_CHAINMAIL_CHESTPLATE, EamItems.MITHRIL_CHAINMAIL_LEGGINGS,
			EamItems.MITHRIL_CHAINMAIL_BOOTS,
			EamItems.MITHRIL_HELMET, EamItems.MITHRIL_CHESTPLATE, EamItems.MITHRIL_LEGGINGS, EamItems.MITHRIL_BOOTS,
			EamItems.RUSTY_IRON_SWORD,
			EamItems.RUSTY_CHAINMAIL_HELMET, EamItems.RUSTY_CHAINMAIL_CHESTPLATE, EamItems.RUSTY_CHAINMAIL_LEGGINGS,
			EamItems.RUSTY_CHAINMAIL_BOOTS,
			EamItems.RUSTY_IRON_HELMET, EamItems.RUSTY_IRON_CHESTPLATE, EamItems.RUSTY_IRON_LEGGINGS, EamItems.RUSTY_IRON_BOOTS,
			EamItems.BLACK_TALISMAN, EamItems.KING_CASTLE_MAP,
			EamItems.BEDROCK_BREAKER, EamItems.COMMANDER_HORN,
			EamItems.FALLEN_LORD_SPAWN_EGG, EamItems.SKELETON_KNIGHT_SPAWN_EGG, EamItems.LOST_SPAWN_EGG, EamItems.LOST_CENTURION_SPAWN_EGG, EamItems.LOST_CAVALRYMAN_SPAWN_EGG, EamItems.UNDEAD_WARLOCK_SPAWN_EGG,
			EamItems.PILLAGER_KING_SPAWN_EGG, EamItems.PILLAGER_GUARD_SPAWN_EGG, EamItems.ELITE_CROSSBOWMAN_SPAWN_EGG, EamItems.NET,
			EamItems.SWAMP_LURKER_SPAWN_EGG,
			EamItems.WIGHT_SPAWN_EGG, EamItems.DRAUGR_SPAWN_EGG, EamItems.WOOD_SPIDER_SPAWN_EGG, EamItems.BLACK_WIDOW_SPAWN_EGG,
			EamItems.VAMPIRE_BAT_SPAWN_EGG, EamItems.UPYR_SPAWN_EGG, EamItems.WARG_SPAWN_EGG, EamItems.WARG_LEADER_SPAWN_EGG,
			EamItems.BABA_YAGA_SPAWN_EGG, EamItems.DROWNED_SOLDIER_SPAWN_EGG);

	public static final CreativeModeTab WORKSHOP = register("workshop", () -> new ItemStack(EamItems.COPPER_ANVIL),
			EamItems.COPPER_ANVIL, EamItems.SILVER_ANVIL, EamItems.MITHRIL_ANVIL, EamItems.OBSIDIAN_BLAST_FURNACE,
			EamItems.SILVER_BREWING_STAND,
			EamItems.SILVER_BARS, EamItems.SILVER_DOOR, EamItems.SILVER_TRAPDOOR,
			EamItems.MITHRIL_BARS, EamItems.MITHRIL_DOOR, EamItems.MITHRIL_TRAPDOOR);

	public static final CreativeModeTab BREWING = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, key("brewing"),
			FabricCreativeModeTab.builder()
					.title(Component.translatable("itemGroup.eam.brewing"))
					.icon(() -> PotionContents.createItemStack(Items.POTION, BrewingModule.HOLY_WATER))
					.displayItems((parameters, output) -> {
						output.accept(EamItems.LAPIS_GUNPOWDER_MIX);
						output.accept(EamItems.SILVER_REDSTONE_DUST);
						output.accept(EamItems.FLOWER_MIX);
						output.accept(EamItems.FERMENTED_ENDER_EYE);
						output.accept(EamItems.GARLIC_ORCHID_MIX);
						output.accept(EamItems.HERBAL_HONEY);
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

	private static CreativeModeTab register(String name, Supplier<ItemStack> icon, Object... items) {
		return Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, key(name), FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.eam." + name))
				.icon(icon)
				.displayItems((parameters, output) -> {
					for (Object item : items) {
						// An entry is an item or a list of items, such as all hollow logs.
						if (item instanceof List<?> list) {
							list.forEach(each -> output.accept((ItemLike) each));
						} else {
							output.accept((ItemLike) item);
						}
					}
				})
				.build());
	}

	private static ResourceKey<CreativeModeTab> key(String name) {
		return ResourceKey.create(Registries.CREATIVE_MODE_TAB, Eam.id(name));
	}
}
