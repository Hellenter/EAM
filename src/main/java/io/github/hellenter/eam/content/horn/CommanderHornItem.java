package io.github.hellenter.eam.content.horn;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.lost.LostCommander;
import io.github.hellenter.eam.mobs.entity.lost.LostWave;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.function.Consumer;

/**
 * The commander's horn of ancient metal, an admin item with no way to get it in survival. Whoever carries it commands
 * the Lost ({@link LostCommander}). Use blows it and calls the chosen wave of the Lost to him; sneak + use picks the
 * next wave: a squad, a pair of heavy cavalrymen, or a centurion with his squad.
 */
public class CommanderHornItem extends Item {
	/** The wave the horn calls. */
	public static final DataComponentType<LostWave> WAVE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Eam.id("commander_horn_wave"),
			DataComponentType.<LostWave>builder().persistent(LostWave.CODEC).networkSynchronized(LostWave.STREAM_CODEC).build());

	public CommanderHornItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack horn = player.getItemInHand(hand);
		LostWave wave = horn.getOrDefault(WAVE, LostWave.SQUAD);
		if (player.isSecondaryUseActive()) {
			LostWave next = wave.next();
			horn.set(WAVE, next);
			if (!level.isClientSide()) {
				player.sendOverlayMessage(Component.translatable("item.eam.commander_horn.selected", Component.translatable(next.translationKey()))
						.withStyle(ChatFormatting.GOLD));
			}
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.4F, 1.4F);
			return InteractionResult.SUCCESS;
		}
		if (level instanceof ServerLevel serverLevel) {
			// A goat horn's call, deeper, like the centurion's.
			serverLevel.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(5),
					SoundSource.PLAYERS, 16.0F, 0.75F);
			serverLevel.gameEvent(GameEvent.INSTRUMENT_PLAY, player.position(), GameEvent.Context.of(player));
			LostCommander.call(serverLevel, player, wave);
			player.sendOverlayMessage(Component.translatable("item.eam.commander_horn.called", Component.translatable(wave.translationKey()))
					.withStyle(ChatFormatting.GOLD));
		}
		player.getCooldowns().addCooldown(horn, LostCommander.cooldown());
		return InteractionResult.SUCCESS;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(ItemStack horn, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		LostWave wave = horn.getOrDefault(WAVE, LostWave.SQUAD);
		builder.accept(Component.translatable("item.eam.commander_horn.wave", Component.translatable(wave.translationKey()))
				.withStyle(ChatFormatting.GOLD));
		builder.accept(Component.translatable("item.eam.commander_horn.desc").withStyle(ChatFormatting.GRAY));
		builder.accept(Component.translatable("item.eam.commander_horn.usage").withStyle(ChatFormatting.DARK_GRAY));
	}
}
