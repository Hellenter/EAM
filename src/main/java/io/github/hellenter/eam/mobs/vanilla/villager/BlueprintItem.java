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
 * A blueprint from the village elder: in hand it shows a ghost of its building four blocks in front of the player, lifted as he looks up. Sneak and use
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
	 * defences. In creative mode a blueprint needs no village, only villagers near enough to build it.
	 */
	public static List<ItemStack> creativeStacks() {
		return creativeBlueprints().stream().map(data -> {
			ItemStack stack = new ItemStack(EamItems.BLUEPRINT);
			stack.set(VillageBuildModule.BLUEPRINT, data);
			return stack;
		}).toList();
	}

	/** What the blueprints of {@link #creativeStacks()} are of; datagen gives each its icon. */
	public static List<BlueprintData> creativeBlueprints() {
		List<BlueprintData> blueprints = new ArrayList<>();
		Set<Identifier> seen = new HashSet<>();
		EconomySettings.DEFAULT.orders().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->
				entry.getValue().templates().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(style ->
						style.getValue().stream().filter(seen::add).forEach(template ->
								blueprints.add(new BlueprintData(template, entry.getKey(), 0)))));
		return blueprints;
	}

	@Override
	public Component getName(ItemStack stack) {
		BlueprintData data = stack.get(VillageBuildModule.BLUEPRINT);
		if (data == null) return super.getName(stack);
		Component building = Component.translatable("gui.eam.village.order." + data.order());
		String style = data.style();
		if (style == null) return Component.translatable("item.eam.blueprint.of", building);
		return Component.translatable("item.eam.blueprint.of_style", building, data.number(), Component.translatable("item.eam.blueprint.style." + style));
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		BlueprintData data = stack.get(VillageBuildModule.BLUEPRINT);
		if (data != null) {
			builder.accept(Component.translatable("item.eam.blueprint.building", Component.translatable("gui.eam.village.order." + data.order()))
					.withStyle(ChatFormatting.GRAY));
			String style = data.style();
			builder.accept(Component.translatable("item.eam.blueprint.style",
					Component.translatable(style == null ? "item.eam.blueprint.style.any" : "item.eam.blueprint.style." + style))
					.withStyle(ChatFormatting.GRAY));
			builder.accept(Component.literal(data.template().getPath()).withStyle(ChatFormatting.DARK_GRAY));
		}
		builder.accept(Component.translatable("item.eam.blueprint.usage").withStyle(ChatFormatting.GRAY));
		builder.accept(Component.translatable("item.eam.blueprint.turn").withStyle(ChatFormatting.DARK_GRAY));
	}
}
