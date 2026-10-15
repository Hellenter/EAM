package io.github.hellenter.eam.mobs.entity.guard;

import io.github.hellenter.eam.content.EamEntities;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

/**
 * Swaps a share of freshly made pillagers for guards, right where vanilla makes them for a patrol or an outpost (the
 * spawn mixins in {@code mixin.mobs.guard}). The guard then goes through the same spawn steps the pillager would have.
 * Raid waves keep their pillagers and get guards on top ({@code mobs.vanilla.raid.VillageRaidsModule}).
 */
public final class GuardSpawns {
	private static volatile boolean enabled;

	private GuardSpawns() {
	}

	static void enable() {
		enabled = true;
	}

	/** {@code created}, or a guard in its place if it is a pillager and the roll says so. */
	public static @Nullable Entity maybeGuard(@Nullable Entity created, Level level, EntitySpawnReason reason) {
		if (!enabled || created == null || created.getType() != EntityTypes.PILLAGER
				|| !PillagerGuardLogic.becomesGuard(level.getRandom().nextDouble(), PillagerGuardModule.settings().spawnShare())) {
			return created;
		}
		PillagerGuard guard = EamEntities.PILLAGER_GUARD.create(level, reason);
		return guard != null ? guard : created;
	}
}
