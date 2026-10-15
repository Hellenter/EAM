package io.github.hellenter.eam.client.mobs.yaga;

import io.github.hellenter.eam.mobs.entity.yaga.YagaStormPayload;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * Baba Yaga's storm as this player sees it: rain and thunder while the server says they stand within its reach, fading
 * in and out as vanilla rain does. The world's own weather is untouched; the storm only raises what the level reports.
 */
public final class YagaStormClient {
	/** Change of the storm's strength per tick: about five seconds to come or go, like vanilla rain. */
	private static final float FADE = 0.01F;

	private static boolean inStorm;
	private static float strength;
	private static float previous;

	private YagaStormClient() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(YagaStormPayload.TYPE, (payload, context) -> inStorm = payload.inStorm());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			inStorm = false;
			strength = 0.0F;
			previous = 0.0F;
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			previous = strength;
			strength = Mth.clamp(strength + (inStorm ? FADE : -FADE), 0.0F, 1.0F);
		});
	}

	/** The rain or thunder level {@code level} reports: the world's own, or the storm's where that is stronger. */
	public static float weather(Level level, float own, float partialTick) {
		if (!(level instanceof ClientLevel) || !level.canHaveWeather()) {
			return own;
		}
		return Math.max(own, Mth.lerp(partialTick, previous, strength));
	}
}
