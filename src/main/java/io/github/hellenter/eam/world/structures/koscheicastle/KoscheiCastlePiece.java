package io.github.hellenter.eam.world.structures.koscheicastle;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.mobs.entity.koschei.Koschei;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
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
 * Maxim's castle template, turned about the middle of its floor. Its bottom layer, the foot of the plinth, stands one
 * block above the ground. Once the chunk with the crystal is placed, Koschei is put in it, sealed until a player comes
 * up to the crystal.
 */
public class KoscheiCastlePiece extends TemplateStructurePiece {
	private final Rotation rotation;

	public KoscheiCastlePiece(StructureTemplateManager manager, BlockPos corner, Rotation rotation) {
		super(KoscheiCastleModule.PIECE, 0, manager, KoscheiCastleModule.TEMPLATE, KoscheiCastleModule.TEMPLATE.toString(),
				settings(rotation, pivot(manager.getOrCreate(KoscheiCastleModule.TEMPLATE))), corner);
		this.rotation = rotation;
	}

	public KoscheiCastlePiece(StructurePieceSerializationContext context, CompoundTag tag) {
		super(KoscheiCastleModule.PIECE, tag, context.structureTemplateManager(),
				id -> settings(rotation(tag), pivot(context.structureTemplateManager().getOrCreate(id))));
		this.rotation = rotation(tag);
	}

	private static Rotation rotation(CompoundTag tag) {
		return Rotation.valueOf(tag.getStringOr("Rot", Rotation.NONE.name()));
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
				// Already shaped in the template; reshaping chunk by chunk would break the walls and chains split across chunks.
				.setKnownShape(true)
				.addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
	}

	/** The whole template's box. */
	public BoundingBox templateBox() {
		return template.getBoundingBox(placeSettings, templatePosition);
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
		BlockPos crystal = StructureTemplate.calculateRelativePosition(placeSettings, KoscheiCastleModule.CRYSTAL).offset(templatePosition);
		if (box.isInside(crystal)) spawnKoschei(level, crystal);
	}

	/** Koschei, sealed in his crystal, facing the template's north. */
	private void spawnKoschei(WorldGenLevel level, BlockPos crystal) {
		Koschei koschei = EamEntities.KOSCHEI.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (koschei == null) return;
		koschei.setPersistenceRequired();
		float yaw = rotation.rotate(Direction.NORTH).toYRot();
		koschei.snapTo(crystal.getX() + 0.5, crystal.getY(), crystal.getZ() + 0.5, yaw, 0.0F);
		koschei.setYHeadRot(yaw);
		koschei.setYBodyRot(yaw);
		koschei.finalizeSpawn(level, level.getCurrentDifficultyAt(crystal), EntitySpawnReason.STRUCTURE, null);
		koschei.sealInCrystal();
		level.addFreshEntityWithPassengers(koschei);
	}
}
