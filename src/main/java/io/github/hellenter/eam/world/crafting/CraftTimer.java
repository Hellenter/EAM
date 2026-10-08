package io.github.hellenter.eam.world.crafting;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;

import java.util.function.Supplier;

/**
 * The craft in progress in one crafting menu. The server counts game ticks since the result appeared; the client
 * only gets the two synced values, to draw the arrow and predict when the result can be taken.
 */
public final class CraftTimer {
	private final Supplier<Player> owner;
	private long start;
	private int total;
	private int clientProgress;
	private int clientTotal;

	private final DataSlot progressSlot = new DataSlot() {
		@Override
		public int get() {
			return progress();
		}

		@Override
		public void set(int value) {
			clientProgress = value;
		}
	};

	private final DataSlot totalSlot = new DataSlot() {
		@Override
		public int get() {
			return total();
		}

		@Override
		public void set(int value) {
			clientTotal = value;
		}
	};

	public CraftTimer(Supplier<Player> owner) {
		this.owner = owner;
	}

	public DataSlot progressSlot() {
		return progressSlot;
	}

	public DataSlot totalSlot() {
		return totalSlot;
	}

	/** Server: a new result appeared in the grid; it takes {@code ticks} from now. */
	public void restart(long now, int ticks) {
		start = now;
		total = ticks;
	}

	/** The result was taken: the next one starts from zero. The server restarts anyway when the grid updates. */
	public void crafted() {
		Player player = owner.get();
		if (player.level().isClientSide()) {
			clientProgress = 0;
		} else {
			start = player.level().getGameTime();
		}
	}

	public int progress() {
		Player player = owner.get();
		return player.level().isClientSide() ? clientProgress : CraftTimeLogic.progress(player.level().getGameTime(), start, total);
	}

	public int total() {
		return owner.get().level().isClientSide() ? clientTotal : total;
	}

	public boolean ready() {
		return CraftTimeLogic.ready(progress(), total());
	}
}
