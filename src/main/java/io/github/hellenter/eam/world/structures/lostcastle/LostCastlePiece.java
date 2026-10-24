package io.github.hellenter.eam.world.structures.lostcastle;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.mobs.entity.lost.Lost;
import io.github.hellenter.eam.mobs.entity.lost.LostRole;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Maxim's Lost castle template, turned about the middle of its floor. After placing its blocks it puts the garrison in
 * where the wool markers were: red the centurion, orange a longbowman, yellow a plain Lost, blue a riderless warhorse.
 * The whole garrison shares one squad id, so one of them seeing the player rouses the others near him; each keeps near
 * his post.
 */
public class LostCastlePiece extends TemplateStructurePiece {
	/** How far one of the garrison strays from his post when no one is fighting him. */
	private static final int POST_RADIUS = 12;
	/** The wool mob markers of each loaded template, found once. */
	private static final Map<StructureTemplate, List<Marker>> MARKERS = Collections.synchronizedMap(new WeakHashMap<>());

	private enum Kind {
		CENTURION, LONGBOWMAN, LOST, HORSE
	}

	private record Marker(BlockPos pos, Kind kind) {
	}

	private static final Map<Block, Kind> WOOL = Map.of(
			Blocks.WOOL.red(), Kind.CENTURION,
			Blocks.WOOL.orange(), Kind.LONGBOWMAN,
			Blocks.WOOL.yellow(), Kind.LOST,
			Blocks.WOOL.blue(), Kind.HORSE);

	private final Rotation rotation;

	public LostCastlePiece(StructureTemplateManager manager, BlockPos corner, Rotation rotation) {
		super(LostCastleModule.PIECE, 0, manager, LostCastleModule.TEMPLATE, LostCastleModule.TEMPLATE.toString(),
				settings(rotation, pivot(manager.getOrCreate(LostCastleModule.TEMPLATE))), corner);
		this.rotation = rotation;
	}

	public LostCastlePiece(StructurePieceSerializationContext context, CompoundTag tag) {
		super(LostCastleModule.PIECE, tag, context.structureTemplateManager(),
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
				// Already shaped in the template; reshaping chunk by chunk would break beds and doors split across chunks.
				.setKnownShape(true)
				.addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
				.addProcessor(LostCastleMarkers.INSTANCE);
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
		UUID garrison = UUID.nameUUIDFromBytes(("eam:lost_castle@" + templatePosition.asLong()).getBytes(StandardCharsets.UTF_8));
		for (Marker marker : MARKERS.computeIfAbsent(template, LostCastlePiece::findMarkers)) {
			BlockPos pos = StructureTemplate.calculateRelativePosition(placeSettings, marker.pos()).offset(templatePosition);
			if (!box.isInside(pos)) continue;
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			spawn(level, marker.kind(), pos, random, garrison);
		}
	}

	private static void spawn(WorldGenLevel level, Kind kind, BlockPos pos, RandomSource random, UUID garrison) {
		EntityType<?> type = switch (kind) {
			case CENTURION -> EamEntities.LOST_CENTURION;
			case LONGBOWMAN -> EamEntities.LOST_LONGBOWMAN;
			case LOST -> EamEntities.LOST;
			case HORSE -> EamEntities.LOST_WARHORSE;
		};
		SpawnGroupData data = switch (kind) {
			case CENTURION -> Lost.garrison(LostRole.CENTURION, garrison);
			case LONGBOWMAN -> Lost.garrison(LostRole.LONGBOWMAN, garrison);
			case LOST -> Lost.garrison(LostCastleLogic.garrisonRole(random.nextDouble()), garrison);
			case HORSE -> null;
		};
		if (!(type.create(level.getLevel(), EntitySpawnReason.STRUCTURE) instanceof Mob mob)) return;
		mob.setPersistenceRequired();
		mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, data);
		mob.setHomeTo(pos, POST_RADIUS);
		level.addFreshEntityWithPassengers(mob);
	}

	/** The wool of the template, in the template's own frame: every piece of it is a marker. */
	private static List<Marker> findMarkers(StructureTemplate template) {
		StructurePlaceSettings plain = new StructurePlaceSettings();
		List<Marker> markers = new ArrayList<>();
		for (Map.Entry<Block, Kind> wool : WOOL.entrySet()) {
			for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(BlockPos.ZERO, plain, wool.getKey())) {
				markers.add(new Marker(info.pos(), wool.getValue()));
			}
		}
		return List.copyOf(markers);
	}
}
