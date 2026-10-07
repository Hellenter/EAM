package io.github.hellenter.eam.content;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.campfire.CampFireBlock;
import io.github.hellenter.eam.content.ground.GroundItemBlock;
import io.github.hellenter.eam.content.ground.GroundItemLogic;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
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
	public static final Block GROUND_STICK = register("ground_stick", p -> new GroundItemBlock(GroundItemLogic.fullLength(1, 1), p),
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

	private EamBlocks() {
	}

	public static void register() {
		// Loads the class, which registers the blocks above.
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
