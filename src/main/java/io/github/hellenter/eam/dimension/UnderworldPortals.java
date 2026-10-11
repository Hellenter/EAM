package io.github.hellenter.eam.dimension;

import io.github.hellenter.eam.dimension.UnderworldLogic.Place;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
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

	/**
	 * Vanilla's portal building, with every height kept inside the underworld level of {@code origin}: it looks for a
	 * floor with room for a frame within 16 blocks, and failing that builds a frame on an obsidian ledge in the rock.
	 */
	public static Optional<BlockUtil.FoundRectangle> createPortal(ServerLevel level, BlockPos origin, Direction.Axis portalAxis) {
		Place target = UnderworldLogic.underworldLevel(origin.getY());
		int minY = UnderworldLogic.portalMinY(target);
		int maxY = UnderworldLogic.portalMaxY(target);
		Direction direction = Direction.get(Direction.AxisDirection.POSITIVE, portalAxis);
		double closestFullDistanceSqr = -1.0;
		BlockPos closestFullPosition = null;
		double closestPartialDistanceSqr = -1.0;
		BlockPos closestPartialPosition = null;
		WorldBorder worldBorder = level.getWorldBorder();
		BlockPos.MutableBlockPos mutable = origin.mutable();

		for (BlockPos.MutableBlockPos columnPos : BlockPos.spiralAround(origin, 16, Direction.EAST, Direction.SOUTH)) {
			int height = Math.min(maxY + 4, level.getHeight(Heightmap.Types.MOTION_BLOCKING, columnPos.getX(), columnPos.getZ()));
			if (worldBorder.isWithinBounds(columnPos) && worldBorder.isWithinBounds(columnPos.move(direction, 1))) {
				columnPos.move(direction.getOpposite(), 1);
				for (int y = height; y >= minY; y--) {
					columnPos.setY(y);
					if (canPortalReplaceBlock(level, columnPos)) {
						int firstEmptyY = y;
						while (y > minY && canPortalReplaceBlock(level, columnPos.move(Direction.DOWN))) {
							y--;
						}
						if (y <= maxY) {
							int deltaY = firstEmptyY - y;
							if (deltaY <= 0 || deltaY >= 3) {
								columnPos.setY(y);
								if (canHostFrame(level, columnPos, mutable, direction, 0)) {
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
						}
					}
				}
			}
		}

		if (closestFullDistanceSqr == -1.0 && closestPartialDistanceSqr != -1.0) {
			closestFullPosition = closestPartialPosition;
			closestFullDistanceSqr = closestPartialDistanceSqr;
		}

		if (closestFullDistanceSqr == -1.0) {
			// No room anywhere near: carve a chamber with an obsidian ledge right at the target height.
			closestFullPosition = new BlockPos(origin.getX() - direction.getStepX(), Mth.clamp(origin.getY(), minY + 1, maxY),
					origin.getZ() - direction.getStepZ());
			closestFullPosition = worldBorder.clampToBounds(closestFullPosition);
			Direction clockWise = direction.getClockWise();
			for (int box = -1; box < 2; box++) {
				for (int width = 0; width < 2; width++) {
					for (int height = -1; height < 3; height++) {
						BlockState state = height < 0 ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState();
						mutable.setWithOffset(closestFullPosition, width * direction.getStepX() + box * clockWise.getStepX(), height,
								width * direction.getStepZ() + box * clockWise.getStepZ());
						level.setBlockAndUpdate(mutable, state);
					}
				}
			}
		}

		for (int width = -1; width < 3; width++) {
			for (int height = -1; height < 4; height++) {
				if (width == -1 || width == 2 || height == -1 || height == 3) {
					mutable.setWithOffset(closestFullPosition, width * direction.getStepX(), height, width * direction.getStepZ());
					level.setBlockAndUpdate(mutable, Blocks.OBSIDIAN.defaultBlockState());
				}
			}
		}

		BlockState portalState = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, portalAxis);
		for (int width = 0; width < 2; width++) {
			for (int height = 0; height < 3; height++) {
				mutable.setWithOffset(closestFullPosition, width * direction.getStepX(), height, width * direction.getStepZ());
				level.setBlock(mutable, portalState, 18);
			}
		}
		return Optional.of(new BlockUtil.FoundRectangle(closestFullPosition.immutable(), 2, 3));
	}

	private static boolean canPortalReplaceBlock(ServerLevel level, BlockPos.MutableBlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.canBeReplaced() && state.getFluidState().isEmpty();
	}

	private static boolean canHostFrame(ServerLevel level, BlockPos origin, BlockPos.MutableBlockPos mutable, Direction direction, int offset) {
		Direction clockWise = direction.getClockWise();
		for (int width = -1; width < 3; width++) {
			for (int height = -1; height < 4; height++) {
				mutable.setWithOffset(origin, direction.getStepX() * width + clockWise.getStepX() * offset, height,
						direction.getStepZ() * width + clockWise.getStepZ() * offset);
				if (height < 0 && !level.getBlockState(mutable).isSolid()) {
					return false;
				}
				if (height >= 0 && !canPortalReplaceBlock(level, mutable)) {
					return false;
				}
			}
		}
		return true;
	}
}
