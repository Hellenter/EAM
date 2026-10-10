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
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * One crypt room: a square template turned about its middle, so it keeps its footprint, with the doors the layout does
 * not use walled up after it is placed. Stairs have doors on two levels.
 */
public class CryptRoomPiece extends TemplateStructurePiece {
	private final Rotation rotation;
	/** Doors to wall up, as a {@link CryptLayout} mask in world directions. */
	private final int sealedDoors;
	/** Doors to wall up on the upper level of stairs. */
	private final int sealedUpperDoors;

	/**
	 * How many layers from the bottom the piece reports as its box, or 0 for all of them. The world shapes terrain
	 * around a piece's box to wall the crypt in, so the entrance reports only its deep part to keep its surface
	 * building from being buried.
	 */
	private final int shapedHeight;

	public CryptRoomPiece(StructureTemplateManager manager, Identifier template, BlockPos position, Rotation rotation,
			int sealedDoors, int sealedUpperDoors, int shapedHeight) {
		super(CryptModule.ROOM_PIECE, 0, manager, template, template.toString(), settings(rotation, pivot(manager, template)), position);
		this.rotation = rotation;
		this.sealedDoors = sealedDoors;
		this.sealedUpperDoors = sealedUpperDoors;
		this.shapedHeight = shapedHeight;
		cutBox();
	}

	public CryptRoomPiece(StructurePieceSerializationContext context, CompoundTag tag) {
		super(CryptModule.ROOM_PIECE, tag, context.structureTemplateManager(),
				id -> settings(Rotation.valueOf(tag.getStringOr("Rot", Rotation.NONE.name())), pivot(context.structureTemplateManager(), id)));
		this.rotation = Rotation.valueOf(tag.getStringOr("Rot", Rotation.NONE.name()));
		this.sealedDoors = tag.getIntOr("Sealed", 0);
		this.sealedUpperDoors = tag.getIntOr("SealedUp", 0);
		this.shapedHeight = tag.getIntOr("Shaped", 0);
		cutBox();
	}

	/** The middle of a square template's floor, which it turns about. */
	private static BlockPos pivot(StructureTemplateManager manager, Identifier template) {
		int size = manager.getOrCreate(template).getSize().getX();
		return new BlockPos(size / 2, 0, size / 2);
	}

	private boolean isBoss() {
		return templateName.startsWith(CryptModule.BOSS_TEMPLATES);
	}

	private void cutBox() {
		if (shapedHeight <= 0) return;
		boundingBox = new BoundingBox(boundingBox.minX(), boundingBox.minY(), boundingBox.minZ(),
				boundingBox.maxX(), Math.min(boundingBox.maxY(), boundingBox.minY() + shapedHeight - 1), boundingBox.maxZ());
	}

	private static StructurePlaceSettings settings(Rotation rotation, BlockPos pivot) {
		return new StructurePlaceSettings()
				.setRotation(rotation)
				.setRotationPivot(pivot)
				// Underground water would otherwise soak the room's slabs, bars and lanterns and flood it from inside.
				.setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING)
				.addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
				.addProcessor(CryptMarkers.INSTANCE);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		super.addAdditionalSaveData(context, tag);
		tag.putString("Rot", rotation.name());
		tag.putInt("Sealed", sealedDoors);
		tag.putInt("SealedUp", sealedUpperDoors);
		tag.putInt("Shaped", shapedHeight);
	}

	@Override
	protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox box, ChunkPos chunk, BlockPos reference) {
		super.postProcess(level, structures, generator, random, box, chunk, reference);
		seal(level, random, box, sealedDoors, 0);
		// The sarcophagus is in the middle of the room, which turns about it.
		BlockPos sarcophagus = templatePosition.offset(CryptBoss.SARCOPHAGUS);
		if (isBoss() && box.isInside(sarcophagus)) CryptBoss.placeMarker(level, sarcophagus);
		seal(level, random, box, sealedUpperDoors, CryptModule.LEVEL_HEIGHT);
	}

	private void seal(WorldGenLevel level, RandomSource random, BoundingBox box, int doors, int floor) {
		int size = template.getSize().getX();
		int last = size - 1;
		int from = CryptModule.doorFrom(size);
		for (int direction : new int[] {CryptLayout.NORTH, CryptLayout.EAST, CryptLayout.SOUTH, CryptLayout.WEST}) {
			if ((doors & direction) == 0) continue;
			for (int along = from; along < from + CryptModule.DOOR_WIDTH; along++) {
				for (int y = floor + 1; y <= floor + CryptModule.DOOR_HEIGHT; y++) {
					BlockPos pos = switch (direction) {
						case CryptLayout.NORTH -> templatePosition.offset(along, y, 0);
						case CryptLayout.SOUTH -> templatePosition.offset(along, y, last);
						case CryptLayout.WEST -> templatePosition.offset(0, y, along);
						default -> templatePosition.offset(last, y, along);
					};
					if (!box.isInside(pos)) continue;
					level.setBlock(pos, (random.nextInt(4) == 0 ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS).defaultBlockState(), 2);
				}
			}
		}
	}
}
