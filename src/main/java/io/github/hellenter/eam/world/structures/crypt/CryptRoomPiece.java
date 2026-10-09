package io.github.hellenter.eam.world.structures.crypt;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * One crypt room: a template turned about the middle of its cell, so it keeps the cell's footprint, with the doors the
 * layout does not use walled up after it is placed.
 */
public class CryptRoomPiece extends TemplateStructurePiece {
	private static final BlockPos PIVOT = new BlockPos(CryptModule.CELL / 2, 0, CryptModule.CELL / 2);

	private final Rotation rotation;
	/** Doors to wall up, as a {@link CryptLayout} mask in world directions. */
	private final int sealedDoors;

	/**
	 * How many layers from the bottom the piece reports as its box, or 0 for all of them. The world shapes terrain
	 * around a piece's box to wall the crypt in, so the entrance reports only its deep part to keep its surface
	 * building from being buried.
	 */
	private final int shapedHeight;

	public CryptRoomPiece(StructureTemplateManager manager, Identifier template, BlockPos position, Rotation rotation,
			int sealedDoors, int shapedHeight) {
		super(CryptModule.ROOM_PIECE, 0, manager, template, template.toString(), settings(rotation), position);
		this.rotation = rotation;
		this.sealedDoors = sealedDoors;
		this.shapedHeight = shapedHeight;
		cutBox();
	}

	public CryptRoomPiece(StructurePieceSerializationContext context, CompoundTag tag) {
		super(CryptModule.ROOM_PIECE, tag, context.structureTemplateManager(),
				id -> settings(Rotation.valueOf(tag.getStringOr("Rot", Rotation.NONE.name()))));
		this.rotation = Rotation.valueOf(tag.getStringOr("Rot", Rotation.NONE.name()));
		this.sealedDoors = tag.getIntOr("Sealed", 0);
		this.shapedHeight = tag.getIntOr("Shaped", 0);
		cutBox();
	}

	private void cutBox() {
		if (shapedHeight <= 0) return;
		boundingBox = new BoundingBox(boundingBox.minX(), boundingBox.minY(), boundingBox.minZ(),
				boundingBox.maxX(), Math.min(boundingBox.maxY(), boundingBox.minY() + shapedHeight - 1), boundingBox.maxZ());
	}

	private static StructurePlaceSettings settings(Rotation rotation) {
		return new StructurePlaceSettings()
				.setRotation(rotation)
				.setRotationPivot(PIVOT)
				.addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
				.addProcessor(CryptMarkers.INSTANCE);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		super.addAdditionalSaveData(context, tag);
		tag.putString("Rot", rotation.name());
		tag.putInt("Sealed", sealedDoors);
		tag.putInt("Shaped", shapedHeight);
	}

	@Override
	protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox box, ChunkPos chunk, BlockPos reference) {
		super.postProcess(level, structures, generator, random, box, chunk, reference);
		BlockPos min = templatePosition;
		for (int direction : new int[] {CryptLayout.NORTH, CryptLayout.EAST, CryptLayout.SOUTH, CryptLayout.WEST}) {
			if ((sealedDoors & direction) == 0) continue;
			for (int along = CryptModule.DOOR_FROM; along < CryptModule.DOOR_FROM + CryptModule.DOOR_WIDTH; along++) {
				for (int y = 1; y <= CryptModule.DOOR_HEIGHT; y++) {
					int last = CryptModule.CELL - 1;
					BlockPos pos = switch (direction) {
						case CryptLayout.NORTH -> min.offset(along, y, 0);
						case CryptLayout.SOUTH -> min.offset(along, y, last);
						case CryptLayout.WEST -> min.offset(0, y, along);
						default -> min.offset(last, y, along);
					};
					if (!box.isInside(pos)) continue;
					level.setBlock(pos, (random.nextInt(4) == 0 ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS).defaultBlockState(), 2);
				}
			}
		}
	}
}
