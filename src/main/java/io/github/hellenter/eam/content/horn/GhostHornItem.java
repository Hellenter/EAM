package io.github.hellenter.eam.content.horn;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.ghosthorse.GhostHorseModule;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * The enchanted horn: a ram's horn bound with Baba Yaga's staff skull, the leshy's antlers, an eye of ender and mithril.
 * Blown, it calls the ghost horse out, or back in with its health and armor. When the horse is killed the horn is left
 * empty ({@code empty_ghost_horn}) until two mithril ingots enchant it again.
 */
public class GhostHornItem extends Item {
	/** The horse the horn holds, or which horse is out. */
	public static final DataComponentType<GhostHorn> HORSE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Eam.id("ghost_horse"),
			DataComponentType.<GhostHorn>builder().persistent(GhostHorn.CODEC).networkSynchronized(GhostHorn.STREAM_CODEC).build());

	private static final int COOLDOWN = 20;

	public GhostHornItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack horn = player.getItemInHand(hand);
		if (level instanceof ServerLevel serverLevel) {
			GhostHorseModule.blow(serverLevel, player, horn);
		}
		player.getCooldowns().addCooldown(horn, COOLDOWN);
		return InteractionResult.SUCCESS;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(ItemStack horn, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		GhostHorn held = horn.getOrDefault(HORSE, GhostHorn.FRESH);
		builder.accept(Component.translatable(held.isOut() ? "item.eam.ghost_horn.out" : "item.eam.ghost_horn.inside")
				.withStyle(ChatFormatting.DARK_AQUA));
		if (!held.armor().isEmpty()) {
			builder.accept(Component.translatable("item.eam.ghost_horn.armor", held.armor().getHoverName()).withStyle(ChatFormatting.GRAY));
		}
		builder.accept(Component.translatable("item.eam.ghost_horn.desc").withStyle(ChatFormatting.GRAY));
	}
}
