package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.mobs.entity.duergar.DuergarProfession;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.TradeCost;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.item.trading.VillagerTrades;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * What each duergar profession ({@link DuergarProfession}) buys and sells, level by level, as a villager's trades: at
 * each level a duergar gets {@code amount} offers picked from that level's list. Jewels are their money: emeralds for
 * everyday goods, diamonds and gold for mithril. Prices stay within EAM's stack limits (gems 16, blocks and food 8).
 * Only the runecarver's master level sells Mending, which villagers no longer trade.
 */
public class EamDuergarTradeProvider extends FabricDynamicRegistryProvider {
	/** Trade experience per trade at each level, as a villager's. */
	private static final int[] XP = {0, 2, 5, 10, 15, 30};

	private record Trade(ResourceKey<VillagerTrade> key, Function<BootstrapContext<VillagerTrade>, VillagerTrade> make) {
	}

	private record Level(DuergarProfession profession, int level, int amount, List<Trade> trades) {
	}

	private static final List<Level> LEVELS = new ArrayList<>();

	static {
		DuergarProfession miner = DuergarProfession.MINER;
		level(miner, 1, 2,
				buy(miner, 1, "coal", Items.COAL, 15, 16),
				buy(miner, 1, "raw_copper", Items.RAW_COPPER, 12, 16),
				sell(miner, 1, "torches", 1, Items.TORCH, 8, 16));
		level(miner, 2, 2,
				sell(miner, 2, "mithril_nuggets", 3, EamItems.MITHRIL_NUGGET, 2, 8),
				swap(miner, 2, "gold_mithril_nugget", Items.GOLD_INGOT, 4, EamItems.MITHRIL_NUGGET, 1, 8),
				swap(miner, 2, "amethyst_mithril_nuggets", Items.AMETHYST_SHARD, 16, EamItems.MITHRIL_NUGGET, 2, 6),
				buy(miner, 2, "raw_iron", Items.RAW_IRON, 6, 12));
		level(miner, 3, 2,
				buy(miner, 3, "raw_silver", EamItems.RAW_SILVER, 6, 12),
				swap(miner, 3, "diamond_mithril_nuggets", Items.DIAMOND, 1, EamItems.MITHRIL_NUGGET, 3, 8),
				sell(miner, 3, "raw_iron", 4, Items.RAW_IRON, 3, 8));
		level(miner, 4, 2,
				swap(miner, 4, "diamond_raw_mithril", Items.DIAMOND, 3, EamItems.RAW_MITHRIL, 1, 4),
				sell(miner, 4, "raw_mithril", 10, EamItems.RAW_MITHRIL, 1, 4));
		level(miner, 5, 2,
				sell(miner, 5, "diamond", 14, Items.DIAMOND, 1, 3),
				sell(miner, 5, "obsidian", 6, Items.OBSIDIAN, 4, 4));

		DuergarProfession mason = DuergarProfession.MASON;
		level(mason, 1, 2,
				buy(mason, 1, "clay_ball", Items.CLAY_BALL, 10, 16),
				sell(mason, 1, "brick", 1, Items.BRICK, 8, 16));
		level(mason, 2, 2,
				buy(mason, 2, "stone", Items.STONE, 8, 16),
				buy(mason, 2, "cobbled_deepslate", Items.COBBLED_DEEPSLATE, 8, 16),
				sell(mason, 2, "chiseled_stone_bricks", 1, Items.CHISELED_STONE_BRICKS, 4, 16));
		level(mason, 3, 2,
				buy(mason, 3, "andesite", Items.ANDESITE, 8, 16),
				sell(mason, 3, "polished_deepslate", 1, Items.POLISHED_DEEPSLATE, 4, 16),
				sell(mason, 3, "deepslate_tiles", 1, Items.DEEPSLATE_TILES, 4, 16),
				sell(mason, 3, "polished_blackstone_bricks", 1, Items.POLISHED_BLACKSTONE_BRICKS, 4, 16));
		level(mason, 4, 2,
				buy(mason, 4, "quartz", Items.QUARTZ, 12, 12),
				sell(mason, 4, "tuff_bricks", 1, Items.TUFF_BRICKS, 4, 16),
				sell(mason, 4, "mud_bricks", 1, Items.MUD_BRICKS, 4, 16));
		level(mason, 5, 2,
				sell(mason, 5, "quartz_pillar", 1, Items.QUARTZ_PILLAR, 1, 12),
				sell(mason, 5, "chiseled_deepslate", 1, Items.CHISELED_DEEPSLATE, 2, 12));

		DuergarProfession grower = DuergarProfession.MUSHROOM_GROWER;
		level(grower, 1, 2,
				buy(grower, 1, "brown_mushroom", Items.BROWN_MUSHROOM, 8, 16),
				buy(grower, 1, "red_mushroom", Items.RED_MUSHROOM, 8, 16),
				sell(grower, 1, "mushroom_stew", 1, Items.MUSHROOM_STEW, 1, 12));
		level(grower, 2, 2,
				buy(grower, 2, "potato", Items.POTATO, 8, 16),
				sell(grower, 2, "baked_potato", 1, Items.BAKED_POTATO, 4, 12),
				sell(grower, 2, "glow_berries", 1, Items.GLOW_BERRIES, 6, 12));
		level(grower, 3, 2,
				buy(grower, 3, "glow_lichen", Items.GLOW_LICHEN, 8, 12),
				sell(grower, 3, "shroomlight", 1, Items.SHROOMLIGHT, 1, 12),
				sell(grower, 3, "rabbit_stew", 2, Items.RABBIT_STEW, 1, 8));
		level(grower, 4, 2,
				sell(grower, 4, "cooked_porkchop", 2, Items.COOKED_PORKCHOP, 4, 8),
				sell(grower, 4, "cheese", 2, EamItems.CHEESE, 2, 8),
				sell(grower, 4, "mycelium", 1, Items.MYCELIUM, 4, 8));
		level(grower, 5, 2,
				sell(grower, 5, "golden_carrot", 4, Items.GOLDEN_CARROT, 3, 8),
				sell(grower, 5, "golden_apple", 6, Items.GOLDEN_APPLE, 1, 2));

		DuergarProfession brewer = DuergarProfession.BREWER;
		level(brewer, 1, 2,
				buy(brewer, 1, "sugar", Items.SUGAR, 12, 16),
				sell(brewer, 1, "glass_bottle", 1, Items.GLASS_BOTTLE, 4, 16));
		level(brewer, 2, 2,
				sell(brewer, 2, "fermented_spider_eye", 1, Items.FERMENTED_SPIDER_EYE, 1, 12),
				sell(brewer, 2, "glowstone_dust", 1, Items.GLOWSTONE_DUST, 4, 12),
				sell(brewer, 2, "redstone", 1, Items.REDSTONE, 4, 12));
		level(brewer, 3, 2,
				sell(brewer, 3, "nether_wart", 3, Items.NETHER_WART, 4, 8),
				sell(brewer, 3, "blaze_powder", 4, Items.BLAZE_POWDER, 1, 8),
				sell(brewer, 3, "magma_cream", 2, Items.MAGMA_CREAM, 1, 8));
		level(brewer, 4, 2,
				sell(brewer, 4, "ghast_tear", 5, Items.GHAST_TEAR, 1, 4),
				sell(brewer, 4, "rabbit_foot", 4, Items.RABBIT_FOOT, 1, 4),
				sell(brewer, 4, "glistering_melon_slice", 2, Items.GLISTERING_MELON_SLICE, 1, 8));
		level(brewer, 5, 2,
				sell(brewer, 5, "herbal_honey", 6, EamItems.HERBAL_HONEY, 1, 4),
				sell(brewer, 5, "fermented_ender_eye", 5, EamItems.FERMENTED_ENDER_EYE, 1, 4));

		DuergarProfession runecarver = DuergarProfession.RUNECARVER;
		level(runecarver, 1, 2,
				buy(runecarver, 1, "paper", Items.PAPER, 24, 16),
				book(runecarver, 1));
		level(runecarver, 2, 2,
				buy(runecarver, 2, "book", Items.BOOK, 4, 12),
				sell(runecarver, 2, "lapis_lazuli", 1, Items.LAPIS_LAZULI, 2, 12),
				book(runecarver, 2));
		level(runecarver, 3, 2,
				book(runecarver, 3),
				sell(runecarver, 3, "experience_bottle", 3, Items.EXPERIENCE_BOTTLE, 1, 8),
				sell(runecarver, 3, "bookshelf", 5, Items.BOOKSHELF, 1, 8));
		level(runecarver, 4, 2,
				book(runecarver, 4),
				sell(runecarver, 4, "experience_bottles", 5, Items.EXPERIENCE_BOTTLE, 2, 8));
		level(runecarver, 5, 2,
				mending(runecarver, 5),
				book(runecarver, 5));

		DuergarProfession armorer = DuergarProfession.ARMORER;
		level(armorer, 1, 2,
				buy(armorer, 1, "coal", Items.COAL, 15, 16),
				sell(armorer, 1, "copper_helmet", 3, Items.COPPER_HELMET, 1, 8),
				sell(armorer, 1, "copper_boots", 3, Items.COPPER_BOOTS, 1, 8));
		level(armorer, 2, 2,
				buy(armorer, 2, "iron_ingot", Items.IRON_INGOT, 4, 12),
				sell(armorer, 2, "silver_helmet", 5, EamItems.SILVER_HELMET, 1, 6),
				sell(armorer, 2, "silver_boots", 4, EamItems.SILVER_BOOTS, 1, 6));
		level(armorer, 3, 2,
				sell(armorer, 3, "silver_chestplate", 8, EamItems.SILVER_CHESTPLATE, 1, 4),
				sell(armorer, 3, "silver_leggings", 7, EamItems.SILVER_LEGGINGS, 1, 4),
				sell(armorer, 3, "iron_helmet", 9, Items.IRON_HELMET, 1, 4),
				sell(armorer, 3, "chainmail_chestplate", 8, Items.CHAINMAIL_CHESTPLATE, 1, 4));
		level(armorer, 4, 2,
				swap(armorer, 4, "diamond_mithril_plate", Items.DIAMOND, 6, EamItems.MITHRIL_PLATE, 1, 2),
				sell(armorer, 4, "iron_chestplate", 14, Items.IRON_CHESTPLATE, 1, 3),
				sell(armorer, 4, "iron_leggings", 12, Items.IRON_LEGGINGS, 1, 3));
		level(armorer, 5, 2,
				mithril(armorer, 5, "mithril_helmet", 10, EamItems.MITHRIL_HELMET),
				mithril(armorer, 5, "mithril_boots", 8, EamItems.MITHRIL_BOOTS));

		DuergarProfession weaponsmith = DuergarProfession.WEAPONSMITH;
		level(weaponsmith, 1, 2,
				buy(weaponsmith, 1, "coal", Items.COAL, 15, 16),
				sell(weaponsmith, 1, "copper_sword", 3, Items.COPPER_SWORD, 1, 8),
				sell(weaponsmith, 1, "copper_knife", 2, EamItems.COPPER_KNIFE, 1, 8));
		level(weaponsmith, 2, 2,
				buy(weaponsmith, 2, "iron_ingot", Items.IRON_INGOT, 4, 12),
				sell(weaponsmith, 2, "silver_sword", 5, EamItems.SILVER_SWORD, 1, 6),
				sell(weaponsmith, 2, "silver_knife", 4, EamItems.SILVER_KNIFE, 1, 6));
		level(weaponsmith, 3, 2,
				buy(weaponsmith, 3, "flint", Items.FLINT, 24, 12),
				sell(weaponsmith, 3, "iron_sword", 9, Items.IRON_SWORD, 1, 4),
				sell(weaponsmith, 3, "iron_knife", 7, EamItems.IRON_KNIFE, 1, 4));
		level(weaponsmith, 4, 2,
				swap(weaponsmith, 4, "diamond_mithril_ingot", Items.DIAMOND, 4, EamItems.MITHRIL_INGOT, 1, 4),
				sell(weaponsmith, 4, "mithril_ingot", 12, EamItems.MITHRIL_INGOT, 1, 4),
				sell(weaponsmith, 4, "duergar_bombs", 3, EamItems.DUERGAR_BOMB, 2, 6));
		level(weaponsmith, 5, 2,
				mithril(weaponsmith, 5, "mithril_sword", 10, EamItems.MITHRIL_SWORD),
				mithril(weaponsmith, 5, "mithril_knife", 8, EamItems.MITHRIL_KNIFE));

		DuergarProfession toolsmith = DuergarProfession.TOOLSMITH;
		level(toolsmith, 1, 2,
				buy(toolsmith, 1, "coal", Items.COAL, 15, 16),
				sell(toolsmith, 1, "copper_pickaxe", 2, Items.COPPER_PICKAXE, 1, 8),
				sell(toolsmith, 1, "copper_shovel", 2, Items.COPPER_SHOVEL, 1, 8));
		level(toolsmith, 2, 2,
				buy(toolsmith, 2, "iron_ingot", Items.IRON_INGOT, 4, 12),
				sell(toolsmith, 2, "silver_pickaxe", 5, EamItems.SILVER_PICKAXE, 1, 6),
				sell(toolsmith, 2, "silver_saw", 4, EamItems.SILVER_SAW, 1, 6),
				sell(toolsmith, 2, "silver_hammer", 4, EamItems.SILVER_HAMMER, 1, 6));
		level(toolsmith, 3, 2,
				buy(toolsmith, 3, "flint", Items.FLINT, 24, 12),
				sell(toolsmith, 3, "iron_pickaxe", 9, Items.IRON_PICKAXE, 1, 4),
				sell(toolsmith, 3, "iron_saw", 7, EamItems.IRON_SAW, 1, 4));
		level(toolsmith, 4, 2,
				swap(toolsmith, 4, "gold_mithril_ingot", Items.GOLD_INGOT, 16, EamItems.MITHRIL_INGOT, 1, 3),
				sell(toolsmith, 4, "iron_hammer", 10, EamItems.IRON_HAMMER, 1, 3),
				sell(toolsmith, 4, "iron_axe", 9, Items.IRON_AXE, 1, 3));
		level(toolsmith, 5, 2,
				mithril(toolsmith, 5, "mithril_pickaxe", 10, EamItems.MITHRIL_PICKAXE),
				mithril(toolsmith, 5, "mithril_axe", 8, EamItems.MITHRIL_AXE));

		// The elder: the clan's map of the bosses' lairs, and mithril by the handful for a block of gems. All at once.
		DuergarProfession elder = DuergarProfession.ELDER;
		level(elder, 1, 3,
				new Trade(key(elder, 1, "lair_map"), context -> VillagerTrade.builder(new TradeCost(Items.EMERALD, 8),
						new TradeCost(Items.COMPASS, 1), new ItemStackTemplate(EamItems.LAIR_MAP), 2, 5, 0.05F).build()),
				swap(elder, 1, "diamond_block_mithril_ingots", Items.DIAMOND_BLOCK, 1, EamItems.MITHRIL_INGOT, 3, 2),
				swap(elder, 1, "emerald_block_mithril_ingots", Items.EMERALD_BLOCK, 2, EamItems.MITHRIL_INGOT, 2, 2));
	}

	public EamDuergarTradeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	private static void level(DuergarProfession profession, int level, int amount, Trade... trades) {
		LEVELS.add(new Level(profession, level, amount, List.of(trades)));
	}

	private static ResourceKey<VillagerTrade> key(DuergarProfession profession, int level, String name) {
		return ResourceKey.create(Registries.VILLAGER_TRADE, Eam.id("duergar/" + profession.getSerializedName() + "/" + level + "/" + name));
	}

	/** He gives {@code count} of {@code gives} for {@code wantsCount} of {@code wants}. */
	private static Trade swap(DuergarProfession profession, int level, String name, ItemLike wants, int wantsCount, Item gives, int count,
			int maxUses) {
		return new Trade(key(profession, level, name), context -> VillagerTrade.builder(new TradeCost(wants, wantsCount),
				new ItemStackTemplate(gives, count), maxUses, XP[level], 0.05F).build());
	}

	/** He buys {@code count} of {@code wants} for an emerald. */
	private static Trade buy(DuergarProfession profession, int level, String name, ItemLike wants, int count, int maxUses) {
		return swap(profession, level, name + "_emerald", wants, count, Items.EMERALD, 1, maxUses);
	}

	/** He sells {@code count} of {@code gives} for {@code emeralds}. */
	private static Trade sell(DuergarProfession profession, int level, String name, int emeralds, Item gives, int count, int maxUses) {
		return swap(profession, level, "emerald_" + name, Items.EMERALD, emeralds, gives, count, maxUses);
	}

	/** Master mithril gear: a full stack of emeralds and {@code diamonds} on top. */
	private static Trade mithril(DuergarProfession profession, int level, String name, int diamonds, Item gives) {
		return new Trade(key(profession, level, "emerald_diamond_" + name), context -> VillagerTrade.builder(new TradeCost(Items.EMERALD, 16),
				new TradeCost(Items.DIAMOND, diamonds), new ItemStackTemplate(gives), 1, XP[level], 0.05F).build());
	}

	/** A book with a random tradeable enchantment for a book and emeralds, priced as a librarian's. */
	private static Trade book(DuergarProfession profession, int level) {
		return new Trade(key(profession, level, "emerald_and_book_enchanted_book"), context -> {
			HolderGetter<Enchantment> enchantments = context.lookup(Registries.ENCHANTMENT);
			return VillagerTrade.builder(new TradeCost(Items.EMERALD, 0), new TradeCost(Items.BOOK, 1),
							new ItemStackTemplate(Items.ENCHANTED_BOOK), 4, XP[level], 0.2F)
					.addModifiers(VillagerTrades.enchantedBook(context.lookup(Registries.ITEM), enchantments.getOrThrow(EnchantmentTags.TRADEABLE)))
					.doubleTradePriceEnchantments(enchantments.getOrThrow(EnchantmentTags.DOUBLE_TRADE_PRICE))
					.build();
		});
	}

	/** The one Mending book anyone trades: three emerald blocks and a book. */
	private static Trade mending(DuergarProfession profession, int level) {
		return new Trade(key(profession, level, "emerald_block_and_book_mending_book"), context -> {
			Holder<Enchantment> mending = context.lookup(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING);
			return VillagerTrade.builder(new TradeCost(Items.EMERALD_BLOCK, 3), new TradeCost(Items.BOOK, 1),
							new ItemStackTemplate(Items.ENCHANTED_BOOK), 2, XP[level], 0.2F)
					.addModifiers(VillagerTrades.enchantedBook(context.lookup(Registries.ITEM), mending, 1))
					.build();
		});
	}

	public static void bootstrapTrades(BootstrapContext<VillagerTrade> context) {
		for (Level level : LEVELS) {
			for (Trade trade : level.trades()) {
				context.register(trade.key(), trade.make().apply(context));
			}
		}
	}

	public static void bootstrapTradeSets(BootstrapContext<TradeSet> context) {
		HolderGetter<VillagerTrade> trades = context.lookup(Registries.VILLAGER_TRADE);
		for (Level level : LEVELS) {
			List<Holder<VillagerTrade>> holders = new ArrayList<>();
			for (Trade trade : level.trades()) {
				holders.add(trades.getOrThrow(trade.key()));
			}
			ResourceKey<TradeSet> key = level.profession().trades(level.level());
			context.register(key, new TradeSet(HolderSet.direct(holders), ContextIntProviders.exactly(level.amount()), false,
					Optional.of(key.identifier().withPrefix("trade_set/"))));
		}
	}

	@Override
	protected void configure(HolderLookup.Provider registries, Entries entries) {
		for (Level level : LEVELS) {
			for (Trade trade : level.trades()) {
				entries.add(registries.lookupOrThrow(Registries.VILLAGER_TRADE), trade.key());
			}
			entries.add(registries.lookupOrThrow(Registries.TRADE_SET), level.profession().trades(level.level()));
		}
	}

	@Override
	public String getName() {
		return "EAM duergar trades";
	}
}
