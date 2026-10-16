package io.github.hellenter.eam.world.farming;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import org.apache.commons.lang3.mutable.MutableFloat;

import java.util.List;

/**
 * Harvest, a hoe enchantment that works like Fortune on ripe crops: each stack of produce gets up to the enchantment's
 * {@code eam:harvest_bonus} extra items. A villager breaking a crop counts the tool in its main hand, so a farmer given an
 * enchanted hoe brings in more too. The bonus per level is in the enchantment JSON.
 */
public final class HarvestModule {
	public static final ResourceKey<Enchantment> HARVEST = ResourceKey.create(Registries.ENCHANTMENT, Eam.id("harvest"));

	/** Crops the enchantment works on, once ripe. */
	public static final TagKey<Block> CROPS = TagKey.create(Registries.BLOCK, Eam.id("harvest_crops"));
	/** The produce of those crops that gets the extra items; seeds do not. */
	public static final TagKey<Item> PRODUCE = TagKey.create(Registries.ITEM, Eam.id("harvest_produce"));

	/** Most extra items per stack of produce; vanilla's value effects (add, multiply, set) work on it. */
	public static final DataComponentType<EnchantmentValueEffect> HARVEST_BONUS = Registry.register(
			BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE, Eam.id("harvest_bonus"),
			DataComponentType.<EnchantmentValueEffect>builder().persistent(EnchantmentValueEffect.CODEC).build());

	private HarvestModule() {
	}

	/** The effect component is registered whatever the config says, so the enchantment always loads. */
	public static void register() {
	}

	public static void init() {
		LootTableEvents.MODIFY_DROPS.register(HarvestModule::modifyDrops);
	}

	private static void modifyDrops(Holder<LootTable> table, LootContext context, List<ItemStack> drops) {
		BlockState state = context.getOptional(LootContextParams.BLOCK_STATE);
		if (state == null || !state.is(CROPS) || !isRipe(state)) return;
		ItemStack tool = tool(context);
		if (tool.isEmpty()) return;
		int bonus = maxBonus(tool, context.getRandom());
		if (bonus <= 0) return;
		for (ItemStack stack : drops) {
			if (stack.is(PRODUCE)) {
				stack.grow(FarmingLogic.harvestExtra(bonus, context.getRandom()::nextInt));
			}
		}
	}

	/** The tool the crop was broken with. Villagers break crops with no tool in the context, so their main hand counts. */
	private static ItemStack tool(LootContext context) {
		if (context.getOptional(LootContextParams.TOOL) instanceof ItemStack tool && !tool.isEmpty()) return tool;
		Entity breaker = context.getOptional(LootContextParams.THIS_ENTITY);
		return breaker instanceof LivingEntity living ? living.getMainHandItem() : ItemStack.EMPTY;
	}

	private static int maxBonus(ItemStack tool, RandomSource random) {
		MutableFloat value = new MutableFloat(0.0F);
		EnchantmentHelper.runIterationOnItem(tool, (enchantment, level) ->
				enchantment.value().modifyUnfilteredValue(HARVEST_BONUS, random, level, value));
		return Math.max(0, Math.round(value.floatValue()));
	}

	/** A crop with an age is ripe at its last age; one without (a melon) always is. */
	private static boolean isRipe(BlockState state) {
		for (Property<?> property : state.getProperties()) {
			if (property instanceof IntegerProperty age && age.getName().equals("age")) {
				int max = age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0);
				return state.getValue(age) == max;
			}
		}
		return true;
	}
}
