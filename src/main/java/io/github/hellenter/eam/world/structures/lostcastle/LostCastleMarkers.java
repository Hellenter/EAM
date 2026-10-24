package io.github.hellenter.eam.world.structures.lostcastle;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
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

import java.util.Map;

/**
 * Maxim's container markers in the Lost castle template: wool stairs become containers opening toward the low side of
 * the step, lime a barrel of provisions, yellow a chest of the soldiers' belongings, orange a chest of arrows and
 * supplies, red a chest of treasure. Runs before the template is turned, so containers face in the template's own frame
 * and turn with it. The wool mob markers are {@link LostCastlePiece}'s.
 */
public final class LostCastleMarkers implements StructureProcessor {
	public static final LostCastleMarkers INSTANCE = new LostCastleMarkers();
	private static final MapCodec<LostCastleMarkers> CODEC = MapCodec.unit(INSTANCE);

	private static final Map<Block, ResourceKey<LootTable>> CHESTS = Map.of(
			Blocks.WOOL_STAIRS.yellow(), LostCastleModule.BELONGINGS_LOOT,
			Blocks.WOOL_STAIRS.orange(), LostCastleModule.SUPPLIES_LOOT,
			Blocks.WOOL_STAIRS.red(), LostCastleModule.TREASURE_LOOT);

	private LostCastleMarkers() {
	}

	@Override
	public StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pivot, BlockPos origin,
			StructureBlockInfo info, StructurePlaceSettings settings) {
		BlockState state = info.state();
		if (state.is(Blocks.WOOL_STAIRS.lime())) {
			BlockState barrel = Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, state.getValue(StairBlock.FACING).getOpposite());
			return new StructureBlockInfo(info.pos(), barrel, lootTag("minecraft:barrel", LostCastleModule.PROVISIONS_LOOT));
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
