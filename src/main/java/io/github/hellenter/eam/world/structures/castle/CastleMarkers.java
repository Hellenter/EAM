package io.github.hellenter.eam.world.structures.castle;

import com.mojang.serialization.MapCodec;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.world.structures.outpost.OutpostMarkers;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.HashMap;
import java.util.Map;

/**
 * Maxim's markers in the castle template. Wool stairs become containers opening toward the low side of the step:
 * lime a barrel of food, green a chest of the King's treasure, yellow a chest of arrows, potions and shields. Wool
 * standing in the open marks a mob as at the outpost (red guard, orange elite crossbowman, yellow pillager, blue
 * horse), and black the King, purple a villager; {@link CastlePiece} puts them in, since telling a marker from wool laid
 * into a floor takes the blocks around it. Runs before the template is turned, so containers face in the template's
 * own frame and turn with it.
 */
public final class CastleMarkers implements StructureProcessor {
	public static final CastleMarkers INSTANCE = new CastleMarkers();
	private static final MapCodec<CastleMarkers> CODEC = MapCodec.unit(INSTANCE);

	/** Wool marker blocks and the mob each one stands for. */
	public static final Map<Block, EntityType<?>> MOBS = mobs();

	private static final Map<Block, ResourceKey<LootTable>> CHESTS = Map.of(
			Blocks.WOOL_STAIRS.green(), CastleModule.TREASURE_LOOT,
			Blocks.WOOL_STAIRS.yellow(), CastleModule.ARMS_LOOT);

	private CastleMarkers() {
	}

	private static Map<Block, EntityType<?>> mobs() {
		Map<Block, EntityType<?>> mobs = new HashMap<>(OutpostMarkers.MOBS);
		mobs.put(Blocks.WOOL.black(), EamEntities.PILLAGER_KING);
		mobs.put(Blocks.WOOL.purple(), EntityTypes.VILLAGER);
		return Map.copyOf(mobs);
	}

	@Override
	public StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pivot, BlockPos origin,
			StructureBlockInfo info, StructurePlaceSettings settings) {
		BlockState state = info.state();
		if (state.is(Blocks.WOOL_STAIRS.lime())) {
			BlockState barrel = Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, state.getValue(StairBlock.FACING).getOpposite());
			return new StructureBlockInfo(info.pos(), barrel, lootTag("minecraft:barrel", CastleModule.FOOD_LOOT));
		}
		ResourceKey<LootTable> chest = CHESTS.get(state.getBlock());
		if (chest != null) {
			BlockState placed = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, state.getValue(StairBlock.FACING).getOpposite());
			return new StructureBlockInfo(info.pos(), placed, lootTag("minecraft:chest", chest));
		}
		return info;
	}

	private static CompoundTag lootTag(String blockEntity, ResourceKey<LootTable> lootTable) {
		CompoundTag tag = new CompoundTag();
		tag.putString("id", blockEntity);
		tag.putString("LootTable", lootTable.identifier().toString());
		return tag;
	}

	@Override
	public MapCodec<? extends StructureProcessor> codec() {
		return CODEC;
	}
}
