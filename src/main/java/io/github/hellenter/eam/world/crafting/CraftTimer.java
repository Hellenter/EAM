package io.github.hellenter.eam.world.crafting;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * The crafting in one crafting menu. A click on the offered result starts a craft; when its time has run the server
 * uses up the ingredients and puts the result into {@link #output()}, which the result slot then shows and the player
 * takes. While the grid still makes the same result, the next craft starts right away.
 * <p>
 * The client only gets the synced progress, total and flags, to draw the arrow and predict clicks.
 */
public final class CraftTimer {
	private static final int RUNNING = 1;
	private static final int HAS_OUTPUT = 2;
	private static final int ACTIVE = 4;

	private final Supplier<Player> owner;
	private long start;
	private int total;
	private boolean running;
	/** What the grid makes now, with the tool check applied. */
	private ItemStack preview = ItemStack.EMPTY;
	private boolean previewChanged;
	/** Finished crafts waiting in the result slot. */
	private ItemStack output = ItemStack.EMPTY;
	/** True while the server itself uses up the ingredients of a finished craft. */
	private boolean completing;

	private int clientProgress;
	private int clientTotal;
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

	public CraftTimer(Supplier<Player> owner) {
		this.owner = owner;
	}

	public DataSlot progressSlot() {
		return progressSlot;
	}

	public DataSlot totalSlot() {
		return totalSlot;
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
		return running ? CraftTimeLogic.progress(now(), start, total) : 0;
	}

	public int total() {
		return client() ? clientTotal : total;
	}

	private int flags() {
		if (client()) {
			return clientFlags;
		}
		return (running ? RUNNING : 0) | (output.isEmpty() ? 0 : HAS_OUTPUT) | (CraftingModule.enabled() ? ACTIVE : 0);
	}

	/** Whether the server runs craft times at all; off, the grid crafts like vanilla. */
	public boolean active() {
		return (flags() & ACTIVE) != 0;
	}

	public boolean running() {
		return (flags() & RUNNING) != 0;
	}

	/** The result slot shows finished crafts, which can be taken. Otherwise it shows the offer, which cannot. */
	public boolean hasOutput() {
		return (flags() & HAS_OUTPUT) != 0;
	}

	/** Server: the craft has run its time. */
	boolean due() {
		return running && CraftTimeLogic.ready(progress(), total);
	}

	ItemStack preview() {
		return preview;
	}

	ItemStack output() {
		return output;
	}

	void setOutput(ItemStack stack) {
		output = stack;
	}

	boolean completing() {
		return completing;
	}

	void setCompleting(boolean value) {
		completing = value;
	}

	/** Server: the grid was evaluated again; returns what the result slot shows. */
	ItemStack updatePreview(ItemStack value) {
		previewChanged = !ItemStack.isSameItemSameComponents(preview, value) || preview.getCount() != value.getCount();
		preview = value;
		return output.isEmpty() ? value : output;
	}

	/**
	 * Server: after the grid was evaluated. A finished craft goes on with the next one; a change by the player keeps
	 * the craft running only while the grid still makes the same result.
	 */
	void afterPreview(int ticks) {
		if (completing) {
			running = !preview.isEmpty();
			start = now();
			total = ticks;
		} else if (!running || previewChanged || preview.isEmpty()) {
			running = false;
			total = ticks;
		}
	}

	/** Server: the player clicked the offered result. */
	void start() {
		if (!running && !preview.isEmpty()) {
			running = true;
			start = now();
		}
	}

	/** Server: the menu closed; the craft in progress is dropped. */
	void stop() {
		running = false;
	}

	/** Client: the output was taken, so the slot shows the offer until the server says otherwise. */
	void outputTakenOnClient() {
		clientFlags &= ~HAS_OUTPUT;
	}
}
