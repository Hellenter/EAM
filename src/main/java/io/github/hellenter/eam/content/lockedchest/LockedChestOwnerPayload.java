package io.github.hellenter.eam.content.lockedchest;

import io.github.hellenter.eam.Eam;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent by a row of a locked chest's owner list: the server gives the chest to that villager, the village or nobody.
 *
 * @param owner {@link LockedChestMenu.OpenData#NOBODY}, {@link LockedChestMenu.OpenData#VILLAGE} or a villager's entity id
 */
public record LockedChestOwnerPayload(int owner) implements CustomPacketPayload {
	public static final Type<LockedChestOwnerPayload> TYPE = new Type<>(Eam.id("locked_chest_owner"));
	public static final StreamCodec<RegistryFriendlyByteBuf, LockedChestOwnerPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT, LockedChestOwnerPayload::owner,
			LockedChestOwnerPayload::new);

	@Override
	public Type<LockedChestOwnerPayload> type() {
		return TYPE;
	}
}
