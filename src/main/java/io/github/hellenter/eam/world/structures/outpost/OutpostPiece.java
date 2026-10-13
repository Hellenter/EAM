package io.github.hellenter.eam.world.structures.outpost;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The outpost template, turned about its middle so it keeps its footprint. Its bottom {@link #GROUND} layers are the
 * cellar and the ground around it; the piece reports its box from the ground up, so the world blends the terrain into
 * the outpost's ground level rather than its cellar floor. After placing the template it puts the garrison in where
 * the wool markers were.
 */
public class OutpostPiece extends TemplateStructurePiece {
	/** The template layer that lies at ground level: its grass. */
	public static final int GROUND = 3;

	private final Rotation rotation;
	/** Where the template places logs of its own; worked out on first use. */
	private volatile Set<BlockPos> templateLogs;

	public OutpostPiece(StructureTemplateManager manager, BlockPos corner, Rotation rotation) {
		super(OutpostModule.PIECE, 0, manager, OutpostModule.TEMPLATE, OutpostModule.TEMPLATE.toString(),
				settings(rotation, pivot(manager.getOrCreate(OutpostModule.TEMPLATE))), corner);
		this.rotation = rotation;
		cutBox();
	}

	public OutpostPiece(StructurePieceSerializationContext context, CompoundTag tag) {
		super(OutpostModule.PIECE, tag, context.structureTemplateManager(),
				id -> settings(Rotation.valueOf(tag.getStringOr("Rot", Rotation.NONE.name())), pivot(context.structureTemplateManager().getOrCreate(id))));
		this.rotation = Rotation.valueOf(tag.getStringOr("Rot", Rotation.NONE.name()));
		cutBox();
	}

	/** The middle of the template's floor, which it turns about. */
	private static BlockPos pivot(StructureTemplate template) {
		return new BlockPos(template.getSize().getX() / 2, 0, template.getSize().getZ() / 2);
	}

	private static StructurePlaceSettings settings(Rotation rotation, BlockPos pivot) {
		return new StructurePlaceSettings()
				.setRotation(rotation)
				.setRotationPivot(pivot)
				.setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING)
				// The template's blocks are already shaped. Reshaping them chunk by chunk breaks a bed whose other half
				// lies in a chunk not placed yet.
				.setKnownShape(true)
				.addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
				.addProcessor(OutpostMarkers.INSTANCE);
	}

	/** The whole template's box, cellar included. */
	public BoundingBox templateBox() {
		return template.getBoundingBox(placeSettings, templatePosition);
	}

	/** Whether the template places a log of its own at {@code pos}, which the tree clearing must keep. */
	public boolean placesLog(BlockPos pos) {
		Set<BlockPos> logs = templateLogs;
		if (logs == null) {
			logs = new HashSet<>();
			for (Holder<Block> log : BuiltInRegistries.BLOCK.getTagOrEmpty(BlockTags.LOGS)) {
				// Relative positions: placing the template narrows the settings' box to the chunk being placed, and the
				// absolute lookup would then leave out every log outside that one chunk.
				for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(templatePosition, placeSettings, log.value(), false)) {
					logs.add(StructureTemplate.calculateRelativePosition(placeSettings, info.pos()).offset(templatePosition));
				}
			}
			templateLogs = logs;
		}
		return logs.contains(pos);
	}

	private void cutBox() {
		BoundingBox whole = templateBox();
		boundingBox = new BoundingBox(whole.minX(), whole.minY() + GROUND, whole.minZ(), whole.maxX(), whole.maxY(), whole.maxZ());
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
		// Placing the template resets the box to the whole template; chunks shaped later must still see the cut one.
		cutBox();
		for (Map.Entry<Block, EntityType<?>> marker : OutpostMarkers.MOBS.entrySet()) {
			for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(templatePosition, placeSettings, marker.getKey())) {
				if (box.isInside(info.pos())) spawn(level, marker.getValue(), info.pos(), random);
			}
		}
	}

	private static void spawn(WorldGenLevel level, EntityType<?> type, BlockPos pos, RandomSource random) {
		if (!(type.create(level.getLevel(), EntitySpawnReason.STRUCTURE) instanceof Mob mob)) return;
		mob.setPersistenceRequired();
		mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
		level.addFreshEntityWithPassengers(mob);
	}
}
