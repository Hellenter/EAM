package io.github.hellenter.eam.client.content.talisman;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.talisman.TalismanActivationPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.player.ItemActivation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** The talisman flies up across the screen like a totem of undying, its crystal glowing purple at the heart. */
public final class TalismanActivation {
	/** The item definition with the glowing crystal. */
	public static final Identifier GLOWING = Eam.id("black_talisman_glowing");

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
			ItemStack glowing = new ItemStack(EamItems.BLACK_TALISMAN);
			glowing.set(DataComponents.ITEM_MODEL, GLOWING);
			context.player().displayItemActivation(glowing);
		});
	}
}
