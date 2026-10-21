package io.github.hellenter.eam.content.lockedchest;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;

import java.util.List;
import java.util.Optional;

/**
 * A locked chest's menu: a chest's three rows (six for a double chest), and on the side whose the chest is, with a button for the player who
 * placed it, the villagers of the village to give it to.
 */
public class LockedChestMenu extends ChestMenu {
	private final List<LockedChestBlockEntity> halves;
	private OpenData data;

	/** Client side: an empty copy the server fills. */
	public LockedChestMenu(int containerId, Inventory inventory, OpenData data) {
		this(containerId, inventory, new SimpleContainer(data.rows() * 9), List.of(), data);
	}

	LockedChestMenu(int containerId, Inventory inventory, Container container, List<LockedChestBlockEntity> halves, OpenData data) {
		super(LockedChestModule.MENU, containerId, inventory, container, data.rows());
		this.halves = halves;
		this.data = data;
	}

	/** The chest's halves, one for a lone chest; on the client none. */
	List<LockedChestBlockEntity> halves() {
		return halves;
	}

	public OpenData data() {
		return data;
	}

	/** On the client, after the server gave the chest to someone else. */
	public void setOwner(Component owner, int current) {
		data = new OpenData(data.rows(), owner, data.canChange(), current, data.candidates());
	}

	/**
	 * What the client shows beside the rows.
	 *
	 * @param rows       three, or six for a double chest
	 * @param owner      whose the chest is
	 * @param canChange  whether this player may give it to someone else
	 * @param current    who has it now: {@link #NOBODY}, {@link #VILLAGE}, a villager's entity id, or {@link #GONE}
	 * @param candidates the villagers he may give it to; empty when he may not
	 */
	public record OpenData(int rows, Component owner, boolean canChange, int current, List<Candidate> candidates) {
		public static final int NOBODY = -1;
		public static final int VILLAGE = -2;
		public static final int GONE = -3;
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenData> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, OpenData::rows,
				ComponentSerialization.STREAM_CODEC, OpenData::owner,
				ByteBufCodecs.BOOL, OpenData::canChange,
				ByteBufCodecs.INT, OpenData::current,
				Candidate.STREAM_CODEC.apply(ByteBufCodecs.list()), OpenData::candidates,
				OpenData::new);
	}

	/**
	 * A villager the chest can be given to.
	 *
	 * @param id         his entity id
	 * @param name       the name a player gave him, if any
	 * @param profession his profession's name
	 * @param level      his trade level
	 * @param distance   how far from the chest he is, in blocks
	 */
	public record Candidate(int id, Optional<Component> name, Component profession, int level, int distance) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Candidate> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Candidate::id,
				ComponentSerialization.OPTIONAL_STREAM_CODEC, Candidate::name,
				ComponentSerialization.STREAM_CODEC, Candidate::profession,
				ByteBufCodecs.VAR_INT, Candidate::level,
				ByteBufCodecs.VAR_INT, Candidate::distance,
				Candidate::new);
	}
}
