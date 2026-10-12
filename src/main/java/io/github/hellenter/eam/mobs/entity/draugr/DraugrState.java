package io.github.hellenter.eam.mobs.entity.draugr;

import java.util.Locale;

/** Where a draugr is in his long sleep. */
public enum DraugrState {
	/** Frozen in the ice or snow under the floor; rises when his block is broken or a waiting draugr calls. */
	SLEEPING,
	/** Frozen like a sleeper, but watching: a player close by makes him rise and wake the sleepers around him in waves. */
	WAITING,
	/** Climbing up out of the floor. */
	RISING,
	/** Up and hunting. */
	AWAKE;

	public boolean buried() {
		return this == SLEEPING || this == WAITING;
	}

	public String serializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static DraugrState byName(String name, DraugrState fallback) {
		for (DraugrState state : values()) {
			if (state.serializedName().equals(name)) {
				return state;
			}
		}
		return fallback;
	}

	public static DraugrState byId(int id) {
		DraugrState[] values = values();
		return id >= 0 && id < values.length ? values[id] : AWAKE;
	}
}
