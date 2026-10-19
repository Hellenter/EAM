package io.github.hellenter.eam.mobs.vanilla.villager;

import net.minecraft.world.item.trading.Merchant;

import org.jspecify.annotations.Nullable;

/** Added to the vanilla trade menu: the villager gear panel beside the trades, once the server turned it on. */
public interface TradePanelMenu {
	@Nullable TradePanel eam$panel();

	void eam$setPanel(TradePanel panel);

	Merchant eam$trader();
}
