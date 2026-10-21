package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The villagers of the elder's village, for the village menu: sent when a player opens it and again after he changes
 * someone's guard duty.
 *
 * @param elderId   the elder the menu is about
 * @param guards    whether guards are on, so villagers can be made guards
 * @param villagers everyone within the elder's reach, him included
 */
public record VillageListPayload(int elderId, boolean guards, List<Resident> villagers) implements CustomPacketPayload {
	public static final Type<VillageListPayload> TYPE = new Type<>(Eam.id("village_list"));
	public static final StreamCodec<RegistryFriendlyByteBuf, VillageListPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, VillageListPayload::elderId,
			ByteBufCodecs.BOOL, VillageListPayload::guards,
			Resident.STREAM_CODEC.apply(ByteBufCodecs.list()), VillageListPayload::villagers,
			VillageListPayload::new);

	@Override
	public Type<VillageListPayload> type() {
		return TYPE;
	}

	/** What a villager is to the village menu, in the order the menu lists them. */
	public enum Role {
		ELDER, GUARD, VILLAGER, CHILD;

		static final StreamCodec<ByteBuf, Role> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Role::ordinal);
	}

	/**
	 * One villager in the list.
	 *
	 * @param id         his entity id
	 * @param name       his name: his own first name and surname, or the one a player gave him
	 * @param profession his profession's name
	 * @param level      his trade level, 1 to 5
	 * @param health     his health now
	 * @param maxHealth  his full health
	 * @param gear       head, chest, legs, feet, main hand and off hand; the main hand is empty while he holds his work item
	 * @param role       guard, child, the elder or anyone else
	 */
	public record Resident(int id, Component name, Component profession, int level, float health, float maxHealth,
			List<ItemStack> gear, Role role) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Resident> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Resident::id,
				ComponentSerialization.STREAM_CODEC, Resident::name,
				ComponentSerialization.STREAM_CODEC, Resident::profession,
				ByteBufCodecs.VAR_INT, Resident::level,
				ByteBufCodecs.FLOAT, Resident::health,
				ByteBufCodecs.FLOAT, Resident::maxHealth,
				ItemStack.OPTIONAL_LIST_STREAM_CODEC, Resident::gear,
				Role.STREAM_CODEC, Resident::role,
				Resident::new);
	}
}
