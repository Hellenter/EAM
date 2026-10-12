package io.github.hellenter.eam.mobs.entity.draugr;

import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A draugr walks the paths a zombie would, around the spires and pits of the ice caves. Only when no path is left to walk
 * and his target stands close by on a ledge just above him does he head straight at it, to claw his way up the wall in
 * between (see {@link Draugr#onClimbable()}).
 */
final class DraugrNavigation extends GroundPathNavigation {
	private final Draugr draugr;

	DraugrNavigation(Draugr draugr, Level level) {
		super(draugr, level);
		this.draugr = draugr;
	}

	@Override
	public void tick() {
		super.tick();
		if (isDone()) {
			Vec3 ledge = draugr.ledgeTarget();
			if (ledge != null) {
				mob.getMoveControl().setWantedPosition(ledge.x, ledge.y, ledge.z, 1.0);
			}
		}
	}
}
