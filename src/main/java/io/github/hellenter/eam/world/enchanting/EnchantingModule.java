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

import java.util.List;

/**
 * The enchanting table also lets a player pick exact enchantments and levels, several at once, paying diamonds
 * (offensive enchantments) or emeralds (supportive ones) from the inventory plus as many experience levels. The vanilla random offers stay.
 * Choosing needs bookshelves around the table: a minimum, one more per enchantment level the item ends up with, and more
 * for higher levels of each enchantment. Prices and shelves are in {@code data/eam/enchanting/costs.json}, the lists in the enchantment tags {@code #eam:offensive} and
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
		List<EnchantingOptions.Pick> picks = EnchantingOptions.check(
				EnchantingOptions.forItem(stack, player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)), payload.choices());
		if (picks == null) {
			return;
		}

		EnchantingSettings current = settings;
		EnchantingLogic.Price price = EnchantingOptions.price(current, picks);
		Item offensive = current.resource(true);
		Item supportive = current.resource(false);
		Inventory inventory = player.getInventory();
		boolean infinite = player.hasInfiniteMaterials();
		if (!EnchantingLogic.affordable(price, inventory.countItem(offensive), inventory.countItem(supportive), offensive == supportive,
				player.experienceLevel, infinite)) {
			return;
		}

		accessor.eam$getAccess().execute((world, pos) -> {
			if (EnchantingOptions.requiredShelves(current, stack, picks) > EnchantingShelves.count(world, pos)) {
				return;
			}
			if (!infinite) {
				take(inventory, offensive, price.offensive());
				take(inventory, supportive, price.supportive());
			}
			// Also rolls a new enchantment seed, so the random offers change as after a vanilla enchant.
			player.onEnchantmentPerformed(stack, infinite ? 0 : price.levels());
			ItemStack result = stack.is(Items.BOOK) ? stack.transmuteCopy(Items.ENCHANTED_BOOK) : stack;
			for (EnchantingOptions.Pick pick : picks) {
				result.enchant(pick.option().enchantment(), pick.level());
			}
			slots.setItem(0, result);

			player.awardStat(Stats.ENCHANT_ITEM);
			CriteriaTriggers.ENCHANTED_ITEM.trigger(player, result, price.levels());
			accessor.eam$getEnchantmentSeed().set(player.getEnchantmentSeed());
			menu.slotsChanged(slots);
			world.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F,
					world.getRandom().nextFloat() * 0.1F + 0.9F);
		});
	}

	private static void take(Inventory inventory, Item item, int count) {
		if (count > 0) {
			inventory.clearOrCountMatchingItems(stack -> stack.is(item), false, count, new SimpleContainer(0));
		}
	}
}
