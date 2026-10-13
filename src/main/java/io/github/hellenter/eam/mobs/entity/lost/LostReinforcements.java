package io.github.hellenter.eam.mobs.entity.lost;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.mobs.entity.cavalry.LostCavalryman;
import io.github.hellenter.eam.mobs.entity.warlock.UndeadWarlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Brings a wave of the Lost near a player: the centurion's horn calls them against the player, the commander's horn
 * calls them to the one who blows it.
 */
public final class LostReinforcements {
	private static final int SPOT_ATTEMPTS = 48;
	/** Everyone who came with the wave is found this close to where it came. */
	private static final double ARRIVED_RANGE = 10.0;

	private LostReinforcements() {
	}

	/**
	 * Brings {@code wave} {@code minDistance} to {@code maxDistance} from {@code player}, best where the player cannot
	 * see when {@code hidden}; failing a spot in that ring, at {@code fallback}. Returns everyone who came.
	 */
	public static List<Mob> bring(ServerLevel level, Player player, LostWave wave, double minDistance, double maxDistance, boolean hidden,
			Vec3 fallback, boolean persistent) {
		Mob mob = switch (wave) {
			case SQUAD -> EamEntities.LOST.create(level, EntitySpawnReason.MOB_SUMMONED);
			case CAVALRY -> EamEntities.LOST_CAVALRYMAN.create(level, EntitySpawnReason.MOB_SUMMONED);
			case CENTURION -> EamEntities.LOST_CENTURION.create(level, EntitySpawnReason.MOB_SUMMONED);
		};
		if (mob == null) {
			return List.of();
		}
		// A rider on his horse needs the horse's room and his own height above it.
		AABB size = wave == LostWave.CAVALRY
				? EamEntities.LOST_WARHORSE.getDimensions().makeBoundingBox(Vec3.ZERO).expandTowards(0.0, 1.4, 0.0)
				: mob.getType().getDimensions().makeBoundingBox(Vec3.ZERO);
		Vec3 spot = spot(level, player, size, minDistance, maxDistance, hidden, fallback, mob.getRandom());
		float yaw = (float) (Mth.atan2(player.getZ() - spot.z, player.getX() - spot.x) * Mth.RAD_TO_DEG) - 90.0F;
		mob.snapTo(spot.x, spot.y, spot.z, yaw, 0.0F);
		if (mob instanceof Lost lost && wave == LostWave.SQUAD) {
			// A called squad brings no centurion of its own.
			lost.bringsCenturion = false;
		}
		if (persistent) {
			// Set before the spawn, so the followers he brings keep it too.
			mob.setPersistenceRequired();
		}
		AABB arrival = new AABB(spot, spot).inflate(ARRIVED_RANGE);
		// The Lost already standing about did not come with the wave.
		List<Mob> before = level.getEntitiesOfClass(Mob.class, arrival, LostReinforcements::marches);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(spot)), EntitySpawnReason.MOB_SUMMONED, null);
		level.addFreshEntityWithPassengers(mob);
		List<Mob> arrived = level.getEntitiesOfClass(Mob.class, arrival, m -> marches(m) && !before.contains(m));
		if (persistent) {
			arrived.forEach(Mob::setPersistenceRequired);
		}
		return arrived;
	}

	/** Those who march in a wave: the Lost, their riders and their warlock. */
	private static boolean marches(Mob mob) {
		return (mob instanceof Lost || mob instanceof LostCavalryman || mob instanceof UndeadWarlock) && mob.isAlive();
	}

	/**
	 * Where a wave comes: on solid ground {@code min} to {@code max} from the player, with room for it, best where the
	 * player cannot see when {@code hidden}. Failing that anywhere in that ring, and failing that next to
	 * {@code fallback}.
	 */
	private static Vec3 spot(ServerLevel level, Player player, AABB size, double minDistance, double maxDistance, boolean hidden,
			Vec3 fallback, RandomSource random) {
		Vec3 seen = null;
		Vec3 eyes = player.getEyePosition();
		double min = Math.min(minDistance, maxDistance);
		double max = Math.max(minDistance, maxDistance);
		for (int attempt = 0; attempt < SPOT_ATTEMPTS; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double distance = min + random.nextDouble() * (max - min);
			Vec3 found = standingSpot(level, BlockPos.containing(player.getX() + Math.cos(angle) * distance, player.getY(),
					player.getZ() + Math.sin(angle) * distance), size, 8);
			if (found == null) {
				continue;
			}
			if (!hidden) {
				return found;
			}
			Vec3 head = found.add(0.0, size.getYsize() * 0.8, 0.0);
			boolean outOfSight = level.clip(new ClipContext(eyes, head, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player)).getType()
					!= HitResult.Type.MISS;
			if (outOfSight) {
				return found;
			}
			if (seen == null) {
				seen = found;
			}
		}
		if (seen != null) {
			return seen;
		}
		Vec3 near = standingSpot(level, BlockPos.containing(fallback), size, 3);
		return near != null ? near : fallback;
	}

	/** A spot on solid ground in the column of {@code column}, within {@code height} up or down, with room for {@code size}. */
	private static @Nullable Vec3 standingSpot(ServerLevel level, BlockPos column, AABB size, int height) {
		for (int dy = height; dy >= -height; dy--) {
			BlockPos pos = column.above(dy);
			BlockPos below = pos.below();
			if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) && level.getFluidState(pos).isEmpty()
					&& level.getFluidState(pos.above()).isEmpty()) {
				Vec3 feet = Vec3.atBottomCenterOf(pos);
				if (level.noCollision(size.move(feet))) {
					return feet;
				}
			}
		}
		return null;
	}
}
