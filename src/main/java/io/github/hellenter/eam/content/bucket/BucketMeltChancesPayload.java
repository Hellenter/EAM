package io.github.hellenter.eam.content.bucket;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

/** Each empty bucket's chance to melt in lava, sent on join and after a datapack reload so the tooltip shows the server's numbers. */
public record BucketMeltChancesPayload(Map<Item, Float> chances) implements CustomPacketPayload {
	public static final Type<BucketMeltChancesPayload> TYPE = new Type<>(Eam.id("bucket_melt_chances"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BucketMeltChancesPayload> STREAM_CODEC =
			ByteBufCodecs.<RegistryFriendlyByteBuf, Item, Float, Map<Item, Float>>map(HashMap::new, ByteBufCodecs.registry(Registries.ITEM),
					ByteBufCodecs.FLOAT).map(BucketMeltChancesPayload::new, BucketMeltChancesPayload::chances);

	@Override
	public Type<BucketMeltChancesPayload> type() {
		return TYPE;
	}
}
