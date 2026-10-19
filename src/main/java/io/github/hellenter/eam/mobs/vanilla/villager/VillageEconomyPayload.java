package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.Optional;

/**
 * The elder's village economy, for the upgrades menu: sent when a player opens it and after each order.
 *
 * @param elderId  the elder the menu is about
 * @param stock    what the village has, in {@link VillageResource} order
 * @param income   what the villagers bring each half a day, upkeep taken off, in {@link VillageResource} order
 * @param status   villagers, beds, guards and golems
 * @param goal     the order the village saves up for, if any
 * @param orders   what can be ordered
 * @param donatable what the player's items would bring the village, in {@link VillageResource} order
 */
public record VillageEconomyPayload(int elderId, List<Double> stock, List<Double> income, Status status, Optional<String> goal,
		List<OrderView> orders, List<Double> donatable) implements CustomPacketPayload {
	public static final Type<VillageEconomyPayload> TYPE = new Type<>(Eam.id("village_economy"));
	public static final StreamCodec<RegistryFriendlyByteBuf, VillageEconomyPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.DOUBLE.apply(ByteBufCodecs.list()), VillageEconomyPayload::stock,
			ByteBufCodecs.DOUBLE.apply(ByteBufCodecs.list()), VillageEconomyPayload::income,
			Status.STREAM_CODEC, VillageEconomyPayload::status,
			ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), VillageEconomyPayload::goal,
			OrderView.STREAM_CODEC.apply(ByteBufCodecs.list()), VillageEconomyPayload::orders,
			ByteBufCodecs.VAR_INT, VillageEconomyPayload::elderId,
			ByteBufCodecs.DOUBLE.apply(ByteBufCodecs.list()), VillageEconomyPayload::donatable,
			(stock, income, status, goal, orders, elderId, donatable) -> new VillageEconomyPayload(elderId, stock, income, status, goal, orders,
					donatable));

	@Override
	public Type<VillageEconomyPayload> type() {
		return TYPE;
	}

	/**
	 * How the village stands.
	 *
	 * @param villagers     villagers of every age
	 * @param beds          beds in the village
	 * @param breedCost     provisions a child costs
	 * @param guards        guards now
	 * @param guardsNeeded  guards the elder wants
	 * @param golems        iron golems in the village
	 * @param golemsAllowed iron golems it may have
	 */
	public record Status(int villagers, int beds, double breedCost, int guards, int guardsNeeded, int golems, int golemsAllowed) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Status> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Status::villagers,
				ByteBufCodecs.VAR_INT, Status::beds,
				ByteBufCodecs.DOUBLE, Status::breedCost,
				ByteBufCodecs.VAR_INT, Status::guards,
				ByteBufCodecs.VAR_INT, Status::guardsNeeded,
				ByteBufCodecs.VAR_INT, Status::golems,
				ByteBufCodecs.VAR_INT, Status::golemsAllowed,
				Status::new);
	}

	/**
	 * One order.
	 *
	 * @param id        the order's id; its name is {@code gui.eam.village.order.<id>}
	 * @param cost      what it takes, in {@link VillageResource} order
	 * @param available whether it can be ordered at all now, the stock aside (a golem only while the village may have one more)
	 * @param ready     blueprints of it the village saved up for, waiting to be taken
	 */
	public record OrderView(String id, List<Double> cost, boolean available, int ready) {
		public static final StreamCodec<RegistryFriendlyByteBuf, OrderView> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, OrderView::id,
				ByteBufCodecs.DOUBLE.apply(ByteBufCodecs.list()), OrderView::cost,
				ByteBufCodecs.BOOL, OrderView::available,
				ByteBufCodecs.VAR_INT, OrderView::ready,
				OrderView::new);
	}
}
