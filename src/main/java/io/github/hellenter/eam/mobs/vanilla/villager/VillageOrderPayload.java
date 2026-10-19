package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent by an order button of the upgrades menu: the elder carries the order out now if the stock covers it, else the
 * village saves up for it. An empty order id stops saving up. With {@code take}, the player takes a blueprint the village
 * saved up for.
 */
public record VillageOrderPayload(int elderId, String order, boolean take) implements CustomPacketPayload {
	public static final Type<VillageOrderPayload> TYPE = new Type<>(Eam.id("village_order"));
	public static final StreamCodec<RegistryFriendlyByteBuf, VillageOrderPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, VillageOrderPayload::elderId,
			ByteBufCodecs.stringUtf8(64), VillageOrderPayload::order,
			ByteBufCodecs.BOOL, VillageOrderPayload::take,
			VillageOrderPayload::new);

	@Override
	public Type<VillageOrderPayload> type() {
		return TYPE;
	}
}
