package io.github.hellenter.eam.world.enchanting;

import io.github.hellenter.eam.mixin.world.enchanting.EnchantmentMenuAccessor;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The enchanting table also lets a player pick an exact enchantment and level, paying diamonds (offensive enchantments)
 * or emeralds (supportive ones) from the inventory plus as many experience levels. The vanilla random offers stay.
 * Prices are in {@code data/eam/enchanting/costs.json}, the lists in the enchantment tags {@code #eam:offensive} and
 * {@code #eam:supportive}.
 */
public final class EnchantingModule {
	private static volatile EnchantingSettings settings = EnchantingSettings.DEFAULT;
	private static boolean enabled;

	private EnchantingModule() {
	}

	/** Payload types are registered whatever the config says, so client and server always agree. */
	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(EnchantChoicePayload.TYPE, EnchantChoicePayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(EnchantingSettingsPayload.TYPE, EnchantingSettingsPayload.STREAM_CODEC);
	}

	public static void init() {
		enabled = true;
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(EnchantingSettingsLoader.ID, new EnchantingSettingsLoader());
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendSettings(handler.player));
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) ->
				server.getPlayerList().getPlayers().forEach(EnchantingModule::sendSettings));
		ServerPlayNetworking.registerGlobalReceiver(EnchantChoicePayload.TYPE, (payload, context) ->
				enchant(context.player(), payload));
	}

	static void setSettings(EnchantingSettings value) {
		settings = value;
	}

	public static EnchantingSettings settings() {
		return settings;
	}

	private static void sendSettings(ServerPlayer player) {
		if (enabled && ServerPlayNetworking.canSend(player, EnchantingSettingsPayload.TYPE)) {
			ServerPlayNetworking.send(player, new EnchantingSettingsPayload(settings));
		}
	}

	private static void enchant(ServerPlayer player, EnchantChoicePayload payload) {
		if (!(player.containerMenu instanceof EnchantmentMenu menu) || !menu.stillValid(player)) {
			return;
		}
		EnchantmentMenuAccessor accessor = (EnchantmentMenuAccessor) menu;
		Container slots = accessor.eam$getEnchantSlots();
		ItemStack stack = slots.getItem(0);
		EnchantingOptions.Option option = EnchantingOptions.find(
				EnchantingOptions.forItem(stack, player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)), payload.enchantment());
		int level = payload.level();
		if (option == null || !option.allows(level)) {
			return;
		}

		EnchantingSettings current = settings;
		Item resource = current.resource(option.offensive());
		int resourceCost = option.resourceCost(current, level);
		int levelCost = option.levelCost(current, level);
		Inventory inventory = player.getInventory();
		boolean infinite = player.hasInfiniteMaterials();
		if (!EnchantingLogic.affordable(inventory.countItem(resource), resourceCost, player.experienceLevel, levelCost, infinite)) {
			return;
		}

		accessor.eam$getAccess().execute((world, pos) -> {
			if (!infinite) {
				inventory.clearOrCountMatchingItems(item -> item.is(resource), false, resourceCost, new SimpleContainer(0));
			}
			// Also rolls a new enchantment seed, so the random offers change as after a vanilla enchant.
			player.onEnchantmentPerformed(stack, infinite ? 0 : levelCost);
			ItemStack result = stack.is(Items.BOOK) ? stack.transmuteCopy(Items.ENCHANTED_BOOK) : stack;
			result.enchant(option.enchantment(), level);
			slots.setItem(0, result);

			player.awardStat(Stats.ENCHANT_ITEM);
			CriteriaTriggers.ENCHANTED_ITEM.trigger(player, result, levelCost);
			accessor.eam$getEnchantmentSeed().set(player.getEnchantmentSeed());
			menu.slotsChanged(slots);
			world.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F,
					world.getRandom().nextFloat() * 0.1F + 0.9F);
		});
	}
}
