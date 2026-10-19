package io.github.hellenter.eam.world.structures.outpost;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import org.jspecify.annotations.Nullable;

/**
 * The map from the outpost's red chest. It stays blank until a player takes it in hand; then it looks for the nearest
 * structure in {@link OutpostModule#KING_CASTLE} and turns into a filled map marking it. While nothing is found it
 * tells the player so and stays blank.
 */
public class KingCastleMapItem extends Item {
	/** How far the map looks, in chunks, as for the explorer maps of cartographers. */
	private static final int SEARCH_RADIUS = 100;
	/** How often a map in hand tries again while it finds nothing, in ticks. */
	private static final int RETRY_TICKS = 40;
	private static final byte ZOOM = 2;

	public KingCastleMapItem(Properties properties) {
		super(properties);
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		if (!(owner instanceof Player player) || (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND)) return;
		if (level.getGameTime() % RETRY_TICKS != 0) return;
		BlockPos castle = level.findNearestMapStructure(OutpostModule.KING_CASTLE, player.blockPosition(), SEARCH_RADIUS, false);
		if (castle == null) {
			player.sendOverlayMessage(Component.translatable("item.eam.king_castle_map.not_found"));
			return;
		}
		ItemStack map = MapItem.create(level, castle.getX(), castle.getZ(), ZOOM, true, true);
		MapItem.renderBiomePreviewMap(level, map);
		MapItemSavedData.addTargetDecoration(map, castle, "+", MapDecorationTypes.RED_X);
		map.set(DataComponents.ITEM_NAME, Component.translatable(getDescriptionId()));
		player.setItemInHand(slot == EquipmentSlot.MAINHAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, map);
	}
}
