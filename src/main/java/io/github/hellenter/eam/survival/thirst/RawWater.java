package io.github.hellenter.eam.survival.thirst;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import io.netty.buffer.ByteBuf;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.ConsumableListener;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Item component {@code eam:raw_water}: the item holds unboiled water, so drinking it may cause food poisoning
 * like a sip straight from a water source. Every potion carries it, but only a water bottle that was not boiled is
 * raw: brewed potions and boiled water ({@link #BOILED_NAME}) are safe.
 */
public record RawWater() implements ConsumableListener, TooltipProvider {
	public static final RawWater INSTANCE = new RawWater();

	public static final Codec<RawWater> CODEC = MapCodec.unitCodec(INSTANCE);
	public static final StreamCodec<ByteBuf, RawWater> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	/**
	 * Potion {@code custom_name} of a boiled water bottle. It names the bottle
	 * ({@code item.minecraft.potion.effect.eam.boiled_water}) and tells it from raw water, while the potion stays
	 * water for brewing, cauldrons and mud.
	 */
	public static final String BOILED_NAME = "eam.boiled_water";
	/** What a furnace makes of a water bottle. */
	public static PotionContents boiledWater() {
		return new PotionContents(Optional.of(Potions.WATER), Optional.empty(), List.of(), Optional.of(BOILED_NAME));
	}

	/** Whether the item's water is unboiled: anything but a potion that carries this component, or a raw water bottle. */
	public static boolean isRaw(DataComponentGetter components) {
		PotionContents potion = components.get(DataComponents.POTION_CONTENTS);
		return potion == null || potion.is(Potions.WATER) && !potion.customName().equals(Optional.of(BOILED_NAME));
	}

	/**
	 * A brewing stand boils everything in its bottles when it brews: raw water bottles left unchanged by the recipe
	 * come out boiled. Brewed potions are never raw anyway.
	 */
	public static void boilAfterBrewing(ItemStack bottle) {
		if (bottle.has(DataComponents.POTION_CONTENTS) && isRaw(bottle)) {
			bottle.set(DataComponents.POTION_CONTENTS, boiledWater());
		}
	}

	@Override
	public void onConsume(Level level, LivingEntity user, ItemStack stack, Consumable consumable) {
		if (user instanceof ServerPlayer player && isRaw(stack)) {
			ThirstModule.drinkRawWater(player);
		}
	}

	@Override
	public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
		if (isRaw(components)) {
			consumer.accept(Component.translatable("item.eam.raw_water").withStyle(ChatFormatting.GRAY));
		}
	}
}
