package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.Optional;

/** Sent after the server opened a villager's trades: the client shows his gear panel beside them. */
public record VillagerTradePanelPayload(int containerId, int villagerId, Optional<TagKey<Item>> tools, boolean elder) implements CustomPacketPayload {
	public static final Type<VillagerTradePanelPayload> TYPE = new Type<>(Eam.id("villager_trade_panel"));
	public static final StreamCodec<RegistryFriendlyByteBuf, VillagerTradePanelPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, VillagerTradePanelPayload::containerId,
			ByteBufCodecs.VAR_INT, VillagerTradePanelPayload::villagerId,
			VillagerGearMenu.OpenData.TAG, VillagerTradePanelPayload::tools,
			ByteBufCodecs.BOOL, VillagerTradePanelPayload::elder,
			VillagerTradePanelPayload::new);

	public TradePanel panel() {
		return new TradePanel(villagerId, tools, elder);
	}

	@Override
	public Type<VillagerTradePanelPayload> type() {
		return TYPE;
	}
}
