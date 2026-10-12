package io.github.hellenter.eam.dimension;

import io.github.hellenter.eam.dimension.UnderworldLogic.Place;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Comparator;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

/**
 * Nether portals with the underworld: the overworld and the underworld's upper level lead to each other, the lower level
 * and the nether lead to each other ({@link UnderworldLogic#portalTarget}). In the underworld a portal is looked for and
 * built only inside the level the traveller is headed for, never across the deep bedrock.
 */
public final class UnderworldPortals {
	private static final int PORTAL_SEARCH_RADIUS = 16;

	private static volatile boolean enabled;

	private UnderworldPortals() {
	}

	public static void enable() {
		enabled = true;
	}

	public static boolean enabled() {
		return enabled;
	}

	/** Where a traveller at {@code y} in {@code level} stands. */
	public static Place place(Level level, int y) {
		if (level.dimension() == Level.OVERWORLD) {
			return Place.OVERWORLD;
		}
		if (level.dimension() == Level.NETHER) {
			return Place.NETHER;
		}
		if (UnderworldDimension.is(level)) {
			return UnderworldLogic.underworldLevel(y);
		}
		return Place.OTHER;
	}

	/** The level a portal at {@code portalPos} leads to, or null to keep vanilla's choice. */
	public static @Nullable ServerLevel targetLevel(ServerLevel current, BlockPos portalPos) {
		if (!enabled) {
			return null;
		}
		Place target = UnderworldLogic.portalTarget(place(current, portalPos.getY()));
		return switch (target) {
			case OVERWORLD -> current.getServer().getLevel(Level.OVERWORLD);
			case NETHER -> current.getServer().getLevel(Level.NETHER);
			case UNDERWORLD_UPPER, UNDERWORLD_LOWER -> current.getServer().getLevel(UnderworldDimension.LEVEL);
			case OTHER -> null;
		};
	}

	/** Roughly where the traveller comes out: scaled like vanilla, at their own height kept inside the target level. */
	public static BlockPos approximateExit(ServerLevel current, ServerLevel target, Entity entity, BlockPos portalPos) {
		double scale = DimensionType.getTeleportationScale(current.dimensionType(), target.dimensionType());
		Place to = UnderworldLogic.portalTarget(place(current, portalPos.getY()));
		int y = UnderworldLogic.arrivalY(to, Mth.floor(entity.getY()));
		return target.getWorldBorder().clampToBounds(entity.getX() * scale, y, entity.getZ() * scale);
	}

	/** Vanilla's portal search, keeping only portals in the same underworld level as the search point. */
	public static Optional<BlockPos> findClosestPortal(ServerLevel level, BlockPos approximateExitPos, boolean toNether, WorldBorder worldBorder) {
		Place wanted = UnderworldLogic.underworldLevel(approximateExitPos.getY());
		PoiManager poiManager = level.getPoiManager();
		// The underworld is compressed 1:8 against the overworld, like the nether, so it searches as near as the nether does.
		int radius = PORTAL_SEARCH_RADIUS;
		poiManager.ensureLoadedAndValid(level, approximateExitPos, radius);
		return poiManager.getInSquare(type -> type.is(PoiTypes.NETHER_PORTAL), approximateExitPos, radius, PoiManager.Occupancy.ANY)
				.map(PoiRecord::getPos)
				.filter(pos -> UnderworldLogic.inLevel(wanted, pos.getY()))
				.filter(worldBorder::isWithinBounds)
				.filter(pos -> level.getBlockState(pos).hasProperty(BlockStateProperties.HORIZONTAL_AXIS))
				.min(Comparator.<BlockPos>comparingDouble(p -> p.distSqr(approximateExitPos)).thenComparingInt(Vec3i::getY));
	}

	/** Whether this mod builds the exit portals in {@code level} rather than vanilla. */
	public static boolean buildsPortalsIn(ServerLevel level) {
		if (UnderworldDimension.is(level)) {
			return true;
		}
		return enabled && (level.dimension() == Level.OVERWORLD || level.dimension() == Level.NETHER);
	}

	/**
	 * Builds the exit portal at the nearest safe spot to {@code origin}: solid ground (not powder snow, magma, leaves, a giant
	 * mushroom or a thin shelf over a drop) wide enough for the frame, four blocks of air above it and no lava next to it. In the overworld only the open surface
	 * counts, never a cave. The search widens ring by ring ({@link UnderworldLogic#PORTAL_SEARCH_RADII}); only when no spot
	 * is found at all does it make one: a platform on the floor, fluid or surface below the target, or a chamber when the
	 * target is buried in solid rock.
	 */
	public static Optional<BlockUtil.FoundRectangle> createPortal(ServerLevel level, BlockPos origin, Direction.Axis portalAxis) {
		Bounds bounds = bounds(level, origin);
		Direction direction = Direction.get(Direction.AxisDirection.POSITIVE, portalAxis);
		BlockPos.MutableBlockPos mutable = origin.mutable();

		BlockPos spot = null;
		int searched = -1;
		for (int radius : UnderworldLogic.PORTAL_SEARCH_RADII) {
			spot = findSpot(level, origin, direction, bounds, searched, radius);
			if (spot != null) {
				break;
			}
			searched = radius;
		}
		if (spot == null) {
			spot = makeSpot(level, origin, direction, bounds, mutable);
		}

		for (int width = -1; width < 3; width++) {
			for (int height = -1; height < 4; height++) {
				if (width == -1 || width == 2 || height == -1 || height == 3) {
					mutable.setWithOffset(spot, width * direction.getStepX(), height, width * direction.getStepZ());
					level.setBlockAndUpdate(mutable, Blocks.OBSIDIAN.defaultBlockState());
				}
			}
		}

		BlockState portalState = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, portalAxis);
		for (int width = 0; width < 2; width++) {
			for (int height = 0; height < 3; height++) {
				mutable.setWithOffset(spot, width * direction.getStepX(), height, width * direction.getStepZ());
				level.setBlock(mutable, portalState, 18);
			}
		}
		return Optional.of(new BlockUtil.FoundRectangle(spot.immutable(), 2, 3));
	}

	/**
	 * The heights a portal's lowest inner block may take in {@code level}, and whether it must stand on the open surface.
	 * In the underworld it stays in the level of {@code origin}, never across the deep bedrock.
	 */
	private record Bounds(int minY, int maxY, boolean surfaceOnly) {
	}

	private static Bounds bounds(ServerLevel level, BlockPos origin) {
		if (UnderworldDimension.is(level)) {
			Place target = UnderworldLogic.underworldLevel(origin.getY());
			return new Bounds(UnderworldLogic.portalMinY(target), UnderworldLogic.portalMaxY(target), false);
		}
		int top = Math.min(level.getMaxY(), level.getMinY() + level.getLogicalHeight() - 1);
		if (level.dimension() == Level.NETHER) {
			// Stay under the bedrock roof.
			return new Bounds(level.getMinY() + 1, top - 9, false);
		}
		return new Bounds(level.getMinY() + 1, top - 4, true);
	}

	/**
	 * The closest safe spot to {@code origin} in the columns between {@code innerRadius} (exclusive, -1 for none) and
	 * {@code radius}: a spot with room to step out on both sides of the portal wins over one without, as in vanilla.
	 */
	private static @Nullable BlockPos findSpot(ServerLevel level, BlockPos origin, Direction direction, Bounds bounds, int innerRadius,
			int radius) {
		double closestFullDistanceSqr = -1.0;
		BlockPos closestFullPosition = null;
		double closestPartialDistanceSqr = -1.0;
		BlockPos closestPartialPosition = null;
		WorldBorder worldBorder = level.getWorldBorder();
		BlockPos.MutableBlockPos mutable = origin.mutable();

		for (BlockPos.MutableBlockPos columnPos : BlockPos.spiralAround(origin, radius, Direction.EAST, Direction.SOUTH)) {
			if (Math.max(Math.abs(columnPos.getX() - origin.getX()), Math.abs(columnPos.getZ() - origin.getZ())) <= innerRadius) {
				continue;
			}
			if (!worldBorder.isWithinBounds(columnPos) || !worldBorder.isWithinBounds(columnPos.move(direction, 1))) {
				continue;
			}
			columnPos.move(direction.getOpposite(), 1);
			int top;
			int bottom;
			if (bounds.surfaceOnly()) {
				// Only the first block above the surface: water, leaves and caves never count.
				top = height(level, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, columnPos);
				bottom = top;
				if (top > bounds.maxY() || top < bounds.minY()) {
					continue;
				}
			} else {
				top = Math.min(bounds.maxY() + 4, height(level, Heightmap.Types.MOTION_BLOCKING, columnPos));
				bottom = bounds.minY();
			}
			for (int y = top; y >= bottom; y--) {
				columnPos.setY(y);
				if (!canPortalReplaceBlock(level, columnPos)) {
					continue;
				}
				int firstEmptyY = y;
				while (y > bounds.minY() && canPortalReplaceBlock(level, columnPos.move(Direction.DOWN))) {
					y--;
				}
				if (y > bounds.maxY() || (bounds.surfaceOnly() && y != top)) {
					continue;
				}
				int deltaY = firstEmptyY - y;
				if (deltaY > 0 && deltaY < 3) {
					continue;
				}
				columnPos.setY(y);
				if (!canHostFrame(level, columnPos, mutable, direction, 0) || !grounded(level, columnPos, mutable, direction)
						|| nearLava(level, columnPos, mutable, direction)) {
					continue;
				}
				double distance = origin.distSqr(columnPos);
				if (canHostFrame(level, columnPos, mutable, direction, -1) && canHostFrame(level, columnPos, mutable, direction, 1)
						&& (closestFullDistanceSqr == -1.0 || closestFullDistanceSqr > distance)) {
					closestFullDistanceSqr = distance;
					closestFullPosition = columnPos.immutable();
				}
				if (closestFullDistanceSqr == -1.0 && (closestPartialDistanceSqr == -1.0 || closestPartialDistanceSqr > distance)) {
					closestPartialDistanceSqr = distance;
					closestPartialPosition = columnPos.immutable();
				}
			}
		}
		return closestFullPosition != null ? closestFullPosition : closestPartialPosition;
	}

	/**
	 * No safe spot anywhere near: make one at the target column. The portal stands on whatever lies under the target (the
	 * surface in the overworld, else the floor or a lava or water surface below), on an obsidian platform with air around.
	 * Only a target buried in solid rock with no open space in its column gets a carved chamber.
	 */
	private static BlockPos makeSpot(ServerLevel level, BlockPos origin, Direction direction, Bounds bounds, BlockPos.MutableBlockPos mutable) {
		WorldBorder worldBorder = level.getWorldBorder();
		BlockPos column = worldBorder.clampToBounds(new BlockPos(origin.getX() - direction.getStepX(), origin.getY(), origin.getZ() - direction.getStepZ()));
		int y;
		if (bounds.surfaceOnly()) {
			y = height(level, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
		} else {
			y = openSpaceNear(level, column, bounds, mutable);
		}
		BlockPos spot = column.atY(Mth.clamp(y, bounds.minY() + 1, bounds.maxY()));

		Direction clockWise = direction.getClockWise();
		for (int box = -1; box < 2; box++) {
			for (int width = 0; width < 2; width++) {
				for (int height = -1; height < 3; height++) {
					BlockState state = height < 0 ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState();
					mutable.setWithOffset(spot, width * direction.getStepX() + box * clockWise.getStepX(), height,
							width * direction.getStepZ() + box * clockWise.getStepZ());
					level.setBlockAndUpdate(mutable, state);
				}
			}
		}
		return spot;
	}

	/** The lowest open block of the open space closest to {@code column}'s height, or that height when the column is solid. */
	private static int openSpaceNear(ServerLevel level, BlockPos column, Bounds bounds, BlockPos.MutableBlockPos mutable) {
		int start = Mth.clamp(column.getY(), bounds.minY() + 1, bounds.maxY());
		for (int offset = 0; offset <= bounds.maxY() - bounds.minY(); offset++) {
			for (int y : new int[] {start - offset, start + offset}) {
				if (y <= bounds.minY() || y > bounds.maxY() + 3) {
					continue;
				}
				mutable.set(column.getX(), y, column.getZ());
				if (canPortalReplaceBlock(level, mutable)) {
					while (y > bounds.minY() + 1 && canPortalReplaceBlock(level, mutable.move(Direction.DOWN))) {
						y--;
					}
					return y;
				}
			}
		}
		return start;
	}

	/**
	 * The first block above the column's top, loading the chunk first: {@link Level#getHeight} reports the bottom of the
	 * world for a chunk that is not loaded yet, which hid every column past the few chunks around the arrival.
	 */
	private static int height(ServerLevel level, Heightmap.Types type, BlockPos column) {
		return level.getChunk(column).getHeight(type, column.getX() & 15, column.getZ() & 15) + 1;
	}

	private static boolean canPortalReplaceBlock(ServerLevel level, BlockPos.MutableBlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.canBeReplaced() && state.getFluidState().isEmpty();
	}

	private static boolean isSafeFloor(BlockState state) {
		return state.isSolid() && state.getFluidState().isEmpty() && !state.is(Blocks.POWDER_SNOW) && !state.is(Blocks.MAGMA_BLOCK)
				&& !state.is(BlockTags.LEAVES) && !(state.getBlock() instanceof HugeMushroomBlock);
	}

	private static boolean canHostFrame(ServerLevel level, BlockPos origin, BlockPos.MutableBlockPos mutable, Direction direction, int offset) {
		Direction clockWise = direction.getClockWise();
		for (int width = -1; width < 3; width++) {
			for (int height = -1; height < 4; height++) {
				mutable.setWithOffset(origin, direction.getStepX() * width + clockWise.getStepX() * offset, height,
						direction.getStepZ() * width + clockWise.getStepZ() * offset);
				if (height < 0 && !isSafeFloor(level.getBlockState(mutable))) {
					return false;
				}
				if (height >= 0 && !canPortalReplaceBlock(level, mutable)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Whether the floor under the portal is ground rather than a thin shelf hanging in the air, like a giant mushroom cap
	 * or a tree's crown: the two blocks under the floor are not open either.
	 */
	private static boolean grounded(ServerLevel level, BlockPos origin, BlockPos.MutableBlockPos mutable, Direction direction) {
		for (int width = 0; width < 2; width++) {
			for (int height = -3; height < -1; height++) {
				mutable.setWithOffset(origin, direction.getStepX() * width, height, direction.getStepZ() * width);
				if (level.getBlockState(mutable).isAir()) {
					return false;
				}
			}
		}
		return true;
	}

	/** Whether lava touches the frame or the space the traveller steps out into. */
	private static boolean nearLava(ServerLevel level, BlockPos origin, BlockPos.MutableBlockPos mutable, Direction direction) {
		Direction clockWise = direction.getClockWise();
		for (int width = -2; width < 4; width++) {
			for (int side = -2; side < 3; side++) {
				for (int height = -1; height < 5; height++) {
					mutable.setWithOffset(origin, direction.getStepX() * width + clockWise.getStepX() * side, height,
							direction.getStepZ() * width + clockWise.getStepZ() * side);
					if (level.getFluidState(mutable).is(FluidTags.LAVA)) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
