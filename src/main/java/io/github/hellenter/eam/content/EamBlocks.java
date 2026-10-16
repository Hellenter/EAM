package io.github.hellenter.eam.content;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.anvil.MetalAnvilBlock;
import io.github.hellenter.eam.content.brewing.SilverBrewingStandBlock;
import io.github.hellenter.eam.content.campfire.CampFireBlock;
import io.github.hellenter.eam.content.torch.UnlitTorchBlock;
import io.github.hellenter.eam.content.torch.UnlitWallTorchBlock;
import io.github.hellenter.eam.content.feeder.FeederBlock;
import io.github.hellenter.eam.content.fridge.FridgeBlock;
import io.github.hellenter.eam.content.fridge.FridgeMenu;
import io.github.hellenter.eam.content.furnace.ObsidianBlastFurnaceBlock;
import io.github.hellenter.eam.content.garlic.GarlicCropBlock;
import io.github.hellenter.eam.content.ground.GroundItemBlock;
import io.github.hellenter.eam.content.ground.GroundItemLogic;
import io.github.hellenter.eam.content.metal.MetalBarsBlock;
import io.github.hellenter.eam.content.metal.MetalDoorBlock;
import io.github.hellenter.eam.content.metal.MetalTrapDoorBlock;
import io.github.hellenter.eam.content.sieve.SieveBlock;
import io.github.hellenter.eam.content.swamp.CattailBlock;
import io.github.hellenter.eam.content.swamp.BigShrubLeavesBlock;
import io.github.hellenter.eam.content.swamp.BigShrubStemBlock;
import io.github.hellenter.eam.content.swamp.DenseShrubBlock;
import io.github.hellenter.eam.content.swamp.DuckweedBlock;
import io.github.hellenter.eam.content.swamp.HollowLogBlock;
import io.github.hellenter.eam.content.swamp.ShelfMushroomBlock;
import io.github.hellenter.eam.content.swamp.SwampGrassBlock;
import io.github.hellenter.eam.content.swamp.SwampShrubBlock;
import io.github.hellenter.eam.content.underworld.GlowshroomVinesBlock;
import io.github.hellenter.eam.content.underworld.GlowshroomVinesPlantBlock;
import io.github.hellenter.eam.dimension.DeepBedrockBlock;
import io.github.hellenter.eam.mobs.entity.crossbowman.NetBlock;

import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TallFlowerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/** Blocks added by EAM. Their items are in {@link EamItems}. Registered whatever the config says. */
public final class EamBlocks {
	/** A pebble lying on the ground, a bit smaller than a button. */
	public static final Block PEBBLE = register("pebble", p -> new GroundItemBlock(GroundItemLogic.small(3, 2, 3), p), groundItem(MapColor.STONE));

	/** A flint shard lying on the ground. */
	public static final Block FLINT_SHARD = register("flint_shard", p -> new GroundItemBlock(GroundItemLogic.small(3, 1, 2), p), groundItem(MapColor.COLOR_BLACK));

	/** A stick as long as the block, lying under trees. Has no item of its own: it drops a vanilla stick. */
	public static final Block GROUND_STICK = register("ground_stick", p -> new GroundItemBlock(GroundItemLogic.stick(2, 1), p),
			groundItem(MapColor.WOOD).sound(SoundType.WOOD));

	/** A fire pit fed with sticks; see {@link CampFireBlock}. Drops come from its block entity, not a loot table. */
	public static final Block CAMP_FIRE = register("camp_fire", CampFireBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.PODZOL)
			.strength(1.0F)
			.sound(SoundType.WOOD)
			.lightLevel(state -> state.getValue(CampFireBlock.LIT) ? 15 : 0)
			.noOcclusion()
			.noLootTable()
			.pushReaction(PushReaction.POPPED));

	/** A torch the rain has put out; see {@link UnlitTorchBlock}. */
	public static final Block UNLIT_TORCH = register("unlit_torch", UnlitTorchBlock::new, BlockBehaviour.Properties.of()
			.noCollision().instabreak().sound(SoundType.WOOD).pushReaction(PushReaction.POPPED));

	/** The wall variant of {@link #UNLIT_TORCH}; drops and names itself as the standing one, like vanilla's wall torch. */
	public static final Block UNLIT_WALL_TORCH = register("unlit_wall_torch", UnlitWallTorchBlock::new, BlockBehaviour.Properties.of()
			.overrideLootTable(UNLIT_TORCH.getLootTable()).overrideDescription(UNLIT_TORCH.getDescriptionId())
			.noCollision().instabreak().sound(SoundType.WOOD).pushReaction(PushReaction.POPPED));

	/** Sifts gravel; see {@link SieveBlock}. Drops itself with its wear as item damage. */
	public static final Block SIEVE = register("sieve", SieveBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.strength(1.5F)
			.sound(SoundType.WOOD)
			.noOcclusion()
			.ignitedByLava());

	/** A trough under a hay rack that livestock eats from; see {@link FeederBlock}. Drops itself and its feed. */
	public static final Block FEEDER = register("feeder", p -> new FeederBlock(false, p), BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.instrument(NoteBlockInstrument.BASS)
			.strength(2.0F, 3.0F)
			.sound(SoundType.WOOD)
			.noOcclusion()
			.ignitedByLava());

	/** A galvanized hay rack over a trough: a feeder that holds and feeds more. */
	public static final Block METAL_FEEDER = register("metal_feeder", p -> new FeederBlock(true, p), BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.requiresCorrectToolForDrops()
			.strength(3.0F, 6.0F)
			.sound(SoundType.METAL)
			.noOcclusion());

	/** A wooden fridge lined with wool: two ice slots and two rows of food; see {@link FridgeBlock}. */
	public static final Block SMALL_FRIDGE = register("small_fridge", p -> new FridgeBlock(FridgeMenu.Size.SMALL, p), BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.instrument(NoteBlockInstrument.BASS)
			.strength(2.5F)
			.sound(SoundType.WOOD)
			.ignitedByLava());

	/** An iron fridge lined with wool: four ice slots and four rows of food. */
	public static final Block LARGE_FRIDGE = register("large_fridge", p -> new FridgeBlock(FridgeMenu.Size.LARGE, p), BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.requiresCorrectToolForDrops()
			.strength(3.0F, 6.0F)
			.sound(SoundType.METAL));

	/** Silver ore: like copper ore, a bit rarer and more common in the mountains. */
	public static final Block SILVER_ORE = register("silver_ore", p -> new DropExperienceBlock(ConstantInt.of(0), p),
			BlockBehaviour.Properties.ofLegacyCopy(Blocks.COPPER_ORE));

	public static final Block DEEPSLATE_SILVER_ORE = register("deepslate_silver_ore", p -> new DropExperienceBlock(ConstantInt.of(0), p),
			BlockBehaviour.Properties.ofLegacyCopy(Blocks.DEEPSLATE_COPPER_ORE));

	public static final Block SILVER_BLOCK = register("silver_block", Block::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.instrument(NoteBlockInstrument.BELL)
			.requiresCorrectToolForDrops()
			.strength(5.0F, 6.0F)
			.sound(SoundType.METAL));

	/** A brewing stand for holy water and light potions; stands in structures instead of the vanilla one. */
	public static final Block SILVER_BREWING_STAND = register("silver_brewing_stand", SilverBrewingStandBlock::new,
			BlockBehaviour.Properties.ofLegacyCopy(Blocks.BREWING_STAND).mapColor(MapColor.METAL));

	/** Repair copper, silver and flint gear, not iron; wear like the vanilla (iron) anvil. */
	public static final Block COPPER_ANVIL = register("copper_anvil", p -> new MetalAnvilBlock(2, p),
			BlockBehaviour.Properties.ofLegacyCopy(Blocks.ANVIL).mapColor(MapColor.COLOR_ORANGE));
	public static final Block SILVER_ANVIL = register("silver_anvil", p -> new MetalAnvilBlock(2, p),
			BlockBehaviour.Properties.ofLegacyCopy(Blocks.ANVIL).mapColor(MapColor.METAL));

	/** Mithril ore: deep underground, a bit rarer than diamond; mined with an iron pickaxe or better. */
	public static final Block MITHRIL_ORE = register("mithril_ore", p -> new DropExperienceBlock(ConstantInt.of(0), p),
			BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_ORE));

	public static final Block DEEPSLATE_MITHRIL_ORE = register("deepslate_mithril_ore", p -> new DropExperienceBlock(ConstantInt.of(0), p),
			BlockBehaviour.Properties.ofLegacyCopy(Blocks.DEEPSLATE_IRON_ORE));

	public static final Block MITHRIL_BLOCK = register("mithril_block", Block::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.ICE)
			.instrument(NoteBlockInstrument.BELL)
			.requiresCorrectToolForDrops()
			.strength(5.0F, 6.0F)
			.sound(SoundType.METAL));

	/** Ancient metal has no ore: it will come from another dimension. */
	public static final Block ANCIENT_METAL_BLOCK = register("ancient_metal_block", Block::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.TERRACOTTA_BROWN)
			.instrument(NoteBlockInstrument.BELL)
			.requiresCorrectToolForDrops()
			.strength(5.0F, 6.0F)
			.sound(SoundType.METAL));

	/** The band of bedrock between the underworld's two levels: only an item in {@code #eam:breaks_deep_bedrock} breaks it. */
	public static final Block DEEP_BEDROCK = register("deep_bedrock", DeepBedrockBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.instrument(NoteBlockInstrument.BASEDRUM)
			.strength(50.0F, 3600000.0F)
			.noLootTable()
			.pushReaction(PushReaction.IMMOVEABLE)
			.isValidSpawn((state, level, pos, type) -> false));

	/** Underworld mushrooms: a chanterelle (orange funnel cap) and a champignon (white dome, some cap blocks glow blue). */
	public static final Block CHANTERELLE_CAP = mushroomBlock("chanterelle_cap", MapColor.COLOR_ORANGE, 0);
	public static final Block CHANTERELLE_STEM = mushroomBlock("chanterelle_stem", MapColor.TERRACOTTA_YELLOW, 0);
	public static final Block CHAMPIGNON_CAP = mushroomBlock("champignon_cap", MapColor.QUARTZ, 0);
	public static final Block GLOWING_CHAMPIGNON_CAP = mushroomBlock("glowing_champignon_cap", MapColor.COLOR_LIGHT_BLUE, 12);
	/** Giant mushrooms of the burnt mushroom forest: charred caps and stems, some caps still smouldering. */
	public static final Block BURNT_MUSHROOM_CAP = mushroomBlock("burnt_mushroom_cap", MapColor.COLOR_BLACK, 0);
	public static final Block BURNT_MUSHROOM_STEM = mushroomBlock("burnt_mushroom_stem", MapColor.COLOR_GRAY, 0);
	public static final Block SMOLDERING_MUSHROOM_CAP = mushroomBlock("smoldering_mushroom_cap", MapColor.COLOR_ORANGE, 7);
	/** Glowing fungus threads hanging from the ceilings of the underground forest: a dim strand ending in a bright tip. */
	public static final Block GLOWSHROOM_VINES = register("glowshroom_vines", GlowshroomVinesBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_CYAN)
			.randomTicks()
			.noCollision()
			.instabreak()
			.sound(SoundType.WEEPING_VINES)
			.lightLevel(state -> 10)
			.pushReaction(PushReaction.POPPED));
	public static final Block GLOWSHROOM_VINES_PLANT = register("glowshroom_vines_plant", GlowshroomVinesPlantBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_CYAN)
			.noCollision()
			.instabreak()
			.sound(SoundType.WEEPING_VINES)
			.lightLevel(state -> 5)
			.pushReaction(PushReaction.POPPED));

	/** Repairs everything and forges mithril plates; wears like the vanilla anvil. */
	public static final Block MITHRIL_ANVIL = register("mithril_anvil", p -> new MetalAnvilBlock(4, p),
			BlockBehaviour.Properties.ofLegacyCopy(Blocks.ANVIL).mapColor(MapColor.ICE));

	/**
	 * Adamantium ore: only in the underworld's lower level, rarer than mithril; mined with a mithril pickaxe or better.
	 * Mostly the deepslate kind, since the depths are deepslate.
	 */
	public static final Block ADAMANTIUM_ORE = register("adamantium_ore", p -> new DropExperienceBlock(ConstantInt.of(0), p),
			BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_ORE).strength(5.0F, 6.0F));
	public static final Block DEEPSLATE_ADAMANTIUM_ORE = register("deepslate_adamantium_ore", p -> new DropExperienceBlock(ConstantInt.of(0), p),
			BlockBehaviour.Properties.ofLegacyCopy(Blocks.DEEPSLATE_IRON_ORE).strength(6.5F, 6.0F));
	public static final Block ADAMANTIUM_BLOCK = register("adamantium_block", Block::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_CYAN)
			.instrument(NoteBlockInstrument.BELL)
			.requiresCorrectToolForDrops()
			.strength(10.0F, 1200.0F)
			.sound(SoundType.METAL));
	/** Repairs everything, adamantium and netherite gear too, and forges adamantium plates. */
	public static final Block ADAMANTIUM_ANVIL = register("adamantium_anvil", p -> new MetalAnvilBlock(5, p),
			BlockBehaviour.Properties.ofLegacyCopy(Blocks.ANVIL).mapColor(MapColor.COLOR_CYAN));

	/** A blast furnace that also smelts mithril; see {@link ObsidianBlastFurnaceBlock}. */
	public static final Block OBSIDIAN_BLAST_FURNACE = register("obsidian_blast_furnace", ObsidianBlastFurnaceBlock::new,
			BlockBehaviour.Properties.ofLegacyCopy(Blocks.BLAST_FURNACE).mapColor(MapColor.COLOR_BLACK).strength(10.0F, 1200.0F));

	/** Silver bars, door and trapdoor: like the iron ones, opened by redstone only. Copper has its own in vanilla. */
	public static final Block SILVER_BARS = register("silver_bars", MetalBarsBlock::new, BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BARS));
	public static final Block SILVER_DOOR = register("silver_door", MetalDoorBlock::new, BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_DOOR));
	public static final Block SILVER_TRAPDOOR = register("silver_trapdoor", MetalTrapDoorBlock::new,
			BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_TRAPDOOR));

	/** Mithril bars, door and trapdoor: twice as hard to break as iron and they stand up to explosions like obsidian. */
	public static final Block MITHRIL_BARS = register("mithril_bars", MetalBarsBlock::new, mithril(Blocks.IRON_BARS));
	public static final Block MITHRIL_DOOR = register("mithril_door", MetalDoorBlock::new, mithril(Blocks.IRON_DOOR));
	public static final Block MITHRIL_TRAPDOOR = register("mithril_trapdoor", MetalTrapDoorBlock::new, mithril(Blocks.IRON_TRAPDOOR));

	/** Swampy forest plants. Duckweed floats on still water; cattails grow on the shore or out of shallow water. */
	public static final Block DUCKWEED = register("duckweed", DuckweedBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.PLANT)
			.noCollision()
			.instabreak()
			.sound(SoundType.LILY_PAD)
			.noOcclusion()
			.pushReaction(PushReaction.POPPED));
	public static final Block CATTAIL = register("cattail", CattailBlock::new, plant());
	public static final Block SWAMP_GRASS = register("swamp_grass", SwampGrassBlock::new, plant().replaceable().offsetType(BlockBehaviour.OffsetType.XYZ));
	public static final Block TALL_SWAMP_GRASS = register("tall_swamp_grass", DoublePlantBlock::new,
			plant().replaceable().offsetType(BlockBehaviour.OffsetType.XZ));
	public static final Block SWAMP_SHRUB = register("swamp_shrub", SwampShrubBlock::new, plant().replaceable().offsetType(BlockBehaviour.OffsetType.XZ));
	/** A small dense shrub on twig legs: you squeeze through it at half speed. Some bear cranberries. */
	public static final Block DENSE_SHRUB = register("dense_shrub", DenseShrubBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.PLANT)
			.noCollision()
			.strength(0.2F)
			.sound(SoundType.AZALEA)
			.noOcclusion()
			.isSuffocating(Blocks::never)
			.isRedstoneConductor(Blocks::never)
			.ignitedByLava()
			.pushReaction(PushReaction.POPPED));
	/** A shelf mushroom on the side of a tree trunk. */
	/** The woody post a big swamp shrub stands on. */
	public static final Block BIG_SHRUB_STEM = register("big_shrub_stem", BigShrubStemBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.strength(0.8F)
			.sound(SoundType.WOOD)
			.noOcclusion()
			.ignitedByLava());
	/** The crown of a big swamp shrub: leaves you push through at half speed. Some bear wolfberries. */
	public static final Block BIG_SHRUB_LEAVES = register("big_shrub_leaves", BigShrubLeavesBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.PLANT)
			.noCollision()
			.strength(0.2F)
			.sound(SoundType.AZALEA_LEAVES)
			.noOcclusion()
			.isSuffocating(Blocks::never)
			.isRedstoneConductor(Blocks::never)
			.ignitedByLava()
			.pushReaction(PushReaction.POPPED));
	public static final Block SHELF_MUSHROOM = register("shelf_mushroom", ShelfMushroomBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_ORANGE)
			.noCollision()
			.strength(0.2F)
			.sound(SoundType.FUNGUS)
			.noOcclusion()
			.ignitedByLava()
			.pushReaction(PushReaction.POPPED));
	/** Hollow logs, one per vanilla log and stem: the bark of a fallen trunk whose core has rotted away. */
	public static final Map<Block, Block> HOLLOW_LOGS = hollowLogs(Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.JUNGLE_LOG,
			Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.MANGROVE_LOG, Blocks.CHERRY_LOG, Blocks.PALE_OAK_LOG, Blocks.CRIMSON_STEM,
			Blocks.WARPED_STEM);
	public static final Block HOLLOW_OAK_LOG = HOLLOW_LOGS.get(Blocks.OAK_LOG);
	public static final Block HOLLOW_BIRCH_LOG = HOLLOW_LOGS.get(Blocks.BIRCH_LOG);
	/** Fireweed (willowherb): a tall pink flower on the edges of bogs. */
	public static final Block FIREWEED = register("fireweed", TallFlowerBlock::new, plant().offsetType(BlockBehaviour.OffsetType.XZ));
	/** Marsh marigold is poisonous: in suspicious stew it gives nausea. */
	public static final Block MARSH_MARIGOLD = register("marsh_marigold", p -> new FlowerBlock(MobEffects.NAUSEA, 7.0F, p),
			plant().offsetType(BlockBehaviour.OffsetType.XZ));
	/** Garlic growing on farmland, planted from a garlic bulb. */
	public static final Block GARLIC_CROP = register("garlic_crop", GarlicCropBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.POPPED));
	/** Wild garlic (ramsons) in plains and forests: the only source of garlic bulbs before the first harvest. */
	public static final Block WILD_GARLIC = register("wild_garlic", p -> new BushBlock(p, 10), plant().replaceable()
			.offsetType(BlockBehaviour.OffsetType.XZ));

	private EamBlocks() {
	}

	public static void register() {
		// Loading the class registered the blocks above. Dense shrubs burn like leaves, hollow logs like logs.
		FlammableBlockRegistry.getDefaultInstance().add(DENSE_SHRUB, 30, 60);
		FlammableBlockRegistry.getDefaultInstance().add(BIG_SHRUB_LEAVES, 30, 60);
		FlammableBlockRegistry.getDefaultInstance().add(BIG_SHRUB_STEM, 5, 5);
		HOLLOW_LOGS.forEach((log, hollow) -> {
			if (log != Blocks.CRIMSON_STEM && log != Blocks.WARPED_STEM) {
				FlammableBlockRegistry.getDefaultInstance().add(hollow, 5, 5);
			}
		});
	}

	/** The elite crossbowman's net: slows whoever stands in it and falls apart after a while; see {@link NetBlock}. */
	public static final Block NET = register("net", NetBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOL)
			.sound(SoundType.COBWEB)
			.forceSolidOn()
			.noCollision()
			.requiresCorrectToolForDrops()
			.strength(2.0F)
			.noLootTable()
			.pushReaction(PushReaction.POPPED));

	private static BlockBehaviour.Properties mithril(Block iron) {
		return BlockBehaviour.Properties.ofLegacyCopy(iron).mapColor(MapColor.ICE).strength(10.0F, 1200.0F);
	}

	/** Like vanilla grass and flowers: no collision, broken at once, burns. */
	private static BlockBehaviour.Properties plant() {
		return BlockBehaviour.Properties.of()
				.mapColor(MapColor.PLANT)
				.noCollision()
				.instabreak()
				.sound(SoundType.GRASS)
				.ignitedByLava()
				.pushReaction(PushReaction.POPPED);
	}

	private static BlockBehaviour.Properties groundItem(MapColor color) {
		return BlockBehaviour.Properties.of()
				.mapColor(color)
				.noCollision()
				.replaceable()
				.instabreak()
				.sound(SoundType.STONE)
				.pushReaction(PushReaction.POPPED);
	}

	private static Map<Block, Block> hollowLogs(Block... logs) {
		Map<Block, Block> hollow = new LinkedHashMap<>();
		for (Block log : logs) {
			String name = "hollow_" + BuiltInRegistries.BLOCK.getKey(log).getPath();
			hollow.put(log, register(name, HollowLogBlock::new, BlockBehaviour.Properties.ofLegacyCopy(log).noOcclusion()));
		}
		return Collections.unmodifiableMap(hollow);
	}

	/** A huge mushroom block like vanilla's: faces show the skin where the mushroom does not continue. */
	private static Block mushroomBlock(String name, MapColor color, int light) {
		return register(name, HugeMushroomBlock::new, BlockBehaviour.Properties.of()
				.mapColor(color)
				.instrument(NoteBlockInstrument.BASS)
				.strength(0.2F)
				.sound(SoundType.WOOD)
				.lightLevel(state -> light)
				.ignitedByLava());
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Eam.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}
}
