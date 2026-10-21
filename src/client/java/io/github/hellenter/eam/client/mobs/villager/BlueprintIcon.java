package io.github.hellenter.eam.client.mobs.villager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.vanilla.villager.BlueprintData;
import io.github.hellenter.eam.mobs.vanilla.villager.VillageBuildModule;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

/**
 * Item model property {@code eam:blueprint}: what a blueprint is of, {@code <style>_<order>} for a house
 * ({@code desert_house_small}) and the order alone for a defence ({@code tower}), so each shows its own icon: the
 * paper coloured by the village style, the emblem by the building.
 */
public record BlueprintIcon() implements SelectItemModelProperty<String> {
	public static final SelectItemModelProperty.Type<BlueprintIcon, String> TYPE =
			SelectItemModelProperty.Type.create(MapCodec.unit(new BlueprintIcon()), Codec.STRING);

	/** Called by the client and by datagen, which writes the model that uses the property. */
	public static void register() {
		SelectItemModelProperties.ID_MAPPER.put(Eam.id("blueprint"), TYPE);
	}

	/** The property's value for a blueprint of {@code order} in {@code style}, {@code null} for any village. */
	public static String key(@Nullable String style, String order) {
		return style == null ? order : style + "_" + order;
	}

	@Override
	public @Nullable String get(ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed,
			ItemDisplayContext displayContext) {
		BlueprintData data = itemStack.get(VillageBuildModule.BLUEPRINT);
		return data == null ? null : key(data.style(), data.order());
	}

	@Override
	public SelectItemModelProperty.Type<BlueprintIcon, String> type() {
		return TYPE;
	}

	@Override
	public Codec<String> valueCodec() {
		return Codec.STRING;
	}
}
