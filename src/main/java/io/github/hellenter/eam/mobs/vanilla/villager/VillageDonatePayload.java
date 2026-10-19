package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent by a resource card of the upgrades menu: the player gives the elder his items that count as {@code resource},
 * one stack of them, or with {@code all} every one.
 */
public record VillageDonatePayload(int elderId, VillageResource resource, boolean all) implements CustomPacketPayload {
	public static final Type<VillageDonatePayload> TYPE = new Type<>(Eam.id("village_donate"));
	public static final StreamCodec<RegistryFriendlyByteBuf, VillageDonatePayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, VillageDonatePayload::elderId,
			VillageResource.STREAM_CODEC, VillageDonatePayload::resource,
			ByteBufCodecs.BOOL, VillageDonatePayload::all,
			VillageDonatePayload::new);

	@Override
	public Type<VillageDonatePayload> type() {
		return TYPE;
	}
}
