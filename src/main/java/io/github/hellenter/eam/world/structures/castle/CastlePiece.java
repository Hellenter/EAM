package io.github.hellenter.eam.world.structures.castle;

import io.github.hellenter.eam.mobs.entity.king.PillagerKing;
import io.github.hellenter.eam.world.structures.outpost.OutpostPiece;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * One tile of the castle template, turned about the middle of the whole castle so the tiles stay together. Its bottom
 * layer stands one block above the ground. After placing its blocks it puts the garrison in where the wool markers
 * were.
 */
public class CastlePiece extends TemplateStructurePiece {
	/** How far the King strays from his place in the hall when no one is fighting him. */
	private static final int KING_HOME_RADIUS = 12;
	/** The mob markers of each tile, found once per loaded template. */
	private static final Map<StructureTemplate, List<Marker>> MARKERS = Collections.synchronizedMap(new WeakHashMap<>());

	private record Marker(BlockPos pos, EntityType<?> type) {
	}

	private final Rotation rotation;
	private final BlockPos pivot;

	public CastlePiece(StructureTemplateManager manager, Identifier template, BlockPos position, Rotation rotation, BlockPos pivot) {
		super(CastleModule.PIECE, 0, manager, template, template.toString(), settings(rotation, pivot), position);
		this.rotation = rotation;
		this.pivot = pivot;
	}

	public CastlePiece(StructurePieceSerializationContext context, CompoundTag tag) {
		super(CastleModule.PIECE, tag, context.structureTemplateManager(), id -> settings(rotation(tag), pivot(tag)));
		this.rotation = rotation(tag);
		this.pivot = pivot(tag);
	}

	private static Rotation rotation(CompoundTag tag) {
		return Rotation.valueOf(tag.getStringOr("Rot", Rotation.NONE.name()));
	}

	private static BlockPos pivot(CompoundTag tag) {
		return new BlockPos(tag.getIntOr("PivotX", 0), tag.getIntOr("PivotY", 0), tag.getIntOr("PivotZ", 0));
	}

	private static StructurePlaceSettings settings(Rotation rotation, BlockPos pivot) {
		return new StructurePlaceSettings()
				.setRotation(rotation)
				.setRotationPivot(pivot)
				.setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING)
				// The template's blocks are already shaped. Reshaping them chunk by chunk breaks a bed or a door whose
				// other half lies in a chunk not placed yet.
				.setKnownShape(true)
				.addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
				.addProcessor(CastleMarkers.INSTANCE);
	}

	/** This tile's box. */
	public BoundingBox templateBox() {
		return template.getBoundingBox(placeSettings, templatePosition);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		super.addAdditionalSaveData(context, tag);
		tag.putString("Rot", rotation.name());
		tag.putInt("PivotX", pivot.getX());
		tag.putInt("PivotY", pivot.getY());
		tag.putInt("PivotZ", pivot.getZ());
	}

	@Override
	protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox box, ChunkPos chunk, BlockPos reference) {
		super.postProcess(level, structures, generator, random, box, chunk, reference);
		for (Marker marker : MARKERS.computeIfAbsent(template, CastlePiece::findMarkers)) {
			BlockPos pos = StructureTemplate.calculateRelativePosition(placeSettings, marker.pos()).offset(templatePosition);
			if (!box.isInside(pos)) continue;
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			Mob mob = OutpostPiece.spawn(level, marker.type(), pos, random);
			if (mob instanceof PillagerKing) mob.setHomeTo(pos, KING_HOME_RADIUS);
		}
	}

	/** The wool of the template that stands in the open, in the template's own frame. */
	private static List<Marker> findMarkers(StructureTemplate template) {
		StructurePlaceSettings plain = new StructurePlaceSettings();
		Set<BlockPos> open = new HashSet<>();
		List<Block> openBlocks = new ArrayList<>(Blocks.CARPET.asList());
		openBlocks.add(Blocks.AIR);
		openBlocks.add(Blocks.CAVE_AIR);
		for (Block block : openBlocks) {
			for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(BlockPos.ZERO, plain, block)) open.add(info.pos());
		}
		List<Marker> markers = new ArrayList<>();
		for (Map.Entry<Block, EntityType<?>> wool : CastleMarkers.MOBS.entrySet()) {
			for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(BlockPos.ZERO, plain, wool.getKey())) {
				if (CastleLogic.marksMob(info.pos(), open::contains)) markers.add(new Marker(info.pos(), wool.getValue()));
			}
		}
		return List.copyOf(markers);
	}
}
