package io.github.hellenter.eam.mobs.vanilla.villager;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
 * A blueprint from the village elder: in hand it shows a ghost of its building where the player looks. Sneak and use
 * turns it a quarter turn, use sets it down for the villagers to build. See {@link VillageBuildModule}.
 */
public class BlueprintItem extends Item {
	public BlueprintItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		BlueprintData data = stack.get(VillageBuildModule.BLUEPRINT);
		if (data == null) return InteractionResult.PASS;
		if (player.isShiftKeyDown()) {
			if (!level.isClientSide()) stack.set(VillageBuildModule.BLUEPRINT, data.turned());
		} else if (player instanceof ServerPlayer serverPlayer) {
			VillageBuildModule.place(serverPlayer, stack, data);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public Component getName(ItemStack stack) {
		BlueprintData data = stack.get(VillageBuildModule.BLUEPRINT);
		if (data == null) return super.getName(stack);
		return Component.translatable("item.eam.blueprint.of", Component.translatable("gui.eam.village.order." + data.order()));
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Component.translatable("item.eam.blueprint.usage").withStyle(ChatFormatting.GRAY));
		builder.accept(Component.translatable("item.eam.blueprint.turn").withStyle(ChatFormatting.DARK_GRAY));
	}
}
