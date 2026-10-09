package io.github.hellenter.eam.world.enchanting;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.enchantment.Enchantment;

/** Sent when the player picks an enchantment and a level at the open enchanting table. The server re-checks everything. */
public record EnchantChoicePayload(Holder<Enchantment> enchantment, int level) implements CustomPacketPayload {
	public static final Type<EnchantChoicePayload> TYPE = new Type<>(Eam.id("enchant_choice"));
	public static final StreamCodec<RegistryFriendlyByteBuf, EnchantChoicePayload> STREAM_CODEC = StreamCodec.composite(
			Enchantment.STREAM_CODEC, EnchantChoicePayload::enchantment,
			ByteBufCodecs.VAR_INT, EnchantChoicePayload::level,
			EnchantChoicePayload::new);

	@Override
	public Type<EnchantChoicePayload> type() {
		return TYPE;
	}
}
