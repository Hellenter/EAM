package io.github.hellenter.eam.client.mobs.villager;

import io.github.hellenter.eam.mobs.vanilla.villager.TradePanel;
import io.github.hellenter.eam.mobs.vanilla.villager.TradePanelMenu;
import io.github.hellenter.eam.mobs.vanilla.villager.VillagerInventoryPayload;
import io.github.hellenter.eam.mobs.vanilla.villager.VillagerTradePanel;
import io.github.hellenter.eam.mobs.vanilla.villager.VillagerTradePanelPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;

import org.jspecify.annotations.Nullable;

/**
 * Draws the villager gear panel to the right of the vanilla trade screen: a window like vanilla's with his armor, his
 * figure, his hands and a button to his inventory. See {@link VillagerTradePanel}.
 */
public final class VillagerTradePanelScreen {
	private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
	private static final int OUTLINE = 0xFF000000;
	private static final int LIGHT = 0xFFFFFFFF;
	private static final int SHADOW = 0xFF555555;
	private static final int BODY = 0xFFC6C6C6;
	private static final int FIGURE_BACKGROUND = 0xFF000000;
	private static final int FIGURE_LEFT = VillagerTradePanel.ARMOR_X + VillagerTradePanel.SLOT;
	private static final int FIGURE_RIGHT = VillagerTradePanel.RIGHT - 6;

	private VillagerTradePanelScreen() {
	}

	/** Shows the panel once the server says these trades are a villager's. */
	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(VillagerTradePanelPayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof MerchantMenu menu && menu.containerId == payload.containerId()) {
				((TradePanelMenu) menu).eam$setPanel(payload.panel());
			}
		});
	}

	/** The inventory button, hidden until the panel shows. */
	public static Button button(int leftPos, int topPos) {
		Button button = Button.builder(Component.translatable("container.eam.villager.inventory"),
						pressed -> ClientPlayNetworking.send(VillagerInventoryPayload.INSTANCE))
				.bounds(leftPos + VillagerTradePanel.LEFT + 5, topPos + VillagerTradePanel.BUTTON_Y,
						VillagerTradePanel.RIGHT - VillagerTradePanel.LEFT - 10, 20)
				.build();
		button.visible = false;
		return button;
	}

	/** Draws the panel under the slots' items, and shows the button with it. */
	public static void extract(MerchantMenu menu, GuiGraphicsExtractor graphics, int leftPos, int topPos, int mouseX, int mouseY, Button button) {
		TradePanel panel = ((TradePanelMenu) menu).eam$panel();
		button.visible = panel != null;
		if (panel == null) return;
		int x0 = leftPos + VillagerTradePanel.LEFT;
		int y0 = topPos + VillagerTradePanel.TOP;
		int x1 = leftPos + VillagerTradePanel.RIGHT;
		int y1 = topPos + VillagerTradePanel.BOTTOM;
		graphics.fill(x0 + 1, y0, x1 - 1, y1, OUTLINE);
		graphics.fill(x0, y0 + 1, x1, y1 - 1, OUTLINE);
		graphics.fill(x0 + 1, y0 + 1, x1 - 2, y1 - 2, LIGHT);
		graphics.fill(x0 + 3, y0 + 3, x1 - 1, y1 - 1, SHADOW);
		graphics.fill(x0 + 3, y0 + 3, x1 - 3, y1 - 3, BODY);
		for (int i = VillagerTradePanel.FIRST_SLOT; i < VillagerTradePanel.FIRST_SLOT + VillagerTradePanel.SLOTS; i++) {
			Slot slot = menu.slots.get(i);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, leftPos + slot.x - 1, topPos + slot.y - 1,
					VillagerTradePanel.SLOT, VillagerTradePanel.SLOT);
		}
		int fx0 = leftPos + FIGURE_LEFT;
		int fy0 = topPos + VillagerTradePanel.ARMOR_Y - 1;
		int fx1 = leftPos + FIGURE_RIGHT;
		int fy1 = fy0 + 4 * VillagerTradePanel.SLOT;
		graphics.fill(fx0, fy0, fx1, fy1, FIGURE_BACKGROUND);
		LivingEntity villager = villager(panel);
		if (villager != null) {
			InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, fx0, fy0, fx1, fy1, 26, 0.0625F, mouseX, mouseY, villager);
		}
	}

	/** Whether a click at {@code mx, my} lands on the panel, so it is not a click outside the screen. */
	public static boolean inPanel(MerchantMenu menu, double mx, double my, int leftPos, int topPos) {
		return ((TradePanelMenu) menu).eam$panel() != null
				&& mx >= leftPos + VillagerTradePanel.LEFT && mx < leftPos + VillagerTradePanel.RIGHT
				&& my >= topPos + VillagerTradePanel.TOP && my < topPos + VillagerTradePanel.BOTTOM;
	}

	private static @Nullable LivingEntity villager(TradePanel panel) {
		Minecraft minecraft = Minecraft.getInstance();
		Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(panel.villagerId());
		return entity instanceof LivingEntity living ? living : null;
	}
}
