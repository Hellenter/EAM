package io.github.hellenter.eam.world.structures.yagahut;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.mobs.entity.yaga.BabaYaga;
import io.github.hellenter.eam.world.structures.EmptyContainerLoot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
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
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Maxim's hut template, turned about its middle. Its bottom layer is the ground it stands on; its door faces the
 * template's north. Once the chunk with the spot in front of the door is placed, Baba Yaga is put there, waiting unseen
 * until a player comes up to the door.
 */
public class YagaHutPiece extends TemplateStructurePiece {
	/** Where Baba Yaga stands in the template: on the ground two blocks out from the steps before the door. */
	static final BlockPos YAGA_SPOT = new BlockPos(11, 1, 12);
	/** The lower half of the hut's own door in the template (not the shed's). */
	static final BlockPos HUT_DOOR = new BlockPos(11, 2, 16);

	private final Rotation rotation;

	public YagaHutPiece(StructureTemplateManager manager, BlockPos corner, Rotation rotation) {
		super(YagaHutModule.PIECE, 0, manager, YagaHutModule.TEMPLATE, YagaHutModule.TEMPLATE.toString(),
				settings(rotation, pivot(manager.getOrCreate(YagaHutModule.TEMPLATE))), corner);
		this.rotation = rotation;
	}

	public YagaHutPiece(StructurePieceSerializationContext context, CompoundTag tag) {
		super(YagaHutModule.PIECE, tag, context.structureTemplateManager(),
				id -> settings(Rotation.valueOf(tag.getStringOr("Rot", Rotation.NONE.name())), pivot(context.structureTemplateManager().getOrCreate(id))));
		this.rotation = Rotation.valueOf(tag.getStringOr("Rot", Rotation.NONE.name()));
	}

	/** The middle of the template's floor, which it turns about. */
	static BlockPos pivot(StructureTemplate template) {
		return new BlockPos(template.getSize().getX() / 2, 0, template.getSize().getZ() / 2);
	}

	private static StructurePlaceSettings settings(Rotation rotation, BlockPos pivot) {
		return new StructurePlaceSettings()
				.setRotation(rotation)
				.setRotationPivot(pivot)
				.setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING)
				// Already shaped in the template; reshaping chunk by chunk would break doors and chests split across chunks.
				.setKnownShape(true)
				.addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
				.addProcessor(new EmptyContainerLoot(Blocks.CHEST, YagaHutModule.CHEST_LOOT))
				.addProcessor(new EmptyContainerLoot(Blocks.BARREL, EmptyContainerLoot.LIGHT_POTIONS));
	}

	/** The whole template's box. */
	public BoundingBox templateBox() {
		return template.getBoundingBox(placeSettings, templatePosition);
	}

	/** The y of the template's ground layer. */
	public int groundY() {
		return templatePosition.getY();
	}

	/** The middle of the template's footprint, on its ground layer. */
	public BlockPos middle() {
		return templatePosition.offset(pivot(template));
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		super.addAdditionalSaveData(context, tag);
		tag.putString("Rot", rotation.name());
	}

	@Override
	protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox box, ChunkPos chunk, BlockPos reference) {
		super.postProcess(level, structures, generator, random, box, chunk, reference);
		BlockPos spot = StructureTemplate.calculateRelativePosition(placeSettings, YAGA_SPOT).offset(templatePosition);
		if (box.isInside(spot)) spawnYaga(level, spot, StructureTemplate.calculateRelativePosition(placeSettings, HUT_DOOR).offset(templatePosition));
	}

	/** Baba Yaga, in front of her door, facing out; unseen until a player comes up to the door. */
	private void spawnYaga(WorldGenLevel level, BlockPos spot, BlockPos door) {
		BabaYaga yaga = EamEntities.BABA_YAGA.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (yaga == null) return;
		yaga.setPersistenceRequired();
		float yaw = rotation.rotate(Direction.NORTH).toYRot();
		yaga.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, yaw, 0.0F);
		yaga.setYHeadRot(yaw);
		yaga.setYBodyRot(yaw);
		yaga.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), EntitySpawnReason.STRUCTURE, null);
		yaga.waitInHut(door);
		level.addFreshEntityWithPassengers(yaga);
	}
}
