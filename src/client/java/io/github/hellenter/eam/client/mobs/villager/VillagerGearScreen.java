package io.github.hellenter.eam.client.mobs.villager;

import io.github.hellenter.eam.mobs.vanilla.villager.VillageOpenPayload;
import io.github.hellenter.eam.mobs.vanilla.villager.VillagerGearMenu;
import io.github.hellenter.eam.mobs.vanilla.villager.VillagerGuardModule;
import io.github.hellenter.eam.mobs.vanilla.villager.VillagerGuardPayload;
import io.github.hellenter.eam.mobs.vanilla.villager.VillagerTradePayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import org.jspecify.annotations.Nullable;

/**
 * A villager's inventory on the vanilla chest background, with the villager drawn beside his armor like the player in
 * his own inventory, a button to his trades and one that makes him a guard (or a guard an ordinary villager again). The
 * elder also has a button to the village menu, {@link VillageScreen}.
 */
public class VillagerGearScreen extends AbstractContainerScreen<VillagerGearMenu> {
	private static final Identifier CONTAINER_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
	private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
	private static final int BODY = 0xFFC6C6C6;
	private static final int FIGURE_BACKGROUND = 0xFF000000;
	private static final int FIGURE_LEFT = 26;
	private static final int FIGURE_RIGHT = 78;

	public VillagerGearScreen(VillagerGearMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, VillagerGearMenu.TOP_HEIGHT + 96);
		inventoryLabelY = imageHeight - 94;
	}

	@Override
	protected void init() {
		super.init();
		Villager villager = villager();
		Button trade = Button.builder(Component.translatable("container.eam.villager.trade"),
						button -> ClientPlayNetworking.send(VillagerTradePayload.INSTANCE))
				.bounds(leftPos + VillagerGearMenu.INVENTORY_X - 1, topPos + VillagerGearMenu.TOP + 2 * VillagerGearMenu.SLOT + 1,
						4 * VillagerGearMenu.SLOT, 20)
				.build();
		trade.active = villager != null && trades(villager);
		addRenderableWidget(trade);
		if (villager != null && !villager.isBaby()) {
			boolean guard = !VillagerGuardModule.isGuardProfession(villager);
			Component label = Component.translatable(guard ? "container.eam.villager.make_guard" : "container.eam.villager.dismiss_guard");
			addRenderableWidget(Button.builder(label, button -> {
						ClientPlayNetworking.send(new VillagerGuardPayload(guard));
						button.visible = false;
					})
					.bounds(leftPos + imageWidth + 2, topPos + VillagerGearMenu.TOP, font.width(label) + 12, 20)
					.build());
		}
		if (villager != null && !villager.isBaby() && villager.getVillagerData().profession().is(VillagerProfession.CARTOGRAPHER)) {
			Component label = Component.translatable("container.eam.villager.village");
			addRenderableWidget(Button.builder(label, button -> ClientPlayNetworking.send(VillageOpenPayload.INSTANCE))
					.bounds(leftPos + imageWidth + 2, topPos + VillagerGearMenu.TOP + 22, font.width(label) + 12, 20)
					.build());
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int top = VillagerGearMenu.TOP_HEIGHT;
		graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, leftPos, topPos, 0.0F, 0.0F, imageWidth, top, 256, 256);
		graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, leftPos, topPos + top, 0.0F, 126.0F, imageWidth, 96, 256, 256);
		graphics.fill(leftPos + 7, topPos + 17, leftPos + 169, topPos + top, BODY);
		for (int i = 0; i < 15; i++) {
			Slot slot = menu.slots.get(i);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, leftPos + slot.x - 1, topPos + slot.y - 1,
					VillagerGearMenu.SLOT, VillagerGearMenu.SLOT);
		}
		int x0 = leftPos + FIGURE_LEFT;
		int y0 = topPos + VillagerGearMenu.TOP - 1;
		int x1 = leftPos + FIGURE_RIGHT;
		int y1 = topPos + top;
		graphics.fill(x0, y0, x1, y1, FIGURE_BACKGROUND);
		Villager villager = villager();
		if (villager != null) {
			InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, x0, y0, x1, y1, 26, 0.0625F, mouseX, mouseY, villager);
		}
	}

	private @Nullable Villager villager() {
		Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(menu.data().villagerId());
		return entity instanceof Villager villager ? villager : null;
	}

	/** Nitwits, guards and villagers without a job have nothing to trade. */
	private static boolean trades(Villager villager) {
		var profession = villager.getVillagerData().profession();
		return !profession.is(VillagerProfession.NONE) && !profession.is(VillagerProfession.NITWIT)
				&& !profession.is(VillagerGuardModule.GUARD);
	}
}
