package io.github.hellenter.eam.client.survival.thirst;

import io.github.hellenter.eam.survival.thirst.DrinkFromSourcePayload;
import io.github.hellenter.eam.survival.thirst.Thirst;
import io.github.hellenter.eam.survival.thirst.ThirstModule;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Holding use with an empty main hand while looking at water asks the server for a sip. The server owns the
 * cooldown and re-checks the target; this only keeps the request rate low.
 */
public final class DrinkFromSourceHandler {
	private static final int REQUEST_INTERVAL = 4;

	private static int cooldown;

	private DrinkFromSourceHandler() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(DrinkFromSourceHandler::tick);
	}

	private static void tick(Minecraft minecraft) {
		if (cooldown > 0) {
			cooldown--;
			return;
		}

		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.gui.screen() != null || !minecraft.options.keyUse.isDown()
				|| !player.getMainHandItem().isEmpty() || !ClientPlayNetworking.canSend(DrinkFromSourcePayload.TYPE)) {
			return;
		}
		Thirst thirst = player.getAttached(ThirstModule.THIRST);
		if (thirst == null || thirst.isFull()) {
			return;
		}

		HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, true);
		if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK
				&& player.level().getFluidState(blockHit.getBlockPos()).is(FluidTags.WATER)) {
			ClientPlayNetworking.send(DrinkFromSourcePayload.INSTANCE);
			cooldown = REQUEST_INTERVAL;
		}
	}
}
