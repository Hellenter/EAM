package io.github.hellenter.eam.client.mobs.koschei;

import io.github.hellenter.eam.mobs.entity.koschei.KoscheiModule;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Under Koschei's convulsions the player's view twitches on its own; the jolts and the slowed steps come from the server. */
public final class ConvulsionsClient {
	/** Largest twitch of the view in one tick, degrees. */
	private static final float YAW_TWITCH = 9.0F;
	private static final float PITCH_TWITCH = 6.0F;

	private ConvulsionsClient() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			LocalPlayer player = client.player;
			if (player == null || client.isPaused() || !player.hasEffect(KoscheiModule.CONVULSIONS)) {
				return;
			}
			RandomSource random = player.getRandom();
			player.setYRot(player.getYRot() + (random.nextFloat() - 0.5F) * 2.0F * YAW_TWITCH);
			player.setXRot(Mth.clamp(player.getXRot() + (random.nextFloat() - 0.5F) * 2.0F * PITCH_TWITCH, -90.0F, 90.0F));
		});
	}
}
