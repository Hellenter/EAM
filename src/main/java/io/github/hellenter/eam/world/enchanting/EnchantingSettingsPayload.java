package io.github.hellenter.eam.world.enchanting;

import io.github.hellenter.eam.Eam;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The server's prices, sent on join and after a datapack reload. The client shows the choice panel only once it has them. */
public record EnchantingSettingsPayload(EnchantingSettings settings) implements CustomPacketPayload {
	public static final Type<EnchantingSettingsPayload> TYPE = new Type<>(Eam.id("enchanting_settings"));
	public static final StreamCodec<RegistryFriendlyByteBuf, EnchantingSettingsPayload> STREAM_CODEC =
			EnchantingSettings.STREAM_CODEC.map(EnchantingSettingsPayload::new, EnchantingSettingsPayload::settings);

	@Override
	public Type<EnchantingSettingsPayload> type() {
		return TYPE;
	}
}
