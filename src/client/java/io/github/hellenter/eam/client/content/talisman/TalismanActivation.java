package io.github.hellenter.eam.client.content.talisman;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.talisman.TalismanActivationPayload;
import io.github.hellenter.eam.content.talisman.TalismanLitPayload;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.ItemActivation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import org.jspecify.annotations.Nullable;

/**
 * The talisman flies up across the screen like a totem of undying. Its crystal stays dark on the way out and glows
 * purple from the very frame the portal it lights appears.
 */
public final class TalismanActivation {
	/** The item definition with the glowing crystal. */
	public static final Identifier GLOWING = Eam.id("black_talisman_glowing");
	/** If the portal blocks never show up here (say, the frame is out of view distance), glow anyway after this. */
	private static final int LIT_FALLBACK_TICKS = 5;

	/** A portal the server says is lit, waiting for its blocks to arrive on this client. */
	private static @Nullable BlockPos pendingPortal;
	private static long pendingDeadline;

	private TalismanActivation() {
	}

	/** What the first-person hand shows: nothing while its talisman is out lighting a portal. */
	public static ItemStack inHand(LocalPlayer player, ItemStack held) {
		ItemActivation activation = player.itemActivation();
		if (held.is(EamItems.BLACK_TALISMAN) && activation.isActive() && activation.item().is(EamItems.BLACK_TALISMAN)) {
			return ItemStack.EMPTY;
		}
		return held;
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(TalismanActivationPayload.TYPE, (payload, context) -> {
			pendingPortal = null;
			context.player().displayItemActivation(new ItemStack(EamItems.BLACK_TALISMAN));
		});
		ClientPlayNetworking.registerGlobalReceiver(TalismanLitPayload.TYPE, (payload, context) -> {
			pendingPortal = payload.portal();
			pendingDeadline = context.player().level().getGameTime() + LIT_FALLBACK_TICKS;
			lightWhenPortalAppears(context.client());
		});
		// Checked every frame, not every tick, so the crystal lights in the same frame as the portal blocks.
		HudElementRegistry.addLast(Eam.id("talisman_glow"), TalismanActivation::onFrame);
		// The HUD is skipped with F1 hidden: the tick check keeps the glow coming then.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (pendingPortal != null) {
				lightWhenPortalAppears(client);
			}
		});
	}

	private static void onFrame(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		if (pendingPortal != null) {
			lightWhenPortalAppears(Minecraft.getInstance());
		}
	}

	private static void lightWhenPortalAppears(Minecraft client) {
		LocalPlayer player = client.player;
		if (pendingPortal == null || player == null) {
			pendingPortal = null;
			return;
		}
		boolean portalShown = player.level().getBlockState(pendingPortal).is(Blocks.NETHER_PORTAL);
		if (!portalShown && player.level().getGameTime() < pendingDeadline) {
			return;
		}
		pendingPortal = null;
		ItemActivation activation = player.itemActivation();
		if (activation.isActive() && activation.item().is(EamItems.BLACK_TALISMAN)) {
			activation.item().set(DataComponents.ITEM_MODEL, GLOWING);
		}
	}
}
