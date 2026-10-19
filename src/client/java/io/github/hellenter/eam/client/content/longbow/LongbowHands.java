package io.github.hellenter.eam.client.content.longbow;

import io.github.hellenter.eam.content.longbow.LongbowItem;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Where vanilla asks whether a player holds or draws a bow, a longbow counts as one: the free hand drops, the view narrows. */
public final class LongbowHands {
	private LongbowHands() {
	}

	/** Whether {@code stack} is a longbow where vanilla checks it for {@code checked}, the bow. */
	public static boolean countsAsBow(ItemStack stack, Object checked) {
		return checked == Items.BOW && stack.getItem() instanceof LongbowItem;
	}
}
