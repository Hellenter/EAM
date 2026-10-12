package io.github.hellenter.eam.mobs.entity.draugr;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The calls of waiting draugr. Each goes out from where the waiting draugr lay and wakes the buried draugr around it
 * in waves, each wave later and reaching further than the one before ({@link DraugrSettings#waves()}). Calls live
 * only in memory: one cut short by a restart simply stops.
 */
final class DraugrAlarms {
	private static final Map<ResourceKey<Level>, List<Alarm>> ALARMS = new HashMap<>();

	private DraugrAlarms() {
	}

	private static final class Alarm {
		private final Vec3 center;
		private final long start;
		private final @Nullable UUID player;
		private int next;

		private Alarm(Vec3 center, long start, @Nullable UUID player) {
			this.center = center;
			this.start = start;
			this.player = player;
		}
	}

	/** A waiting draugr at {@code center} rose at {@code player}; the first wave goes out at once. */
	static void raise(ServerLevel level, Vec3 center, @Nullable Player player) {
		Alarm alarm = new Alarm(center, level.getGameTime(), player == null ? null : player.getUUID());
		ALARMS.computeIfAbsent(level.dimension(), key -> new ArrayList<>()).add(alarm);
		advance(level, alarm);
	}

	static void tick(ServerLevel level) {
		List<Alarm> alarms = ALARMS.get(level.dimension());
		if (alarms == null || alarms.isEmpty()) {
			return;
		}
		for (Iterator<Alarm> it = alarms.iterator(); it.hasNext(); ) {
			if (advance(level, it.next())) {
				it.remove();
			}
		}
	}

	static void clear() {
		ALARMS.clear();
	}

	/** Sends out every wave of {@code alarm} now due; true once it has none left. */
	private static boolean advance(ServerLevel level, Alarm alarm) {
		List<DraugrSettings.Wave> waves = DraugrModule.settings().waves();
		int due = DraugrLogic.wavesDue(waves, alarm.next, level.getGameTime() - alarm.start);
		for (int i = alarm.next; i < due; i++) {
			wake(level, alarm, waves.get(i).radius());
		}
		alarm.next = due;
		return due >= waves.size();
	}

	private static void wake(ServerLevel level, Alarm alarm, double radius) {
		Player player = alarm.player == null ? null : level.getPlayerByUUID(alarm.player);
		int stagger = DraugrModule.settings().wakeStagger();
		AABB box = AABB.ofSize(alarm.center, radius * 2.0, radius * 2.0, radius * 2.0);
		for (Draugr draugr : level.getEntitiesOfClass(Draugr.class, box,
				d -> d.state().buried() && d.distanceToSqr(alarm.center) <= radius * radius)) {
			draugr.wake(player, stagger <= 0 ? 0 : draugr.getRandom().nextInt(stagger + 1));
		}
	}
}
