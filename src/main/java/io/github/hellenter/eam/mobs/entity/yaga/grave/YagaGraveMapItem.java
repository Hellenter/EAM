package io.github.hellenter.eam.mobs.entity.yaga.grave;

import io.github.hellenter.eam.Eam;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/**
 * Baba Yaga's map, dropped from her ashes. It remembers where she died; when a player reads it, her grave is raised in
 * the swamp near that spot ({@link YagaGrave}) and the map turns into a filled map marking it with a red cross. A map
 * that remembers nothing (from the creative tab) raises the grave near the player.
 */
public class YagaGraveMapItem extends Item {
	/** Where she died. */
	public static final DataComponentType<GlobalPos> DEATH_SPOT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			Eam.id("yaga_death_spot"), DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC)
					.networkSynchronized(GlobalPos.STREAM_CODEC).build());
	/** A map of 256 blocks a side: the grave is never more than a few dozen blocks from where she died. */
	private static final byte ZOOM = 1;

	public YagaGraveMapItem(Properties properties) {
		super(properties);
	}

	/** Her map, remembering where she died. */
	public static ItemStack forDeathAt(ItemStack map, GlobalPos spot) {
		map.set(DEATH_SPOT, spot);
		return map;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}
		ItemStack stack = player.getItemInHand(hand);
		GlobalPos spot = stack.get(DEATH_SPOT);
		if (spot != null && spot.dimension() != level.dimension()) {
			player.sendOverlayMessage(Component.translatable("item.eam.yaga_grave_map.wrong_world").withStyle(ChatFormatting.GRAY));
			return InteractionResult.FAIL;
		}
		BlockPos death = spot != null ? spot.pos() : player.blockPosition();
		BlockPos grave = YagaGrave.findSite(serverLevel, death, serverLevel.getRandom());
		YagaGrave.build(serverLevel, grave, Direction.Plane.HORIZONTAL.getRandomDirection(serverLevel.getRandom()), serverLevel.getRandom());

		ItemStack map = MapItem.create(serverLevel, grave.getX(), grave.getZ(), ZOOM, true, true);
		MapItem.renderBiomePreviewMap(serverLevel, map);
		MapItemSavedData.addTargetDecoration(map, grave, "+", MapDecorationTypes.RED_X);
		map.set(DataComponents.ITEM_NAME, Component.translatable(getDescriptionId()));
		serverLevel.playSound(null, player, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, player.getSoundSource(), 1.0F, 0.8F);
		player.awardStat(Stats.ITEM_USED.get(this));
		stack.consume(1, player);
		if (stack.isEmpty()) {
			return InteractionResult.SUCCESS.heldItemTransformedTo(map);
		}
		if (!player.getInventory().add(map)) {
			player.drop(map, false, Prediction.SERVER_ONLY);
		}
		return InteractionResult.SUCCESS;
	}
}
