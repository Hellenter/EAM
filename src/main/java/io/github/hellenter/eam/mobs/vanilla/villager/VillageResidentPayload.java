package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * One villager of the elder's village in detail, for his page in the village menu.
 *
 * @param elderId   the elder whose village menu it is
 * @param resident  his row of the list: name, profession, level, health, gear and role
 * @param xp        his trade experience
 * @param workItem  the work item of his trade, if he has one
 * @param inventory his inventory
 * @param jobSite   where his job site is, if he has one
 * @param home      where his bed is, if he has one
 * @param chest     where his locked chest is, or for the elder the village store's nearest chest
 * @param chestItems what is in that chest
 */
public record VillageResidentPayload(int elderId, VillageListPayload.Resident resident, int xp, ItemStack workItem,
		List<ItemStack> inventory, Optional<BlockPos> jobSite, Optional<BlockPos> home, Optional<BlockPos> chest,
		List<ItemStack> chestItems) implements CustomPacketPayload {
	public static final Type<VillageResidentPayload> TYPE = new Type<>(Eam.id("village_resident"));
	public static final StreamCodec<RegistryFriendlyByteBuf, VillageResidentPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, VillageResidentPayload::elderId,
			VillageListPayload.Resident.STREAM_CODEC, VillageResidentPayload::resident,
			ByteBufCodecs.VAR_INT, VillageResidentPayload::xp,
			ItemStack.OPTIONAL_STREAM_CODEC, VillageResidentPayload::workItem,
			ItemStack.OPTIONAL_LIST_STREAM_CODEC, VillageResidentPayload::inventory,
			BlockPos.STREAM_CODEC.apply(ByteBufCodecs::optional), VillageResidentPayload::jobSite,
			BlockPos.STREAM_CODEC.apply(ByteBufCodecs::optional), VillageResidentPayload::home,
			BlockPos.STREAM_CODEC.apply(ByteBufCodecs::optional), VillageResidentPayload::chest,
			ItemStack.OPTIONAL_LIST_STREAM_CODEC, VillageResidentPayload::chestItems,
			VillageResidentPayload::new);

	@Override
	public Type<VillageResidentPayload> type() {
		return TYPE;
	}
}
