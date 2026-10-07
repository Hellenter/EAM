package io.github.hellenter.eam.survival.thirst;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.ConsumableListener;
import net.minecraft.world.level.Level;

/**
 * Item component {@code eam:hydration}: how much water drinking or eating the item restores.
 * Vanilla calls every {@link ConsumableListener} component when a consumable finishes, so no mixin is needed.
 */
public record Hydration(int water) implements ConsumableListener {
	public static final Codec<Hydration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(1, Thirst.MAX).fieldOf("water").forGetter(Hydration::water)
	).apply(instance, Hydration::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, Hydration> STREAM_CODEC =
			ByteBufCodecs.VAR_INT.map(Hydration::new, Hydration::water).cast();

	@Override
	public void onConsume(Level level, LivingEntity user, ItemStack stack, Consumable consumable) {
		if (user instanceof ServerPlayer player) {
			ThirstModule.drink(player, water);
		}
	}
}
