package io.github.hellenter.eam.content.lockedchest;

import io.github.hellenter.eam.Eam;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent by the owner button of a locked chest: the server gives the chest to the next one who can have it. */
public record LockedChestOwnerPayload() implements CustomPacketPayload {
	public static final LockedChestOwnerPayload INSTANCE = new LockedChestOwnerPayload();
	public static final Type<LockedChestOwnerPayload> TYPE = new Type<>(Eam.id("locked_chest_owner"));
	public static final StreamCodec<RegistryFriendlyByteBuf, LockedChestOwnerPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<LockedChestOwnerPayload> type() {
		return TYPE;
	}
}
