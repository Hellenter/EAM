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

import org.jspecify.annotations.Nullable;

/**
 * A locked chest's menu: a chest's three rows, and on the side whose the chest is, with a button for the player who
 * placed it to give it to a villager near it or to the village.
 */
public class LockedChestMenu extends ChestMenu {
	private final @Nullable LockedChestBlockEntity chest;
	private OpenData data;

	/** Client side: an empty copy the server fills. */
	public LockedChestMenu(int containerId, Inventory inventory, OpenData data) {
		this(containerId, inventory, new SimpleContainer(LockedChestBlockEntity.SIZE), null, data);
	}

	LockedChestMenu(int containerId, Inventory inventory, LockedChestBlockEntity chest, OpenData data) {
		this(containerId, inventory, chest, chest, data);
	}

	private LockedChestMenu(int containerId, Inventory inventory, Container container, @Nullable LockedChestBlockEntity chest, OpenData data) {
		super(LockedChestModule.MENU, containerId, inventory, container, 3);
		this.chest = chest;
		this.data = data;
	}

	/** The chest, on the server only. */
	@Nullable LockedChestBlockEntity chest() {
		return chest;
	}

	public OpenData data() {
		return data;
	}

	/** On the client, after the server gave the chest to someone else. */
	public void setOwnerName(Component owner) {
		data = new OpenData(owner, data.canChange());
	}

	/**
	 * What the client shows beside the rows.
	 *
	 * @param owner     whose the chest is
	 * @param canChange whether this player may give it to someone else
	 */
	public record OpenData(Component owner, boolean canChange) {
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenData> STREAM_CODEC = StreamCodec.composite(
				ComponentSerialization.STREAM_CODEC, OpenData::owner,
				ByteBufCodecs.BOOL, OpenData::canChange,
				OpenData::new);
	}
}
