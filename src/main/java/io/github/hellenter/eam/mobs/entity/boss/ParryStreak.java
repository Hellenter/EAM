package io.github.hellenter.eam.mobs.entity.boss;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** One boss's memory of who parried it successfully and when, for {@link ParryPenalty}. Not saved: a reload forgets. */
public final class ParryStreak {
	private final Map<UUID, Long> lastParry = new HashMap<>();

	/** Whether a parry by {@code player} at game time {@code now} is a repeat within the penalty's memory. */
	public boolean penalized(UUID player, long now, ParryPenalty penalty) {
		Long last = lastParry.get(player);
		return last != null && now - last <= penalty.memory();
	}

	/** {@code player} parried successfully at game time {@code now}. */
	public void parried(UUID player, long now) {
		lastParry.put(player, now);
	}
}
