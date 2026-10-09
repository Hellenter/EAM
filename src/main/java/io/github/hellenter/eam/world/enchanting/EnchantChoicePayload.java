package io.github.hellenter.eam.world.enchanting;

import io.github.hellenter.eam.Eam;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

/**
 * Sent when the player enchants the item at the open enchanting table with the enchantments and levels they picked. The
 * server re-checks everything.
 */
public record EnchantChoicePayload(List<EnchantingOptions.Choice> choices) implements CustomPacketPayload {
	/** More than any item can take; a longer list is refused while decoding. */
	public static final int MAX_CHOICES = 32;
	public static final Type<EnchantChoicePayload> TYPE = new Type<>(Eam.id("enchant_choice"));
	private static final StreamCodec<RegistryFriendlyByteBuf, EnchantingOptions.Choice> CHOICE_CODEC = StreamCodec.composite(
			Enchantment.STREAM_CODEC, EnchantingOptions.Choice::enchantment,
			ByteBufCodecs.VAR_INT, EnchantingOptions.Choice::level,
			EnchantingOptions.Choice::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, EnchantChoicePayload> STREAM_CODEC = StreamCodec.composite(
			CHOICE_CODEC.apply(ByteBufCodecs.list(MAX_CHOICES)), EnchantChoicePayload::choices,
			EnchantChoicePayload::new);

	@Override
	public Type<EnchantChoicePayload> type() {
		return TYPE;
	}
}
