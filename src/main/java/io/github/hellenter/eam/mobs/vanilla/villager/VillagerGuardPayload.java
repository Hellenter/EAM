package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent by the guard button of a villager's inventory: the server makes him a guard, or an ordinary villager again. */
public record VillagerGuardPayload(boolean guard) implements CustomPacketPayload {
	public static final Type<VillagerGuardPayload> TYPE = new Type<>(Eam.id("villager_guard"));
	public static final StreamCodec<RegistryFriendlyByteBuf, VillagerGuardPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, VillagerGuardPayload::guard, VillagerGuardPayload::new);

	@Override
	public Type<VillagerGuardPayload> type() {
		return TYPE;
	}
}
