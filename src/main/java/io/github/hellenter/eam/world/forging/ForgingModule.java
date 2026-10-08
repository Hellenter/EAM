package io.github.hellenter.eam.world.forging;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamItems;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

/**
 * Iron plates for iron armor. An iron ingot smelts into hot iron, the anvil forges hot iron into plates, and hot iron
 * left alone turns back into an ingot a minute after it leaves the furnace. The recipes are datagen files that load
 * only while the {@code iron_plates} flag is on ({@link #ENABLED}); with it off the vanilla ingot armor recipes return.
 */
public final class ForgingModule {
	/** Game time at which a hot iron stack turns back into iron ingots; set when it leaves the furnace. */
	public static final DataComponentType<Long> COOLS_AT = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Eam.id("cools_at"),
			DataComponentType.<Long>builder().persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG).build());

	/** True while the {@code iron_plates} flag is on; the plate recipes load under it, the ingot ones under {@code not}. */
	public static final ResourceCondition ENABLED = new EnabledCondition();

	private static final ResourceConditionType<EnabledCondition> ENABLED_TYPE =
			ResourceConditionType.create(Eam.id("iron_plates_enabled"), MapCodec.unit(EnabledCondition::new));

	private static boolean enabled;

	private ForgingModule() {
	}

	/** The component and condition types must exist even with the module off, or saved items and recipe files fail to load. */
	public static void register() {
		ResourceConditions.register(ENABLED_TYPE);
	}

	public static void init() {
		enabled = true;
	}

	/** Game time at which {@code stack} cools; hot iron still in the furnace output has not started cooling. */
	public static long coolsAt(ItemStack stack) {
		return stack.getOrDefault(COOLS_AT, ForgingLogic.NOT_STARTED);
	}

	/** What the anvil forges from {@code input} and {@code addition}: iron plates, or empty when it is not forging. */
	public static ItemStack anvilResult(ItemStack input, ItemStack addition, long now) {
		if (!enabled || !input.is(EamItems.HOT_IRON_INGOT)) {
			return ItemStack.EMPTY;
		}
		int plates = ForgingLogic.plates(input.getCount(), addition.isEmpty(), ForgingLogic.isCooled(coolsAt(input), now));
		return plates > 0 ? new ItemStack(EamItems.IRON_PLATE, plates) : ItemStack.EMPTY;
	}

	private record EnabledCondition() implements ResourceCondition {
		@Override
		public ResourceConditionType<?> getType() {
			return ENABLED_TYPE;
		}

		@Override
		public boolean test(RegistryOps.RegistryInfoLookup registryInfo) {
			return Eam.config().ironPlates();
		}
	}
}
