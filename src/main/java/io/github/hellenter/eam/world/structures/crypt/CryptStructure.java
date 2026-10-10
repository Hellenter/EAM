package io.github.hellenter.eam.world.structures.crypt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.hellenter.eam.world.structures.VillageModule;
import io.github.hellenter.eam.world.structures.crypt.CryptLayout.Bounds;
import io.github.hellenter.eam.world.structures.crypt.CryptLayout.Cell;
import io.github.hellenter.eam.world.structures.crypt.CryptLayout.Fit;
import io.github.hellenter.eam.world.structures.crypt.CryptLayout.Piece;
import io.github.hellenter.eam.world.structures.crypt.CryptLayout.Role;
import io.github.hellenter.eam.world.structures.crypt.CryptLayout.Room;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntBinaryOperator;

/**
 * A crypt: an entrance on the surface whose stairs lead down to the first level of rooms laid out by
 * {@link CryptLayout}; each level's stairs lead to the next, and the last ends in the boss room. The rooms are Maxim's
 * templates under {@code eam:crypt/}, turned to fit their cells.
 *
 * @param rooms          how many rooms the main path of each level tries to have
 * @param branches       how many side passages each level tries to have
 * @param entranceGround the entrance template's layer that sits at ground level
 * @param deepRooms      room templates that only appear from the second level down
 * @param levels         how many levels the crypt has
 * @param bigRooms       how many big rooms the crypt has, on different levels while there are enough
 * @param tier2Treasuries the most treasuries with a tier-2 chest each level may have, from the top; levels past the
 *                       end of the list have no limit
 * @param village        when present, the crypt only generates by an abandoned village
 * @param unusedPieces   templates the crypt leaves out for now, of any kind
 */
public class CryptStructure extends Structure {
	public static final MapCodec<CryptStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			settingsCodec(instance),
			IntProviders.codec(2, 32).fieldOf("rooms").forGetter(crypt -> crypt.rooms),
			IntProviders.codec(0, 8).fieldOf("branches").forGetter(crypt -> crypt.branches),
			Codec.INT.fieldOf("entrance_ground").forGetter(crypt -> crypt.entranceGround),
			Identifier.CODEC.listOf().optionalFieldOf("deep_rooms", List.of()).forGetter(crypt -> crypt.deepRooms),
			Codec.intRange(1, 4).optionalFieldOf("levels", 2).forGetter(crypt -> crypt.levels),
			IntProviders.codec(0, 8).optionalFieldOf("big_rooms", ConstantInt.of(0)).forGetter(crypt -> crypt.bigRooms),
			ExtraCodecs.NON_NEGATIVE_INT.listOf().optionalFieldOf("max_tier_2_treasuries", List.of()).forGetter(crypt -> crypt.tier2Treasuries),
			VillageRule.CODEC.optionalFieldOf("village").forGetter(crypt -> crypt.village),
			Identifier.CODEC.listOf().optionalFieldOf("unused_pieces", List.of()).forGetter(crypt -> crypt.unusedPieces)
	).apply(instance, CryptStructure::new));

	/**
	 * How far above a piece's box the world still raises terrain to wall the crypt in. Rooms near the surface report a
	 * lower box, or the ground above them would swell into mounds the shape of the crypt.
	 */
	private static final int SHAPE_REACH = 12;
	/** Pieces must stay within this many chunks of the chunk the crypt starts in, or the world never places them. */
	private static final int CHUNK_REACH = 8;
	/** How far the level may spread sideways and north at most, in cells, however much room the chunks leave. */
	private static final int MAX_SIDEWAYS = 6;
	private static final int MAX_NORTH = 8;
	/** How far from the village's middle the entrance may stand, in blocks, and the step the search takes. */
	private static final int ENTRANCE_SEARCH = 48;
	private static final int ENTRANCE_STEP = 8;
	/** Free ground the entrance keeps around it from village buildings and streets, in blocks. */
	private static final int ENTRANCE_MARGIN = 3;
	/** How uneven the ground under the entrance may be, in blocks. */
	private static final int ENTRANCE_SLOPE = 4;

	/**
	 * Where crypts generate: by abandoned villages only, with {@code chanceBeforeIronAge} before a player of the world
	 * earned the iron age (all villages are abandoned then) and {@code chanceAfterIronAge} after. After the iron age,
	 * {@code abandonedAfterIronAge} of the villages still generate abandoned, so crypts keep turning up.
	 */
	public record VillageRule(float chanceBeforeIronAge, float chanceAfterIronAge, float abandonedAfterIronAge) {
		public static final Codec<VillageRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0, 1).fieldOf("chance_before_iron_age").forGetter(VillageRule::chanceBeforeIronAge),
				Codec.floatRange(0, 1).fieldOf("chance_after_iron_age").forGetter(VillageRule::chanceAfterIronAge),
				Codec.floatRange(0, 1).fieldOf("abandoned_villages_after_iron_age").forGetter(VillageRule::abandonedAfterIronAge)
		).apply(instance, VillageRule::new));
	}

	private final IntProvider rooms;
	private final IntProvider branches;
	private final int entranceGround;
	private final List<Identifier> deepRooms;
	private final int levels;
	private final IntProvider bigRooms;
	private final List<Integer> tier2Treasuries;
	private final Optional<VillageRule> village;
	private final List<Identifier> unusedPieces;

	public CryptStructure(StructureSettings settings, IntProvider rooms, IntProvider branches, int entranceGround,
			List<Identifier> deepRooms, int levels, IntProvider bigRooms, List<Integer> tier2Treasuries, Optional<VillageRule> village,
			List<Identifier> unusedPieces) {
		super(settings);
		this.rooms = rooms;
		this.branches = branches;
		this.entranceGround = entranceGround;
		this.deepRooms = deepRooms;
		this.levels = levels;
		this.bigRooms = bigRooms;
		this.tier2Treasuries = tier2Treasuries;
		this.village = village;
		this.unusedPieces = unusedPieces;
	}

	public Optional<VillageRule> village() {
		return village;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		CryptPieces pieces = CryptPieces.load(context.structureTemplateManager()).without(unusedPieces);
		if (pieces.entrances().isEmpty() || pieces.rooms().isEmpty()) return Optional.empty();
		ChunkPos chunk = context.chunkPos();
		RandomSource random = context.random();
		List<BoundingBox> buildings = List.of();
		if (village.isPresent()) {
			Optional<List<BoundingBox>> abandoned = CryptVillages.abandonedVillage(context);
			if (abandoned.isEmpty()) return Optional.empty();
			float chance = VillageModule.villagesUnlocked() ? village.get().chanceAfterIronAge() : village.get().chanceBeforeIronAge();
			if (random.nextFloat() >= chance) return Optional.empty();
			buildings = abandoned.get();
		}
		CryptPieces.Entrance entrance = pieces.entrances().get(random.nextInt(pieces.entrances().size()));
		Optional<BlockPos> ground = entranceSpot(context, entrance, buildings);
		if (ground.isEmpty()) return Optional.empty();
		BlockPos origin = ground.get().below(entranceGround);
		// Cell (0, 0) lies north of the entrance, its south door in line with the entrance's way out.
		BlockPos firstCell = origin.offset(entrance.exitX() - CryptModule.CELL / 2, 0, -CryptModule.CELL);
		int big = pieces.rooms().stream().anyMatch(piece -> piece.role() == Role.BIG) ? bigRooms.sample(random) : 0;
		// Without a stairs template the crypt keeps to its first level.
		int depth = pieces.stairs().isEmpty() ? 1 : levels;
		List<CryptLayout> layouts = CryptLayout.generate(random::nextInt, bounds(chunk, firstCell), depth, big,
				() -> rooms.sample(random), () -> branches.sample(random));
		if (layouts == null) return Optional.empty();
		IntBinaryOperator surface = (x, z) -> surface(context, x, z, Heightmap.Types.WORLD_SURFACE_WG);
		return Optional.of(new GenerationStub(origin, builder -> build(builder, context.structureTemplateManager(), pieces,
				entrance, origin, firstCell, layouts, surface, random)));
	}

	/**
	 * The ground in the middle of the entrance's way out, with the entrance's corner at its x and z. Next to a village
	 * the entrance stands on dry, fairly flat ground clear of the village's buildings and streets, as near its middle as
	 * that allows; otherwise it stands at the chunk's corner on dry land.
	 */
	private Optional<BlockPos> entranceSpot(GenerationContext context, CryptPieces.Entrance entrance, List<BoundingBox> buildings) {
		ChunkPos chunk = context.chunkPos();
		var size = context.structureTemplateManager().getOrCreate(entrance.id()).getSize();
		List<int[]> offsets = new ArrayList<>();
		if (village.isEmpty()) {
			offsets.add(new int[] {0, 0});
		} else {
			for (int dx = -ENTRANCE_SEARCH; dx <= ENTRANCE_SEARCH; dx += ENTRANCE_STEP) {
				for (int dz = -ENTRANCE_SEARCH; dz <= ENTRANCE_SEARCH; dz += ENTRANCE_STEP) offsets.add(new int[] {dx, dz});
			}
			offsets.sort(Comparator.comparingInt(offset -> offset[0] * offset[0] + offset[1] * offset[1]));
		}
		for (int[] offset : offsets) {
			int x = chunk.getMinBlockX() + offset[0];
			int z = chunk.getMinBlockZ() + offset[1];
			BoundingBox footprint = new BoundingBox(x - ENTRANCE_MARGIN, Integer.MIN_VALUE / 2, z - ENTRANCE_MARGIN,
					x + size.getX() - 1 + ENTRANCE_MARGIN, Integer.MAX_VALUE / 2, z + size.getZ() - 1 + ENTRANCE_MARGIN);
			if (buildings.stream().anyMatch(footprint::intersects)) continue;
			int exitX = x + entrance.exitX();
			int ground = surface(context, exitX, z, Heightmap.Types.WORLD_SURFACE_WG);
			// The entrance stands on dry land: under water the stairs would flood the whole crypt.
			if (surface(context, exitX, z, Heightmap.Types.OCEAN_FLOOR_WG) < ground) continue;
			if (village.isPresent() && !flat(context, x, z, size.getX(), size.getZ(), ground)) continue;
			return Optional.of(new BlockPos(x, ground, z));
		}
		return Optional.empty();
	}

	private static boolean flat(GenerationContext context, int x, int z, int width, int depth, int ground) {
		for (int[] corner : new int[][] {{0, 0}, {width - 1, 0}, {0, depth - 1}, {width - 1, depth - 1}}) {
			int height = surface(context, x + corner[0], z + corner[1], Heightmap.Types.WORLD_SURFACE_WG);
			if (Math.abs(height - ground) > ENTRANCE_SLOPE) return false;
		}
		return true;
	}

	private static int surface(GenerationContext context, int x, int z, Heightmap.Types heightmap) {
		return context.chunkGenerator().getFirstOccupiedHeight(x, z, heightmap, context.heightAccessor(), context.randomState());
	}

	/** The cells a level may use so every piece stays within {@link #CHUNK_REACH} chunks of the starting chunk. */
	private static Bounds bounds(ChunkPos chunk, BlockPos firstCell) {
		int west = chunk.getMinBlockX() - CHUNK_REACH * 16;
		int east = chunk.getMaxBlockX() + CHUNK_REACH * 16;
		int north = chunk.getMinBlockZ() - CHUNK_REACH * 16;
		int cell = CryptModule.CELL;
		return new Bounds(
				Math.max(-MAX_SIDEWAYS, Math.ceilDiv(west - firstCell.getX(), cell)),
				Math.min(MAX_SIDEWAYS, Math.floorDiv(east - (cell - 1) - firstCell.getX(), cell)),
				Math.max(-MAX_NORTH, Math.ceilDiv(north - firstCell.getZ(), cell)),
				0);
	}

	private void build(StructurePiecesBuilder builder, StructureTemplateManager manager, CryptPieces pieces,
			CryptPieces.Entrance entrance, BlockPos origin, BlockPos firstCell, List<CryptLayout> levels, IntBinaryOperator surface,
			RandomSource random) {
		builder.addPiece(new CryptRoomPiece(manager, entrance.id(), origin, Rotation.NONE, 0, 0, Math.max(1, entranceGround - SHAPE_REACH)));
		int depth = levels.size();
		for (int level = 0; level < depth; level++) {
			CryptLayout layout = levels.get(level);
			boolean deep = level > 0;
			List<Piece<Identifier>> templates = pieces.rooms().stream().filter(piece -> deep || !deepRooms.contains(piece.id())).toList();
			int tier2Left = level < tier2Treasuries.size() ? tier2Treasuries.get(level) : Integer.MAX_VALUE;
			BlockPos levelCorner = firstCell.below(level * CryptModule.LEVEL_HEIGHT);
			for (Map.Entry<Cell, Room> entry : layout.rooms().entrySet()) {
				Cell cell = entry.getKey();
				Room room = entry.getValue();
				BlockPos position = levelCorner.offset(cell.x() * CryptModule.CELL, 0, cell.z() * CryptModule.CELL);
				Role role = room.role();
				// Big rooms are put in whole below; the stairs are one piece two levels high, put in from the level above.
				if (role == Role.BIG || cell.equals(layout.up())) continue;
				if (cell.equals(layout.down())) {
					addStairs(builder, manager, pieces, position.below(CryptModule.LEVEL_HEIGHT),
							levels.get(level + 1).rooms().get(cell).doors(), room.doors(), surface, random);
					continue;
				}
				List<Piece<Identifier>> choice = templates;
				if (role == Role.TREASURY && tier2Left <= 0) {
					choice = templates.stream().filter(piece -> !pieces.tier2Treasuries().contains(piece.id())).toList();
				}
				Fit<Identifier> fit = CryptLayout.fit(choice, role, room.doors(), random::nextInt);
				if (fit == null) continue;
				if (role == Role.TREASURY && pieces.tier2Treasuries().contains(fit.id())) tier2Left--;
				builder.addPiece(new CryptRoomPiece(manager, fit.id(), position, Rotation.values()[fit.quarters()], fit.extraDoors(), 0,
						shapedHeight(manager, fit.id(), position, surface)));
			}
			for (CryptLayout.Big big : layout.bigs()) {
				Fit<Identifier> fit = CryptLayout.fit(templates, Role.BIG, big.doors(layout.rooms()), random::nextInt);
				if (fit == null) continue;
				BlockPos position = levelCorner.offset(big.corner().x() * CryptModule.CELL, 0, big.corner().z() * CryptModule.CELL);
				builder.addPiece(new CryptRoomPiece(manager, fit.id(), position, Rotation.values()[fit.quarters()], fit.extraDoors(), 0,
						shapedHeight(manager, fit.id(), position, surface)));
			}
			CryptLayout.Boss boss = layout.boss();
			if (boss != null && !pieces.bosses().isEmpty()) {
				Identifier id = pieces.bosses().get(random.nextInt(pieces.bosses().size()));
				BlockPos position = levelCorner.offset((boss.middle().x() - 1) * CryptModule.CELL, 0, (boss.middle().z() - 1) * CryptModule.CELL);
				builder.addPiece(new CryptRoomPiece(manager, id, position, Rotation.values()[random.nextInt(4)], ~boss.door() & 0xF, 0,
						shapedHeight(manager, id, position, surface)));
			}
		}
	}

	/**
	 * How many layers of a room at {@code corner} the world shapes terrain around, so it stays {@link #SHAPE_REACH}
	 * below the lowest ground over the room: 0 for all of them.
	 */
	private static int shapedHeight(StructureTemplateManager manager, Identifier template, BlockPos corner, IntBinaryOperator surface) {
		var size = manager.getOrCreate(template).getSize();
		int last = size.getX() - 1;
		int ground = Integer.MAX_VALUE;
		for (int[] spot : new int[][] {{0, 0}, {last, 0}, {0, last}, {last, last}, {last / 2, last / 2}}) {
			ground = Math.min(ground, surface.applyAsInt(corner.getX() + spot[0], corner.getZ() + spot[1]));
		}
		int layers = ground - SHAPE_REACH - corner.getY();
		return layers >= size.getY() ? 0 : Math.max(1, layers);
	}

	/** Puts in stairs turned so they have the doors both levels need, with the rest walled up. */
	private static void addStairs(StructurePiecesBuilder builder, StructureTemplateManager manager, CryptPieces pieces,
			BlockPos position, int lowerDoors, int upperDoors, IntBinaryOperator surface, RandomSource random) {
		CryptPieces.Stairs stairs = pieces.stairs().get(random.nextInt(pieces.stairs().size()));
		int first = random.nextInt(4);
		for (int k = 0; k < 4; k++) {
			int quarters = (first + k) % 4;
			int lower = CryptLayout.rotate(stairs.lowerDoors(), quarters);
			int upper = CryptLayout.rotate(stairs.upperDoors(), quarters);
			if ((lower & lowerDoors) != lowerDoors || (upper & upperDoors) != upperDoors) continue;
			builder.addPiece(new CryptRoomPiece(manager, stairs.id(), position, Rotation.values()[quarters],
					lower & ~lowerDoors, upper & ~upperDoors, shapedHeight(manager, stairs.id(), position, surface)));
			return;
		}
	}

	@Override
	public StructureType<?> type() {
		return CryptModule.CRYPT;
	}
}
