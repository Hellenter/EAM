package io.github.hellenter.eam.world.structures.outpost;

import com.mojang.serialization.MapCodec;

import io.github.hellenter.eam.content.EamEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.LevelReader;
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
 * Turns the marker blocks of Maxim's outpost template into what they stand for:
 * <ul>
 * <li>red wool: a pillager guard, orange wool: an elite crossbowman, yellow wool: a pillager, blue wool: a horse,
 * standing where the wool was (the wool turns to air here; {@link OutpostPiece} puts the mob in);</li>
 * <li>wool stairs: a chest opening toward the low side of the step, lime with a few arrows, yellow with basic things,
 * orange with better ones, red with the castle map;</li>
 * <li>barrels: some food.</li>
 * </ul>
 * Runs before the template is turned, so the chest is faced in the template's own frame and turns with it.
 */
public final class OutpostMarkers implements StructureProcessor {
	public static final OutpostMarkers INSTANCE = new OutpostMarkers();
	private static final MapCodec<OutpostMarkers> CODEC = MapCodec.unit(INSTANCE);

	/** Wool marker blocks and the mob each one stands for. */
	public static final Map<Block, EntityType<?>> MOBS = Map.of(
			Blocks.WOOL.red(), EamEntities.PILLAGER_GUARD,
			Blocks.WOOL.orange(), EamEntities.ELITE_CROSSBOWMAN,
			Blocks.WOOL.yellow(), EntityTypes.PILLAGER,
			Blocks.WOOL.blue(), EntityTypes.HORSE);

	private static final Map<Block, ResourceKey<LootTable>> CHESTS = Map.of(
			Blocks.WOOL_STAIRS.lime(), OutpostModule.ARROWS_LOOT,
			Blocks.WOOL_STAIRS.yellow(), OutpostModule.BASIC_LOOT,
			Blocks.WOOL_STAIRS.orange(), OutpostModule.BETTER_LOOT,
			Blocks.WOOL_STAIRS.red(), OutpostModule.SPECIAL_LOOT);

	private OutpostMarkers() {
	}

	@Override
	public StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pivot, BlockPos origin,
			StructureBlockInfo info, StructurePlaceSettings settings) {
		BlockState state = info.state();
		if (MOBS.containsKey(state.getBlock())) return new StructureBlockInfo(info.pos(), Blocks.AIR.defaultBlockState(), null);
		ResourceKey<LootTable> chest = CHESTS.get(state.getBlock());
		if (chest != null) {
			BlockState placed = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, state.getValue(StairBlock.FACING).getOpposite());
			return new StructureBlockInfo(info.pos(), placed, lootTag("minecraft:chest", chest));
		}
		if (state.is(Blocks.BARREL)) return new StructureBlockInfo(info.pos(), state, lootTag("minecraft:barrel", OutpostModule.BARREL_LOOT));
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
