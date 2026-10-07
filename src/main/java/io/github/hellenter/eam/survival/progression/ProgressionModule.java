package io.github.hellenter.eam.survival.progression;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * MITE growth with XP level: health, food and water bars start at three icons and grow every few levels. Also
 * the grace period after the first spawn and after every death, when hunger and thirst do not drain.
 * Hunger and thirst read {@link #capacity} and {@link #inGracePeriod}; this module itself sets max health.
 */
public final class ProgressionModule {
	private static final Identifier MAX_HEALTH_MODIFIER = Eam.id("level_max_health");

	private static boolean enabled;
	private static volatile ProgressionRules rules = ProgressionRules.DEFAULT;

	private ProgressionModule() {
	}

	public static void init() {
		enabled = true;
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(ProgressionRulesLoader.ID, new ProgressionRulesLoader());
		ServerTickEvents.END_SERVER_TICK.register(ProgressionModule::tick);
	}

	static void setRules(ProgressionRules newRules) {
		rules = newRules;
	}

	/** Size of the player's health, food and water bars; vanilla 20 while the module is off. */
	public static int capacity(ServerPlayer player) {
		return enabled ? ProgressionLogic.capacity(player.experienceLevel, rules) : ProgressionRules.VANILLA_CAPACITY;
	}

	/** Whether hunger and thirst are paused for the player right after spawning. */
	public static boolean inGracePeriod(ServerPlayer player) {
		return enabled && ProgressionLogic.inGracePeriod(player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH)), rules);
	}

	private static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			updateMaxHealth(player);
		}
	}

	/** A transient modifier, recomputed every tick, so it never ends up in save files. */
	private static void updateMaxHealth(ServerPlayer player) {
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth == null) {
			return;
		}
		double amount = capacity(player) - ProgressionRules.VANILLA_CAPACITY;
		AttributeModifier current = maxHealth.getModifier(MAX_HEALTH_MODIFIER);
		if (current == null || current.amount() != amount) {
			maxHealth.addOrUpdateTransientModifier(new AttributeModifier(MAX_HEALTH_MODIFIER, amount, AttributeModifier.Operation.ADD_VALUE));
		}
		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
	}
}
