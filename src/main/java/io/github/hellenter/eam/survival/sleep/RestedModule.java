package io.github.hellenter.eam.survival.sleep;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * «Rested»: a player who sleeps through the night in a safe room wakes up mining faster, getting hungry and thirsty
 * slower and healing faster. Numbers are in the {@code rested} object of {@code data/eam/sleep/safe_room.json}.
 */
public final class RestedModule {
	public static final Holder<MobEffect> RESTED = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT, Eam.id("rested"), new RestedEffect());

	private static final Identifier MINING_MODIFIER = Eam.id("rested");

	/** Players lying in a bed that passed the safe room check, and whether they slept long enough last tick. */
	private static final Map<UUID, Boolean> SLEEPERS = new HashMap<>();

	private static boolean enabled;

	private RestedModule() {
	}

	/** Forces class loading so the effect is registered whether or not the module is on. */
	static void registerTypes() {
	}

	static void init() {
		enabled = true;
		ServerTickEvents.END_SERVER_TICK.register(RestedModule::tick);
	}

	/** The bed at which {@code player} is about to sleep stands in a safe room. */
	static void sleepsSafely(ServerPlayer player) {
		if (enabled) {
			SLEEPERS.put(player.getUUID(), false);
		}
	}

	/**
	 * Runs after the level woke its sleepers and updated the sky, so a player who slept long enough and got up in
	 * daylight slept through the night.
	 */
	private static void tick(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Boolean>> it = SLEEPERS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Boolean> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				it.remove();
			} else if (player.isSleeping()) {
				entry.setValue(player.isSleepingLongEnough());
			} else {
				if (entry.getValue() && player.level().isBrightOutside()) {
					grant(player);
				}
				it.remove();
			}
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			updateMining(player);
		}
	}

	private static void grant(ServerPlayer player) {
		RestedSettings rested = SafeRoomModule.settings().rested();
		if (rested.durationTicks() > 0) {
			player.addEffect(new MobEffectInstance(RESTED, rested.durationTicks(), 0, false, false, true));
			player.sendOverlayMessage(Component.translatable("eam.sleep.rested").withStyle(ChatFormatting.GOLD));
		}
	}

	/** Keeps the mining bonus on exactly while the effect is. */
	private static void updateMining(ServerPlayer player) {
		AttributeInstance instance = player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
		if (instance == null) {
			return;
		}
		double amount = player.hasEffect(RESTED) ? SafeRoomModule.settings().rested().miningBonus() : 0.0;
		AttributeModifier current = instance.getModifier(MINING_MODIFIER);
		if (amount == 0.0) {
			if (current != null) {
				instance.removeModifier(MINING_MODIFIER);
			}
		} else if (current == null || current.amount() != amount) {
			instance.addOrUpdateTransientModifier(
					new AttributeModifier(MINING_MODIFIER, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}

	/** Food and water drain at this fraction of the normal rate. */
	public static float drainFactor(Player player) {
		return player.hasEffect(RESTED) ? SafeRoomModule.settings().rested().drainFactor() : 1.0F;
	}

	/** Natural regeneration runs this many times faster. */
	public static float regenFactor(Player player) {
		return player.hasEffect(RESTED) ? SafeRoomModule.settings().rested().regenFactor() : 1.0F;
	}
}
