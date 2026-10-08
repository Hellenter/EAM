package io.github.hellenter.eam.world.forging;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * Forging in one anvil menu. A click on the offered plate starts hammering; when a plate is done the server takes the
 * hot iron for it from the left slot and adds the plate to {@link #output()}, which the result slot then shows and
 * the player takes. While the left slot still holds enough hot iron, the next plate starts right away.
 * <p>
 * The client only gets the synced progress and flags, to draw the arrow and predict clicks.
 */
public final class AnvilForge {
	private static final int RUNNING = 1;
	private static final int HAS_OUTPUT = 2;
	private static final int ACTIVE = 4;

	private final Supplier<Player> owner;
	private long start;
	private int total;
	private boolean running;
	/** Plates forged and not taken yet. */
	private ItemStack output = ItemStack.EMPTY;

	private int clientProgress;
	private int clientFlags;

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

	private final DataSlot flagsSlot = new DataSlot() {
		@Override
		public int get() {
			return flags();
		}

		@Override
		public void set(int value) {
			clientFlags = value;
		}
	};

	public AnvilForge(Supplier<Player> owner) {
		this.owner = owner;
	}

	public DataSlot progressSlot() {
		return progressSlot;
	}

	public DataSlot flagsSlot() {
		return flagsSlot;
	}

	private boolean client() {
		return owner.get().level().isClientSide();
	}

	private long now() {
		return owner.get().level().getGameTime();
	}

	public int progress() {
		if (client()) {
			return clientProgress;
		}
		return running ? ForgingLogic.progress(now(), start, total) : 0;
	}

	private int flags() {
		if (client()) {
			return clientFlags;
		}
		return (running ? RUNNING : 0) | (output.isEmpty() ? 0 : HAS_OUTPUT) | (ForgingModule.enabled() ? ACTIVE : 0);
	}

	/** Whether the server forges plates at all; off, the anvil works like vanilla. */
	public boolean active() {
		return (flags() & ACTIVE) != 0;
	}

	public boolean running() {
		return (flags() & RUNNING) != 0;
	}

	/** The result slot shows forged plates, which can be taken. Otherwise it shows the offer, which cannot. */
	public boolean hasOutput() {
		return (flags() & HAS_OUTPUT) != 0;
	}

	/** Server: the plate being hammered is done. */
	boolean due() {
		return running && ForgingLogic.forged(now(), start, total);
	}

	ItemStack output() {
		return output;
	}

	void setOutput(ItemStack stack) {
		output = stack;
	}

	/** Server: start hammering a plate that takes {@code ticks}. */
	void start(int ticks) {
		running = true;
		start = now();
		total = ticks;
	}

	/** Server: hammering stops, because the hot iron ran out, cooled or was taken away, or the menu closed. */
	void stop() {
		running = false;
	}

	/** Client: the plates were taken, so the slot shows the offer until the server says otherwise. */
	void outputTakenOnClient() {
		clientFlags &= ~HAS_OUTPUT;
	}
}
