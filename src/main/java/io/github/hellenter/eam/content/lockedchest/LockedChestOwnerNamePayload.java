package io.github.hellenter.eam.content.lockedchest;

import io.github.hellenter.eam.Eam;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent after the owner list changed whose a locked chest is: the open menu shows the new owner. */
public record LockedChestOwnerNamePayload(int containerId, Component owner, int current) implements CustomPacketPayload {
	public static final Type<LockedChestOwnerNamePayload> TYPE = new Type<>(Eam.id("locked_chest_owner_name"));
	public static final StreamCodec<RegistryFriendlyByteBuf, LockedChestOwnerNamePayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, LockedChestOwnerNamePayload::containerId,
			ComponentSerialization.STREAM_CODEC, LockedChestOwnerNamePayload::owner,
			ByteBufCodecs.INT, LockedChestOwnerNamePayload::current,
			LockedChestOwnerNamePayload::new);

	@Override
	public Type<LockedChestOwnerNamePayload> type() {
		return TYPE;
	}
}
