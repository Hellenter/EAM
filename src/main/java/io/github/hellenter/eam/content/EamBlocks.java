package io.github.hellenter.eam.content;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.anvil.MetalAnvilBlock;
import io.github.hellenter.eam.content.brewing.SilverBrewingStandBlock;
import io.github.hellenter.eam.content.campfire.CampFireBlock;
import io.github.hellenter.eam.content.furnace.ObsidianBlastFurnaceBlock;
import io.github.hellenter.eam.content.ground.GroundItemBlock;
import io.github.hellenter.eam.content.ground.GroundItemLogic;
import io.github.hellenter.eam.content.metal.MetalBarsBlock;
import io.github.hellenter.eam.content.metal.MetalDoorBlock;
import io.github.hellenter.eam.content.metal.MetalTrapDoorBlock;
import io.github.hellenter.eam.content.sieve.SieveBlock;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

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

	/** Sifts gravel; see {@link SieveBlock}. Drops itself with its wear as item damage. */
	public static final Block SIEVE = register("sieve", SieveBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.strength(1.5F)
			.sound(SoundType.WOOD)
			.noOcclusion()
			.ignitedByLava());

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

	/** Repairs everything and forges mithril plates; wears like the vanilla anvil. */
	public static final Block MITHRIL_ANVIL = register("mithril_anvil", p -> new MetalAnvilBlock(4, p),
			BlockBehaviour.Properties.ofLegacyCopy(Blocks.ANVIL).mapColor(MapColor.ICE));

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

	private EamBlocks() {
	}

	public static void register() {
		// Loads the class, which registers the blocks above.
	}

	private static BlockBehaviour.Properties mithril(Block iron) {
		return BlockBehaviour.Properties.ofLegacyCopy(iron).mapColor(MapColor.ICE).strength(10.0F, 1200.0F);
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

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Eam.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}
}
