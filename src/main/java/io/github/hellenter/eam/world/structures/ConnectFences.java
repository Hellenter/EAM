package io.github.hellenter.eam.world.structures;

import com.mojang.serialization.MapCodec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

/**
 * Joins each fence of a template to its neighbours in the same template, as if it had just been placed. A jigsaw piece
 * is placed with its saved block states as they are, and some templates were saved with bare fence posts, so without
 * this a fence row stands as separate posts. A side whose neighbour is not part of the template is left as saved; the
 * game joins the piece's outer edges to the world itself.
 */
public final class ConnectFences implements StructureProcessor {
	public static final ConnectFences INSTANCE = new ConnectFences();
	public static final MapCodec<ConnectFences> CODEC = MapCodec.unit(INSTANCE);

	private ConnectFences() {
	}

	/** Registers the processor type, so processor lists in data can name it. */
	public static void register() {
		Registry.register(BuiltInRegistries.STRUCTURE_PROCESSOR, Eam.id("connect_fences"), CODEC);
	}

	/** The whole piece is needed, so a fence on a chunk edge sees its neighbour in the next chunk. */
	@Override
	public boolean evaluatesEntirePieceState() {
		return true;
	}

	@Override
	public List<StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level, BlockPos position, BlockPos referencePos,
			List<StructureBlockInfo> originalBlockInfoList, List<StructureBlockInfo> processedBlockInfoList,
			StructurePlaceSettings settings) {
		// Positions are already in the world, states are rotated only when placed: compare them as they will stand.
		Map<BlockPos, BlockState> placed = new HashMap<>();
		for (StructureBlockInfo info : processedBlockInfoList) {
			placed.put(info.pos(), info.state().mirror(settings.getMirror()).rotate(settings.getRotation()));
		}
		Rotation back = switch (settings.getRotation()) {
			case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
			case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
			default -> settings.getRotation();
		};
		List<StructureBlockInfo> result = new ArrayList<>(processedBlockInfoList.size());
		for (StructureBlockInfo info : processedBlockInfoList) {
			BlockState state = placed.get(info.pos());
			if (!(state.getBlock() instanceof FenceBlock fence)) {
				result.add(info);
				continue;
			}
			BlockState joined = state;
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				BlockState neighbour = placed.get(info.pos().relative(direction));
				if (neighbour != null) {
					boolean solid = neighbour.isFaceSturdy(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, direction.getOpposite());
					joined = joined.setValue(FenceBlock.PROPERTY_BY_DIRECTION.get(direction),
							fence.connectsTo(neighbour, solid, direction.getOpposite()));
				}
			}
			result.add(joined == state ? info
					: new StructureBlockInfo(info.pos(), joined.rotate(back).mirror(settings.getMirror()), info.nbt()));
		}
		return result;
	}

	@Override
	public MapCodec<ConnectFences> codec() {
		return CODEC;
	}
}
