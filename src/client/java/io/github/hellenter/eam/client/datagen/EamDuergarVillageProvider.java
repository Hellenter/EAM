package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.mobs.entity.duergar.DuergarVillager;
import io.github.hellenter.eam.world.structures.village.DuergarVillage;
import io.github.hellenter.eam.world.structures.village.DuergarVillage.Style;
import io.github.hellenter.eam.world.structures.village.DuergarVillageStructure;

import com.mojang.datafixers.util.Pair;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.TradeCost;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProtectedBlockProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Duergar villages ({@link DuergarVillage}): one jigsaw structure per style, each in its own biome, all in one structure
 * set so two villages never stand close. A village stands on a wide cave floor between y 58 and 150, each piece set down
 * onto the floor under it ({@link DuergarVillageStructure}); its pieces clear only the room they need and the ground
 * under them is filled in gently (beard_thin), so it follows its cave. No monsters spawn inside.
 * Also the duergar trades: what each role buys and sells.
 */
public class EamDuergarVillageProvider extends FabricDynamicRegistryProvider {
	private static final String TEMPLATES = "eam:duergar_village/";
	public static final ResourceKey<StructureProcessorList> PROCESSORS =
			ResourceKey.create(Registries.PROCESSOR_LIST, Eam.id("duergar_village"));

	public EamDuergarVillageProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	/** Bedrock and the like stay whole where a village hall reaches them. */
	public static void bootstrapProcessors(BootstrapContext<StructureProcessorList> context) {
		HolderGetter<net.minecraft.world.level.block.Block> blocks = context.lookup(Registries.BLOCK);
		context.register(PROCESSORS, new StructureProcessorList(List.of(
				new ProtectedBlockProcessor(blocks.getOrThrow(BlockTags.FEATURES_CANNOT_REPLACE)))));
	}

	public static void bootstrapPools(BootstrapContext<StructureTemplatePool> context) {
		HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
		Holder<StructureProcessorList> processors = context.lookup(Registries.PROCESSOR_LIST).getOrThrow(PROCESSORS);
		Holder<StructureTemplatePool> empty = pools.getOrThrow(Pools.EMPTY);
		for (Style style : Style.values()) {
			String dir = style.id() + "/";
			context.register(style.pool("start"), new StructureTemplatePool(empty, List.of(
					piece(dir + "plaza", processors, 1)), StructureTemplatePool.Projection.RIGID));
			// Straight streets, turns, forks, ramps up or down and bridges, so a village follows its cave.
			context.register(style.pool("streets"), new StructureTemplatePool(pools.getOrThrow(style.pool("ends")), List.of(
					piece(dir + "street/1", processors, 5),
					piece(dir + "street/2", processors, 2),
					piece(dir + "street/3", processors, 2),
					piece(dir + "street/4", processors, 3),
					piece(dir + "street/5", processors, 3),
					piece(dir + "ramp", processors, 3),
					piece(dir + "corner", processors, 3),
					piece(dir + "fork", processors, 2),
					piece(dir + "crossing", processors, 1),
					piece(dir + "bridge", processors, 1)), StructureTemplatePool.Projection.RIGID));
			context.register(style.pool("ends"), new StructureTemplatePool(empty, List.of(
					piece(dir + "end/1", processors, 2),
					piece(dir + "end/2", processors, 1)), StructureTemplatePool.Projection.RIGID));
			// Ten kinds of building and a yard; small homes most often, one drinking hall at most now and then.
			context.register(style.pool("houses"), new StructureTemplatePool(empty, List.of(
					piece(dir + "house/1", processors, 4),
					piece(dir + "house/2", processors, 4),
					piece(dir + "house/3", processors, 3),
					piece(dir + "house/4", processors, 2),
					piece(dir + "house/5", processors, 2),
					piece(dir + "house/6", processors, 1),
					piece(dir + "house/7", processors, 2),
					piece(dir + "house/8", processors, 2),
					piece(dir + "house/9", processors, 2),
					piece(dir + "house/10", processors, 1),
					piece(dir + "house/yard", processors, 2)), StructureTemplatePool.Projection.RIGID));
		}
	}

	/** Lit halls with their own folk in them: no monster spawns inside a village. */
	public static void bootstrap(BootstrapContext<Structure> context) {
		Map<MobCategory, StructureSpawnOverride> spawns = Map.of(MobCategory.MONSTER, new StructureSpawnOverride(
				StructureSpawnOverride.BoundingBoxType.STRUCTURE, WeightedList.of()));
		for (Style style : Style.values()) {
			context.register(style.structure(), new DuergarVillageStructure(
					new Structure.StructureSettings(context.lookup(Registries.BIOME).getOrThrow(style.biomes()), spawns,
							GenerationStep.Decoration.UNDERGROUND_STRUCTURES, TerrainAdjustment.BEARD_THIN),
					context.lookup(Registries.TEMPLATE_POOL).getOrThrow(style.pool("start")), 6, 58, 150, 56, 1, 14, 0.15F));
		}
	}

	public static void bootstrapSets(BootstrapContext<StructureSet> context) {
		HolderGetter<Structure> structures = context.lookup(Registries.STRUCTURE);
		List<StructureSet.StructureSelectionEntry> entries = new ArrayList<>();
		for (Style style : Style.values()) {
			entries.add(StructureSet.entry(structures.getOrThrow(style.structure())));
		}
		context.register(DuergarVillage.STRUCTURE_SET, new StructureSet(entries,
				new RandomSpreadStructurePlacement(32, 12, RandomSpreadType.LINEAR, 20261014)));
	}

	private static Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer> piece(
			String name, Holder<StructureProcessorList> processors, int weight) {
		return Pair.of(StructurePoolElement.single(TEMPLATES + name, processors, LiquidSettings.IGNORE_WATERLOGGING), weight);
	}

	// ---- Trades: mithril for jewels ----

	private static ResourceKey<VillagerTrade> trade(String name) {
		return ResourceKey.create(Registries.VILLAGER_TRADE, Eam.id("duergar/" + name));
	}

	private static final ResourceKey<VillagerTrade> SMITH_DIAMOND_INGOT = trade("smith/diamond_mithril_ingot");
	private static final ResourceKey<VillagerTrade> SMITH_EMERALD_INGOT = trade("smith/emerald_mithril_ingot");
	private static final ResourceKey<VillagerTrade> SMITH_GOLD_INGOT = trade("smith/gold_mithril_ingot");
	private static final ResourceKey<VillagerTrade> SMITH_DIAMOND_PLATE = trade("smith/diamond_mithril_plate");
	private static final ResourceKey<VillagerTrade> MINER_DIAMOND_NUGGETS = trade("miner/diamond_mithril_nuggets");
	private static final ResourceKey<VillagerTrade> MINER_EMERALD_NUGGETS = trade("miner/emerald_mithril_nuggets");
	private static final ResourceKey<VillagerTrade> MINER_GOLD_NUGGET = trade("miner/gold_mithril_nugget");
	private static final ResourceKey<VillagerTrade> MINER_AMETHYST_NUGGETS = trade("miner/amethyst_mithril_nuggets");
	private static final ResourceKey<VillagerTrade> MINER_DIAMOND_RAW = trade("miner/diamond_raw_mithril");
	private static final ResourceKey<VillagerTrade> ELDER_LAIR_MAP = trade("elder/lair_map");
	private static final ResourceKey<VillagerTrade> ELDER_DIAMOND_BLOCK_INGOTS = trade("elder/diamond_block_mithril_ingots");
	private static final ResourceKey<VillagerTrade> ELDER_EMERALD_BLOCK_INGOTS = trade("elder/emerald_block_mithril_ingots");

	private static final List<ResourceKey<VillagerTrade>> TRADES = List.of(SMITH_DIAMOND_INGOT, SMITH_EMERALD_INGOT, SMITH_GOLD_INGOT,
			SMITH_DIAMOND_PLATE, MINER_DIAMOND_NUGGETS, MINER_EMERALD_NUGGETS, MINER_GOLD_NUGGET, MINER_AMETHYST_NUGGETS, MINER_DIAMOND_RAW,
			ELDER_LAIR_MAP, ELDER_DIAMOND_BLOCK_INGOTS, ELDER_EMERALD_BLOCK_INGOTS);

	public static void bootstrapTrades(BootstrapContext<VillagerTrade> context) {
		// The smith sells ingots and plates, a few a day.
		register(context, SMITH_DIAMOND_INGOT, Items.DIAMOND, 4, EamItems.MITHRIL_INGOT, 1, 4);
		register(context, SMITH_EMERALD_INGOT, Items.EMERALD, 12, EamItems.MITHRIL_INGOT, 1, 4);
		register(context, SMITH_GOLD_INGOT, Items.GOLD_INGOT, 16, EamItems.MITHRIL_INGOT, 1, 3);
		register(context, SMITH_DIAMOND_PLATE, Items.DIAMOND, 6, EamItems.MITHRIL_PLATE, 1, 2);
		// Miners sell what they dig: nuggets and raw mithril, for gems and gold.
		register(context, MINER_DIAMOND_NUGGETS, Items.DIAMOND, 1, EamItems.MITHRIL_NUGGET, 3, 8);
		register(context, MINER_EMERALD_NUGGETS, Items.EMERALD, 3, EamItems.MITHRIL_NUGGET, 2, 8);
		register(context, MINER_GOLD_NUGGET, Items.GOLD_INGOT, 4, EamItems.MITHRIL_NUGGET, 1, 8);
		register(context, MINER_AMETHYST_NUGGETS, Items.AMETHYST_SHARD, 16, EamItems.MITHRIL_NUGGET, 2, 6);
		register(context, MINER_DIAMOND_RAW, Items.DIAMOND, 3, EamItems.RAW_MITHRIL, 1, 4);
		// The elder: the clan's map of the bosses' lairs, and mithril by the handful for a block of gems.
		context.register(ELDER_LAIR_MAP, VillagerTrade.builder(new TradeCost(Items.EMERALD, 8), new TradeCost(Items.COMPASS, 1),
				new ItemStackTemplate(EamItems.LAIR_MAP), 2, 5, 0.05F).build());
		register(context, ELDER_DIAMOND_BLOCK_INGOTS, Items.DIAMOND_BLOCK, 1, EamItems.MITHRIL_INGOT, 3, 2);
		register(context, ELDER_EMERALD_BLOCK_INGOTS, Items.EMERALD_BLOCK, 2, EamItems.MITHRIL_INGOT, 2, 2);
	}

	private static void register(BootstrapContext<VillagerTrade> context, ResourceKey<VillagerTrade> key, Item wants, int count, Item gives,
			int givesCount, int maxUses) {
		context.register(key, VillagerTrade.builder(new TradeCost(wants, count), new ItemStackTemplate(gives, givesCount), maxUses, 2, 0.05F)
				.build());
	}

	/** Each role offers these; the smith and the elder all of theirs, a miner four of his five. */
	public static void bootstrapTradeSets(BootstrapContext<TradeSet> context) {
		HolderGetter<VillagerTrade> trades = context.lookup(Registries.VILLAGER_TRADE);
		tradeSet(context, DuergarVillager.Role.SMITH, 4, trades, SMITH_DIAMOND_INGOT, SMITH_EMERALD_INGOT, SMITH_GOLD_INGOT, SMITH_DIAMOND_PLATE);
		tradeSet(context, DuergarVillager.Role.MINER, 4, trades, MINER_DIAMOND_NUGGETS, MINER_EMERALD_NUGGETS, MINER_GOLD_NUGGET,
				MINER_AMETHYST_NUGGETS, MINER_DIAMOND_RAW);
		tradeSet(context, DuergarVillager.Role.ELDER, 3, trades, ELDER_LAIR_MAP, ELDER_DIAMOND_BLOCK_INGOTS, ELDER_EMERALD_BLOCK_INGOTS);
	}

	@SafeVarargs
	private static void tradeSet(BootstrapContext<TradeSet> context, DuergarVillager.Role role, int amount, HolderGetter<VillagerTrade> trades,
			ResourceKey<VillagerTrade>... keys) {
		List<Holder<VillagerTrade>> holders = new ArrayList<>();
		for (ResourceKey<VillagerTrade> key : keys) {
			holders.add(trades.getOrThrow(key));
		}
		context.register(role.trades(), new TradeSet(HolderSet.direct(holders), ContextIntProviders.exactly(amount), false,
				Optional.of(role.trades().identifier().withPrefix("trade_set/"))));
	}

	@Override
	protected void configure(HolderLookup.Provider registries, Entries entries) {
		for (ResourceKey<VillagerTrade> trade : TRADES) {
			entries.add(registries.lookupOrThrow(Registries.VILLAGER_TRADE), trade);
		}
		for (DuergarVillager.Role role : DuergarVillager.Role.values()) {
			entries.add(registries.lookupOrThrow(Registries.TRADE_SET), role.trades());
		}
		entries.add(registries.lookupOrThrow(Registries.PROCESSOR_LIST), PROCESSORS);
		for (Style style : Style.values()) {
			for (String kind : List.of("start", "streets", "ends", "houses")) {
				entries.add(registries.lookupOrThrow(Registries.TEMPLATE_POOL), style.pool(kind));
			}
			entries.add(registries.lookupOrThrow(Registries.STRUCTURE), style.structure());
		}
		entries.add(registries.lookupOrThrow(Registries.STRUCTURE_SET), DuergarVillage.STRUCTURE_SET);
	}

	@Override
	public String getName() {
		return "EAM duergar villages";
	}
}
