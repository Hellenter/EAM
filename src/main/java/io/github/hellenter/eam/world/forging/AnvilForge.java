package io.github.hellenter.eam.world.forging;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Forging in one anvil menu. A click on the offered plate starts hammering; when a plate is done the server takes the
 * hot iron for it from both input slots and adds the plate to {@link #output()}, which the result slot then shows and
 * the player takes. While both slots still hold hot iron, the next plate starts right away.
 * <p>
 * The client only gets the synced progress, total, flags and anvil tier, to draw the arrow, predict clicks and tell
 * which items this anvil cannot work on.
 */
public final class AnvilForge {
	private static final int RUNNING = 1;
	private static final int HAS_OUTPUT = 2;
	private static final int ACTIVE = 4;
	private static final int REPAIR = 8;

	private final Supplier<Player> owner;
	private final IntSupplier anvilTier;
	private long start;
	private int total;
	private boolean running;
	/** Both sides: the result slot shows a free repair. */
	private boolean repairing;
	/** Plates forged and not taken yet. */
	private ItemStack output = ItemStack.EMPTY;

	private int clientProgress;
	private int clientTotal;
	private int clientFlags;
	private int clientAnvilTier = AnvilTiers.IRON;

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

	private final DataSlot anvilTierSlot = new DataSlot() {
		@Override
		public int get() {
			return anvilTier();
		}

		@Override
		public void set(int value) {
			clientAnvilTier = value;
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

	/** @param anvilTier server: the tier of the anvil block this menu was opened on */
	public AnvilForge(Supplier<Player> owner, IntSupplier anvilTier) {
		this.owner = owner;
		this.anvilTier = anvilTier;
	}

	public DataSlot totalSlot() {
		return totalSlot;
	}

	public DataSlot anvilTierSlot() {
		return anvilTierSlot;
	}

	/** Ticks the plate being hammered takes. */
	public int total() {
		return client() ? clientTotal : total;
	}

	/** Tier of the anvil: 2 for copper and silver, 3 for iron, 4 for mithril. */
	public int anvilTier() {
		return client() ? clientAnvilTier : anvilTier.getAsInt();
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
		return (running ? RUNNING : 0) | (output.isEmpty() ? 0 : HAS_OUTPUT) | (ForgingModule.enabled() ? ACTIVE : 0)
				| (AnvilRepair.enabled() ? REPAIR : 0);
	}

	/** Whether the server repairs for free; off, repair works like vanilla. */
	public boolean repairActive() {
		return (flags() & REPAIR) != 0;
	}

	public boolean repairing() {
		return repairing;
	}

	void setRepairing(boolean repairing) {
		this.repairing = repairing;
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
