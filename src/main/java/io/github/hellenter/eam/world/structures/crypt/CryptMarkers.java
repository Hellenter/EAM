package io.github.hellenter.eam.world.structures.crypt;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

/**
 * Turns the marker blocks Maxim builds crypt rooms with into what they stand for:
 * <ul>
 * <li>red wool: a trial spawner of skeleton swordsmen, yellow wool: of skeleton archers, green wool: of skeleton
 * knights (configs in {@code CryptSpawners});</li>
 * <li>bamboo stairs: a tier 1 chest, cherry stairs: a tier 2 chest, opening toward the low side of the step;</li>
 * <li>light blue wool: a vault, blue wool: an ominous vault, facing a random side;</li>
 * <li>red glass: a skeleton skull on the floor 5% of the time, yellow glass: 2% of the time, air otherwise.</li>
 * </ul>
 * Runs before the template is turned, so the chest is faced in the room's own frame and turns with it.
 */
public final class CryptMarkers implements StructureProcessor {
	public static final CryptMarkers INSTANCE = new CryptMarkers();
	private static final MapCodec<CryptMarkers> CODEC = MapCodec.unit(INSTANCE);

	private static final String SWORDSMAN = "eam:crypt/swordsman/";
	private static final String ARCHER = "eam:crypt/archer/";
	private static final String KNIGHT = "eam:crypt/knight/";

	private CryptMarkers() {
	}

	@Override
	public StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pivot, BlockPos origin,
			StructureBlockInfo info, StructurePlaceSettings settings) {
		BlockState state = info.state();
		if (state.is(Blocks.WOOL.red())) return spawner(info, SWORDSMAN);
		if (state.is(Blocks.WOOL.yellow())) return spawner(info, ARCHER);
		if (state.is(Blocks.WOOL.green())) return spawner(info, KNIGHT);
		if (state.is(Blocks.BAMBOO_STAIRS)) return chest(info, CryptModule.TIER_1_CHEST);
		if (state.is(Blocks.CHERRY_STAIRS)) return chest(info, CryptModule.TIER_2_CHEST);
		if (state.is(Blocks.WOOL.lightBlue())) return vault(info, false, settings.getRandom(info.pos()));
		if (state.is(Blocks.WOOL.blue())) return vault(info, true, settings.getRandom(info.pos()));
		if (state.is(Blocks.STAINED_GLASS.red())) return skull(info, 5, settings.getRandom(info.pos()));
		if (state.is(Blocks.STAINED_GLASS.yellow())) return skull(info, 2, settings.getRandom(info.pos()));
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

	private static StructureBlockInfo vault(StructureBlockInfo info, boolean ominous, RandomSource random) {
		CompoundTag config = new CompoundTag();
		CompoundTag key = new CompoundTag();
		key.putString("id", ominous ? "minecraft:ominous_trial_key" : "minecraft:trial_key");
		config.put("key_item", key);
		config.putString("loot_table", ominous ? "minecraft:chests/trial_chambers/reward_ominous" : "minecraft:chests/trial_chambers/reward");
		CompoundTag tag = new CompoundTag();
		tag.putString("id", "minecraft:vault");
		tag.put("config", config);
		BlockState vault = Blocks.VAULT.defaultBlockState()
				.setValue(VaultBlock.OMINOUS, ominous)
				.setValue(VaultBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random));
		return new StructureBlockInfo(info.pos(), vault, tag);
	}

	private static StructureBlockInfo skull(StructureBlockInfo info, int percent, RandomSource random) {
		BlockState state = random.nextInt(100) < percent
				? Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, random.nextInt(16))
				: Blocks.AIR.defaultBlockState();
		return new StructureBlockInfo(info.pos(), state, null);
	}

	@Override
	public MapCodec<? extends StructureProcessor> codec() {
		return CODEC;
	}
}
