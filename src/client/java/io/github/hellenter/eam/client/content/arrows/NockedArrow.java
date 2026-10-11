package io.github.hellenter.eam.client.content.arrows;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import io.github.hellenter.eam.Eam;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

/**
 * Item model property {@code eam:nocked_arrow}: the arrow item the holder of a bow would shoot next, so the drawn bow
 * shows a copper, silver or mithril head.
 */
public record NockedArrow() implements SelectItemModelProperty<Item> {
	public static final Codec<Item> VALUE_CODEC = BuiltInRegistries.ITEM.byNameCodec();
	public static final SelectItemModelProperty.Type<NockedArrow, Item> TYPE =
			SelectItemModelProperty.Type.create(MapCodec.unit(new NockedArrow()), VALUE_CODEC);

	/** Called by the client and by datagen, which writes models that use the property. */
	public static void register() {
		SelectItemModelProperties.ID_MAPPER.put(Eam.id("nocked_arrow"), TYPE);
	}

	@Override
	public @Nullable Item get(ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed,
			ItemDisplayContext displayContext) {
		if (owner == null) {
			return null;
		}
		ItemStack projectile = owner.getProjectile(itemStack);
		return projectile.isEmpty() ? null : projectile.getItem();
	}

	@Override
	public SelectItemModelProperty.Type<NockedArrow, Item> type() {
		return TYPE;
	}

	@Override
	public Codec<Item> valueCodec() {
		return VALUE_CODEC;
	}
}
