package io.github.hellenter.eam.world.structures.crypt;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

/**
 * Turns the marker blocks Maxim builds crypt rooms with into what they stand for:
 * <ul>
 * <li>red wool: a trial spawner of melee mobs, yellow wool: a trial spawner of archers;</li>
 * <li>bamboo stairs: a tier 1 chest, cherry stairs: a tier 2 chest, opening toward the low side of the step.</li>
 * </ul>
 * Runs before the template is turned, so the chest is faced in the room's own frame and turns with it.
 */
public final class CryptMarkers implements StructureProcessor {
	public static final CryptMarkers INSTANCE = new CryptMarkers();
	private static final MapCodec<CryptMarkers> CODEC = MapCodec.unit(INSTANCE);

	// Vanilla trial chamber spawners until the crypt gets its own mobs.
	private static final String MELEE = "minecraft:trial_chamber/melee/zombie/";
	private static final String RANGED = "minecraft:trial_chamber/ranged/skeleton/";

	private CryptMarkers() {
	}

	@Override
	public StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pivot, BlockPos origin,
			StructureBlockInfo info, StructurePlaceSettings settings) {
		BlockState state = info.state();
		if (state.is(Blocks.WOOL.red())) return spawner(info, MELEE);
		if (state.is(Blocks.WOOL.yellow())) return spawner(info, RANGED);
		if (state.is(Blocks.BAMBOO_STAIRS)) return chest(info, CryptModule.TIER_1_CHEST);
		if (state.is(Blocks.CHERRY_STAIRS)) return chest(info, CryptModule.TIER_2_CHEST);
		return info;
	}

	private static StructureBlockInfo spawner(StructureBlockInfo info, String config) {
		CompoundTag tag = new CompoundTag();
		tag.putString("id", "minecraft:trial_spawner");
		tag.putString("normal_config", config + "normal");
		tag.putString("ominous_config", config + "ominous");
		return new StructureBlockInfo(info.pos(), Blocks.TRIAL_SPAWNER.defaultBlockState(), tag);
	}

	private static StructureBlockInfo chest(StructureBlockInfo info, Identifier lootTable) {
		CompoundTag tag = new CompoundTag();
		tag.putString("id", "minecraft:chest");
		tag.putString("LootTable", lootTable.toString());
		BlockState chest = Blocks.CHEST.defaultBlockState()
				.setValue(ChestBlock.FACING, info.state().getValue(StairBlock.FACING).getOpposite());
		return new StructureBlockInfo(info.pos(), chest, tag);
	}

	@Override
	public MapCodec<? extends StructureProcessor> codec() {
		return CODEC;
	}
}
