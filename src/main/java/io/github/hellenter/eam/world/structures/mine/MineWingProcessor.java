package io.github.hellenter.eam.world.structures.mine;

import io.github.hellenter.eam.content.EamBlocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Turns a piece of a mine wing ({@link MineWingLogic}) into its caved-in or flooded state. It works on the whole piece at
 * once, so the rubble and the water line agree across chunk borders: a block is open when one can walk through it (air,
 * rails, torches, cobwebs), and how high it is above the floor is counted down through the open blocks under it.
 *
 * <p>Caved in: some supports, beams and lights are gone, rubble ({@link MineWingLogic#rubble}) lies on the floors, and
 * the first few blocks behind {@code entrance} are filled to the roof. Flooded: every open block at or under
 * {@code waterLevel} is water, and whatever can hold water holds it.
 */
public record MineWingProcessor(Kind kind, int waterLevel, Optional<BlockPos> entrance, long seed) implements StructureProcessor {
	public static final MapCodec<MineWingProcessor> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Kind.CODEC.fieldOf("kind").forGetter(MineWingProcessor::kind),
			Codec.INT.optionalFieldOf("water_level", 0).forGetter(MineWingProcessor::waterLevel),
			BlockPos.CODEC.optionalFieldOf("entrance").forGetter(MineWingProcessor::entrance),
			Codec.LONG.fieldOf("seed").forGetter(MineWingProcessor::seed)
	).apply(instance, MineWingProcessor::new));

	/** How far around the entrance (across and along the tunnel) the cave-in that closes it reaches. */
	private static final int PLUG_REACH = 2;
	/** How high above the entrance's floor the cave-in reaches: the tunnel's headroom and the beam over it. */
	private static final int PLUG_HEIGHT = 4;
	private static final double BROKEN_SUPPORT = 0.4;
	private static final double LOST_LIGHT = 0.7;

	public enum Kind implements StringRepresentable {
		COLLAPSED("collapsed"),
		FLOODED("flooded");

		public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

		private final String name;

		Kind(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public static MineWingProcessor collapsed(Optional<BlockPos> entrance, long seed) {
		return new MineWingProcessor(Kind.COLLAPSED, 0, entrance, seed);
	}

	public static MineWingProcessor flooded(int waterLevel, long seed) {
		return new MineWingProcessor(Kind.FLOODED, waterLevel, Optional.empty(), seed);
	}

	@Override
	public boolean evaluatesEntirePieceState() {
		return true;
	}

	@Override
	public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level, BlockPos position,
			BlockPos referencePos, List<StructureTemplate.StructureBlockInfo> originalBlockInfoList,
			List<StructureTemplate.StructureBlockInfo> processedBlockInfoList, StructurePlaceSettings settings) {
		List<StructureTemplate.StructureBlockInfo> blocks = new ArrayList<>(processedBlockInfoList.size());
		for (StructureTemplate.StructureBlockInfo info : processedBlockInfoList) {
			BlockState state = kind == Kind.COLLAPSED ? broken(info.pos(), info.state()) : info.state();
			blocks.add(state == info.state() ? info : new StructureTemplate.StructureBlockInfo(info.pos(), state, info.nbt()));
		}
		Set<BlockPos> open = new HashSet<>();
		for (StructureTemplate.StructureBlockInfo info : blocks) {
			if (open(info.state())) {
				open.add(info.pos());
			}
		}
		for (int i = 0; i < blocks.size(); i++) {
			StructureTemplate.StructureBlockInfo info = blocks.get(i);
			BlockState state = kind == Kind.COLLAPSED ? caveIn(info, open) : flood(info, open.contains(info.pos()));
			if (state != info.state()) {
				blocks.set(i, new StructureTemplate.StructureBlockInfo(info.pos(), state, state.hasBlockEntity() ? info.nbt() : null));
			}
		}
		return blocks;
	}

	/** A support, beam or light that has fallen out of a caved-in piece: air instead. */
	private BlockState broken(BlockPos pos, BlockState state) {
		boolean support = state.is(EamBlocks.STRIPPED_POPLAR_FENCE) || state.is(Blocks.STRIPPED_POPLAR_LOG)
				|| state.is(Blocks.POPLAR_FENCE) || state.is(Blocks.POPLAR_PLANKS) || state.is(Blocks.POPLAR_SLAB);
		boolean light = state.is(Blocks.LANTERN) || state.is(Blocks.WALL_TORCH) || state.is(Blocks.TORCH);
		double roll = MineWingLogic.random(seed, pos.getX(), pos.getY(), pos.getZ(), 3);
		if (support && roll < BROKEN_SUPPORT || light && roll < LOST_LIGHT) {
			return Blocks.AIR.defaultBlockState();
		}
		return state;
	}

	private BlockState caveIn(StructureTemplate.StructureBlockInfo info, Set<BlockPos> open) {
		BlockPos pos = info.pos();
		if (!open.contains(pos)) {
			return info.state();
		}
		if (entrance.isPresent() && plug(entrance.get(), pos)) {
			return rubbleBlock(pos);
		}
		int above = 1;
		for (BlockPos below = pos.below(); open.contains(below); below = below.below()) {
			above++;
		}
		int rubble = MineWingLogic.rubble(seed, pos.getX(), pos.getZ());
		if (above <= rubble) {
			return rubbleBlock(pos);
		}
		if (above == rubble + 1 && rubble < UnderworldMineLogic.HEIGHT
				&& MineWingLogic.random(seed, pos.getX(), pos.getY(), pos.getZ(), 4) < 0.3) {
			return Blocks.COBBLESTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
		}
		return info.state();
	}

	private static boolean plug(BlockPos entrance, BlockPos pos) {
		int dy = pos.getY() - entrance.getY();
		return Math.abs(pos.getX() - entrance.getX()) <= PLUG_REACH && Math.abs(pos.getZ() - entrance.getZ()) <= PLUG_REACH
				&& dy > 0 && dy <= PLUG_HEIGHT;
	}

	private BlockState rubbleBlock(BlockPos pos) {
		double roll = MineWingLogic.random(seed, pos.getX(), pos.getY(), pos.getZ(), 5);
		if (roll < 0.65) {
			return Blocks.COBBLESTONE.defaultBlockState();
		}
		return roll < 0.85 ? Blocks.STONE.defaultBlockState() : Blocks.ANDESITE.defaultBlockState();
	}

	private BlockState flood(StructureTemplate.StructureBlockInfo info, boolean open) {
		BlockState state = info.state();
		if (info.pos().getY() > waterLevel) {
			return state;
		}
		if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
			return state.setValue(BlockStateProperties.WATERLOGGED, true);
		}
		return open ? Blocks.WATER.defaultBlockState() : state;
	}

	/** Whether one walks through the block: what rubble covers and water fills. */
	private static boolean open(BlockState state) {
		return !state.isSolid() && !state.is(Blocks.LADDER) && !state.is(Blocks.WATER) && !state.is(Blocks.LAVA);
	}

	@Override
	public MapCodec<? extends StructureProcessor> codec() {
		return CODEC;
	}
}
