package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.content.EamItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

	/**
	 * A blueprint of every building the default orders offer, for the creative tab: each house of each style and the
	 * defences. In creative mode a blueprint builds at once, without a village.
	 */
	public static List<ItemStack> creativeStacks() {
		List<ItemStack> stacks = new ArrayList<>();
		Set<Identifier> seen = new HashSet<>();
		EconomySettings.DEFAULT.orders().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->
				entry.getValue().templates().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(style ->
						style.getValue().stream().filter(seen::add).forEach(template -> {
							ItemStack stack = new ItemStack(EamItems.BLUEPRINT);
							stack.set(VillageBuildModule.BLUEPRINT, new BlueprintData(template, entry.getKey(), 0));
							stacks.add(stack);
						})));
		return stacks;
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
		BlueprintData data = stack.get(VillageBuildModule.BLUEPRINT);
		if (data != null) builder.accept(Component.literal(data.template().getPath()).withStyle(ChatFormatting.DARK_GRAY));
		builder.accept(Component.translatable("item.eam.blueprint.usage").withStyle(ChatFormatting.GRAY));
		builder.accept(Component.translatable("item.eam.blueprint.turn").withStyle(ChatFormatting.DARK_GRAY));
	}
}
